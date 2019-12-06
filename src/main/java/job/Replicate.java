import java.net.InetSocketAddress;
import java.io.IOException;
import java.net.Socket;
import java.io.File;
import java.util.TreeMap;
import java.util.Set;
import java.util.concurrent.Callable;
import java.io.IOException;

class Replicate extends Job<Void> {

	public final Chunk target;
	public final InetSocketAddress serverWithChunk;
	private Messenger messenger;

	public Replicate(InetSocketAddress serverWithChunk, Chunk target) {
		this.target = target;
		this.serverWithChunk = serverWithChunk;
	}

	public void setMessenger(Messenger messenger) {
		this.messenger = messenger;
	}

	@Override
	public Void call() throws IOException {
		Request<Read, String> msg = new Request<>(new Read(target), serverWithChunk);
		String payload = messenger.send(msg);
		target.createNewFile();

		ChunkWriter cw = new ChunkWriter(target);
		cw.append(payload);
		if(target.getPadding() > 0) {
			cw.pad();
		}
		return null;
	}

}
