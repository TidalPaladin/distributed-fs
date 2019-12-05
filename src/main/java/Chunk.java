import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class Chunk extends File implements Comparable<Chunk> {

	/* Maximum chunk size */
	public static final int SIZE = 4096;

	/* Padding character for chunks */
	public static final char PAD = '\0';

	private long padding = 0;

	public final File chunkOf;
	public final int index;

	public Chunk(File chunkOf, File chunkFile, int index) {
		this.file = chunkFile;
		this.chunkOf = chunkOf;
		this.index = index;
	}

	public Chunk(Chunk predecessor) {
		this(predecessor.chunkOf, predecessor.file, predecessor.index + 1);
	}

	public Chunk(File chunkOf) {
		this(chunkOf, new File(UUID.randomUUID().toString()), 0);
	}

	@Override
	public long length() {
		return super.length() - padding;
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
		return SIZE - super.length();
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
		return String.format("Chunk(%s, %s)", chunkOf, getName());
	}

	// Compares chunks by comparing top level file name then chunk index
	@Override
	public int compareTo(Chunk other) {
		int chunkDiff = this.chunkOf.compareTo(other.chunkOf);
		if(chunkDiff != 0) {
			return chunkDiff;
		}
		return this.index - other.index;
	}
}
