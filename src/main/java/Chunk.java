import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class Chunk extends File {

	/* Maximum chunk size */
	public static final int SIZE = 4096;

	/* Padding character for chunks */
	public static final char PAD = '\0';

	private long padding = 0;

	public final File chunkOf;
	public final int index;
	private long size;

	public Chunk(File chunkOf, String name, int index) {
		super(name);
		this.chunkOf = chunkOf;
		this.index = index;
	}

	public Chunk(Chunk prev) {
		this(prev.chunkOf, UUID.randomUUID().toString(), prev.index+1);
	}

	public Chunk(File chunkOf) {
		this(chunkOf, UUID.randomUUID().toString(), 0);
	}

	@Override
	public long length() {
		return exists() ? super.length() - padding : size;
	}

	public void saveSize() {
		this.size = length();
	}


	/**
	 * Gets the useable space remaining in this chunk. This
	 * is the difference between the number of bytes already
	 * written in this chunk and the maximum chunk size.
	 *
	 * @return 	SIZE - super.length();
	 *
	 */
	@Override
	public long getFreeSpace() {
		return exists() ? SIZE - super.length() : SIZE - size;
	}

	@Override
	public boolean canWrite() {
		return getFreeSpace() > 0 && super.canWrite();
	}


	public long getPadding() {
		return padding;
	}
	public void setPadding(long val) {
		padding = val;
	}

	// Returns a trivial string representation of chunk and chunk file
	@Override
	public String toString() {
		return String.format("Chunk(of=%s, size=%d, index=%d, used/total=%d/%d)",
				chunkOf,
        size,
				index,
				length(),
				super.length()
		);
	}

	// Compares chunks by comparing top level file name then chunk index
	@Override
	public int compareTo(File other) {
		if(other instanceof Chunk) {
      Chunk o = (Chunk) other;
			int chunkDiff = this.chunkOf.compareTo(o.chunkOf);
			if(chunkDiff != 0) {
				return chunkDiff;
			}
			return this.index - o.index;
		}
    return super.compareTo(other);
	}
}
