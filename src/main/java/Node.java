import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class Node implements Runnable {
	protected static final Logger log = LogManager.getLogger("Node");

	public static final int
		PORT = 32000,
		MAX_APPEND = 1024;

	protected final ServerSocket socket;
	protected InetSocketAddress returnAddr;

	protected ExecutorService service = Executors.newFixedThreadPool(10);
	protected Messenger messenger;

	public Node(InetSocketAddress returnAddr) throws IOException {
		socket = new ServerSocket(PORT);
		this.returnAddr = returnAddr;
		this.messenger= new Messenger(returnAddr);
	}

	@Override
	public void run() {
	   service.submit(new SocketThread());
	}

	public abstract void onMessage(Socket s);

	// Producer task. Reads messages from socket and runs callbacks
	protected class SocketThread implements Runnable {
		@Override
		public void run() {
			log.info("Started listening for incomding connections");
			while(!socket.isClosed()) {
				try {
					Socket s = socket.accept();
					log.debug("Accepted socket connection");
					onMessage(s);
					s.close();
				}
				catch(Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			}
			log.debug("Stopped listening for incoming connections");
		}
	}

	public String read(File file, InetSocketAddress metaServer) throws IOException, InterruptedException {
		log.info("Started read job");
		Read job = new Read(file);
		Request<Read, TreeMap<Chunk, ChunkMeta>> msg = new Request<>(job, metaServer);
		TreeMap<Chunk, ChunkMeta> locations = messenger.send(msg);
		log.info("Chunks in " + file + ": " + locations.size());

		StringBuilder sb = new StringBuilder();
		for(Chunk chunk : locations.keySet()) {
			ChunkMeta meta = locations.get(chunk);
			InetSocketAddress server = meta.getServers().iterator().next();
			String payload = read(chunk, server);
			sb.append(payload);
		}
		return sb.toString();
	}

	public String read(Chunk chunk, InetSocketAddress server) throws IOException, InterruptedException {
		Request<Read, String> msg = new Request<>(new Read(chunk), server);
		log.info("Requesting " + chunk);
		String payload = messenger.send(msg);
		return payload;
	}

	public boolean append(File file, String payload, InetSocketAddress metaServer) throws IOException, InterruptedException {
		Append job = new Append(file, payload);
		Request<Append, TreeMap<Chunk, ChunkMeta>> permissionRequest = new Request<>(job, metaServer);

		TreeMap<Chunk, ChunkMeta> chunks = messenger.send(permissionRequest);
		if(chunks.isEmpty()) {
			log.info("Metaserver replied with abort");
			return false;
		}
		log.info("Metaserver approved append");

		Chunk lastChunk = chunks.lastKey();
		ChunkMeta lastChunkMeta = chunks.get(lastChunk);
		log.info("Identified last chunk " + lastChunk);
		Set<InetSocketAddress> quorum = lastChunkMeta.getServers();
		Append chunkJob = new Append(lastChunk, payload);

		boolean ready = phaseOne(chunkJob, quorum);
		if(!ready) {
			return false;
		}

		phaseTwo(chunkJob, quorum);
		return true;
	}


	public TreeMap<Chunk, ChunkMeta> locate(File file, InetSocketAddress metaServer) throws IOException, InterruptedException {
		Locate job = new Locate(file);
		Request<Locate, TreeMap<Chunk, ChunkMeta>> msg = new Request<>(job, metaServer);
		TreeMap<Chunk, ChunkMeta> reply = messenger.send(msg);
		if(reply == null) {
			log.error("Failed to locate all chunks for " + file);
			throw new IOException("not all chunks were available");
		}
		return reply;
	}


	protected boolean phaseOne(Job job, Set<InetSocketAddress> quorum) throws IOException {
		boolean success = true;
		for(InetSocketAddress server : quorum) {
			Request<Job, Job> msg = new Request<>(job, server);
			log.info("Sending phase 1 of " + job + " to " + server);
			Job reply = messenger.send(msg);

			if(!(reply instanceof Commit)) {
				log.info("Server " + server + " aborted job " + job);
				success = false;
				break;
			}
		}

		if(!success) {
			log.info("Sending abort of phase 1 to quorum");
			for(InetSocketAddress server : quorum) {
				Message<Abort> msg = new Message<>(new Abort(job), server);
				messenger.send(msg);
			}
		}
		return success;
	}

	protected void phaseTwo(Job job, Set<InetSocketAddress> quorum) throws IOException {
		Commit commit = new Commit(job);
		for(InetSocketAddress server : quorum) {
			Message<Commit> msg = new Message<>(commit, server);
			log.info("Sending phase 2 commit of " + job + " to " + server);
			messenger.send(msg);
		}
	}

	// Handles termination, interrupting daemon threads and closing sockets
	private void terminate() {
		log.trace("Terminating...");
		try {
			log.info("Closed socket");
			socket.close();
			service.shutdownNow();
		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
		log.trace("Finished termination job");
	}

	public String toString() {
		return String.format("%s(%s)", getClass().getName(), returnAddr);
	}
}
