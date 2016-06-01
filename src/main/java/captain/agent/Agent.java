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
	}

	public Config config() {
		return config;
	}

	public void start() {
		Spark.port(config.bindPort());
		Spark.threadPool(config.threadNum());
		client.observe(new Observer(shared)).start();
		initHandlers();
	}

	public void initHandlers() {
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
			Map<String, Object> slots = new HashMap<String, Object>();
			for (String key : keys) {
				slots.put(key, this.shared.allocKvSlot(key));
			}
			this.shared.sync();
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
			Map<String, Object> slots = new HashMap<String, Object>();
			for (String name : names) {
				slots.put(name, this.shared.allocServiceSlot(name));
			}
			this.shared.sync();
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
