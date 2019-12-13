import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.*;

public class Metaserver extends Node {
	protected static final Logger log = LogManager.getLogger("MetaServer");

	public static final int MAX_MISSED = 3;
	public static final int HEARTBEAT_FREQ = 3;
	public static final int RF = 3;
	private final long maxSeconds = MAX_MISSED * HEARTBEAT_FREQ;


	// Meta maps
	private final ConcurrentMap<InetSocketAddress, ServerMeta> servers, deadServers;
	private final ConcurrentMap<File, FileMeta> files;
	private final ConcurrentMap<Chunk, ChunkMeta> chunks;


	public Metaserver(InetSocketAddress returnAddr) throws IOException {
		super(returnAddr);
		this.chunks = new ConcurrentHashMap<Chunk, ChunkMeta>();
		this.files = new ConcurrentHashMap<File, FileMeta>();
		servers = new ConcurrentHashMap<>();
		deadServers = new ConcurrentHashMap<>();
	}

	@Override
	public void run() {
		super.run();
		while(true) {
			long currentTime = System.nanoTime();
			for(InetSocketAddress addr : servers.keySet()) {
				ServerMeta server = servers.get(addr);
				if(currentTime - server.getLastHeartbeat() > maxSeconds * 1E9) {
					log.warn("Server " + addr + " missed allowed heartbeat window");
					deadServers.put(addr, server);
					servers.remove(addr);
					log.info("Moved  " + addr + " to dead server list");
          markUnavailable(deadServers.get(addr));
				}
			}
			try {
				Thread.sleep(maxSeconds * 1000);
			} catch(InterruptedException ex) {}
		}
	}


	public void onMessage(Socket s) {
		try {
			Message msg = messenger.receive(s);
			Job j = msg.job;
			log.info("Got message: " + j.toString());
			if(j instanceof Create) {
				Create job = (Create) j;
				if(files.containsKey(job.target)) {
					messenger.send(s, Boolean.valueOf(false));
				}
				else {
					create(job.target);
					messenger.send(s, Boolean.valueOf(true));
				}
			}
			else if(j instanceof Locate) {
				Locate job = (Locate) j;
				job.setMetadata(chunks, files);
				TreeMap<Chunk, ChunkMeta> reply = locate(job);
				messenger.send(s, reply);
			}
			else if(j instanceof Append) {
				Append job = (Append) j;
				if(!available(job.target)) {
					log.info("Cannot run job " + job);
					messenger.send(s, new TreeMap<>());
          return;
				}
        if(!canAppend(job)) {
					log.info("Creating additional chunk for file");
					createNext(job.target);
        }
        log.info("Sending locations for append request");
        Locate locate = new Locate(job.target);
        messenger.send(s, locate(locate));
			}
			else if(j instanceof Heartbeat) {
				Heartbeat job = (Heartbeat) j;
				job.setServerList(servers);
				job.setDeadServerList(deadServers);
				ServerMeta nowAvailable = job.call();
        if(nowAvailable != null) {
					log.info("Marking chunks available");
          markAvailable(nowAvailable);
        }
			}
			else if(j instanceof Read) {
				Read job = (Read) j;
				if(!canRead(job.target)) {
					log.info("Cannot run job " + job);
					messenger.send(s, new TreeMap<>());
				}
				else {
					log.info("Sending locations for read request");
					Locate locate = new Locate(job.target);
					messenger.send(s, locate(locate));
				}
			}

		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
	}


	private void create(File file) {
		Chunk firstChunk = new Chunk(file);
		FileMeta fileMeta = new FileMeta(file);
		fileMeta.addChunk(firstChunk);
		files.put(file, fileMeta);
		create(firstChunk);
	}

	private void create(Chunk chunk) {
		ChunkMeta chunkMeta = new ChunkMeta(chunk);
		Set<InetSocketAddress> servers = getServersForChunk(chunk);
		chunkMeta.setServers(servers);
		chunks.put(chunk, chunkMeta);

		Create job = new Create(chunk);
		log.info("Running " + job + " on servers: " + servers);
		try {
			for(InetSocketAddress server : servers) {
				Message<Create> msg = new Message(job, server);
				messenger.send(msg);
			}
		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
	}

	private void createNext(Chunk prev) {
		Chunk nextChunk = new Chunk(prev);
		create(nextChunk);
	}

	private void createNext(File file) {
    FileMeta fileMeta = files.get(file);
    Chunk prevChunk = fileMeta.getLastChunk();
		createNext(prevChunk);
	}

	private boolean available(File file) {
		Locate locate = new Locate(file);
		TreeMap<Chunk, ChunkMeta> chunkMeta = locate(locate);
		Chunk lastChunk = chunkMeta.lastKey();
		return chunkMeta.get(lastChunk).isAvailable();
	}

	private boolean canAppend(Append job) {
		Locate locate = new Locate(job.target);
		TreeMap<Chunk, ChunkMeta> chunkMeta = locate(locate);
		Chunk lastChunk = chunkMeta.lastKey();
		ChunkMeta lastMeta = chunks.get(lastChunk);
    if(lastChunk.getFreeSpace() < job.payload.length()) {
      return false;
    }
    else if(!lastMeta.isAvailable()) {
      return false;
    }
		return true;
	}

	private boolean canRead(File file) {
    if(!files.containsKey(file)) {
      return false;
    }
		Locate locate = new Locate(file);
		TreeMap<Chunk, ChunkMeta> chunkMeta = locate(locate);
		for(ChunkMeta cm : chunkMeta.values()) {
			if(!cm.isAvailable()) {
				return false;
			}
		}
		return true;
	}

	private TreeMap<Chunk, ChunkMeta> locate(Locate job) {
		job.setMetadata(chunks, files);
		return job.call();
	}

	private void replicate(Chunk chunk) {
		ChunkMeta chunkMeta = chunks.get(chunk);
		Set<InetSocketAddress> servers = getServersForChunk(chunk);
		chunkMeta.setServers(servers);
		chunks.put(chunk, chunkMeta);

		Create job = new Create(chunk);
		log.info("Running " + job + " on servers: " + servers);
		try {
			for(InetSocketAddress server : servers) {
				Message<Create> msg = new Message(job, server);
				messenger.send(msg);
			}
		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
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

  private void markUnavailable(ServerMeta serverMeta) {
    for(File file : files.keySet()) {
      FileMeta fileMeta = files.get(file);
      for(Chunk chunk : chunks.keySet()) {
        ChunkMeta chunkMeta = chunks.get(chunk);
        if(serverMeta.hasChunk(chunk)) {
          chunkMeta.markUnavailable();
        }
      }
    }
  }

  private void markAvailable(ServerMeta serverMeta) {
    for(File file : files.keySet()) {
      FileMeta fileMeta = files.get(file);
      for(Chunk chunk : chunks.keySet()) {
        ChunkMeta chunkMeta = chunks.get(chunk);
        if(serverMeta.hasChunk(chunk)) {
          chunkMeta.markAvailable();
        }
      }
    }
  }
}
