/* This class defines Node and its structure*/
public class Node {
	String port;
	String num;
    String ipAdd;
    public  Node(String num, String ipAdd, String port){
    	this.port = port;
    	this.num = num;
        this.ipAdd = ipAdd;
    }
    public void setPort(String port) {
        this.port = port;
    }
    public String getPort() {
        return port;
    }
    public void setnum(String num) {
        this.num = num;
    }
    public String getnum() {
        return num;
    }
    public void setipAdd(String ipAdd) {
        this.ipAdd = ipAdd;
    }
    public String getipAdd() {
        return ipAdd;
    }    
}
