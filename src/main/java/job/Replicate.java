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

	public Replicate(InetSocketAddress serverWithChunk, Chunk target) {
		this.target = target;
		this.serverWithChunk = serverWithChunk;
	}

	@Override
	public Void call() throws IOException {
		Request<Read, String> readChunk = new Request<>(0, 0, new Read(target));
		String chunkContent = readChunk.send(serverWithChunk);
		Append appendJob = new Append(target, chunkContent);
		appendJob.run();
	}

}
