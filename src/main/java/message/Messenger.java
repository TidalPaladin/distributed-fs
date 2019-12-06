import org.apache.logging.log4j.*;
import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class Messenger {
	protected static final Logger log = LogManager.getLogger("Messenger");

	public final InetSocketAddress returnAddr;

	public Messenger(InetSocketAddress returnAddr) {
		this.returnAddr = returnAddr;
	}

	public void send(Socket socket, Object msg) throws IOException {
		log.info("Sending object " + msg);
		ObjectOutputStream oos = null;
		oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(msg);
		oos.flush();
	}

	public void send(Socket socket, Message msg) throws IOException {
		log.info("Sending message " + msg);
		ObjectOutputStream oos = null;
		Message postmarked = new Message(msg, returnAddr);
		oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(postmarked);
		oos.flush();
	}

	public void send(Message msg) throws IOException {
		Socket s = new Socket();
		try {
			s.connect(msg.sendTo);
			send(s, msg);
		}
		finally {
			log.info("Closing socket");
			s.close();
		}
	}

	public <T> T send(Request<?, T> msg) throws IOException {
		Request<?,T> postmarked = new Request<>(msg, returnAddr);
		log.info("Sending request msg: " + postmarked);
		Socket s = new Socket();
		log.info("Connecting");
		s.connect(postmarked.sendTo);
		log.info("OOS");
		ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
		log.info("OIS");
		ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

		log.info("WRITE");
		oos.writeObject(postmarked);
		oos.flush();

		// Get reply
		log.info("Waiting for reply");

		try {
			Object replyObj = ois.readObject();
			return (T) replyObj;
		}
		catch(ClassNotFoundException ex) {
			log.error(ex.getMessage(), ex);
			return null;
		}
	}

	public static Message receive(Socket socket) throws IOException, ClassNotFoundException {
		log.info("Receiving connection");
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		Message msg = (Message) ois.readObject();
		ois.close();
		return msg;
	}
}
