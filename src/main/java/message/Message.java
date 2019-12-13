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
	public final InetSocketAddress sendTo, returnTo;

	private UUID uuid;

	public Message(T job, InetSocketAddress to, InetSocketAddress from) {
		this.timestamp = System.nanoTime();
		this.job = job;
		this.uuid = UUID.randomUUID();
		this.sendTo = to;
		this.returnTo = from;
	}

	public Message(T job, InetSocketAddress to) {
		this(job, to, null);
	}

	public Message(Message<T> other, InetSocketAddress from) {
		this(other.job, other.sendTo, from);
	}

	/* Compares messages based on timestamp / PID */
	@Override
	public int compareTo(Message other) {
		if(this.timestamp != other.timestamp) {
			return (int)(this.timestamp - other.timestamp);
		}
		return this.uuid.compareTo(other.uuid);
	}

	// Helper function
	public boolean isHigherPriorityThan(Message other) {
		return this.compareTo(other) < 0;
	}

	@Override
	public String toString() {
		return String.format("Message(%s)", uuid);
	}
}
