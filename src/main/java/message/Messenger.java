import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class Messenger {

	public final InetSocketAddress returnAddr;

	public Messenger(InetSocketAddress returnAddr) {
		this.returnAddr = returnAddr;
	}

	public void send(Socket socket, Object msg) throws IOException {
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(msg);
			oos.flush();
		}
		finally {
			oos.close();
		}
	}

	public void send(Socket socket, Message msg) throws IOException {
		ObjectOutputStream oos = null;
		try {
			Message postmarked = new Message(msg, returnAddr);
			oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(postmarked);
			oos.flush();
		}
		finally {
			oos.close();
		}
	}

	public void send(Message msg) throws IOException {
		Socket s = new Socket();
		try {
			s.connect(msg.sendTo);
			send(s, msg);
		}
		finally {
			s.close();
		}
	}

	public <T> T send(Request<?, T> msg) throws IOException {
		Socket s = new Socket();
		ObjectInputStream ois = null;
		T reply = null;
		try {
			// Send request
			s.connect(msg.sendTo);
			send(s, msg);

			// Get reply
			ois = new ObjectInputStream(s.getInputStream());
			Object replyObj = ois.readObject();
			reply = (T) replyObj;
		}
		catch(ClassNotFoundException ex) {
		}
		finally {
			s.close();
			return reply;
		}
	}

	public static Message receive(Socket socket) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		Message msg = (Message) ois.readObject();
		ois.close();
		return msg;
	}
}
