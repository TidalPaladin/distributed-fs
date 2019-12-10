class Commit<T> extends Job<T> {

	public final Job<T> job;

	public Commit(Job<T> job) {
		this.job = job;
	}

	public boolean isForJob(Job other) {
		return this.job.equals(other);
	}

	@Override
	public String toString() {
		return String.format("Commit(%s)", job);
	}


	@Override
	public T call() {
		try {
			return job.call();
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
}
