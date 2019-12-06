import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

class Append<T extends File> extends Job<Void> {

	public final T target;
	public final String payload;

	public Append(T target, String payload) {
		this.target = target;
		this.payload = payload;
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof Append)) {
			return false;
		}
		Append oth = (Append) other;
		return target.equals(oth.target) && payload.equals(oth.payload);
	}

	@Override
	public Void call() throws IOException{
		try {
			if(!target.canWrite()) {
				throw new IOException("Failed to write to file");
			}

			if(target instanceof Chunk) {
				ChunkWriter cw = new ChunkWriter((Chunk) target);
				if(!cw.canWrite(payload)) {
					throw new IOException("Chunk could not be written to");
				}
				if(payload == null) {
					cw.pad();
				} else {
					cw.append(payload);
				}
				cw.close();
			}
			else {
				BufferedWriter bw = new BufferedWriter(new FileWriter(target));
				bw.append(payload);
				bw.close();
			}
		}
		finally {
			return null;
		}
	}
}
