package captain.agent;

import org.json.JSONObject;

public class KvItem implements Comparable<KvItem>{

	private String key;

	private JSONObject value;

	private long version;
	
	public KvItem(String key, JSONObject value, long version) {
		this.key = key;
		this.value = value;
		this.version = version;
	}

	public String getKey() {
		return key;
	}

	public KvItem setKey(String key) {
		this.key = key;
		return this;
	}

	public JSONObject getValue() {
		return value;
	}
	
	public KvItem setValue(JSONObject value) {
		this.value = value;
		return this;
	}

	public long getVersion() {
		return version;
	}

	public KvItem setVersion(long version) {
		this.version = version;
		return this;
	}

	@Override
	public int compareTo(KvItem o) {
		return this.key.compareTo(o.key);
	}

}
