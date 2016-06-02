package captain.agent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import captain.CaptainClient;
import captain.ServiceItem;
import spark.Spark;

public class Agent {

	private final static Logger LOG = LoggerFactory.getLogger(Agent.class);

	public static void main(String[] args) {
		Agent bootstrap = new Agent();
		bootstrap.initialize(args);
		bootstrap.start();
	}

	private Config config;
	private CaptainClient client;
	private SharedMemory shared;
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
		List<ServiceItem> origins = new ArrayList<ServiceItem>();
		for (String origin : config.origins()) {
			String[] pair = origin.split(":");
			ServiceItem item = new ServiceItem(pair[0], Integer.parseInt(pair[1]));
			origins.add(item);
		}
		client = new CaptainClient(origins);
		client.keepAlive(config.keepAlive()).checkInterval(config.interval());
		shared = new SharedMemory(config.shmfile());
		for (String name : shared.serviceNames()) {
			client.watch(name);
		}
		for (String key : shared.kvKeys()) {
			client.watchKv(key);
		}
	}

	public Config config() {
		return config;
	}

	public void start() {
		Spark.port(config.bindPort());
		Spark.threadPool(config.threadNum());
		Spark.staticFileLocation("/static");
		client.observe(new Observer(shared)).start();
		initAPIHandlers();
		initUIHandlers();
	}

	public void initUIHandlers() {
		Spark.get("/agent/", (req, res) -> {
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("config", config);
			context.put("services", shared.serviceNames());
			context.put("kvs", shared.kvKeys());
			return Spark.modelAndView(context, "agent.ftl");
		}, engine);

		Spark.get("/agent/service/", (req, res) -> {
			String name = req.queryParams("name");
			Map<String, Object> context = new HashMap<String, Object>();
			context.put("config", config);
			context.put("services", this.shared.getServices(name));
			return Spark.modelAndView(context, "agent_service.ftl");
		}, engine);

		Spark.get("/agent/kv/", (req, res) -> {
			Map<String, Object> context = new HashMap<String, Object>();
			String key = req.queryParams("key");
			context.put("config", config);
			context.put("kv", this.shared.getKv(key));
			return Spark.modelAndView(context, "agent_kv.ftl");
		}, engine);

		Spark.get("/agent/kv/unwatch", (req, res) -> {
			String key = req.queryParams("key");
			client.unwatchKv(key);
			res.redirect("/agent/");
			return null;
		}, engine);

	}

	public void initAPIHandlers() {
		Spark.get("/api/kv/get", jsonType, (req, res) -> {
			Map<String, Object> result = new HashMap<String, Object>();
			String key = req.queryParams("key");
			if (Helpers.isEmpty(key)) {
				result.put("ok", false);
				result.put("reason", "param illegal");
				return result;
			}
			KvItem kv = this.shared.getKv(key);
			result.put("kv", kv);
			result.put("ok", true);
			return result;
		}, jsonify);
		Spark.get("/api/kv/watch", jsonType, (req, res) -> {
			Map<String, Object> result = new HashMap<String, Object>();
			String[] keys = req.queryMap("key").values();
			for (String key : keys) {
				if (key.length() > 64) {
					result.put("ok", false);
					result.put("reason", "key length should be less than 64");
					return result;
				}
			}
			Map<String, Object> slots = new HashMap<String, Object>();
			for (String key : keys) {
				slots.put(key, this.shared.allocKvSlot(key));
			}
			this.client.watchKv(keys);
			result.put("slots", slots);
			result.put("ok", true);
			return result;
		}, jsonify);
		Spark.get("/api/service/get", jsonType, (req, res) -> {
			Map<String, Object> result = new HashMap<String, Object>();
			String name = req.queryParams("name");
			if (Helpers.isEmpty(name)) {
				result.put("ok", false);
				result.put("reason", "param illegal");
				return result;
			}
			ServiceSet services = this.shared.getServices(name);
			result.put("services", services);
			result.put("ok", true);
			return result;
		}, jsonify);
		Spark.get("/api/service/watch", jsonType, (req, res) -> {
			Map<String, Object> result = new HashMap<String, Object>();
			String[] names = req.queryMap("name").values();
			for (String name : names) {
				if (name.length() > 64) {
					result.put("ok", false);
					result.put("reason", "name length should be less than 64");
					return result;
				}
			}
			Map<String, Object> slots = new HashMap<String, Object>();
			for (String name : names) {
				slots.put(name, this.shared.allocServiceSlot(name));
			}
			this.client.watch(names);
			result.put("slots", slots);
			result.put("ok", true);
			return result;
		}, jsonify);
	}

	public void halt() {
		Spark.stop();
	}
}
