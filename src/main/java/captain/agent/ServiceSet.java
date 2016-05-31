package captain.agent;

import java.util.Set;

import captain.ServiceItem;

public class ServiceSet {

	private Set<ServiceItem> items;
	private long version;
	private String name;

	public ServiceSet(String name, Set<ServiceItem> items, long version) {
		this.name = name;
		this.items = items;
		this.version = version;
	}

	public Set<ServiceItem> getItems() {
		return items;
	}
	
	public void setItems(Set<ServiceItem> items) {
		this.items = items;
	}

	public long getVersion() {
		return version;
	}
	
	public void setVersion(long version) {
		this.version = version;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
}
