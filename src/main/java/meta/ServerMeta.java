import java.io.*;
import java.util.*;
import java.net.InetSocketAddress;

public class ServerMeta {

	private Set<Chunk> chunks;
	private long lastHeartbeat;

	private Set<Chunk> incoming, missing;

	public ServerMeta(InetSocketAddress addr) {
		this.chunks = new TreeSet<>();
		this.lastHeartbeat = System.nanoTime();
	}

	public long getLastHeartbeat() {
		return lastHeartbeat;
	}

	public void update(Heartbeat payload) {
		lastHeartbeat = System.nanoTime();
		Set<Chunk> newChunks = payload.storedChunks;

		incoming = new TreeSet<>(newChunks);
		incoming.removeAll(chunks);

		missing = new TreeSet<>(chunks);
		missing.removeAll(newChunks);
		chunks = newChunks;
	}

	public Set<Chunk> getChunks() {
		return chunks;
	}

	public Set<Chunk> getMissing() {
		return missing;
	}

	public Set<Chunk> getNewChunks() {
		return incoming;
	}

	public boolean hasChunk(Chunk chunk) {
		return chunks.contains(chunk);
	}
}
