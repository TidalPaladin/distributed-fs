import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Client extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("Client");
	private static final Random r = new Random();

	// Index for server 0
	private static final int S0 = 0;

	// Delay values read from env vars
	private static final int
		requestLow = Integer.parseInt(System.getenv("REQ_LOW")),
		requestHigh = Integer.parseInt(System.getenv("REQ_HIGH")),
		releaseLow = Integer.parseInt(System.getenv("REL_LOW")),
		releaseHigh = Integer.parseInt(System.getenv("REL_HIGH"));

	private static final BlockingQueue<Integer> grants = new PriorityBlockingQueue<>();

	// Notified whenever S_0 replies to request
	private static final Object success = new Object();

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
	public Client(int id) throws IOException{
		super(id);
		log.info("Request delay: " + requestLow + " to " + requestHigh);
		log.info("Release delay: " + releaseLow + " to " + releaseHigh);
	}

	/**
	 * Selects a random quorum of servers
	 *
	 * @param root	The subscript of the server that is the root node
	 * 												of the binary tree. E.g. root=2 for the subtree rooted
	 * 												at S_2.
	 * 												pre: Must be on the interval [1, 7]
	 *
	 * @param seed	Random seed value for quorum selection. A new seed will be
	 * 												generated for recursive getQuorum calls.
	 *													post: this.r will have a changed seed state
	 *
	 * @return 	The set of server indices representing the selected quorum.
	 * 									I.e {1, 3, 5} for a quorum of servers 1, 3, and 5
	 */
	private static Set<Integer> getQuorum(int root, long seed) {
		if(root <= 0 || root > 7) {
			throw new IllegalArgumentException("must have 0 < root <= 7");
		}

		// Case iv - leaf node; return a quorum of just this node
		final boolean isLeaf = 2 * root >= servers.length;
		if(isLeaf) {
			Set<Integer> result = new HashSet<>();
			result.add(root);
			return result;
		}

		// Choose random case i-iii and a new seed for recursion
		Set<Integer> result = null;
		r.setSeed(seed);
		long newSeed = r.nextLong();
		switch(r.nextInt(3)+1) {
			// Root + left subtree
			case 1:
				result = getQuorum(2*root, newSeed);
				result.add(root);
				break;

			// Root + right subtree
			case 2:
				result = getQuorum(2*root+1, newSeed);
				result.add(root);
				break;

			// left + right subtrees
			case 3:
				result = getQuorum(2*root, newSeed);
				result.addAll(getQuorum(2*root+1, newSeed));
				break;
		}
		return result;
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

	// Noop, clients dont get requests
	public void onRequest(Request msg){
		log.warn("Got request message, nothing to do");
	}

	// Noop, clients dont get release
	public void onRelease(Release msg){
		log.warn("Got release message, nothing to do");
	}

	// Notify waiting thread of a new grant
	public void onGrant(Grant msg){
		log.trace("Got grant message");
		try {
			grants.put(msg.origin);
		}
		catch(InterruptedException ex) {
			log.error(ex);
		}
	}

	// Notify waiting thread we received result from S_0
	public void onSatisfied(Satisfied msg){
		log.trace("Got satisfied message");
		synchronized(success){
			success.notify();
		}
	}

	// Sleeps a random number of seconds on the interval [low, high]
	private void sleep(int low, int high){
		int sleep = r.nextInt(high - low +1) + low;
		log.info("Sleeping " + sleep + "s");
		try {
			Thread.sleep(sleep*1000);
		}
		catch(InterruptedException ex) {
			log.debug("Interrupted while sleeping");
		}
	}
}
