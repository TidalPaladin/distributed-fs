import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Callable;

class Read<T extends File> extends Job<String> {

	public final T target;

	public Read(T target) {
		this.target = target;
	}

	@Override
	public String call() throws IOException {
		if(target.canRead()) {
			BufferedReader br = new BufferedReader(new FileReader(target));
			StringBuilder sb = new StringBuilder();
			while(br.ready()) {
				sb.append(br.readLine());
			}
			br.close();
			return sb.toString();
		}
		return null;
	}
}
