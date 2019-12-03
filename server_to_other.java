import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Scanner;
public class server_to_other{
	private server servobj;
	private Socket servsoc;
	Scanner sertermin;
	DataInputStream frm_otertoser;
	DataOutputStream to_oterfrmser;
	public server_to_other(server serv, Socket servsoc){
		this.servobj = serv;
		this.servsoc = servsoc;
		try{
			sertermin = new Scanner(System.in);
			frm_otertoser = new DataInputStream(servsoc.getInputStream());
			to_oterfrmser = new DataOutputStream(servsoc.getOutputStream());
		}
		catch(Exception e){e.printStackTrace();}
		Thread clnttooter = new Thread(){
			public void run(){
				System.out.println("Inside ctosthread run");
				while (cmdxfer(sertermin,frm_otertoser,to_oterfrmser,servobj)){}				
			}
		};
		clnttooter.setDaemon(true);
		clnttooter.start();
	}
	public boolean cmdxfer(Scanner termin, DataInputStream serin, DataOutputStream serout, server servobj){
		try{
			String termline;
			termline = serin.readUTF();
			switch(termline){
			case "CCREP":{
				String chunkname = serin.readUTF();
				String inputdata = serin.readUTF();
				String filename = servobj.createfile(chunkname);
					//StringBuilder strwrite = new StringBuilder();
					//strwrite.append("CHUNKCREATED");
					//strwrite.append(servobj.ser_num);
				servobj.writetofile(filename, inputdata);
				serout.writeUTF("CHKCRETD");
				serout.writeUTF(servobj.ser_num);
			}
			}
		}
		catch(Exception e){e.printStackTrace();}
		return true;
	}
}