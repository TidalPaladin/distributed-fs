import java.io.*;
import java.util.*;
import java.net.Socket;
import java.net.MulticastSocket;
import java.net.SocketAddress;

public class Chunk implements Comparable<Chunk> {

	/* Maximum chunk size */
	public static final int SIZE = 4096;

	/* Padding character for chunks */
	public static final char PAD = '\0';

	public final File file;

	public Chunk(File file) {
		this.file = file;
	}

	/**
		* Checks if the file associated with this chunk exists on the filesystem.
		*
		* @return 	True if this.getFile().exists(), false otherwise
		*/
	public boolean exists() {
		return file.exists();
	}

	/**
		* Gets a File object corresponding to the file used to store
		* this chunk's data to the disk.
		*
		* @return 	this.chunkFile
		*
		*/
	public File getFile() {
		return file;
	}

	/**
		* Gets the number of bytes currently written in this chunk.
		* This is equivalent to the chunk file size.
		*
		* @return 	this.chunkFile.length() in bytes
		*
		*/
	public long getSize() {
		return file.length();
	}

	/**
		* Gets the useable space remaining in this chunk. This
		* is the difference between the number of bytes already
		* written in this chunk and the maximum chunk size.
		*
		* @return 	SIZE - this.getSize()
		*
		*/
	public long getFreeSpace() {
		return SIZE - file.length();
	}

	/**
		* Reads data from this chunk, stopping upon reaching
		* null character padding.
		*
		* @throws IOException	Upon error reading file
		*
		* @return 	A string of read characters
		*
		*/
	public String	read() throws IOException{
		return read(0, SIZE);
	}

	/**
		* Reads data from this chunk, stopping upon reaching
		* null character padding.
		*
		* @param off		Offset at which to begin reading
		*
		* @throws IOException	Upon error reading file
		*
		* @return 	A string of read characters
		*
		*/
	public String read(int off) throws IOException{
		return read(off, SIZE);
	}

	/**
		* Reads data from this chunk, stopping upon reaching
		* null character padding.
		*
		* @param off		Offset at which to begin reading
		*
		* @param len		Number of characters to read
		*
		* @throws IOException	Upon error reading file
		*
		* @return 	A string of read characters
		*
		*/
	public String read(int off, int len) throws IOException {

			/* Read data from file as specified */
			final char[] buf = new char[SIZE];
			BufferedReader in = new BufferedReader(new FileReader(file));
			in.read(buf, off, len);
			in.close();

			/* Determine a stopping point based on len or start of null padding */
			int padBoundary = Arrays.asList(buf).indexOf(PAD);
			if(padBoundary < 0) {
					padBoundary = SIZE+1;
			}

			StringBuilder sb = new StringBuilder(SIZE);
			sb.append(buf, 0, Math.min(len, padBoundary));
			return sb.toString();
	}


	/**
		* Checks if the given character buffer can be written to the chunk file
		* at the given offset without exceeding the maximum chunk size.
		*
		* @param buf		Buffer of characters to be writen to the chunk.
		* @param off		Offset at which the data will be written
		*
		* @throws IOException	Upon error reading length info from file
		*
		* @return True if SIZE >= buf.length + off
		*
		*/
	public boolean canWrite(char[] buf, int off) throws IOException {
			/* Check write will not exceed chunk size limit */
			return buf.length + off <= SIZE;
	}

	/**
		* Writes data to this chunk at a given offset.
		*
		* @param buf		Buffer of characters to write to the chunk. The number of
		* 												bytes to be written must not exceed the maximum chunk size.
		* 												I.E. this.getFreeSpace() >= buf.length
		*
		* @param off		Offset at which to write the data
		*
		* @throws IOException	Upon error writing to file
		*
		*/
	public void write(char[] buf, int off) throws IOException {
			/* Check write will not exceed chunk size limit */
			if(!canWrite(buf, off)) {
					String msg = String.format("Writing %i bytes exceeds chunk limit %i", buf.length, SIZE);
					throw new IOException(msg);
			}

			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(buf, off, buf.length);
			out.flush();
			out.close();
	}


	/**
		* Generates a file name for the underlying chunk file based on the original file
		* and the chunk index of thix chunk
		*
		* @param file			Parent file that this chunk is partially storing
		*
		* @param index		Index of this chunk
		*
		* @return A chunk file name of form "original_file%%chunk_index"
		*
		*/
	public static File generateFileName(File file, int index) {
			String name = String.format("%s%%%d", file.getName(), index);
			return new File(name);
	}

	/**
		* Pads this chunk with the null character until the maximum chunk size is reached.
		*
		* @throws IOException	Upon error writing to file
		*
		*/
	public void pad() throws IOException {
			long start = getSize(), pad_size = getFreeSpace();

			char[] buf = new char[(int)pad_size];
			Arrays.fill(buf, PAD);
			write(buf, (int)start);
	}

	// Returns a trivial string representation of chunk and chunk file
	@Override
	public String toString() {
			return String.format("Chunk(%s)", file);
	}

	// Compares chunks by comparing the chunk file paths
	@Override
	public int compareTo(Chunk other) {
			return this.file.compareTo(other.file);
	}

	// Tests chunk equality by testing chunk file equality
	@Override
	public boolean equals(Object other) {
			if(other instanceof Chunk) {
					Chunk oth = (Chunk) other;
					return this.file.equals(oth.file);
			}
			return false;
	}

	// Computes hash code using File.hashCode() of the chunk files
	@Override
	public int hashCode() {
			return this.file.hashCode();
	}

}
