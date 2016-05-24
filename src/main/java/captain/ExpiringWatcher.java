package captain;

public class ExpiringWatcher extends Thread {
	
	private DiscoveryService discovery;
	private boolean stop;
	
	public ExpiringWatcher(DiscoveryService discovery) {
		this.discovery = discovery;
	}
	
	public void run() {
		while(!stop) {
			this.discovery.trimAllExpired();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				stop = true;
			}
		}
	}
	
	public void quit() {
		this.stop = true;
	}
	
}
