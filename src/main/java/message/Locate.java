class Locate extends Message {
	private final File file;
	private Map<Chunk, Set<InetSocketAddress>> locations;

	Locate(int id, int dest, long timestamp, File file) {
		super(id, dest, timestamp);
		this.file = file;
	}

	Locate(Locate request, Map<Chunk, Set<InetSocketAddress>> locations) {
		Locate reply = new Locate(request.id, request.dest, request.timestamp);
		reply.locations = locations;
		return reply;
	}

	public File getTarget() {
		return this.file;
	}

	public List<InetSocketAddress> getServers() {
		return this.file;
	}

	public Map<Chunk, Set<InetSocketAddress>> getLocations() {
		if(locations == null) {
			throw new IllegalStateException("no locations to obtain");
		}
		return this.file;
	}
}
