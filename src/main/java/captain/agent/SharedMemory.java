package captain.agent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import captain.CaptainException;
import captain.ServiceItem;

/**
 * memory = service_header_size + max_services * max_service_record_size +
 * kv_header_size + max_kvs * max_kv_record_size
 * 
 * service_header_size = max_services * 4byte
 * 
 * kv_header_size = max_kvs * 4byte
 * 
 * max_service_record_size = max_items_per_service * max_item_size
 *
 */
public class SharedMemory {

	private final static int MAX_ITEM_SIZE = 64;
	private final static int MAX_ITEMS_PER_SERVICE = 1024;
	private final static int MAX_SERVICES = 128;
	private final static int MAX_KVS = 64;
	private final static int MAX_KV_RECORD_SIZE = 1024 * 64;
	private final static int SERVICE_HEADER_SIZE = MAX_SERVICES * 4;
	private final static int KV_HEADER_SIZE = MAX_KVS * 4;
	private final static int MAX_SERVICE_RECORD_SIZE = MAX_ITEMS_PER_SERVICE * MAX_ITEM_SIZE;
	private final static int KV_HEADER_OFFSET = SERVICE_HEADER_SIZE + MAX_SERVICE_RECORD_SIZE * MAX_SERVICES;

	private Map<String, Integer> service2slots = new HashMap<String, Integer>();
	private Map<String, Integer> kv2slots = new HashMap<String, Integer>();
	private Map<Integer, Integer> serviceSlot2Blocks = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> kvSlot2Blocks = new HashMap<Integer, Integer>();
	private boolean[] serviceSlotsState = new boolean[MAX_SERVICES];
	private boolean[] kvSlotsState = new boolean[MAX_KVS];
	private boolean[] serviceBlocksState = new boolean[MAX_SERVICES];
	private boolean[] kvBlocksState = new boolean[MAX_KVS];

	private RamDiskFile file;

	public SharedMemory(String shmfile) {
		this.file = new RamDiskFile(shmfile);
	}

	public synchronized int allocServiceSlot(String name) {
		Integer slot = this.service2slots.get(name);
		if (slot != null) {
			return slot;
		}
		int i;
		for (i = 0; i < MAX_SERVICES; i++) {
			if (!serviceSlotsState[i]) {
				break;
			}
		}
		this.serviceSlotsState[i] = true;
		this.service2slots.put(name, i);
		this.serviceSlot2Blocks.put(i, MAX_SERVICES); // block = MAX_SERVICES means invalid block
		this.file.putInt(i * 4, MAX_SERVICES);
		return i;
	}

	public synchronized int allocKvSlot(String key) {
		Integer slot = this.kv2slots.get(key);
		if (slot != null) {
			return slot;
		}
		int i;
		for (i = 0; i < MAX_KVS; i++) {
			if (!kvSlotsState[i]) {
				break;
			}
		}
		this.kvSlotsState[i] = true;
		this.kv2slots.put(key, i);
		this.kvSlot2Blocks.put(i, MAX_KVS); // block = MAX_KVS means invalid block
		this.file.putInt(i * 4 + KV_HEADER_OFFSET, MAX_KVS);
		return i;
	}

	public synchronized void updateService(String name, long version, List<ServiceItem> services) {
		int i;
		int currentSlot = this.service2slots.get(name);
		int currentBlock = this.serviceSlot2Blocks.get(currentSlot);
		for (i = 0; i < MAX_SERVICES; i++) {
			if (!serviceBlocksState[i]) {
				break;
			}
		}
		if (i == MAX_SERVICES) {
			throw new CaptainException("service slots full");
		}

		int newBlock = i;
		// mark new block busy
		serviceBlocksState[newBlock] = true;

		int offset = SERVICE_HEADER_SIZE + newBlock * MAX_SERVICE_RECORD_SIZE;
		StringBuffer buffer = new StringBuffer();
		for (int k = 0; k < services.size(); k++) {
			ServiceItem item = services.get(k);
			String pair = String.format("%s:%s", item.getHost(), item.getPort());
			buffer.append(pair);
			if (k < services.size() - 1) {
				buffer.append(',');
			}
		}
		byte[] bytes = buffer.toString().getBytes();
		// write serialized services to new block
		file.putLong(offset, version);
		file.putInt(offset + 8, bytes.length);
		if (bytes.length > 0) {
			file.setBytes(offset + 12, bytes);
		}
		file.putInt(currentSlot * 4, newBlock);
		if (currentBlock < MAX_SERVICES) {
			// mark old block free
			this.serviceBlocksState[currentBlock] = false;
		}

		// point service slot to new block
		this.serviceSlot2Blocks.put(currentSlot, newBlock);
	}

	public synchronized void updateKv(String key, long version, JSONObject json) {
		int i;
		int currentSlot = this.kv2slots.get(key);
		int currentBlock = this.kvSlot2Blocks.get(currentSlot);
		for (i = 0; i < MAX_KVS; i++) {
			if (!kvBlocksState[i]) {
				break;
			}
		}
		if (i == MAX_KVS) {
			throw new CaptainException("kv slots full");
		}

		int newBlock = i;
		// mark new block busy
		kvBlocksState[newBlock] = true;

		int offset = KV_HEADER_OFFSET + KV_HEADER_SIZE + newBlock * MAX_KV_RECORD_SIZE;
		byte[] bytes = json.toString().getBytes();
		// write serialized services to new block
		file.putLong(offset, version);
		file.putInt(offset + 8, bytes.length);
		file.setBytes(offset + 12, bytes);
		// write new block index to header slot
		file.putInt(KV_HEADER_OFFSET + currentSlot * 4, newBlock);

		if (currentBlock < MAX_KVS) {
			// mark old block free
			this.kvBlocksState[currentBlock] = false;
		}

		// point service slot to new block
		this.kvSlot2Blocks.put(currentSlot, newBlock);
	}

	public synchronized ServiceSet getServices(String name) {
		Integer slot = this.service2slots.get(name);
		ServiceSet set = new ServiceSet(name, Collections.emptySet(), -1);
		if (slot == null) {
			return set;
		}
		int block = this.file.getInt(slot * 4);
		if(block == MAX_SERVICES) {
			return set;
		}
		int offset = SERVICE_HEADER_SIZE + block * MAX_SERVICE_RECORD_SIZE;
		long version = this.file.getLong(offset);
		int len = this.file.getInt(offset + 8);
		if (len > 0) {
			byte[] bytes = this.file.getBytes(offset + 12, len);
			String[] pairs = new String(bytes).split(",");
			Set<ServiceItem> items = new HashSet<ServiceItem>();
			for (String pair : pairs) {
				String[] parts = pair.split(":");
				ServiceItem item = new ServiceItem(parts[0], Integer.parseInt(parts[1]));
				items.add(item);
			}
			set.setItems(items);
		}
		set.setVersion(version);
		return set;
	}

	public synchronized KvItem getKv(String key) {
		KvItem kv = new KvItem(key, new JSONObject(), -1);
		Integer slot = this.kv2slots.get(key);
		if (slot == null) {
			return kv;
		}
		int block = this.file.getInt(slot * 4);
		if(block == MAX_KVS) {
			return kv;
		}
		int offset = KV_HEADER_OFFSET + KV_HEADER_SIZE + block * MAX_KV_RECORD_SIZE;
		long version = this.file.getLong(offset);
		int len = this.file.getInt(offset + 8);
		byte[] bytes = this.file.getBytes(offset + 12, len);
		kv.setValue(new JSONObject(new String(bytes)));
		kv.setVersion(version);
		return kv;
	}

	public synchronized void sync() {
		this.file.sync();
	}

}
