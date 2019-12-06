import org.apache.logging.log4j.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Client extends Node {
	protected static final Logger log = LogManager.getLogger("Client");

	protected InetSocketAddress metaServer;

	public Client(InetSocketAddress returnAddr, InetSocketAddress metaServer) throws IOException {
		super(returnAddr);
		this.metaServer = metaServer;
	}

	@Override
	public void run() {
		String result = null;
		for(;;) {
			try {
				Future<Job> choice = service.submit(new Menu());
				choice.get();

				if(choice instanceof Read) {
					result = read(((Read) choice).target);
				}
				else if(choice instanceof Create) {
					result = create(((Create) choice).target);
				}
				else if(choice instanceof Append) {
					Append a = (Append) choice;
					boolean success = append(a.target, a.payload);
					if(success) {
						result = "Append success";
					}
					else {
						result = "Append failed";
					}
				}
			}
			catch(Exception ex) {
				result = ex.getMessage();
			}
			finally {
				System.out.println("Result:\n" + result + "\n");
			}
		}
	}


	public String create(File file) throws IOException, InterruptedException {
		log.info("Started create job");
		Create job = new Create(file);
		Request<Create, Boolean> msg = new Request<>(job, metaServer);
		boolean success = messenger.send(msg);

		if(success) {
			return "File creation registered with metadata server.";
		}
		else {
			throw new IOException("Metaserver failed to create file");
		}
	}

	public void onMessage(Socket s) {
	}

	public String read(File file) throws IOException, InterruptedException {
		return read(file, metaServer);
	}

	public boolean append(File file, String payload) throws IOException, InterruptedException {
		return append(file, payload, metaServer);
	}

	public TreeMap<Chunk, ChunkMeta> locate(File file) throws IOException, InterruptedException {
		return locate(file, metaServer);
	}
}
