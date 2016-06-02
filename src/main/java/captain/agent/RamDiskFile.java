package captain.agent;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import captain.CaptainException;

public class RamDiskFile implements Closeable {

	private final static Logger LOG = LoggerFactory.getLogger(RamDiskFile.class);

	private FileChannel channel;

	public RamDiskFile(String loc) {
		Path path = Paths.get(loc);
		try {
			channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
		} catch (IOException e) {
			LOG.error("open mem disk file error", e);
		}
	}

	@Override
	public void close() {
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				LOG.error("close mem disk file error", e);
			}
		}
	}

	public void putLong(int index, long version) {
		ByteBuffer src = ByteBuffer.allocate(8);
		src.putLong(version);
		src.flip();
		try {
			channel.position(index);
			channel.write(src);
		} catch (IOException e) {
			LOG.error("write long to memdisk error", e);
			throw new CaptainException("write long to memdisk error");
		}
	}

	public void putInt(int index, int value) {
		ByteBuffer src = ByteBuffer.allocate(4);
		src.putInt(value);
		src.flip();
		try {
			channel.position(index);
			channel.write(src);
		} catch (IOException e) {
			LOG.error("write int to memdisk error", e);
			throw new CaptainException("write int to memdisk error");
		}
	}

	public void setBytes(int index, byte[] bytes) {
		ByteBuffer src = ByteBuffer.wrap(bytes);
		try {
			channel.position(index);
			channel.write(src);
		} catch (IOException e) {
			LOG.error("write bytes to memdisk error", e);
			throw new CaptainException("write bytes to memdisk error");
		}
	}
	
	public byte[] getBytes(int index, int length) {
		ByteBuffer src = ByteBuffer.allocate(length);
		try {
			channel.position(index);
			channel.read(src);
			src.flip();
			return src.array();
		} catch (IOException e) {
			LOG.error("write bytes to memdisk error", e);
			throw new CaptainException("write bytes to memdisk error");
		}
	}

	public long getLong(int index) {
		ByteBuffer dest = ByteBuffer.allocate(8);
		try {
			channel.position(index);
			channel.read(dest);
			dest.flip();
			return dest.getLong();
		} catch (IOException e) {
			LOG.error("read long to memdisk error", e);
			throw new CaptainException("read long to memdisk error");
		}
	}

	public int getInt(int index) {
		ByteBuffer dest = ByteBuffer.allocate(4);
		try {
			channel.position(index);
			channel.read(dest);
			dest.flip();
			return dest.getInt();
		} catch (IOException e) {
			LOG.error("read int to memdisk error", e);
			throw new CaptainException("read int to memdisk error");
		}
	}
	
}
