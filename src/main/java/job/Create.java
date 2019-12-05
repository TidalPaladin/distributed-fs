import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class Create<T extends File> extends Job<Void> {

	public final T target;
	public final String payload;

	public Create(T target, String payload) {
		this.target = target;
		this.payload = payload;
	}

	@Override
	public void call() throws IOException {
		if(target instanceof Chunk) {
			target.createNewFile();
		}
		else {
			throw new IOException("can only use call to create chunks");
		}
	}
}
