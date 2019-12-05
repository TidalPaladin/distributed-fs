import java.io.*;
import java.util.*;
import java.net.InetSocketAddress;

public class ChunkMeta implements Serializable {

	private final Chunk chunk;
	private final Set<InetSocketAddress> servers;
	private boolean available;

	public ChunkMeta(Chunk chunk) {
		this.chunk = chunk;
		this.servers = new HashSet<>();
		this.available = false;
	}

	public Chunk getChunk() {
		return chunk;
	}

	public Set<InetSocketAddress> getServers() {
		return servers;
	}

	public void addServer(InetSocketAddress server) {
		servers.add(server);
	}

	public void removeServer(InetSocketAddress server) {
		servers.remove(server);
	}

	public void markUnavailable() {
		available = false;
	}

	public void markAvailable() {
		available = true;
	}

	public boolean isAvailable() {
		return available;
	}

	public int getNumReplicas() {
		return servers.size();
	}

	@Override
	public String toString() {
		return String.format(
				"ChunkMeta(%s, %d, %s)",
				chunk.getParentFile(),
				chunk.getIndex(),
				getServers()
		);
	}
}
