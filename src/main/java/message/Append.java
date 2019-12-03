class Append extends Message {

	private final Chunk chunk;
	private final String data;

	Append(int id, int dest, long timestamp, Chunk chunk) {
		super(id, dest, timestamp);
		storedFiles = files;
		this.chunk = chunk;
	}

	public Chunk getChunk() {
		return this.chunk;
	}
}
