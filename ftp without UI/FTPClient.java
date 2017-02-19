import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.StringTokenizer;



class FTPClient {
	public Socket controlSocket = null;
	public Socket dataSocket = null;
	public boolean Passive=false;
	public BufferedReader controlInput;
	public PrintWriter controlOutput;
	public BufferedInputStream upload;
	public BufferedReader userInput;
	public String FTPhost;
	public int port = 21;
	public String username = null;
	public String password = null;
	public SocketAddress remoteAddr;
	
	public FTPClient() throws Exception {
		userInput = new BufferedReader(new InputStreamReader(System.in));
	}
	
	public String readUntil(String st) throws Exception {
		String msg;
		do{
			msg = controlInput.readLine();
			if (msg != null)
				System.out.println(msg);
			else 
				break;
		}while(!msg.startsWith(st));
		return msg;
	}

	public void dispatch() throws Exception {
		while (true) {
			System.out.print("<<");
			String cmdline;
			cmdline = userInput.readLine();
			String args[] = cmdline.split(" ");
			if(args[0].equals("help") && args.length == 1){
				help();
			} else if (args[0].equals("open") && args.length == 3) {
				open(args);
			} else if (args[0].equals("user") && args.length == 2) {
				user(args);
			} else if (args[0].equals("pass") && args.length == 2) {
				pass(args);
			} else if (args[0].equals("port") && args.length == 2) {
				port(args);
			} else if (args[0].equals("passive") && args.length == 1) {
				passive();
			} else if (args[0].equals("list") && args.length == 1) {
				list();
			}else if (args[0].equals("stor") && args.length == 2) {
				stor(args);
			} else if (args[0].equals("retr") && args.length == 2) {
				retr(args);
			}/* else if (args[0].equals("rename") && args.length == 3) {
				rename(args);
			}*/ else if (args[0].equals("delete") && args.length == 2) {
				delete(args);
			} else if (args[0].equals("mkdir") && args.length == 2) {
				mkdir(args);
			} else if (args[0].equals("cd") && args.length == 2) {
				cd(args);
			} else if (args[0].equals("type") && args.length == 2) {
				type(args);
			} else if (args[0].equals("noop") && args.length == 1) {
				noop(args);
			} else if (args[0].equals("quit") && args.length == 1) {
				quit();
				break;
			} else {
				//System.out.println("args[0]:" + args[0] + "args[1]:" + args[1] + "args.length:" + Integer.toString(args.length));
				System.out.println("Unrecognized Command/Argument Retry!");
			}
			
		}
	}
	
	public void help() {
		System.out.println("********************");
		System.out.println("help");
		System.out.println("open [127.0.0.1] [21]");
		System.out.println("user [cse6324]");
		System.out.println("pass [cse6324]");
		System.out.println("port [127,0,0,1,4,10]");
		System.out.println("passive");
		System.out.println("list"); 
		System.out.println("stor [uploadfile]");
		System.out.println("retr [downloadfile]");
		//System.out.println("rename [oldname] [newname]");
		System.out.println("delete [deletefile]");
		System.out.println("mkdir [dir]");
		System.out.println("cd [dir]");
		System.out.println("noop");
		System.out.println("quit");
		System.out.println("********************");
	}
	
