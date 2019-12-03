import java.util.*;

public class filemap{
	String filename;
	List<String> file_chunks;
	HashMap<String,List<meta_to_other>> replicasermap = new HashMap<>();
	public filemap(String filename){
		this.filename = filename;
	}
	public void addchunkfile(String chunkname){
		this.file_chunks.add(chunkname);
	}
}