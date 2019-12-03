import java.io.*;
import java.net.*;
import java.util.*;
public class metaserver{
	String num;
	ServerSocket meta_socket;
	String mser_ipaddr;
	List<filemap> fileobjlist = new ArrayList<>();
	HashMap<String,Socket> clnt_soc_map = new HashMap<>();
	HashMap<String, meta_to_other> clnt_channel_map = new HashMap<>();
	HashMap<String, meta_to_other> serv_channel_map = new HashMap<>();
	private List<Node> clientnodes = new ArrayList<>();
	private List<Node> servernodes = new ArrayList<>();
	private List<Node> metaservernode = new ArrayList<>();
	public List<Node> get_server_nodes(){return this.servernodes;}
	public List<Node> get_client_nodes(){return this.clientnodes;}
	public List<Node> get_metaserver(){return this.metaservernode;}
	public void set_server_nodes(List<Node> sernodes){this.servernodes = sernodes;}
	public void set_client_nodes(List<Node> clntnodes){this.clientnodes = clntnodes;}
	public metaserver(String num){
		this.num = num;
	}
	public class mserparse extends Thread{
		metaserver metaser;
		Scanner msertermin;
		public mserparse (metaserver mser){
			this.metaser = mser;
		}
		public void commandrunner(Scanner msertermin){
			String cmdin = msertermin.nextLine();
			switch(cmdin){
				case "SETUP":{
					try{
						for(int temp=0; temp < metaser.clientnodes.size();temp++){
							Socket soc = new Socket(metaser.get_client_nodes().get(temp).getipAdd(),Integer.valueOf(metaser.get_client_nodes().get(temp).port));
							meta_to_other msertoclnt = new meta_to_other(metaser,soc,Integer.toString(temp),"client");
							metaser.clnt_soc_map.put(Integer.toString(temp),soc);
							metaser.clnt_channel_map.put(Integer.toString(temp),msertoclnt);
						}
						for(int temp=0; temp < metaser.servernodes.size();temp++){
							Socket soc = new Socket(metaser.get_client_nodes().get(temp).getipAdd(),Integer.valueOf(metaser.get_client_nodes().get(temp).port));
							meta_to_other msertoserv = new meta_to_other(metaser,soc,Integer.toString(temp),"server");
							metaser.clnt_soc_map.put(Integer.toString(temp),soc);
							metaser.serv_channel_map.put(Integer.toString(temp),msertoserv);
						}
					}
					catch(Exception e){e.printStackTrace();}
				}
			}		
		}
		public void run(){
			msertermin = new Scanner(System.in);
			while(true){commandrunner(msertermin);}
		}
	}
	public synchronized void selreplicaser(String filename,String inpdata){
		System.out.println("Inside selreplicaser");
		filemap fileobj = new filemap(filename);
		this.fileobjlist.add(fileobj);
		List<meta_to_other> replicaserver = new ArrayList<>();
		StringBuilder chunkname = new StringBuilder();
		chunkname.append(filename);
		chunkname.append("_");
		chunkname.append("1");
		String cname = chunkname.toString();
		fileobj.addchunkfile(cname);
		int max = 5;
		int numneed = 3;
		Random rng = new Random();
		Set<Integer> generated = new LinkedHashSet<Integer>();
		while (generated.size() < numneed)
		{
		    Integer next = rng.nextInt(max);
		    generated.add(next);
		}
		for(int i=0; i < generated.size();i++){
			meta_to_other mserchannel = serv_channel_map.get(Integer.toString(i));
			mserchannel.sendcreatechunkreq(cname,inpdata);
			replicaserver.add(mserchannel);			
		}
		fileobj.replicasermap.put(cname, replicaserver);		
	}
	public synchronized void selchkser(String filename, String offset){
		int i = 0;
		Boolean temp = true;
		while(i < this.fileobjlist.size()){
			if(this.fileobjlist.get(i).filename.equals(filename)){
				Integer filesnum = this.fileobjlist.get(i).file_chunks.size();
				Integer reqfiles = Integer.getInteger(offset) % 4096;
			}
		}
	}
	private void mssoccreation(metaserver mser){
		try{	
			mser.meta_socket = new ServerSocket(Integer.valueOf(mser.clientnodes.get(Integer.valueOf(mser.num)).port));
			System.out.println("client socket created");
			InetAddress currip = InetAddress.getLocalHost();
	        mser.mser_ipaddr = currip.getHostAddress();
	        String hostname = currip.getHostName();
	        System.out.println("client IP address : " + mser.mser_ipaddr);
	        System.out.println("client Hostname : " + hostname);
		}
		catch (IOException e){e.printStackTrace();
		System.exit(-1);}
		mserparse cmdparser = new mserparse(mser);
		cmdparser.start();// client thread should come here
	}
	private void setSerList(){
		try {
			BufferedReader bufred = new BufferedReader(new FileReader("ServerDetails.txt"));
			try {
				StringBuilder strbuld = new StringBuilder();
				String ln = bufred.readLine();
				while (ln != null) {
					strbuld.append(ln);
					strbuld.append(System.lineSeparator());
					List<String> serv_read = Arrays.asList(ln.split(","));
					Node serv_node = new Node(serv_read.get(0),serv_read.get(1),serv_read.get(2));
					this.get_server_nodes().add(serv_node);  
					ln = bufred.readLine();
				}
	        }finally {
	           	bufred.close();
	        }
	    }
		catch (Exception e) { e.printStackTrace();  }
	} 
	private void setClntlist(){
		try {
			BufferedReader bufred = new BufferedReader(new FileReader("ClientDetails.txt"));
			try {
				StringBuilder strbuld = new StringBuilder();
				String ln = bufred.readLine();
				while (ln != null) {
					strbuld.append(ln);
					List<String> clnt_read = Arrays.asList(ln.split(","));
					Node clnt_node= new Node(clnt_read.get(0),clnt_read.get(1),clnt_read.get(2));
					this.get_client_nodes().add(clnt_node);
					strbuld.append(System.lineSeparator());
					ln = bufred.readLine();
				}
			} finally {
				bufred.close();
			}
		}
		catch (Exception e) {e.printStackTrace();
		}
	}
	private void set_Metaserver(){
		try {
			BufferedReader bufred = new BufferedReader(new FileReader("MetaserverDetails.txt"));
			try {
				StringBuilder strbuld = new StringBuilder();
				String ln = bufred.readLine();
				while (ln != null) {
					strbuld.append(ln);
					List<String> metaser_read = Arrays.asList(ln.split(","));
					Node metaser_node= new Node(metaser_read.get(0),metaser_read.get(1),metaser_read.get(2));
					this.get_metaserver().add(metaser_node);
					strbuld.append(System.lineSeparator());
					ln = bufred.readLine();
				}
			} finally {
				bufred.close();
			}
		}
		catch (Exception e) {e.printStackTrace();
		}
	}
	public static void main(String[] args)
	{	if (args.length != 1)
        {   System.out.println("Enter correct input");
            System.exit(1);
        }
		metaserver metaser = new metaserver(args[0]);
		System.out.println("Created metaserver");
		metaser.setSerList();
		metaser.setClntlist();
		metaser.set_Metaserver();
		metaser.mssoccreation(metaser);
	}
}