import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.UUID;

public class ChunkWriter extends FileWriter {

	/* Padding character for chunks */
	public static final char PAD = '\0';
	private final Chunk chunk;

	public ChunkWriter(Chunk chunk) throws IOException {
		super(chunk);
		this.chunk = chunk;
	}

	/**
	 * Checks if the given character buffer can be written to the chunk file
	 * at the given offset without exceeding the maximum chunk size.
	 *
	 * @param buf		Buffer of characters to be writen to the chunk.
	 *
	 * @throws IOException	Upon error reading length info from file
	 *
	 * @return True if chunk is writeable and data will fit in remaining space
	 *
	 */
	public boolean canWrite(char[] buf) throws IOException {
		return chunk.getFreeSpace() > buf.length && chunk.canWrite();
	}
	public boolean canWrite(String buf) throws IOException {
		return canWrite(buf.toCharArray());
	}

	/**
	 * Pads this chunk with the null character until the maximum chunk size is reached.
	 *
	 * @throws IOException	Upon error writing to file
	 *
	 */
	public void pad() throws IOException {
		int padSize = (int) chunk.getFreeSpace();
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < padSize; i++) {
			sb.append(PAD);
		}
		append(sb.toString());
	}
}





