import org.apache.logging.log4j.*;
import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.Callable;

class Heartbeat extends Job<Void> {
	private static final Logger log = LogManager.getLogger("Heartbeat");

	public final Set<Chunk> storedChunks;
	public final InetSocketAddress source;
	private Map<InetSocketAddress, ServerMeta> servers, deadServers;

	public Heartbeat(InetSocketAddress source, Set<Chunk> stored) {
		this.storedChunks = stored;
		this.source = source;
	}

	public void setServerList(Map<InetSocketAddress, ServerMeta> meta){
		this.servers = meta;
	}

	public void setDeadServerList(Map<InetSocketAddress, ServerMeta> meta){
		this.deadServers = meta;
	}

	@Override
	public Void call() {
		if(servers == null || deadServers == null) {
			throw new IllegalStateException("must set server/dead server list");
		}

		if(servers.containsKey(source)) {
			ServerMeta serverMeta = servers.get(source);
			serverMeta.update(this);

			Set<Chunk> newChunks = serverMeta.getNewChunks();
			Set<Chunk> missing = serverMeta.getMissing();
			if(!newChunks.isEmpty()) {
				log.info("Heartbeat contained new chunks: " + newChunks);
			}
			if(!missing.isEmpty()) {
				log.info("Heartbeat missing chunks: " + missing);
			}
		}
		else if(deadServers.containsKey(source)) {
			log.info("Beginning recovery of dead server");
			//restore(source);
		}
		else {
			log.info("Registered new server " + source);
			servers.put(source, new ServerMeta(source));
		}
		return null;
	}
}
