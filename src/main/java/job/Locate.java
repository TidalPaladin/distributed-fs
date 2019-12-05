import java.io.File;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

class Locate extends Job<TreeSet<Chunk, ChunkMeta>> {

	public final File target;

	private Map<File, FileMeta> files = null;
	private Map<Chunk, ChunkMeta> chunks = null;

	public Locate(File target) {
		this.target = target;
	}

	public void setMetadata(Map<Chunk, ChunkMeta>chunks, Map<File, FileMeta> files) {
		this.meta = meta;
	}

	@Override
	public TreeSet<Chunk, ChunkMeta> call() {
		if(chunks == null || files == null) {
			throw new IllegalStateException("must set metadata before calling");
		}

		FileMeta fileMeta = files.get(file);
		TreeSet<Chunk> chunks = fileMeta.getChunks();

		TreeMap<Chunk, ChunkMeta> result = new TreeMap<>(this.chunks);
		payload.keySet().retainAll(chunks);
		return result;
	}
}
