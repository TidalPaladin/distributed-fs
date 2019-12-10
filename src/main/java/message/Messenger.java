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
		log.debug("Sending object " + msg + " on " + socket);
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(msg);
		oos.flush();
	}

	public void send(Socket socket, Message msg) throws IOException {
		Message postmarked = new Message(msg, returnAddr);
		send(socket, (Object) postmarked);
	}

	public void send(Message msg) throws IOException {
		try (
			Socket s = new Socket();
		) {
			s.connect(msg.sendTo);
			send(s, msg);
			s.close();
		}
	}

	public <T> T send(Request<?, T> msg) throws IOException {
		Request<?,T> postmarked = new Request<>(msg, returnAddr);
		try (
			Socket s = new Socket();
		) {
			s.connect(postmarked.sendTo);
			send(s, postmarked);
			Object replyObj = receiveObject(s);
			return (T) replyObj;
		}
	}

	public static Message receive(Socket socket) throws IOException {
		return (Message) receiveObject(socket);
	}

	public static Object receiveObject(Socket socket) throws IOException {
		log.debug("Receiving connection");
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		try {
			return ois.readObject();
		}
		catch(ClassNotFoundException ex) {
			log.error(ex.getMessage(), ex);
			return null;
		}
	}
}
