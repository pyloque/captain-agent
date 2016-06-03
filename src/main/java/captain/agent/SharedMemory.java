package captain.agent;

import java.nio.ByteBuffer;
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
 * memory_size = magic_header + max_service_names_size + max_kv_names_size +
 * service_header_size + max_services * max_service_record_size + kv_header_size
 * + max_kvs * max_kv_record_size
 * 
 * max_service_names_size = max_service_name_len * max_services
 * 
 * max_kv_names_size = max_kv_name_len * max_kvs
 * 
 * service_header_size = max_services * 4byte
 * 
 * kv_header_size = max_kvs * 4byte
 * 
 * max_service_record_size = max_items_per_service * max_item_size
 *
 */
public class SharedMemory {

	private final static byte[] MAGIC_HEADER = "captain".getBytes();
	private final static int MAGIC_HEADER_SIZE = MAGIC_HEADER.length;
	private final static int MAX_ITEM_SIZE = 64;
	private final static int MAX_ITEMS_PER_SERVICE = 1024;
	private final static int MAX_SERVICES = 128;
	private final static int MAX_KVS = 64;
	private final static int MAX_KV_RECORD_SIZE = 1024 * 64;

	private final static int SERVICE_NAMES_OFFSET = MAGIC_HEADER_SIZE;
	private final static int MAX_SERVICE_NAME_LEN = 128;
	private final static int KV_NAMES_OFFSET = SERVICE_NAMES_OFFSET + MAX_SERVICE_NAME_LEN * MAX_SERVICES;
	private final static int MAX_KV_NAME_LEN = 128;

	private final static int SERVICE_HEADER_SIZE = MAX_SERVICES * 4;
	private final static int KV_HEADER_SIZE = MAX_KVS * 4;
	private final static int MAX_SERVICE_RECORD_SIZE = MAX_ITEMS_PER_SERVICE * MAX_ITEM_SIZE;

	private final static int SERVICE_HEADER_OFFSET = KV_NAMES_OFFSET + MAX_KV_NAME_LEN * MAX_KVS;

	private final static int KV_HEADER_OFFSET = SERVICE_HEADER_OFFSET + SERVICE_HEADER_SIZE
			+ MAX_SERVICE_RECORD_SIZE * MAX_SERVICES;

	private final static int MEMORY_SIZE = KV_HEADER_OFFSET + KV_HEADER_SIZE + MAX_KV_RECORD_SIZE * MAX_KVS;

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
		this.initialize();
	}

	public synchronized Set<String> serviceNames() {
		return this.service2slots.keySet();
	}

	public synchronized Set<String> kvKeys() {
		return this.kv2slots.keySet();
	}

	public void initialize() {
		byte[] bytes = this.file.getBytes(0, MAGIC_HEADER_SIZE);
		boolean flag = true;
		for (int i = 0; i < MAGIC_HEADER_SIZE; i++) {
			if (bytes[i] != MAGIC_HEADER[i]) {
				flag = false;
				break;
			}
		}
		if (flag) {
			this.readMemory();
		} else {
			this.formatMemory();
		}
	}

	public void readMemory() {
		for (int i = 0; i < MAX_SERVICES; i++) {
			int offset = SERVICE_NAMES_OFFSET + i * MAX_SERVICE_NAME_LEN;
			int len = this.file.getInt(offset);
			if (len == 0) {
				continue;
			}
			byte[] bytes = this.file.getBytes(offset + 4, len);
			String name = new String(bytes);
			this.service2slots.put(name, i);
			this.serviceSlotsState[i] = true;
		}
		for (int i = 0; i < MAX_SERVICES; i++) {
			int offset = SERVICE_HEADER_OFFSET + i * 4;
			int block = this.file.getInt(offset);
			if (block != MAX_SERVICES) {
				this.serviceSlot2Blocks.put(i, block);
				this.serviceBlocksState[block] = true;
			}
		}
		for (int i = 0; i < MAX_KVS; i++) {
			int offset = KV_NAMES_OFFSET + i * MAX_KV_NAME_LEN;
			int len = this.file.getInt(offset);
			if (len == 0) {
				continue;
			}
			byte[] bytes = this.file.getBytes(offset + 4, len);
			String key = new String(bytes);
			this.kv2slots.put(key, i);
			this.kvSlotsState[i] = true;
		}
		for (int i = 0; i < MAX_KVS; i++) {
			int offset = KV_HEADER_OFFSET + i * 4;
			int block = this.file.getInt(offset);
			if (block != MAX_KVS) {
				this.kvSlot2Blocks.put(i, block);
				this.kvBlocksState[block] = true;
			}
		}
	}

	public void formatMemory() {
		for (int i = 0; i < MEMORY_SIZE; i += 4) {
			// clear zero
			this.file.putInt(i, 0);
		}
		for (int i = 0; i < MAX_SERVICES; i++) {
			int offset = SERVICE_HEADER_OFFSET + i * 4;
			this.file.putInt(offset, MAX_SERVICES);
		}
		for (int i = 0; i < MAX_KVS; i++) {
			int offset = KV_HEADER_OFFSET + i * 4;
			this.file.putInt(offset, MAX_KVS);
		}
		this.file.setBytes(0, MAGIC_HEADER);
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
		this.serviceSlot2Blocks.put(i, MAX_SERVICES); // block = MAX_SERVICES
														// means invalid block
		this.file.putInt(i * 4 + SERVICE_HEADER_OFFSET, MAX_SERVICES);

		int offset = SERVICE_NAMES_OFFSET + i * MAX_SERVICE_NAME_LEN;
		this.file.putInt(offset, name.length());
		this.file.setBytes(offset + 4, name.getBytes());
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
		this.kvSlot2Blocks.put(i, MAX_KVS); // block = MAX_KVS means invalid
											// block
		this.file.putInt(i * 4 + KV_HEADER_OFFSET, MAX_KVS);

		int offset = KV_NAMES_OFFSET + i * MAX_KV_NAME_LEN;
		this.file.putInt(offset, key.length());
		this.file.setBytes(offset + 4, key.getBytes());
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

		int offset = SERVICE_HEADER_OFFSET + SERVICE_HEADER_SIZE + newBlock * MAX_SERVICE_RECORD_SIZE;
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
		ByteBuffer buf = ByteBuffer.allocate(12 + bytes.length);
		buf.putLong(version);
		buf.putInt(bytes.length);
		buf.put(bytes);
		buf.flip();
		// write serialized services to new block
		file.setBytes(offset, buf.array());
		file.putInt(SERVICE_HEADER_OFFSET + currentSlot * 4, newBlock);
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
		ByteBuffer buf = ByteBuffer.allocate(12 + bytes.length);
		buf.putLong(version);
		buf.putInt(bytes.length);
		buf.put(bytes);
		buf.flip();
		// write serialized services to new block
		file.setBytes(offset, buf.array());
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
		int block = this.file.getInt(SERVICE_HEADER_OFFSET + slot * 4);
		if (block == MAX_SERVICES) {
			return set;
		}
		int offset = SERVICE_HEADER_OFFSET + SERVICE_HEADER_SIZE + block * MAX_SERVICE_RECORD_SIZE;
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
		int block = this.file.getInt(KV_HEADER_OFFSET + slot * 4);
		if (block == MAX_KVS) {
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

}
