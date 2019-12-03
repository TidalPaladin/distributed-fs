// Subclasses for various message types
class Read extends Message {

	private final Chunk chunk;

	Read(int id, int dest, long timestamp, Chunk chunk) {
		super(id, dest, timestamp);
		this.chunk = chunk;
	}

	public Chunk getChunk() {
		return this.chunk;
	}
}
