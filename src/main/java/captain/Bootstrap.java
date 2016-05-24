package captain;

import spark.Spark;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Bootstrap {

	public static void main(String[] args) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.initialize(args);
		bootstrap.start();
	}

	private int port = 6789;
	private String redisHost = "localhost";
	private int redisPort = 6379;

	private RedisStore redis;
	private DiscoveryService discovery;
	private final static GsonTransformer jsonify = new GsonTransformer();
	private final static FreeMarkerEngine engine = new FreeMarkerEngine();
	private final static String jsonType = "application/json";

	public Bootstrap() {
		this(6789);
	}

	public Bootstrap(int port) {
		this(port, "localhost", 6379);
	}

	public Bootstrap(int port, String redisHost, int redisPort) {
		this.port = port;
		this.redisHost = redisHost;
		this.redisPort = redisPort;
	}

	public void initialize(String[] args) {
		if (args.length > 0) {
			this.port = Integer.parseInt(args[0]);
		}
		if (args.length > 1) {
			String[] pair = args[1].split(":");
			this.redisHost = pair[0];
			this.redisPort = Integer.parseInt(pair[1]);
		}
		this.redis = new RedisStore(this.redisHost, this.redisPort);
		this.discovery = new DiscoveryService(this.redis);
	}

	public Bootstrap redisStore(RedisStore redis) {
		this.redis = redis;
		return this;
	}

	public Bootstrap discovery(DiscoveryService discovery) {
		this.discovery = discovery;
		return this;
	}

	public Bootstrap port(int port) {
		this.port = port;
		return this;
	}

	public int port() {
		return this.port;
	}

	public void start() {
		Spark.port(port);
		Spark.staticFileLocation("/static");

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
				this.discovery.keepService(new ServiceItem(name, host, Integer.parseInt(port), Integer.parseInt(ttl)));
				result.put("ok", true);
			}
			return result;
		}, jsonify);

		Spark.get("/api/service/cancel", jsonType, (req, res) -> {
			String name = req.queryParams("name");
			String host = req.queryParams("host");
			String port = req.queryParams("port");
			Map<String, Object> result = new HashMap<String, Object>();
			if (Helpers.isEmpty(name) || Helpers.isEmpty(host) || Helpers.isEmpty(port) || !Helpers.isInteger(port)) {
				res.status(400);
				result.put("reason", "params illegal");
			} else {
				this.discovery.cancelService(new ServiceItem(name, host, Integer.parseInt(port)));
				result.put("ok", true);
			}
			return result;
		}, jsonify);

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
			Set<String> nameset = this.discovery.getServiceNames();
			String[] names = new String[nameset.size()];
			nameset.toArray(names);
			Map<String, Integer> services = this.discovery.getMultiServiceLens(names);
			long version = this.discovery.getServiceGlobalVersion();
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("services", services);
			context.put("version", version);
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
			return Spark.modelAndView(context, "service_detail.ftl");
		}, engine);

	}

	public void halt() {
		this.redis.close();
		Spark.stop();
	}
}
