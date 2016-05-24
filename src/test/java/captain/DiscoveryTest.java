package captain;

import java.io.IOException;
import java.net.ServerSocket;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import junit.framework.Assert;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class DiscoveryTest {

	private Bootstrap bootstrap;
	private String urlRoot;
	private RedisStore redis;
	private String name = "sample";

	@Before
	public void setup() {
		bootstrap = new Bootstrap();
		JedisPoolConfig config = new JedisPoolConfig();
		JedisPool pool = new JedisPool(config, "localhost");
		redis = new RedisStore(pool);
		bootstrap.initialize(redis);
		bootstrap.port(randomPort());
		bootstrap.start();
		this.urlRoot = "http://localhost:" + bootstrap.port();
		this.clean();
	}

	private int randomPort() {
		ServerSocket socket;
		try {
			socket = new ServerSocket(0);
			int port = socket.getLocalPort();
			socket.close();
			return port;
		} catch (IOException e) {
			throw new RuntimeException("impossible here");
		}
	}

	@Test
	public void testDiscovery() throws UnirestException {
		JSONObject js = Unirest.get(urlRoot + "/api/service/keep").queryString("name", name)
				.queryString("host", "localhost").queryString("port", 6000).queryString("ttl", 30).asJson().getBody()
				.getObject();
		Assert.assertTrue(js.getBoolean("ok"));
		js = Unirest.get(urlRoot + "/api/service/version").queryString("name", name).asJson().getBody().getObject();
		Assert.assertTrue(js.getBoolean("ok"));
		Assert.assertEquals(1, js.getJSONObject("versions").getInt(name));
		js = Unirest.get(urlRoot + "/api/service/set").queryString("name", name).asJson().getBody().getObject();
		Assert.assertTrue(js.getBoolean("ok"));
		Assert.assertEquals(1, js.getInt("version"));
		Assert.assertEquals(1, js.getJSONArray("services").length());
		JSONObject item = js.getJSONArray("services").getJSONObject(0);
		Assert.assertEquals("localhost", item.getString("host"));
		Assert.assertEquals(6000, item.getInt("port"));
	}

	@After
	public void teardown() {
		clean();
		bootstrap.halt();
	}
	
	private void clean() {
		redis.execute(jedis -> {
			jedis.del("service_version_" + name);
			jedis.del("service_set_" + name);
			jedis.srem("service_names", name);
		});
	}

}
