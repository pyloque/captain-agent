package captain.agent;

import captain.CaptainClient;
import captain.ICaptainObserver;

public class Observer implements ICaptainObserver {

	private SharedMemory shared;

	public Observer(SharedMemory shared) {
		this.shared = shared;
	}

	@Override
	public void kvUpdate(CaptainClient client, String key) {
		this.shared.updateKv(key, client.kvVersion(key), client.kv(key));
		this.shared.sync();
	}

	@Override
	public void serviceUpdate(CaptainClient client, String name) {
		this.shared.updateService(name, client.serviceVersion(name), client.selectAll(name));
		this.shared.sync();
	}

	@Override
	public void online(CaptainClient client, String name) {
	}

	@Override
	public void allOnline(CaptainClient client) {

	}

	@Override
	public void offline(CaptainClient client, String name) {

	}

}
