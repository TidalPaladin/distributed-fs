import org.apache.logging.log4j.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.Thread;

public class Main {

	private static final Logger log = LogManager.getRootLogger();

	public static void main(String[] args) {

		try {
			String mode = args[0];

			final InetSocketAddress host = new InetSocketAddress(
				InetAddress.getLocalHost(),
				Integer.parseInt(System.getenv("PORT"))
			);

			final InetSocketAddress meta = new InetSocketAddress(
				System.getenv("META"),
				Integer.parseInt(System.getenv("PORT"))
			);

			Node node = null;
			if(mode.equalsIgnoreCase("client")) {
				log.info("Using metaserver: " + meta);
				node = new Client(host, meta);
			}
			else if(mode.equalsIgnoreCase("server")) {
				log.info("Using metaserver: " + meta);
				node = new Server(host, meta);
			}
			else if(mode.equalsIgnoreCase("meta")) {
				node = new Metaserver(host);
			}

			log.info("Starting node: " + node);
			node.run();

		}
		catch(Exception ex) {
			log.error(ex.getMessage(), ex);
		}
		System.exit(0);
	}
}
