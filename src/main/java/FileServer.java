import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class FileServer extends AbstractImpl {
	private static final Logger log = LogManager.getLogger("FileServer");
	private static final String path = "file.txt";

	private static final BlockingQueue<Request> requests = new PriorityBlockingQueue<>();
	private static final Set<Integer> unsatisfied = new HashSet<>();

	/**
		* Constructs a file server identified by a given integer id.
		* The file server will append the line:
		*
		* 	"request from client_id, time"
		*
		* to `file.txt` upon receiving a request from a client.
		* It is expected that requesting clients obey a mutual exclusion
		* strategy such that only one client at a time will request a write.
		*
		* Upon receiving a SATISFIED message from each client the file server
		* will bring the distributed computation to an end by sending a SATISFIED
		* message to all other servers. It is expected that all other servers will
		* shut down on receipt of this message. Finally, this server will shut down.
		*
		* @param id	Unique positive integer identifying the client
		*
		* @throws IOException	If local socket could not be opened
		*/
	public FileServer() throws IOException{
		super(0);
		// All clients are unsatisfied at start
		for(int i = 1; i <= clients.length; i++){
			unsatisfied.add(i);
		}
	}

	// processes queued client requests and sends reply to client
	@Override
	public void consumer() {
		for(;;) {
			try {
				// Receive request, write to file.txt, send reply to client
				Request r = requests.take();
				write(r.origin, System.nanoTime(), r.timestamp);
				Satisfied s = new Satisfied(id, r.origin, System.nanoTime());
				String host = clients[s.dest-1];
				send(s, host);
			}
			catch(IOException ex) {
				log.error(ex.getMessage(), ex);
			}
			catch(InterruptedException ex) {
				break;
			}
		}
	}

	// Queue request for processing
	public void onRequest(Request msg){
		log.trace("Got request message from " + msg.origin);
		boolean ok = false;
		while(!ok) {
			ok = requests.offer(msg);
		}
	}

	// Note satisfied clients, handle termination when all clients satisfied
	public void onSatisfied(Satisfied msg){
		// Record the newly satisfied client
		log.trace("Got satisfied message from " + msg.origin);
		unsatisfied.remove(msg.origin);

		// If all clients satisfied, begin termination process
		if(unsatisfied.isEmpty()){
			log.info("Received termination signal from all clients");
			log.info("Printing file contents");

			// Print file.txt at conclusion
			try {
				printFile();
			}
			catch(IOException ex) {
				log.error(ex.getMessage(), ex);
			}

			// Notify servers of shutdown
			final long timestamp = System.nanoTime();
			for(int i = 1; i < servers.length; i++){
				Satisfied s = new Satisfied(id, i, System.nanoTime());
				try {
					log.trace("Sending termination signal to server " + i);
					send(msg, servers[i]);
				}
				catch(IOException ex){
					log.error(ex.getMessage(), ex);
				}
			}

			// Self terminate
			terminate();
		}
	}

	// Dummy abstract implementation, file server should not get RELEASEs
	public void onRelease(Release msg){
		log.warn("Got release message, nothing to do");
	}

	// Dummy abstract implementation, file server should not get GRANTs
	public void onGrant(Grant msg){
		log.warn("Got grant message, nothing to do");
	}

	// Writes the client id / timestamp to file.txt
	private static void write(int clientId, long t_recv, long t_sent) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(path, true));
		String line = String.format(
				"request from %d sent at %d, received at %d\n",
				clientId,
				t_sent,
				t_recv
		);
		bw.write(line);
		log.info("Wrote to " + path + " -> " + line);
		bw.flush();
		bw.close();
	}

	// Prints contents of file.txt
	private static void printFile() throws IOException {
		try (BufferedReader br = new BufferedReader(new FileReader("file.txt"))) {
			 String line = null;
			 System.out.println("File contents at termination:");
			 while ((line = br.readLine()) != null) {
					 System.out.println(line);
			 }
		}
	}
}
