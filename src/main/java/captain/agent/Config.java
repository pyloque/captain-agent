package captain.agent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ini4j.Wini;

public class Config {

	private int bindPort = 6790;
	private int threadNum = Runtime.getRuntime().availableProcessors();
	private int keepAlive = 5;
	private int interval = 1000;
	private String shmfile = "/tmp/ramdisk/captain/agent";
	private List<String> origins = new ArrayList<String>();
	private String inifile = System.getProperty("user.home") + "/.captain/captain.agent.ini";

	public Config() {
		this.origins.add("localhost:6789");
	}

	public int bindPort() {
		return bindPort;
	}

	public Config bindPort(int bindPort) {
		this.bindPort = bindPort;
		return this;
	}

	public Config threadNum(int threadNum) {
		this.threadNum = threadNum;
		return this;
	}

	public int threadNum() {
		return this.threadNum;
	}

	public String inifile() {
		return inifile;
	}

	public Config inifile(String inifile) {
		this.inifile = inifile;
		return this;
	}

	public Config shmfile(String shmfile) {
		this.shmfile = shmfile;
		return this;
	}
	
	public String shmfile() {
		return this.shmfile;
	}
	
	public int keepAlive() {
		return keepAlive;
	}

	public Config keepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
		return this;
	}

	public int interval() {
		return interval;
	}

	public void interval(int interval) {
		this.interval = interval;
	}

	public List<String> origins() {
		return this.origins;
	}

	public void save() throws IOException {
		if (this.inifile == null || this.inifile.isEmpty()) {
			return;
		}
		File f = new File(this.inifile);
		if (!f.exists()) {
			f.getParentFile().mkdirs();
			f.createNewFile();
		}
		Wini ini = new Wini(f);
		ini.put("server", "port", this.bindPort);
		ini.put("server", "origins", String.join(",", origins));
		ini.put("server", "thread", this.threadNum);
		ini.put("watch", "keepalive", this.keepAlive);
		ini.put("watch", "interval", this.interval);
		ini.put("shm", "file", this.shmfile);
		ini.store();
	}

	public void load() throws IOException {
		if (this.inifile == null || this.inifile.isEmpty()) {
			return;
		}
		File f = new File(this.inifile);
		if (f.exists() && f.isFile()) {
			Wini ini = new Wini(f);
			Integer bindPort = ini.get("server", "port", Integer.class);
			String origins = ini.get("server", "origins", String.class);
			Integer threadNum = ini.get("server", "thread", Integer.class);
			Integer interval = ini.get("watch", "interval", Integer.class);
			Integer keepAlive = ini.get("watch", "keepalive", Integer.class);
			String shmfile = ini.get("shm", "file", String.class);
			if (bindPort != null) {
				this.bindPort = bindPort;
			}
			if (origins != null) {
				this.origins = Arrays.asList(origins.split(","));
			}
			if (threadNum != null) {
				this.threadNum = threadNum;
			}
			if (interval != null) {
				this.interval = interval;
			}
			if (keepAlive != null) {
				this.keepAlive = keepAlive;
			}
			if(shmfile != null) {
				this.shmfile = shmfile;
			}
		}
	}

}
