// Subclasses for various message types
class Heartbeat extends Message {

	private final List<File> storedFiles;

	Heartbeat(int id, int dest, long timestamp, List<File> files) {
		super(id, dest, timestamp);
		storedFiles = files;
	}

	public List<File> getStoredFiles() {
		return this.storedFiles;
	}
}
