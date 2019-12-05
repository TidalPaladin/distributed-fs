import java.io.*;
import java.net.*;
import java.util.*;

public class meta_to_other{
	metaserver metaser;
	Socket sersock;
	String sernum;
	String type;
	Scanner termin;
	int tickker;
	DataInputStream frm_otertometa;
	DataOutputStream to_oterfrmmeta;
	public meta_to_other(metaserver metaser, Socket sersock, String sernum,String type){
		this.metaser = metaser;
		this.sersock = sersock;
		this.sernum = sernum;
		this.type = type;
		try
		{	termin = new Scanner(System.in);
			frm_otertometa = new DataInputStream(sersock.getInputStream());
			to_oterfrmmeta = new DataOutputStream(sersock.getOutputStream());
		}
		catch(IOException e){e.printStackTrace();}
		Thread msertooter = new Thread(){
			public void run(){
				System.out.println("Inside ctosthread run");
				while (cmdxfer(termin,frm_otertometa,to_oterfrmmeta,metaser)){}				
			}
		};
		msertooter.setDaemon(true);
		msertooter.start();
		while(true){}
	}
	public boolean cmdxfer(Scanner termin,DataInputStream input,DataOutputStream output, metaserver mser){
		try{  	
			String serline = "";
		  	serline = input.readUTF();
		  	switch(serline){
		  	case "CREQ" :{
		  		String filename = input.readUTF();
		  		String inpdata = input.readUTF();
		  		mser.selreplicaser(filename,inpdata);
		  		
		  		}
		  	case "CHKCRETD":{
		  		String servernum = input.readUTF();
		  		System.out.println("CHUNK CREATED IN SERVER" + servernum);
		  		}
		  	case "RREQ":{
		  		String filename = input.readUTF();
		  		List<String> serlist = mser.selchkser(filename);
		  		int temp = serlist.size();
		  		output.writeUTF("RSERLIST");
		  		output.writeUTF(filename);
		  		for(int i=0; i<temp; i++){
		  			output.writeUTF(serlist.get(temp));
		  		}
		  		output.writeUTF("OVER");
		  		}
		  	case "HEARTBEAT" :{
		  		String filenum = input.readUTF();
		  		String fname;
		  		List<String> filelist = new ArrayList<>();
		  		fname = input.readUTF();
		  		while(! fname.equals("END")){
		  			filelist.add(fname);
		  			fname = input.readUTF();
		  		}
		  		mser.updatelist(filelist);
		  	}
		  	}	  	
		}
		catch(Exception e){e.printStackTrace();}
		return true;
		}
	public synchronized void sendcreatechunkreq(String cname, String inpdata){
		try{
			System.out.println("Inside sendcreatechunkreq");
			this.to_oterfrmmeta.writeUTF("CCREP");
			this.to_oterfrmmeta.writeUTF(cname);
			this.to_oterfrmmeta.writeUTF(inpdata);
		}
		catch(Exception e){e.printStackTrace();}
	}
}
