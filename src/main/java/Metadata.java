import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.InetSocketAddress;

public class Metadata extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("Metadata");
	private static final int MAX_MISSED = 3;
	private static final int RF = 3;

	// Meta maps
	private final Map<InetSocketAddress, ServerMeta> servers, deadServers;
	private final Map<File, FileMeta> files;
	private final Map<Chunk, ChunkMeta> chunks;

	private final Thread heartbeatRecv, heartbeatMon, request, replicate;

	public Metadata(InetSocketAddress addr) throws IOException{
		super(0, addr);
		servers = new HashMap<>();
		deadServers = new HashMap<>();
		files = new HashMap<>();
		chunks = new TreeMap<>();

		heartbeatRecv = new Thread(new HeartbeatReceive());
		heartbeatRecv.setDaemon(true);
		threads.add(heartbeatRecv);

		heartbeatMon = new Thread(new HeartbeatMonitor());
		heartbeatMon.setDaemon(true);
		threads.add(heartbeatMon);

		request = new Thread(new RequestHandler());
		request.setDaemon(true);
		threads.add(request);

		replicate = new Thread(new ReplicationMonitor());
		replicate.setDaemon(true);
		threads.add(replicate);
	}

	private class HeartbeatMonitor implements Runnable {
		@Override
		public void run() {
			long maxSeconds = MAX_MISSED * Server.HEARTBEAT_FREQ;

			while(true) {
				long currentTime = System.nanoTime();
				for(InetSocketAddress addr : servers.keySet()) {
					ServerMeta server = servers.get(addr);
					if(currentTime - server.getLastHeartbeat() > maxSeconds * 1E9) {
						log.warn("Server " + addr + " missed allowed heartbeat window");
						deadServers.put(addr, server);
						servers.remove(addr);
						log.info("Moved  " + addr + " to dead server list");
					}
				}
				try {
					Thread.sleep(maxSeconds * 1000);
				} catch(InterruptedException ex) {}
			}
		}
	}

	private class HeartbeatReceive implements Runnable {
		@Override
		public void run() {
			while(!socket.isClosed()) {
				try {
					Message<Heartbeat> msg = buffer.take(Job.Type.HEARTBEAT);
					InetSocketAddress src = msg.getReturnAddress();
					log.debug("Received heartbeat from " + src);

					if(servers.containsKey(src)) {
						ServerMeta serverMeta = servers.get(src);
						serverMeta.update(msg.job);

						Set<Chunk> newChunks = serverMeta.getNewChunks();
						Set<Chunk> missing = serverMeta.getMissing();
						if(!newChunks.isEmpty()) {
							log.info("Heartbeat contained new chunks: " + newChunks);
						}
						if(!missing.isEmpty()) {
							log.info("Heartbeat missing chunks: " + missing);
						}
					}
					else if(deadServers.containsKey(src)) {
						log.info("Beginning recovery of dead server");
						restore(src);
					}
					else {
						log.info("Registered new server " + src);
						servers.put(src, new ServerMeta(src));
					}
				}
				catch(InterruptedException ex) {
					// Will be interrupted when time to terminate
					return;
				}

			}
		}
	}

	private class RequestHandler implements Runnable {
		@Override
		public void run() {
			while(!socket.isClosed()) {
				try {
					Message msg = buffer.take(Job.Type.REQUEST);

					if(msg.job instanceof CreateFile) {
						log.info("Got create file request");
						CreateChunk job = create(((CreateFile) msg.job).file);
						Message reply = null;

						for(InetSocketAddress server : chunks.get(job.chunk).getServers()) {
							reply = new Message(id, -1, job);
							reply.setDestAddress(server);
							send(reply);
						}
						log.info("Sent reply to selected chunk servers");

						reply = msg.getReply();
						send(reply);
						log.info("Sent reply to requesting client");
					}

					else if(msg.job instanceof Locate) {
						log.info("Got locate file request");
						Locate job = locate(((Locate) msg.job).file);
						Message reply = msg.getReply(job);
						send(reply);
						log.info("Sent file location reply to client");
					}

					else if(msg.job instanceof AppendFile) {
						log.info("Got append file request");
						File file = ((AppendFile) msg.job).file;
						String payload = ((AppendFile) msg.job).payload;
						Locate replyJob = append(file, payload);
						Message reply = msg.getReply(replyJob);
						send(reply);
						log.info("Sent file location reply to client");
					}
				}
				catch(IOException ex) {
					log.error(ex.getMessage(), ex);
				}
				catch(InterruptedException ex) {
					// Will be interrupted when time to terminate
					return;
				}
			}
		}
	}


	private class ReplicationMonitor implements Runnable {
		@Override
		public void run() {
			while(true) {
				try {
					Thread.sleep(1000);
				}
				catch(InterruptedException ex) {
				}
			}
		}
	}

	public CreateChunk create(File file) {
		if(files.containsKey(file)) {
			throw new IllegalArgumentException("that file already exists");
		}

		// Pick random servers
		Chunk chunk = new Chunk(file);
		Set<InetSocketAddress> newServers = getServersForChunk(chunk);
		log.info("Using servers " + newServers + " for first chunk of new file");

		FileMeta fileMeta = new FileMeta(file);
		fileMeta.addChunk(chunk);

		ChunkMeta chunkMeta = new ChunkMeta(chunk);
		for(InetSocketAddress server : servers.keySet()) {
			chunkMeta.addServer(server);
		}

		files.put(file, fileMeta);
		chunks.put(chunk, chunkMeta);
		log.info("Created metadata for new file/chunk: ");
		log.info(chunkMeta);
		return new CreateChunk(chunk);
	}

	public Locate locate(File file) {
		FileMeta fileMeta = files.get(file);
		TreeSet<Chunk> chunks = fileMeta.getChunks();
		TreeMap<Chunk, ChunkMeta> payload = new TreeMap<>(this.chunks);
		payload.keySet().retainAll(chunks);
		return new Locate(file, payload);
	}

	public Locate append(File file, String payload) throws IOException {
		TreeMap<Chunk, ChunkMeta> locations = locate(file).meta;
		Chunk lastChunk = locations.lastKey();

		if(!lastChunk.canWrite(payload)) {
			log.info("Creating new chunk file due to overflow");
			Chunk nextChunk = new Chunk(file);
			files.get(file).addChunk(nextChunk);

			ChunkMeta newMeta = new ChunkMeta(nextChunk);
			Set<InetSocketAddress> newServers = getServersForChunk(nextChunk);
			log.info("Using servers " + newServers + " for next chunk of new file");
			for(InetSocketAddress server : servers.keySet()) {
				newMeta.addServer(server);
			}
			chunks.put(nextChunk, newMeta);
			log.info(newMeta);

			for(InetSocketAddress server : newServers) {
				Message reply = new Message(id, -1, new CreateChunk(nextChunk));
				reply.setDestAddress(server);
				send(reply);
			}
			log.info("Created next chunk on selected chunk servers");

			TreeMap<Chunk, ChunkMeta> loc = new TreeMap<>(this.chunks);
			locations.keySet().retainAll(files.get(file).getChunks());
			return new Locate(file, loc);
		}
		else {
			log.info("Continuing from partial chunk");
			return locate(file);
		}
	}

	public void restore(InetSocketAddress server) {
	}

	private int getMaxReplicas() {
		return Math.min(RF, servers.size());
	}

	private Set<InetSocketAddress> getServersForChunk(Chunk chunk) {
		Set<InetSocketAddress> group = null;

		// Initialize empty set or current known server set
		if(chunks.containsKey(chunk)) {
			group = chunks.get(chunk).getServers();
		}
		else {
			group = new HashSet<>();
		}

		int numToAdd = getMaxReplicas() - group.size();
		if(numToAdd > 0) {
			List<InetSocketAddress> possible = new ArrayList<>(servers.keySet());
			possible.removeAll(group);
			Collections.shuffle(possible);
			List<InetSocketAddress> add = possible.subList(0, numToAdd);
			group.addAll(add);
		}
		return group;
	}
}
