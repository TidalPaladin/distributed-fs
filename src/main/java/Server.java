import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.UUID;

public class Server extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("Server");
	public static final int HEARTBEAT_FREQ = 5;

	private final InetSocketAddress metaServer;
	private final Thread heartbeat, request;
	private final Set<Chunk> chunks;


	/**
	 * Constructs a server identified by a given integer id. A server
	 * will respond to the highest priority request received from any
	 * client, and will become locked until the GRANTed client responds
	 * with a RELEASE. For file server functionality (S0), see the
	 * FileServer class.
	 *
	 * @param id	Unique positive integer identifying the client
	 *
	 * @throws IOException	If local socket could not be opened
	 */
	public Server(int id, InetSocketAddress addr, InetSocketAddress metaServer) throws IOException {
		super(id, addr);
		this.metaServer = metaServer;
		this.chunks = new TreeSet<Chunk>();

		heartbeat = new Thread(new HeartbeatJob());
		heartbeat.setDaemon(true);
		threads.add(heartbeat);

		request = new Thread(new RequestHandler());
		request.setDaemon(true);
		threads.add(request);
	}


	private class HeartbeatJob implements Runnable {
		@Override
		public void run() {
			while(!socket.isClosed()) {
				try {
					log.debug("Sending heartbeat message");
					Message<Heartbeat> msg = new Message(id, 0, new Heartbeat(chunks));
					send(msg, metaServer);
					Thread.sleep(HEARTBEAT_FREQ * 1000);
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

	private class RequestHandler implements Runnable {
		@Override
		public void run() {
			while(!socket.isClosed()) {
				try {
					Message msg = buffer.take(Job.Type.REQUEST);
					if(msg.job instanceof CreateChunk) {
						log.info("Got create chunk request");
						CreateChunk job = (CreateChunk) msg.job;
						create(job.chunk);
						Message<CreateChunk> reply = msg.getReply(job);
						send(reply);
					}
					else if(msg.job instanceof Read) {
						log.info("Got read request");
						Read job = (Read) msg.job;
						String payload = read(job.chunk);
						Read result = new Read(job.chunk, payload);
						Message<Read> reply = msg.getReply(result);
						send(reply);
					}
					else if(msg.job instanceof Append) {
						log.info("Got append request");
						Append job = (Append) msg.job;
						Message<Commit> reply1 = msg.getReply(new Commit(job));
						send(reply1);
						log.info("Sent phase 1 commit message");
						msg = buffer.take(Job.Type.REPLY);
						if(msg.job instanceof Commit) {
							log.info("Received phase 2 commit message");
							append(job.chunk, job.payload);
						}
					}
					else if(msg.job instanceof Replicate) {
						log.info("Got replicate job");
						Replicate job = (Replicate) msg.job;
						replicate(job.chunk, job.from);
						Message<Replicate> reply = msg.getReply();
						send(reply);
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

	public void create(Chunk chunk) throws IOException {
		chunk.getFile().createNewFile();
		chunks.add(chunk);
		log.info("Created chunk " + chunk);
	}


	public String read(Chunk chunk) throws IOException {
		String result = chunk.read();
		log.info("Read chunk " + chunk);
		return result;
	}

	public void append(Chunk chunk, String payload) throws IOException {
		chunk.append(payload);
		log.info("Wrote to chunk: " + chunk);
	}

	public void replicate(Chunk chunk, InetSocketAddress source) throws IOException, InterruptedException {
		log.trace("Started replicate job");
		Message<Read> msg = new Message<>(id, -1, new Read(chunk));
		send(msg, source);

		Message<Job> reply = buffer.take(Job.Type.REPLY);
		Read replyJob = (Read) reply.job;

		Chunk newChunk = replyJob.chunk;
		String payload = replyJob.payload;
		create(newChunk);
		append(newChunk, payload);
	}
}
