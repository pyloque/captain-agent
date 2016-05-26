package captain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.exceptions.JedisConnectionException;
import spark.Spark;

public class Bootstrap {

	private final static Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

	public static void main(String[] args) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.initialize(args);
		bootstrap.watch().start();
	}

	private Config config;

	private RedisStore redis;
	private DiscoveryService discovery;
	private ExpiringWatcher watcher;
	private final static GsonTransformer jsonify = new GsonTransformer();
	private final static FreeMarkerEngine engine = new FreeMarkerEngine();
	private final static String jsonType = "application/json";

	public void initialize(String[] args) {
		Config config = new Config();
		if (args.length > 0) {
			config.inifile(args[0]);
		}
		try {
			config.load();
		} catch (IOException e) {
			LOG.error("load config file error", e);
			System.exit(-1);
		}
		this.initWithConfig(config);
	}

	public void initWithConfig(Config config) {
		this.config = config;
		this.redis = new RedisStore(config.redisUri());
		this.discovery = new DiscoveryService(this.redis);
		this.watcher = new ExpiringWatcher(this.discovery);
		this.watcher.interval(config.interval()).setDaemon(true);
	}

	public void switchRedis() {
		RedisStore oldRedis = this.redis;
		this.redis = new RedisStore(config.redisUri());
		this.discovery = new DiscoveryService(this.redis);
		oldRedis.close();
	}

	public RedisStore redis() {
		return this.redis;
	}
	
	public Config config() {
		return config;
	}

	public Bootstrap watch() {
		if (config.readonly()) {
			this.watcher.start();
		}
		return this;
	}

	public void start() {
		Spark.ipAddress(config.bindHost());
		Spark.port(config.bindPort());
		Spark.staticFileLocation("/static");

		if (!config.readonly()) {
			Spark.get("/api/service/keep", jsonType, (req, res) -> {
				String name = req.queryParams("name");
				String host = req.queryParams("host");
				String port = req.queryParams("port");
				String ttl = req.queryParams("ttl");
				Map<String, Object> result = new HashMap<String, Object>();
				result.put("ok", false);
				if (Helpers.isEmpty(name) || Helpers.isEmpty(host) || Helpers.isEmpty(port) || !Helpers.isInteger(port)
						|| !Helpers.isInteger(ttl)) {
					res.status(400);
					result.put("reason", "params illegal");
				} else {
					this.discovery
							.keepService(new ServiceItem(name, host, Integer.parseInt(port), Integer.parseInt(ttl)));
					result.put("ok", true);
				}
				return result;
			}, jsonify);

			Spark.get("/api/service/cancel", jsonType, (req, res) -> {
				String name = req.queryParams("name");
				String host = req.queryParams("host");
				String port = req.queryParams("port");
				Map<String, Object> result = new HashMap<String, Object>();
				if (Helpers.isEmpty(name) || Helpers.isEmpty(host) || Helpers.isEmpty(port)
						|| !Helpers.isInteger(port)) {
					res.status(400);
					result.put("reason", "params illegal");
				} else {
					this.discovery.cancelService(new ServiceItem(name, host, Integer.parseInt(port)));
					result.put("ok", true);
				}
				return result;
			}, jsonify);
		}

		Spark.get("/api/service/version", jsonType, (req, res) -> {
			String[] names = req.queryMap("name").values();
			Map<String, Object> result = new HashMap<String, Object>();
			Map<String, Long> versions = this.discovery.getMultiServiceVersions(names);
			result.put("versions", versions);
			result.put("ok", true);
			return result;
		}, jsonify);

		Spark.get("/api/service/dirty", jsonType, (req, res) -> {
			String version = req.queryParams("version");
			Map<String, Object> result = new HashMap<String, Object>();
			if (Helpers.isEmpty(version) || !Helpers.isLong(version)) {
				res.status(400);
				result.put("reason", "params illegal");
			} else {
				long gversion = this.discovery.getServiceGlobalVersion();
				result.put("dirty", Long.parseLong(version) != gversion);
				result.put("version", gversion);
				result.put("ok", true);
			}
			return result;
		}, jsonify);

		Spark.get("/api/service/set", jsonType, (req, res) -> {
			String name = req.queryParams("name");
			Map<String, Object> result = new HashMap<String, Object>();
			if (Helpers.isEmpty(name)) {
				res.status(400);
				result.put("reason", "params illegal");
			} else {
				ServiceSet set = this.discovery.getServiceSet(name);
				result.put("ok", true);
				result.put("version", set.version());
				result.put("services", set.items());
			}
			return result;
		}, jsonify);

		Spark.get("/service/", (req, res) -> {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("config", config);
			try {
				Set<String> nameset = this.discovery.getServiceNames();
				String[] names = new String[nameset.size()];
				nameset.toArray(names);
				Map<String, Integer> services = this.discovery.getMultiServiceLens(names);
				long version = this.discovery.getServiceGlobalVersion();
				context.put("services", services);
				context.put("version", version);
			} catch (JedisConnectionException e) {
				context.put("reason", e.toString());
				context.put("stacktraces", e.getStackTrace());
			}
			return Spark.modelAndView(context, "service_all.ftl");
		}, engine);

		Spark.get("/service/set/", (req, res) -> {
			String name = req.queryParams("name");
			ServiceSet set = this.discovery.getServiceSet(name);
			long version = set.version();
			Set<ServiceItem> services = set.items();
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("version", version);
			context.put("services", services);
			context.put("name", name);
			context.put("config", config);
			return Spark.modelAndView(context, "service_detail.ftl");
		}, engine);

		Spark.get("/service/config/", (req, res) -> {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("config", config);
			return Spark.modelAndView(context, "config_edit.ftl");
		}, engine);

		Spark.post("/service/config/", (req, res) -> {
			String redisHost = req.queryParams("redisHost");
			int redisPort = Integer.parseInt(req.queryParams("redisPort"));
			int redisDb = Integer.parseInt(req.queryParams("redisDb"));
			config.redisHost(redisHost).redisPort(redisPort).redisDb(redisDb);
			try {
				config.save();
			} catch (IOException e) {
				LOG.error("save config error", e);
			}
			switchRedis();
			res.redirect("/service/");
			return null;
		});

	}

	public void halt() {
		if (this.watcher.isAlive()) {
			this.watcher.interrupt();
			this.watcher.quit();
		}
		this.redis.close();
		Spark.stop();
	}
}
