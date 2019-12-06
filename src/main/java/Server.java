import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Server extends Node {
	protected static final Logger log = LogManager.getLogger("Server");

	protected InetSocketAddress metaServer;
	private final Set<Chunk> chunks;

	public Server(InetSocketAddress returnAddr, InetSocketAddress metaServer) throws IOException {
		super(returnAddr);
		this.metaServer = metaServer;
		this.chunks = new TreeSet<Chunk>();
	}

	@Override
	public void run() {
		super.run();
		while(!socket.isClosed()) {
			try {
				log.info("Sending heartbeat message");
				Message<Heartbeat> msg = new Message(new Heartbeat(returnAddr, chunks), metaServer);
				messenger.send(msg);
				Thread.sleep(Metaserver.HEARTBEAT_FREQ * 1000);
			}
			catch(Exception ex) {
				log.error(ex.getMessage(), ex);
			}
		}
	}

	public void onMessage(Socket s) {
		try {
			Message msg = messenger.receive(s);
			Job j = msg.job;
			log.info("Got message: " + j.toString());

			if(j instanceof Commit) {
				Commit job = (Commit) j;
				job.call();
			}
			else if(j instanceof Create) {
				Create job = (Create) j;
				job.call();
				chunks.add((Chunk) job.target);
			}
			else if(j instanceof Replicate) {
				Replicate job = (Replicate) j;
				job.setMessenger(messenger);
				job.call();
			}
			else if(j instanceof Read) {
				Read job = (Read) j;
				String payload = job.call();
				messenger.send(s, payload);
			}
			else if(j instanceof Append) {
				Append job = (Append) j;
				Chunk chunk = (Chunk) job.target;
				String payload = job.payload;

				ChunkWriter cw = new ChunkWriter(chunk);
				if(cw.canWrite(payload)) {
					Job reply = new Commit(job);
					messenger.send(s, reply);
				}
				else {
					Job reply = new Abort(job);
					messenger.send(s, reply);
				}
			}
		}
		catch(Exception ex) {
			log.error(ex.getMessage(), ex);
		}

	}

	public String read(File file) throws IOException, InterruptedException {
		return read(file, metaServer);
	}

	public boolean append(File file, String payload) throws IOException, InterruptedException {
		return append(file, payload, metaServer);
	}

	public TreeMap<Chunk, ChunkMeta> locate(File file) throws IOException, InterruptedException {
		return locate(file, metaServer);
	}
}
