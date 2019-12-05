import java.util.*;
import java.io.*;
import java.net.*;
public class server{
	String ser_num;
	ServerSocket ser_socket;
	String ser_ipaddr;
	List<Node> ser_clientnodes = new ArrayList<>();
	List<Node> ser_servernodes = new ArrayList<>();
	List<Node> ser_metaservernode = new ArrayList<>();
	List<Socket> serconnectedsocks = new ArrayList<Socket>();
	List<server_to_other> serconnections = new ArrayList<server_to_other>();
	List<String> filepresent = new ArrayList<String>();
	HashMap<String,Socket> ser_socket_map = new HashMap<>(); //this has client sockets
	HashMap<String,server_to_other> ser_channel_map = new HashMap<>();// this has channel of all client to servers
	public server(String num)
	{	this.ser_num = num;
		System.out.println("Inside default constructor");
	}
	public void set_clnt_nodes(List<Node> clntnode){this.ser_clientnodes = clntnode;}
	public List<Node> get_client_nodes(){return this.ser_clientnodes;}
	public List<Node> get_metaserver_nodes(){return this.ser_metaservernode;}
	public void set_metaserver_nodes(List<Node> metaservernode){this.ser_metaservernode = metaservernode;}
	public void set_ser_nodes(List<Node> sernode){this.ser_servernodes = sernode;}
	public List<Node> get_Server_nodes(){return this.ser_servernodes;}
	public class sercmdparser extends Thread{
        server curr;
        Scanner termin;
        public sercmdparser(server curr){this.curr = curr;}
        public void termcmd(Scanner inpcmd)
        {  	String line = inpcmd.nextLine();
        	if (line.equals("SETUP")){
        		try{
        			for(int sernum = Integer.valueOf(curr.ser_num)+1 ; sernum < curr.get_client_nodes().size(); sernum++){
        				Socket clntconn = new Socket(curr.get_client_nodes().get(sernum).getipAdd(),Integer.valueOf(curr.get_client_nodes().get(sernum).getPort()));
        				server_to_other stosconn = new server_to_other(curr,clntconn);
        				curr.ser_socket_map.put(Integer.toString(sernum), clntconn);
        				curr.ser_channel_map.put(Integer.toString(sernum),stosconn);
        			}
        		}
        		catch (Exception e){e.printStackTrace();}
        	}
        }
        public void run()
        {  	termin = new Scanner(System.in);
        	while(true){termcmd(termin);}
        }
    }
	public synchronized String createfile(String filename){
		StringBuilder fname = new StringBuilder();
		fname.append(filename);
		fname.append(".txt");
		File createdfile = new File(fname.toString());
		String createdfname = fname.toString();
		this.filepresent.add(filename);
		return createdfname;
	} 
	
	public synchronized void writetofile(String filename, String inpdata) throws IOException {
        BufferedWriter wrtr = new BufferedWriter(new FileWriter(filename,true));
        wrtr.append(inpdata);
        wrtr.close();
    }
	public synchronized String readfile(String filename) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String returndata = reader.toString();
		reader.close();
		return returndata;
	}
	private void ssoccreation(server curr)
	{	try
		{	
			ser_socket = new ServerSocket(Integer.valueOf(curr.ser_servernodes.get(Integer.valueOf(curr.ser_num)).port));
			System.out.println("server socket created");
			InetAddress currip = InetAddress.getLocalHost();
            curr.ser_ipaddr = currip.getHostAddress();
            String hostname = currip.getHostName();
            System.out.println("client IP address : " + curr.ser_ipaddr);
            System.out.println("client Hostname : " + hostname);
		}
		catch (IOException e){e.printStackTrace();
		System.exit(-1);}
		sercmdparser sercmdparser = new sercmdparser(curr);
		sercmdparser.start();
		while(true){
			try{
				Socket connectedsocket = ser_socket.accept();
				server_to_other otosconn = new server_to_other(curr,connectedsocket);
				curr.serconnectedsocks.add(connectedsocket);
				curr.serconnections.add(otosconn);
			}
			catch(Exception e){e.printStackTrace();}
		}
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
					this.get_Server_nodes().add(serv_node);  
					ln = bufred.readLine();
				}
	            } finally {
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
					this.get_metaserver_nodes().add(metaser_node);
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
		server curr_server = new server(args[0]);
		System.out.println("Created server number"+args[0]);
		curr_server.setSerList();
		curr_server.setClntlist();
		curr_server.set_Metaserver();
		curr_server.ssoccreation(curr_server);
	}
}