import java.io.*;
import java.util.*;
import java.net.InetSocketAddress;

public class FileMeta {

	private final File file;
	private final TreeSet<Chunk> chunks;

	public FileMeta(File file) {
		this.file = file;
		this.chunks = new TreeSet<>();
	}

	public TreeSet<Chunk> getChunks() {
		return chunks;
	}

	public void update(Heartbeat payload) {
	}

	public void addChunk(Chunk chunk) {
		chunks.add(chunk);
	}

	public void update(Chunk chunk) {
		chunks.remove(chunk);
		chunks.add(chunk);
	}

	public boolean hasChunk(Chunk chunk) {
		return chunks.contains(chunk);
	}

	public long getNumChunks() {
		return chunks.size();
	}

	public Chunk getLastChunk() {
		return chunks.last();
	}
}
