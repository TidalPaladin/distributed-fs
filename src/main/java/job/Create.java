import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class Create<T extends File> extends Job<Void> {

	public final T target;

	public Create(T target) {
		this.target = target;
	}

	@Override
	public Void call() throws IOException {
		if(target instanceof Chunk) {
			target.createNewFile();
		}
		return null;
	}
}
