package captain;

public class ServiceItem {

	private String name;
	private String host;
	private int port;
	private int ttl;
	
	public ServiceItem(String name, String host, int port) {
		this(name, host, port, 0);
	}

	public ServiceItem(String name, String host, int port, int ttl) {
		this.name = name;
		this.host = host;
		this.port = port;
		this.ttl = ttl;
	}

	public String key() {
		return String.format("%s:%s", host, port);
	}

	public String name() {
		return name;
	}
	
	public int ttl() {
		return ttl;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode() & this.host.hashCode() & Integer.hashCode(this.port);
	}

	@Override
	public boolean equals(Object obj) {
		ServiceItem other = (ServiceItem) obj;
		return this.name.equals(other.name) && this.host.equals(other.host) && this.port == other.port;
	}

}
