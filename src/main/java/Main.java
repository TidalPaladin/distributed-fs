import org.apache.logging.log4j.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.Thread;

public class Main {

	private static final Logger log = LogManager.getRootLogger();

	public static void main(String[] args) {

		try {
			final int port = Integer.parseInt(System.getenv("PORT"));
			final String hostname = System.getenv("HOSTNAME");
			final InetSocketAddress host = new InetSocketAddress(hostname, port);
			final String metahost = System.getenv("META");
			final InetSocketAddress meta = new InetSocketAddress(metahost, port);
			final int id = Integer.parseInt(System.getenv("ID"));
			String mode = args[0];

			log.info("Using metaserver: " + meta);

			Thread t = null;
			if(mode.equalsIgnoreCase("client")) {
				Client client = new Client(host, meta);
				t = new Thread(client);
			}
			else if(mode.equalsIgnoreCase("server")) {
				Server server = new Server(host, meta);
				t = new Thread(server);
			}
			else if(mode.equalsIgnoreCase("meta")) {
				Metaserver server = new Metaserver(host);
				t = new Thread(server);
			}

			t.start();
			t.join();
		}
		catch(UnknownHostException ex) {
			log.error(ex.getMessage(), ex);
		}
		catch(InterruptedException | IOException ex) {
			log.error(ex.getMessage(), ex);
		}

		System.exit(0);
	}
}
