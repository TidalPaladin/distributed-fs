import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("Client");
	private static final Scanner scanner = new Scanner(System.in);

	private final InetSocketAddress metaServer;
	private final BlockingQueue<Message> replies = new SynchronousQueue<>();

	private final Thread menu;

	/**
	 * Constructs a client identified by a given integer id. Clients
	 * will make 20 requests, using a unique quorum to obtain a lock
	 * for each request. Once permission is obtained from the quorum,
	 * client will request server S0 to perform the file operation.
	 *
	 * @param id	Unique positive integer identifying the client
	 *
	 * @throws IOException	If local socket could not be opened
	 */
	public Client(InetSocketAddress addr, InetSocketAddress metaServer) throws IOException{
		super(addr);
		this.metaServer = metaServer;
		menu = new Thread(new Menu());
		menu.setDaemon(true);
	}

	@Override
	public void run() {
	   Future future = service.submit(new Menu());
	   super.run();
	   future.get();
	}

	private class Menu implements Runnable {
		@Override
		public void run() {
			for(;;) {
				System.out.println("\n");
				System.out.println("Usage:");
				System.out.println("create <filename>");
				System.out.println("read <filename>");
				System.out.println("append <filename> <content>");
				System.out.print("> ");
				scanner.reset();

				try {
					String cmd = scanner.next();

					if(!scanner.hasNext()) {
						throw new IllegalArgumentException("incorrect usage");
					}
					File file = new File(scanner.next());

					String payload = null;
					if(cmd.equalsIgnoreCase("append")) {
						if(!scanner.hasNextLine()) {
							throw new IllegalArgumentException("incorrect usage");
						}
						payload = scanner.nextLine();
						if(payload.length() > MAX_APPEND) {
							String msg = "append len must be <= " + MAX_APPEND;
							throw new IllegalArgumentException(msg);
						}
					}
					String result = makeRequest(cmd, file, payload);
					System.out.println();
					System.out.print("Result:\n" + result);
				}
				catch (Exception ex) {
					log.error(ex.getMessage(), ex);
				}
			}
		}
	}


	public String makeRequest(String cmd, File file, String payload) throws IOException, InterruptedException {
		if(cmd.equalsIgnoreCase("create")) {
			return create(file);
		}
		else if(cmd.equalsIgnoreCase("read")) {
			return read(file);
		}
		else if(cmd.equalsIgnoreCase("append")) {
			return append(file, payload);
		}
		else {
			throw new IllegalArgumentException("invalid operation");
		}
	}


	public TreeMap<Chunk, ChunkMeta> locate(File file) throws IOException, InterruptedException {
		Locate job = new Locate(file);
		Request<Locate, TreeMap<Chunk, ChunkMeta>> msg = new Request<>(job, metaServer, returnAddr);
		TreeMap<Chunk, ChunkMeta> reply = msg.send();
		if(reply == null) {
			log.error("Failed to locate all chunks for " + file);
			throw new IOException("not all chunks were available");
		}
		return reply;
	}

	public String create(File file) throws IOException, InterruptedException {
		log.info("Started create job");
		Create job = new Create(file);
		Request<Create, Boolean> msg = new Request<>(job, metaServer, returnAddr);
		boolean success = msg.send();

		if(success) {
			return "File creation registered with metadata server.";
		}
		else {
			throw new IOEXception("Metaserver failed to create file");
		}
	}

	public String append(File file, String payload) throws IOException, InterruptedException {
		log.info("Started append job for file " + file);

		Message<AppendFile> locationRequest = new Message<>(id, -1, new AppendFile(file, payload));
		locationRequest.setDestAddress(metaServer);
		locationRequest.setReturnAddress(returnAddr);
		send(locationRequest);
		Message<Job> reply = buffer.take(Job.Type.REPLY);
		if(!(reply.job instanceof Locate)) {
			log.info("Metaserver replied with abort");
			return "Append failed, metaserver instructed to abort";
		}

		Locate locate = (Locate) reply.job;
		Chunk lastChunk = locate.meta.lastKey();
		ChunkMeta lastChunkMeta = locate.meta.get(lastChunk);

		// Phase one
		boolean success = true;
		Message<Append> phaseOne = new Message<>(id, -1, new Append(lastChunk, payload));
		for(InetSocketAddress server : lastChunkMeta.getServers()) {
			log.info("Requesting append on " + server);
			send(phaseOne, server);
			reply = buffer.take(Job.Type.REPLY);
			Job job = reply.job;
			if(job instanceof Abort) {
				log.info("Server aborted: " + server);
				success = false;
				break;
			}
		}

		// Phase two
		Message phaseTwo = null;
		if(success) {
			phaseTwo = new Message<>(id, -1, new Commit(phaseOne.job));
			log.info("Prepared commit message for phase two");
		}
		else {
			phaseTwo = new Message<>(id, -1, new Abort(phaseOne.job));
			log.info("Prepared abort message for phase two");
		}

		for(InetSocketAddress server : lastChunkMeta.getServers()) {
			log.debug("Sending phase two message to " + server);
			send(phaseTwo, server);
		}

		if(success) {
			return "Append success";
		}
		else {
			return "Received abort during commit";
		}
	}

}
