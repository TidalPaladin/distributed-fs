import java.io.*;
import java.net.Socket;
import java.util.*;

public class client_to_other{
	client clntobj;
	Socket socketconnected;
	String number;
	String type;
	Scanner clnttermin;
	DataInputStream frm_otertoclnt;
	DataOutputStream to_oterfrmclnt;
	public client_to_other(client client,Socket soc_conn,String num){
		this.clntobj = client;
		this.socketconnected = soc_conn;
		this.number = num;
		try{
			clnttermin = new Scanner(System.in);
			frm_otertoclnt = new DataInputStream(soc_conn.getInputStream());
			to_oterfrmclnt = new DataOutputStream(soc_conn.getOutputStream());
		}
		catch(Exception e){e.printStackTrace();}
		Thread clnttooter = new Thread(){
			public void run(){
				System.out.println("Inside ctosthread run");
				while (cmdxfer(clnttermin,frm_otertoclnt,to_oterfrmclnt,clntobj)){}				
			}
		};
		clnttooter.setDaemon(true);
		clnttooter.start();
	}
	public boolean cmdxfer(Scanner sysin, DataInputStream clntin, DataOutputStream clntout, client objclnt){
		try{
			String strline;
			strline = clntin.readUTF();
			switch(strline){
			case "RSERLIST":{
				String filename = clntin.readUTF();
				List<String> serlist = new ArrayList<>();
				String inpread = clntin.readUTF();
				while (inpread.equals("OVER")){
					serlist.add(inpread);
					inpread = clntin.readUTF();
				}
				objclnt.readtoser(serlist,filename);
			}
			case "READDATA":{
				String rdata = clntin.readUTF();
				System.out.println(rdata);
			}
			}
		}
		catch(Exception e){e.printStackTrace();}
		return true;
	}
	public synchronized void sendcreatereq(String filename,String data){
		try{
			System.out.println("Inside the sendcreatereq");
			this.to_oterfrmclnt.writeUTF("CREQ");
			this.to_oterfrmclnt.writeUTF(filename);
			this.to_oterfrmclnt.writeUTF(data);
		}
		catch(Exception e){e.printStackTrace();}
	}
	public synchronized void sendreadreq(String filename){
		try{
			System.out.println("Inside sendreadreq");
			this.to_oterfrmclnt.writeUTF("RREQ");
			this.to_oterfrmclnt.writeUTF(filename);
		}
		catch(Exception e){e.printStackTrace();}
	}
	public synchronized void readreqtoser(String filename){
		try{
			System.out.println("Inside readreqtoser");
			this.to_oterfrmclnt.writeUTF("READREQ");
			this.to_oterfrmclnt.writeUTF(filename);
		}
		catch(Exception e){e.printStackTrace();}
	} 
}