	public void open(String [] args) throws IOException {
		port = Integer.parseInt(args[2]);
		controlSocket = new Socket();
		remoteAddr = new InetSocketAddress(args[1], port);
		controlSocket.connect(remoteAddr, 3000);
		controlInput = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
		controlOutput = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream()), true);
		String reply = controlInput.readLine();
		System.out.println(reply);
	}

	public void user(String [] args) throws Exception {
		username = args[1];
		controlOutput.println("USER " + username);	
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void pass(String [] args) throws Exception {
		password = args[1];
		controlOutput.println("PASS " + password);
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void port(String [] args) throws Exception {
		controlOutput.println("PORT " + args[1]);
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public Socket passive() throws Exception{		
		controlOutput.println("PASV");
		String reply = controlInput.readLine();
		System.out.println(reply);
		if (reply.startsWith("227 ")) {
			String ip = null;
			int port = 0;
			int left = reply.indexOf('[');
			int right = reply.indexOf(']', left + 1);
			if (right > 0) {			
				String sub = reply.substring(left + 1, right);
				StringTokenizer tokenizer = new StringTokenizer(sub, ",");
				try {
					ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken();
					port = Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken());
				} catch (Exception e) {
					throw new IOException("Format error: " + reply);
				}
			}
			return new Socket(ip, port);
		} else {
			throw new IOException("Failed to set passive mode: " + reply);
		}
	}
	
	public void list() throws Exception{
		if (dataSocket != null) {
			dataSocket.close();
		}
		dataSocket = passive();
		controlOutput.println("LIST -a -1");
		String reply = controlInput.readLine();
		System.out.println(reply);
		BufferedReader br = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
		do{
			reply = br.readLine();
			if(reply!=null)
				System.out.println(reply);
			else 
				break;
		}while(true);
		dataSocket.close();
	}
	
	public boolean stor(String [] args) throws FileNotFoundException, IOException, Exception {
		String filename = args[1];
		String reply;
		File f = new File(filename);
		if (!f.exists()) {
			System.out.println("File Doesn't Exist at Client");
			return false;
		}		
		if (dataSocket != null) {
			dataSocket.close();
		}
		dataSocket = passive();
		controlOutput.println("TYPE I");
		controlOutput.println("STOR " + f.getName());
		BufferedInputStream dataInput = new BufferedInputStream(new FileInputStream(f));
		BufferedOutputStream dataOutput = new BufferedOutputStream(dataSocket.getOutputStream());
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = dataInput.read(buffer)) != -1) {
			dataOutput.write(buffer, 0, bytesRead);
		}
		dataOutput.flush();
		dataOutput.close();
		dataInput.close();
		
		readUntil("250 ");
		dataSocket.close();
		return true;
	}
	
	public boolean retr(String [] args) throws FileNotFoundException, IOException, Exception{
		String filename = args[1];
		String clientPath = "./";
		String reply;
		if (dataSocket != null) {
			dataSocket.close();
		}
		dataSocket = passive();
		controlOutput.println("TYPE A");
		reply = controlInput.readLine();
		System.out.println(reply);
		controlOutput.println("RETR " + filename);
		reply = controlInput.readLine();
		System.out.println(reply);
		if(reply.startsWith("5")){
			return false;
		}
		BufferedOutputStream dataOutput = new BufferedOutputStream(new FileOutputStream(new File(clientPath, filename)));
		BufferedInputStream dataInput = new BufferedInputStream(dataSocket.getInputStream());
		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = dataInput.read(buffer)) != -1) {
			dataOutput.write(buffer, 0, bytesRead);
		}
		dataOutput.flush();
		dataOutput.close();
		dataInput.close();

		//readUntil("250 ");
		dataSocket.close();
		return true;
	}
	
	public void rename(String [] args) throws Exception{
		controlOutput.println("RNFR " + args[1]);
		String reply = controlInput.readLine();
		System.out.println(reply);
		controlOutput.println("RNTO " + args[2]);
		reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void delete(String [] args) throws Exception {
		controlOutput.println("DELE " + args[1]);
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void mkdir(String [] args) throws Exception {
		controlOutput.println("MKD " + args[1]);
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void cd(String [] args) throws Exception{
		controlOutput.println("CWD "+ args[1]);
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void type(String [] args) throws Exception{
		controlOutput.println("TYPE A");
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void noop(String [] args) throws Exception{
		controlOutput.println("NOOP ");
		String reply = controlInput.readLine();
		System.out.println(reply);
	}
	
	public void quit() throws Exception {
		if(dataSocket != null)
			dataSocket.close();
		if(controlSocket != null)
			controlSocket.close();
		System.out.println("quit and bye!");
	}


	public static void main(String[] args) throws Exception {
		FTPClient ftp = new FTPClient();
		ftp.dispatch();
	}
}