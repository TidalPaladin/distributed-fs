import java.io.*;
import java.util.*;

public class FileMeta {

	/* Mapping of file to list of chunks that make up that file */
	private final Map<File, List<Chunk>> chunks;

	/* Mapping of chunk to list of servers that store that chunk */
	private final Map<Chunk, Set<InetSocketAddress>> servers;
	private final Map<InetSocketAddress, Set<Chunk>> reverseServers;

	/* Two level map of <Chunk, Server> to last updated timestamp */
	private final Map<Chunk, String> lastUpdateChunk;
	private final Map<String, Long> lastUpdateServer;

	public FileMeta() {
			this.chunks = new HashMap<>();
			this.servers = new HashMap<>();
			this.reverseServers = new HashMap<>();
			this.lastUpdateChunk = new HashMap<>();
			this.lastUpdateServer = new HashMap<>();
	}

	/**
		* Gets an ordered list of chunks associated with a file.
		*
		* @param file	File to retrieve chunks for
		*
		* @return List of Chunks such that result[0] is the first chunk
		*
		*/
	public List<Chunk> getChunks(File file) {
			return chunks.get(file);
	}

	/**
		* Gets an ordered list of chunks associated with a file over a given range
		*
		* @param file	File to retrieve chunks for
		* @param off		Byte offset in file
		* @param len		Number of bytes relative to off
		*
		* @return Chunks associated with bytes off -> off + len in file
		*
		*/
	public List<Chunk> getChunks(File file, int off, int len) {
			List<Chunk> chunkList = chunks.get(file);
			if(chunkList == null) {
					throw new NoSuchElementException(String.format("file %s not known", file));
			}

			final int startChunk = off / Chunk.SIZE;
			final int endChunk = (int) Math.ceil(1.0 * (off+len) / Chunk.SIZE);
			return chunkList.subList(startChunk, endChunk);
	}

	/**
		* Gets an ordered list of offsets associated with chunks of a file over a
		* given range.
		*
		* @param file	File to retrieve chunks for
		* @param off		Byte offset in file
		* @param len		Number of bytes relative to off
		*
		* @return	Offsets associated with bytes off -> off + len in file
		*									result.get(0) gives the offset to be used when reading the first chunk
		*
		*/
	public List<Chunk> getOffsets(File file, int off, int len) {
			List<Chunk> chunkList = chunks.get(file);
			if(chunkList == null) {
					throw new NoSuchElementException(String.format("file %s not known", file));
			}

			final int startChunk = off / Chunk.SIZE;
			final int endChunk = (int) Math.ceil(1.0 * (off+len) / Chunk.SIZE);
			return chunkList.subList(startChunk, endChunk);
	}

	/**
		* Gets a set of all currently known chunks.
		*
		*
		* @return Unordered set of all known Chunk objects
		*
		*/
	public Set<Chunk> getChunks() {
			return servers.keySet();
	}

	/**
		* Gets a set of servers that are storing a given chunk.
		*
		* @param chunk	The chunk to look up servers for
		*
		* @return Unordered of server hostnames storing chunk
		*
		*/
	public Set<InetSocketAddress> getServers(Chunk chunk) {
			return servers.get(chunk);
	}

	/**
		* Gets an unordered set of all known servers
		*
		* @return Unordered of server hostnames
		*
		*/
	public Set<InetSocketAddress> getServers() {
			return reverseServers.keySet();
	}

	/**
		* Gets an unordered set of all known files.
		*
		* @return Unordered set of files
		*
		*/
	public Set<File> getFiles() {
			return chunks.keySet();
	}

	/**
		* Adds a file to be tracked.
		*
		* @param file	The file name
		*/
	public void addFile(File file) {
			chunks.put(file, new ArrayList<>());
	}

	/**
		* Checks if a file name is currently being tracked.
		*
		* @param file	The file name
		*
		* @return true if addFile(file) was called previously
		*/
	public boolean hasFile(File file) {
			return chunks.keySet().contains(file);
	}

	/**
		* Checks if a file name is currently being tracked.
		*
		* @param chunk		Chunk to test as being stored by server
		* @param server	Server to test as storing chunk
		*
		* @return True if chunk is stored on server, false otherwise
		*/
	public boolean hasChunk(Chunk chunk, String server) {
			Set<String> serverSet = servers.get(chunk);
			if(serverSet != null) {
					return serverSet.contains(server);
			}
			return false;
	}

	/**
		* Records that a given server is hosting a given chunk
		*
		* @param chunk		Chunk being hosted by a new server
		* @param server	Server that is storing chunk
		*
		*/
	public void addServer(Chunk chunk, InetSocketAddress server) {
			Set<String> value = servers.get(chunk);
			if(value == null) {
					value = new HashSet<String>();
			}
			value.add(server);
			servers.put(chunk, value);
	}

	/**
		* Ensures that a given server is not recorded as storing a given chunk.
		* If the chunk is not currently being stored by server, no errors will be produced.
		* The given chunk must exist in the metadata record.
		*
		* @param chunk		Chunk not being hosted by server.
		* 														pre: this.getChunks().contains(chunk)
		*
		* @param server	Server that is not storing chunk
		*
		*/
	public void removeServer(Chunk chunk, InetSocketAddress server) {
			Set<String> value = servers.get(chunk);
			if(value == null) {
					throw new NoSuchElementException(String.format("chunk %s not found", chunk));
			}
			value.remove(server);
			servers.put(chunk, value);
	}
}
