import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class meta_to_other{
	private metaserver metaser;
	private Socket sersock;
	private String sernum;
	private String type;
	Scanner termin;
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
		  		String offset = input.readUTF();
		  		mser.selchkser(filename,offset);
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
