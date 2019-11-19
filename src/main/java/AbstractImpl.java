import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;

public abstract class AbstractImpl implements Runnable {
	protected static final Logger log = LogManager.getLogger("AbstractImpl");

	protected static final int port = 32000;
	protected static String[] servers, clients;

	protected final ServerSocket socket;
	protected final int id;
	protected int sendCount, receiveCount;

	private final Thread prodThread, consThread;

	public AbstractImpl(int id) throws IOException {
		socket = new ServerSocket(port);
		this.id = id;
		prodThread = new Thread(new Producer());
		consThread = new Thread(new Consumer());
		prodThread.setDaemon(true);
		consThread.setDaemon(true);
		sendCount = 0;
		receiveCount = 0;
	}

	/* Runnable, starts producer and consumer threads */
	@Override
	public void run() {
		prodThread.start();
		consThread.start();
		try {
			prodThread.join();
			consThread.join();
		}
		catch(InterruptedException ex){
		}
	}

	// Sends a message to the given host
	public void send(Message msg, String host) throws IOException {
		if(msg == null) {
			throw new IllegalArgumentException("msg should not be null");
		}
		else if(host == null){
			throw new IllegalArgumentException("host should not be null");
		}

		Socket s = new Socket(host, port);
		msg.send(s);
		s.close();
		sendCount++;
	}

	// Override this method to generate outgoing messages as needed
	protected abstract void consumer();

	private class Consumer implements Runnable {
		@Override
		public void run() { consumer(); }
	}

	// Producer task. Reads messages from socket and runs callbacks
	private class Producer implements Runnable {
		@Override
		public void run() {
			log.debug("Listening for incoming messages");
			while(!socket.isClosed()) {
				Socket s = null;
				try {
					s = socket.accept();
					log.trace("Accepted socket connection");
					Message msg =  Message.receive(s);
					onMessage(msg);
				}
				catch(SocketException ex) {
				}
				catch(IOException | ClassNotFoundException ex) {
					log.error(ex.getMessage(), ex);
				}
				finally {
					try { if(s != null && !s.isClosed()) s.close(); } catch(IOException ex) {}
				}
			}
		}
	}

	// Called upon receiving any message. Calls the appropriate callback
	public void onMessage(Message msg) {
		receiveCount++;
		if(msg instanceof Request) {
			onRequest((Request) msg);
		}
		else if(msg instanceof Release) {
			onRelease((Release) msg);
		}
		else if(msg instanceof Grant) {
			onGrant((Grant) msg);
		}
		else if(msg instanceof Satisfied) {
			onSatisfied((Satisfied) msg);
		}
	}

	// Assigns the array of server hostnames
	public static void setServers(String[] hosts) {
		if(hosts == null) {
			throw new IllegalArgumentException("hosts must not be null");
		}
		servers = hosts;
	}

	public static void setClients(String[] hosts) {
		if(hosts == null) {
			throw new IllegalArgumentException("hosts must not be null");
		}
		clients = hosts;
	}

	// Called upon receiving a REQUEST message
	protected abstract void onRequest(Request msg);

	// Called upon receiving a RELEASE message
	protected abstract void onRelease(Release msg);

	// Called upon receiving a GRANT message
	protected abstract void onGrant(Grant msg);

	// Called upon receiving a SATISFIED message
	protected abstract void onSatisfied(Satisfied msg);


	// Handles termination, interrupting daemon threads and closing sockets
	protected void terminate() {
		log.trace("Terminating...");
		try {
			log.info("Closed socket");
			socket.close();
			prodThread.interrupt();
			consThread.interrupt();
		}
		catch(IOException ex) {
			log.error(ex.getMessage(), ex);
		}
		log.trace("Finished termination job");
	}

	// Prints send and receive counts
	public void summary() {
		System.out.println("Total sent: " + sendCount);
		System.out.println("Total received: " + receiveCount);
	}

}

// Subclasses for various message types
class Request extends Message {
	Request(int id, int dest, long timestamp) { super(id, dest, timestamp); }
}
class Grant extends Message {
	Grant(int id, int dest, long timestamp) { super(id, dest, timestamp); }
}
class Release extends Message {
	Release(int id, int dest, long timestamp) { super(id, dest, timestamp); }
}
class Satisfied extends Message {
	Satisfied(int id, int dest, long timestamp) { super(id, dest, timestamp); }
}
