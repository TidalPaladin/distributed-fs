class CreateChunk extends Message {

	private final Chunk chunk;

	CreateChunk(int id, int dest, long timestamp, Chunk chunk) {
		super(id, dest, timestamp);
		storedFiles = files;
		this.chunk = chunk;
	}

	public Chunk getChunk() {
		return this.chunk;
	}
}
