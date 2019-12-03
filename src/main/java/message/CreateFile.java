class CreateFile extends Message {

	private final File file;

	CreateFile(int id, int dest, long timestamp, File file) {
		super(id, dest, timestamp);
		this.file = file;
	}

	public File getFile() {
		return this.file;
	}
}
