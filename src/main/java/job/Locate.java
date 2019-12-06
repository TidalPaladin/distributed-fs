import java.io.File;
import java.util.TreeMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.net.InetSocketAddress;
import java.util.concurrent.Callable;

class Locate extends Job<TreeMap<Chunk, ChunkMeta>> {

	public final File target;

	private Map<File, FileMeta> files = null;
	private Map<Chunk, ChunkMeta> chunks = null;

	public Locate(File target) {
		this.target = target;
	}

	public void setMetadata(Map<Chunk, ChunkMeta>chunks, Map<File, FileMeta> files) {
		this.chunks = chunks;
		this.files = files;
	}

	@Override
	public String toString() {
		return String.format("Locate(%s)", target);
	}

	@Override
	public TreeMap<Chunk, ChunkMeta> call() {
		if(chunks == null || files == null) {
			throw new IllegalStateException("must set metadata before calling");
		}

		if(!files.containsKey(target)) {
			return null;
		}
		FileMeta fileMeta = files.get(target);
		TreeSet<Chunk> chunks = fileMeta.getChunks();

		TreeMap<Chunk, ChunkMeta> result = new TreeMap<>(this.chunks);
		result.keySet().retainAll(chunks);
		return result;
	}
}
