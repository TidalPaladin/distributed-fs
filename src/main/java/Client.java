import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("Client");

	private static final BlockingQueue<InetSocketAddress>
		grants = new PriorityBlockingQueue<>();

	// Notified whenever S_0 replies to request
	private static final Object success = new Object();

	// Notified whenever S_0 replies to request
	private static final Scanner scanner =
		new Scanner(System.in);


	private final InetSocketAddress metaServer;
	private final Queue<Message> replies = new SynchronousQueue<>();

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
	public Client(int id, InetSocketAddress metaServer) throws IOException{
		super(id);
		this.metaServer = metaServer;
	}

	// Sends the 20 requests
	public void consumer() {
		for(int i = 1; i <= 20; i++) {
			try {

				/* Sleep random seconds before request*/
				log.info("Sleeping before sending request");
				sleep(requestLow, requestHigh);

				final int initialSendCount = sendCount,
					initialReceiveCount = receiveCount;

				log.info("Sending request " + i);

				/* Randomly pick server quorum */
				int initialRoot = 1;
				Set<Integer> quorum = getQuorum(initialRoot, r.nextLong());
				System.out.println("Using quorum: " + quorum);

				/* Send request to each server in quorum */
				long timestamp = System.nanoTime();
				for(Integer serverId : quorum) {
					String host = servers[serverId];
					Request req = new Request(id, serverId, timestamp);
					log.debug("Sending request to " + host);
					send(req, servers[req.dest]);
				}

				/* Wait for all GRANTS from quorum */
				Set<Integer> grants = new HashSet<>();
				while(grants.size() < quorum.size()) {
					grants.add(Client.grants.take());
					log.trace("Have quorum replies: " + grants);
					log.trace("Need: " + quorum);
				}
				log.debug("Received GRANT from all severs");

				/* Print total message counts and delay */
				final long elapsed = System.nanoTime() - timestamp;
				int delta = sendCount - initialSendCount;
				delta += (receiveCount - initialReceiveCount);
				System.out.println("Exchanged " + delta + " messages to obtain this lock");
				System.out.println("Took " + elapsed / 1e6 + "ms to obtain this lock");

				/* Send request to S0 */
				Request req = new Request(id, S0, System.nanoTime());
				send(req, servers[req.dest]);

				/* Wait to be notified of reply from S0 */
				log.info("Sent request to S0, waiting for reply");
				synchronized(success) {
					success.wait();
				}
				log.info("Got reply from S0");

				/* Sleep random range of seconds before release */
				log.info("Sleeping before sending release");
				sleep(releaseLow, releaseHigh);

				/* Send release to quorum */
				for(Integer serverId : quorum) {
					String host = servers[serverId];
					Release rel = new Release(id, serverId, System.nanoTime());
					log.debug("Sending release to " + host);
					send(rel, servers[rel.dest]);
				}
			}
			catch(IOException | InterruptedException ex) {
				log.error(ex.getMessage(), ex);
			}
		}

		/* Finished all reqeusts. Notify S0 and close socket */
		log.info("Finished all 20 requests");
		long timestamp = System.nanoTime();
		Satisfied req = new Satisfied(id, S0, timestamp);
		try {
			send(req, servers[req.dest]);
			log.info("Sent termination signal to S0");
			socket.close();
		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
		terminate();
	}

	public void menu() {
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
				String result = makeRequest(smd, file, payload);
				System.out.println();
				System.out.print("Result:\n" + result);
			}
			catch (Exception ex) {
				System.out.println(ex.getMessage());
			}
		}
	}

	public String makeRequest(String cmd, File file, String payload) {
		if(cmd.equalsIgnoreCase("create")) {
			Create msg = new Create(id, 0, System.nanoTime(), file);
			send(msg, metaServer);
			Message reply = replies.take();
			if(reply instanceof Create) {
				return "Created " + file;
			}
			else {
				return "Error creating " + file;
			}
		}
		else if(cmd.equalsIgnoreCase("read")) {
			Locate locateReq = new Locate(id, 0, System.nanoTime(), file);
			send(msg, metaServer);
			Message reply = replies.take();
			if(reply instanceof Unavailable) {
				return "Chunks for file " + file + " are unavailable";
			}

			Map<Chunk, Set<InetSocketAddress>> locations =
				((Locate) reply).getLocations();

			StringBuilder sb = new StringBuilder();
			for(Chunk chunk : locations.keySet()) {
				InetSocketAddress server = locations.get(chunk).iterator().next();
				Read msg = new Read(id, 0, System.nanoTime(), file);


			}




				/* Wait for all GRANTS from quorum */
				Set<Integer> grants = new HashSet<>();
				while(grants.size() < quorum.size()) {
					grants.add(Client.grants.take());
					log.trace("Have quorum replies: " + grants);
					log.trace("Need: " + quorum);
				}
				log.debug("Received GRANT from all severs");
		}
		else if(cmd.equalsIgnoreCase("append")) {
			Append msg = new Read(id, 0, System.nanoTime(), file, payload);
			send(msg, metaServer);
		}
	}

	@Override
	public void onMessage(Message msg) {
		replies.put(msg);
	}
}
