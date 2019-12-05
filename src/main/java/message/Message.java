import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class Message<T extends Job> implements Comparable<Message<?>>, Serializable {

	/* Separator for multi-element payloads */
	public static final char SEPARATOR = '\n';
	public static final int SIZE = 1000;

	/* Immutable message attributes */
	public final long timestamp;
	public final T job;

	private InetSocketAddress sendTo, returnTo;
	private UUID uuid, replyToUUID;

	public Message(T job, InetSocketAddress to, InetSocketAddress from) {
		this.timestamp = System.nanoTime();
		this.job = job;
		this.uuid = UUID.randomUUID();
		this.sendTo = to;
		this.returnTo = from;
	}

	public Message(T job) {
		this(job, null, null);
	}

	public void setDestAddress(InetSocketAddress addr) {
		this.sendTo = addr;
	}

	public void setReturnAddress(InetSocketAddress addr) {
		this.returnTo = addr;
	}

	public InetSocketAddress getDestAddress() {
		return this.sendTo;
	}

	public InetSocketAddress getReturnAddress() {
		return this.returnTo;
	}

	public void send(Socket socket) throws IOException {
		// Serialize message into byte array
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(this);
		oos.flush();
		oos.close();
	}

	public void send(InetSocketAddress addr) throws IOException {
		Socket s = new Socket();
		s.connect(addr);
		send(s);
		s.close();
	}

	public void send() throws IOException {
		if(sendTo == null) {
			throw new IllegalStateException("sendTo address was not assigned");
		}
		send(this.sendTo);
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
		return this.uuid - other.uuid;
	}

	// Helper function
	public boolean isHigherPriorityThan(Message other) {
		return this.compareTo(other) < 0;
	}

	public <E extends Job> Message<E> getReply(E job) {
		Message<E> reply = new Message<>(job);
		reply.sendTo = returnTo;
		reply.returnTo = sendTo;
		reply.replyToUUID = uuid;
		return reply;
	}

	public Message<T> getReply() {
		return getReply(this.job);
	}

	public boolean isReplyTo(Message other) {
		if(!isReply()) {
			return false;
		}
		return replyToUUID.equals(other.uuid);
	}

	public boolean isReply() {
		return replyToUUID != null;
	}

	@Override
	public String toString() {
		return String.format("Message(%s)", uuid);
	}
}
