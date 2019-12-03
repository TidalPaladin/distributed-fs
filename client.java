import java.util.*;
import java.io.*;
import java.net.*;
public class client{
	String clnt_num;
	ServerSocket clnt_socket;
	Socket connectedsoc;
	String clnt_ipaddr;
	List<Node> clnt_clientnodes = new ArrayList<>();
	List<Node> clnt_servernodes = new ArrayList<>();
	List<Node> clnt_metaservernode = new ArrayList<>();
	List<Socket> connectedsockets = new ArrayList<Socket>();
	List<client_to_other> clientconnections = new ArrayList<client_to_other>();
	HashMap<String,Socket> ser_socket_map = new HashMap<>(); //this has client sockets
	HashMap<String,client_to_other> ser_channel_map = new HashMap<>();// this has channel of all client to servers
	public client(String num)
	{	this.clnt_num = num;
		System.out.println("Inside default constructor");
	}
	public void set_clnt_nodes(List<Node> clntnode){this.clnt_clientnodes = clntnode;}
	public List<Node> get_client_nodes(){return this.clnt_clientnodes;}
	public List<Node> get_metaserver_nodes(){return this.clnt_metaservernode;}
	public void set_metaserver_nodes(List<Node> metaservernode){this.clnt_metaservernode = metaservernode;}
	public void set_ser_nodes(List<Node> sernode){this.clnt_servernodes = sernode;}
	public List<Node> get_Server_nodes(){return this.clnt_servernodes;}
	public class clntcmdparser extends Thread{
        client curr;
        Scanner termin;
        public clntcmdparser(client curr){this.curr = curr;}
        public void termcmd(Scanner inpcmd)
        {  	String line = inpcmd.nextLine();
        	if (line.equals("SETUP")){
        		try{
        			for(int sernum = 0; sernum < curr.get_Server_nodes().size();sernum++){
        				Socket serconn = new Socket(curr.get_Server_nodes().get(sernum).getipAdd(), Integer.valueOf(curr.get_Server_nodes().get(sernum).getPort()));
        				client_to_other ctoosconn = new client_to_other(curr,serconn,Integer.toString(sernum));
        				curr.ser_socket_map.put(Integer.toString(sernum), serconn);
        				curr.ser_channel_map.put(Integer.toString(sernum), ctoosconn);
        			}
        		}
        		catch (Exception e){e.printStackTrace();}
        	}
        	else if(line.equals("CREATE")){
        		String fname = inpcmd.nextLine();
        		String data = inpcmd.nextLine();
        		curr.clientconnections.get(0).sendcreatereq(fname,data);
        	}
        	else if(line.equals("READ")){
        		String fname = inpcmd.nextLine();
        		String offset = inpcmd.nextLine();
        		curr.clientconnections.get(0).sendreadreq(fname,offset);
        	}
        }
        public void run()
        {  	termin = new Scanner(System.in);
        	while(true){termcmd(termin);}
        }
    }
	private void csoccreation(client curr)
	{	try
		{	
			clnt_socket = new ServerSocket(Integer.valueOf(curr.clnt_clientnodes.get(Integer.valueOf(curr.clnt_num)).port));
			System.out.println("client socket created");
			InetAddress currip = InetAddress.getLocalHost();
            curr.clnt_ipaddr = currip.getHostAddress();
            String hostname = currip.getHostName();
            System.out.println("client IP address : " + curr.clnt_ipaddr);
            System.out.println("client Hostname : " + hostname);
		}
		catch (IOException e){e.printStackTrace();
		System.exit(-1);}
		clntcmdparser clntcmdparser = new clntcmdparser(curr);
		clntcmdparser.start();// client thread should come here
		Thread current_node = new Thread() {
			public void run(){
				while(true){
					try{ 	
						connectedsoc = clnt_socket.accept();
						client_to_other otocconn = new client_to_other(curr,connectedsoc,"9");
						curr.connectedsockets.add(connectedsoc);
						curr.clientconnections.add(otocconn);
					}
					catch(Exception e){ e.printStackTrace();
					System.exit(-1);}
				}
			}
		};
		current_node.setDaemon(true);
		current_node.start();
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
	                String all_sevrs = strbuld.toString();
	                System.out.println(all_sevrs);
	                System.out.println(this.get_Server_nodes().size());
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
				String all_clnts = strbuld.toString();
				System.out.println(all_clnts);
				System.out.println(this.get_client_nodes().size());
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
				String all_clnts = strbuld.toString();
				System.out.println(all_clnts);
				System.out.println(this.get_metaserver_nodes().size());
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
		client curr_client = new client(args[0]);
		System.out.println("Created Client number"+args[0]);
		curr_client.setSerList();
		curr_client.setClntlist();
		curr_client.set_Metaserver();
		curr_client.csoccreation(curr_client);
	}
}