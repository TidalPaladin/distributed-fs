import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class Request<ReqestT extends Job, ReplyT> extends Message<RequestT> {

	@Override
	public Message<ReplyT> send(InetSocketAddress addr) throws IOException {
		Socket s = null;
		ReplyT reply;
		try {
			Socket s = new Socket();
			s.connect(addr);
			send(s);

			// Get reply
			ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
			Object replyObj = ois.readObject();

			ReplyT reply = (ReplyT) replyObj;
		}
		catch(IOException | ClassNotFoundException ex) {
			throw ex;
		}
		finally {
			ois.close();
			s.close();
			return reply;
		}
	}

	@Override
	public Message<ReplyT> send() throws IOException {
		if(sendTo == null) {
			throw new IllegalStateException("sendTo address was not assigned");
		}
		return send(this.sendTo);
	}
}
