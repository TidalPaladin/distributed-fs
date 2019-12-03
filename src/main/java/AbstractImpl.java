import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;

public abstract class AbstractImpl implements Runnable {
	protected static final Logger log = LogManager.getLogger("AbstractImpl");

	protected static final int
		PORT = 32000;
		MAX_APPEND = 1024;

	protected final ServerSocket socket;
	protected final int id;
	private final Thread prodThread, consThread;

	public AbstractImpl(int id) throws IOException {
		socket = new ServerSocket(PORT);
		this.id = id;
		prodThread = new Thread(new Producer());
		consThread = new Thread(new Consumer());
		prodThread.setDaemon(true);
		consThread.setDaemon(true);
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
	public void send(Message msg, InetSocketAddress host) throws IOException {
		if(msg == null) {
			throw new IllegalArgumentException("msg should not be null");
		}
		else if(host == null){
			throw new IllegalArgumentException("host should not be null");
		}

		Socket s = new Socket(host);
		msg.send(s);
		s.close();
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
					try {
						if(s != null && !s.isClosed()) s.close();
					}
					catch(IOException ex) {}
				}
			}
		}
	}

	// Called upon receiving any message
	public abstract void onMessage(Message msg);

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
}
