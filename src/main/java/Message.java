import java.io.*;
import java.net.Socket;
import java.net.MulticastSocket;
import java.net.SocketAddress;

public abstract class Message implements Comparable<Message>, Serializable {

	/* Separator for multi-element payloads */
	public static final char SEPARATOR = '\n';
	public static final int SIZE = 1000;

	/* Immutable message attributes */
	public final long timestamp;
	public final int origin, dest;

	public Message(int origin, int dest, long timestamp) {
		this.timestamp = timestamp;
		this.origin = origin;
		this.dest = dest;
	}

	public void send(Socket socket) throws IOException {
		// Serialize message into byte array
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(this);
		oos.flush();
		oos.close();
	}

	public static Message receive(Socket socket) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		Message msg = (Message) ois.readObject();
		ois.close();
		return msg;
	}

	/* Compares messages based on timestamp / PID */
	@Override
	public int compareTo(Message other) {
		if(this.timestamp != other.timestamp) {
			return (int)(this.timestamp - other.timestamp);
		}
		return this.origin - other.origin;
	}
	// Helper function
	public boolean isHigherPriorityThan(Message other) {
		return this.compareTo(other) < 0;
	}
}
