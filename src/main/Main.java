import org.apache.logging.log4j.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.Thread;

public class Main {

	private static final Logger log = LogManager.getRootLogger();

	public static void main(String[] args) {

		try {
			final InetAddress host = InetAddress.getByName(System.getenv("HOSTNAME"));

			// Load client/server list fron environment vars
			final String[] servers = parseEnvList("SERVERS"),
				clients = parseEnvList("CLIENTS");

			// Load client/server id from environtment var
			final int id = Integer.parseInt(System.getenv("ID"));

			// Initialize static client/server lists
			FileServer.setServers(servers);
			Client.setServers(servers);
			Server.setServers(servers);
			FileServer.setClients(clients);
			Client.setClients(clients);
			Server.setClients(clients);

			// Spawn client/server/fileserver based on runtime arg
			String mode = args[0];
			if(mode.equalsIgnoreCase("server")) {
				log.info("Starting server " + id);
				if(id == 0) {
					FileServer server = new FileServer();
					Thread.sleep(3);
					server.run();
					server.summary();
				}
				else {
					Server server = new Server(id);
					Thread.sleep(3);
					server.run();
					server.summary();
				}
			}
			else {
				log.info("Starting client " + id);
				Client client = new Client(id);
				Thread.sleep(3);
				client.run();
				client.summary();
			}
		}
		catch(UnknownHostException ex) {
			log.error(ex.getMessage(), ex);
		}
		catch(InterruptedException | IOException ex) {
			log.error(ex.getMessage(), ex);
		}
		System.exit(0);
	}

	// Helper method to parse comma separated items in an env var to an array
	private static String[] parseEnvList(String name) {
		return System.getenv(name).split(",");
	}
}
