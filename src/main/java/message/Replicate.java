class Replicate extends Message {

	private final Chunk chunk;
	private final InetSocketAddress from;

	public Replicate(int id, int dest, long timestamp, InetSocketAddress fromServer, Chunk chunk) {
		super(id, dest, timestamp);
		this.chunk = chunk;
		this.from = fromServer;
	}

	public Chunk getChunk() {
		return this.chunk;
	}

	public InetSocketAddress getSource() {
		return this.from;
	}
}
