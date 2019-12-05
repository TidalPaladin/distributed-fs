import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AbstractImpl implements Runnable {
	protected static final Logger log = LogManager.getLogger("AbstractImpl");

	protected static final int
		PORT = 32000,
		MAX_APPEND = 1024;

	protected final ServerSocket socket;
	protected InetSocketAddress returnAddr;

	protected ExecutorService service = Executors.newFixedThreadPool(10);

	public AbstractImpl(InetSocketAddress returnAddr) throws IOException {
		socket = new ServerSocket(PORT);
		this.returnAddr = returnAddr;
	}

	@Override
	public void run() {
	   Future future = service.submit(new SocketThread());
	   future.get();
	}

	public abstract void onMessage(Socket s);

	// Producer task. Reads messages from socket and runs callbacks
	private class SocketThread implements Runnable {
		@Override
		public void run() {
			while(!socket.isClosed()) {
				Socket s = null;
				try {
					s = socket.accept();
					log.debug("Accepted socket connection");
					onMessage(s);
				}
				catch(SocketException ex) {
				}
				catch(IOException | InterruptedException | ClassNotFoundException ex) {
					log.error(ex.getMessage(), ex);
				}
				finally {
					try {
						if(s != null && !s.isClosed()) s.close();
					}
					catch(IOException ex) {}
				}
			}
		}
	}

	public String read(File file) throws IOException, InterruptedException {
		log.info("Started read job");
		Read job = new Read(file);
		Request<Create, TreeMap<Chunk, ChunkMeta>> msg = new Request<>(job, metaServer, returnAddr);
		TreeMap<Chunk, ChunkMeta> locations = msg.send();
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
		Request<Read, String> msg = new Message<>(new Read(chunk), server, returnAddr);
		log.info("Requesting " + chunk);
		String payload = msg.send();
		return payload;
	}

	// Handles termination, interrupting daemon threads and closing sockets
	protected void terminate() {
		log.trace("Terminating...");
		try {
			log.info("Closed socket");
			socket.close();
			for(Thread t : threads) {
				t.interrupt();
			}
		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
		log.trace("Finished termination job");
	}
}
