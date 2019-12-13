import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Menu implements Callable<Job> {

	private static final Scanner scanner = new Scanner(System.in);

	@Override
	public Job call() {
		System.out.println("\n");
		System.out.println("Usage:");
		System.out.println("create <filename>");
		System.out.println("read <filename>");
		System.out.println("append <filename> <content>");
		System.out.print("> ");
		scanner.reset();

		try {
			String cmd = scanner.next();

			if(!scanner.hasNext()) {
				throw new IllegalArgumentException("incorrect usage");
			}
			File file = new File(scanner.next());

			String payload = null;
			if(cmd.equalsIgnoreCase("append")) {
				if(!scanner.hasNextLine()) {
					throw new IllegalArgumentException("incorrect usage");
				}
				payload = scanner.nextLine();
				if(payload.length() > Node.MAX_APPEND) {
					String msg = "append len must be <= " + Node.MAX_APPEND;
					throw new IllegalArgumentException(msg);
				}
			}
			return makeRequest(cmd, file, payload);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private Job makeRequest(String cmd, File file, String payload) throws IOException, InterruptedException {
		if(cmd.equalsIgnoreCase("create")) {
			return new Create(file);
		}
		else if(cmd.equalsIgnoreCase("read")) {
			return new Read(file);
		}
		else if(cmd.equalsIgnoreCase("append")) {
			return new Append(file, payload);
		}
		else {
			throw new IllegalArgumentException("invalid operation");
		}
	}
}
