class Unavailable extends Message {
	private final File file;

	Unavailable(int id, int dest, long timestamp, File file) {
		super(id, dest, timestamp);
		this.file = file;
	}

	public File getTarget() {
		return this.file;
	}
}
