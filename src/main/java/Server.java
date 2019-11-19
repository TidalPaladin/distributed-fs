import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Server extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("Server");

	private static final BlockingQueue<Request> requests = new PriorityBlockingQueue<>();
	private static final BlockingQueue<Release> releases = new SynchronousQueue<>();


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
	public Server(int id) throws IOException{
		super(id);
	}

	// Grant the highest priority request and wait for release
	@Override
	public void consumer() {
		while(!socket.isClosed()) {
			try {
				/* Send grant to highest priority request */
				Request r = requests.take();
				Grant grant = new Grant(id, r.origin, System.nanoTime());
				String host = clients[grant.dest-1];
				log.info("Sending grant to " + host);
				send(grant, host);

				/* Wait for release */
				releases.take();
				log.info("Got release from " + host);
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

	// Put requests into priority queue
	public void onRequest(Request msg){
		log.trace("Got request message from " + msg.origin);
		boolean ok = false;
		while(!ok) {
			ok = requests.offer(msg);
		}
	}

	// Alert consumer upon receiving RELEASE from GRANTed client
	public void onRelease(Release msg){
		log.trace("Got release message from " + msg.origin);
		boolean ok = false;
		while(!ok) {
			ok = releases.offer(msg);
		}
	}

	// Dummy abstract implementation, server should not get GRANTs
	public void onGrant(Grant msg){
		log.warn("Got grant message, nothing to do");
	}

	public void onSatisfied(Satisfied msg){
		log.trace("Got satisfied message, terminating...");
		terminate();
	}
}
