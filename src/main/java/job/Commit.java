class Commit extends Job<Void> {

	public final Job job;

	public Commit(Job job) {
		this.job = job;
	}

	public boolean isForJob(Job other) {
		return this.job.equals(other);
	}

	@Override
	public Void call() {
		try {
			job.call();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}
