class Abort extends Job<Void> {

	public final Job job;

	public Abort(Job job) {
		this.job = job;
	}

	public boolean isForJob(Job other) {
		return this.job.equals(other);
	}

	@Override
	public String toString() {
		return String.format("Abort(%s)", job);
	}

	@Override
	public Void call() {
		return null;
	}
}
