import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Stack;
import java.util.StringTokenizer;


public class FTPServer extends Thread {
    public ServerSocket server = null;
    public Socket controlSocket = null;  
    public BufferedReader br = null;  
    public PrintWriter pw = null;  
    public static int port;
	
	class ServerThread extends Thread {  
        public Socket controlSocket = null;  
        public BufferedReader controlInput = null;  
        public PrintWriter controlOutput = null;  
        public String username = null;
        public String password = null;
        public boolean auth;
        public String pathfix;
        public Stack<String> path;
        public String typeMode = "A";
        
        private ServerSocket dataSocket = null;
        public InetAddress host;
        final static int backlog = 5;
        
        public ServerThread(Socket sk) {
        	this.controlSocket = sk;
        	host = sk.getInetAddress();
        }  
		
        public void run() {  
        	try {
        		pathfix="./data";
            	path = new Stack<String>();
                controlOutput = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream()), true);  
                controlInput = new BufferedReader(new InputStreamReader(controlSocket.getInputStream())); 
                controlOutput.println("220 welcome to FTPserver");               
                String cmdLine;
                while ((cmdLine = controlInput.readLine()) != null) {
                	System.out.println(cmdLine);
                	String [] arg = cmdLine.split(" ");
                	switch (arg[0]){
                		case "USER":	user(arg);
                	 					break;
                	 	case "PASS":	pass(arg);
                	 					break;
                	 	case "PASV":	pasv();
                	 					break;
                	 	case "PORT":	port(arg);
                	 					break;
                	 	case "TYPE":	type(arg);
                	 					break;
                	 	case "STOR":	stor(arg);
                	 					break;
                	 	case "LIST":	list(arg);
                	 					break;
                	 	case "RETR":	retr(arg);
                	 					break;
                	 	case "DELE":	dele(arg);
                	 					break;
                	 	case "CWD":		cd(arg);
                	 					break;
                	 	case "MKD":		mkdir(arg);
                	 					break;
                	 	case "NOOP":	noop(arg);
                	 					break;
                	 	default:		controlOutput.println("202 Command not implemented, superfluous at this site.");
                	 					break;
                	}
                	controlOutput.flush();
                	System.out.println("one cmd has been finished");
                }
            } catch (IOException e) {
            	e.printStackTrace();
            }  
        } 
		
        public void type(String [] args) {
        	if (args.length == 1) {
        		controlOutput.println("200 [type] in default ASCII");
        	} else if (args.length == 2) {
        		typeMode = args[1];
        		controlOutput.println("200 [type] changed");
        	} else {
        		controlOutput.println("500 Syntax error, command unrecognized.");
        	}
        }
       
        public void user(String [] args) {
        	if( args.length > 1) {
        		username = args[1];
        		controlOutput.println("331 User name okay, need password");
        	} else {
        		controlOutput.println("500 Syntax error, command unrecognized.");
        	}
        }
		
        public void pass(String [] args){
        	if(args.length > 1) {
        		password = args[1];
        		auth=true;
        		controlOutput.println("230 User logged in, proceed.");
        	} else {
        		controlOutput.println("500 Syntax error, command unrecognized.");
        	}
        }
		
        public String pwd() {
    		// List the current working directory.
    		String p = "/";
    		for (String e:path) {
    			p += e + "/";
    		}
    		return p;
    	}

    	private String getpath() {
    		return pathfix + pwd();
    	}
		
    	private boolean valid(String s) {
    		return (s.indexOf('/') < 0);
    	}
		
        public void cd(String [] args) {
        	if (args.length > 1) {
        		String dir = args[1];
        		if (!valid(dir)) {
        			controlOutput.println("451 move only one level at one time.'/' is not allowed");
        		} else {
        			if ("..".equals(dir)) {
        				if (path.size() > 0) {
        					path.pop();
        				} else {
        					controlOutput.println("451 Requested action aborted: already in root dir.");
        					return;
        				}
        			} else if (".".equals(dir)) {
        				return;
        			} else {
        				File f = new File(getpath());
        				if (!f.exists()){
        					controlOutput.println("451 Requested action aborted: Directory does not exist: " + dir);
        					return;
        				} else if (!f.isDirectory()){
        					controlOutput.println("451 Requested action aborted:Not a directory: " + dir);
        					return;
        				} else {
        					path.push(dir);
        				}
        			}
        			controlOutput.println("250 dir switched.");
        		}
        	} else {
        		controlOutput.println("500 Syntax error, command unrecognized.");
        	}
        }
		
        public void mkdir(String [] args) {
        	if(args.length > 1) {
        		String destDirName = getpath() + args[1];
        		File dir = new File(destDirName);
    		    if (dir.exists()) {
    		    	System.out.println("creating a folder" + destDirName + "failed, target folder already existed");
    		    	controlOutput.println("451 Requested dir exists.");
    		    	return;
    		    }
    		    if (!destDirName.endsWith(File.separator))
    		    	destDirName = destDirName + File.separator;
    		    if (dir.mkdirs()) {
	    		     System.out.println("creating a folder" + destDirName + "sucessed");
	    		     controlOutput.println("250 Requested file action okay, completed.");
    		    } else {
    		    	controlOutput.println("500 something wrong.");
    		    }
        	} else {
        		controlOutput.println("500 Syntax error, command unrecognized.");
        	}
        }
		
        public void dele(String [] args) {
        	if (args.length > 1) {
        		String destDirName=getpath()+args[1];
        		File dir = new File(destDirName);
    		    if (dir.exists()) {
    		    	if (!dir.isDirectory()) {
    		    		dir.delete();
	    		    	controlOutput.println("250 file delete");
	    		    	return;
    		    	} else {
    		    		delFolder(destDirName);
    		    		controlOutput.println("250 folder delete");
	    		    	return;
    		    	}
    		    } else {
    		    	controlOutput.println("451 no such file or folder");
    		    }
        	} else {
        		controlOutput.println("500 Syntax error, command unrecognized.");
        	}
        }
        
        public void pasv() throws IOException {
        	System.out.println("host1:" + host.toString());
        	String ss = host.toString().replace("/", "");
        	System.out.println("host1:" + ss);
        	InetAddress hostIP = InetAddress.getByName(ss);
        	StringTokenizer tokenizer = new StringTokenizer(ss, "."); 
        	
        	if (dataSocket != null) {
        		dataSocket.close();
        	}
        	dataSocket = new ServerSocket(0, backlog, hostIP);
        	InetSocketAddress h = (InetSocketAddress) (dataSocket.getLocalSocketAddress());
        	int p = h.getPort();
        	int p1 = p%256;
        	int p0 = p/256;
        	int b0 = (int) Integer.parseInt(tokenizer.nextToken());
        	int b1 = (int) Integer.parseInt(tokenizer.nextToken());
        	int b2 = (int) Integer.parseInt(tokenizer.nextToken());
        	int b3 = (int) Integer.parseInt(tokenizer.nextToken());
        	System.out.printf("227 [%d,%d,%d,%d,%d,%d]\n",b0,b1,b2,b3,p0,p1);
        	controlOutput.printf("227 [%d,%d,%d,%d,%d,%d]\n",b0,b1,b2,b3,p0,p1);
        }
        
        public void port(String [] args) throws IOException {
        	// port 127,0,0,1,4,32
    		StringTokenizer st = new StringTokenizer(args[1], ",");
    		String ip;
    		ip = st.nextToken() + "." + st.nextToken() + "." + st.nextToken() + "." + st.nextToken();
    		port = Integer.parseInt(st.nextToken()) * 256 + Integer.parseInt(st.nextToken());
    		//dataSocket = new ServerSocket(port, backlog, host);
    		controlOutput.println("200 [PORT] command okay");
        }
        
        public void noop(String [] args) {
        	controlOutput.println("200 [NOOP] Command okay");
        }
        
        public void list(String [] args) {
        	String [] lst= new File(getpath()).list();
        	//controlOutput.println("[LIST] processing");
        	String send="";
        	for(String t:lst){
        		send = send.concat(t).concat("\n");
        	}
        	send = send.concat(".\n");
        	send = send.concat("..\n");
        	ByteArrayInputStream is = new ByteArrayInputStream(send.getBytes());
        	new Thread(new GetThread(dataSocket, is, controlOutput)).start();
        }
		
        public void retr(String [] args) {
        	String destFile = getpath() + args[1];
        	FileInputStream f;
			try {
				f = new FileInputStream(destFile);
				new Thread(new GetThread(dataSocket, f, controlOutput)).start();
				//controlOutput.println("250 Requested file action okay, completed");
			} catch (FileNotFoundException e) {
					e.printStackTrace();
					controlOutput.println("451 no such file or folder");
			}
        }
		
        public void stor(String [] args) {
        	String destFile = getpath() + args[1];
			FileOutputStream f;
			try {
				f = new FileOutputStream(destFile);
				new Thread(new PutThread(dataSocket, f, controlOutput)).start();
				//controlOutput.println("250 Requested file action okay, completed");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				controlOutput.println("451 Requested action aborted: local error in processing");
			}
        }
		
        private class GetThread implements Runnable {
    		private ServerSocket dataChan = null;
    		private InputStream file = null;
    		PrintWriter pt;

    		public GetThread(ServerSocket s, InputStream f, PrintWriter out) {
    			dataChan = s;
    			file =  f;
    			pt = out;
    		}
			
    		public void run() {
    			try {
    				Socket xfer = dataChan.accept();
    				//pt.println("125 Data Connection already open; transfer starting");
    				BufferedOutputStream out = new BufferedOutputStream(xfer.getOutputStream());
    				System.out.println(file.toString());
    				byte[] sendBytes = new byte[4096];
    				int iLen = 0;
    				while ((iLen = file.read(sendBytes)) != -1) {
    					out.write(sendBytes, 0, iLen);
    				}
    				out.flush();
    				out.close();
    				xfer.close();
    				//pt.println("226 Closing Data Connection");
    				file.close();
    				System.out.println("get over");
    				pt.println("250 Requested file action okay, completed");
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	}
		
        private class PutThread implements Runnable {
    		private ServerSocket dataChan = null;
    		private FileOutputStream file = null;
    		PrintWriter pt;
    		
    		public PutThread(ServerSocket s, FileOutputStream f,PrintWriter out) {
    			dataChan = s;
    			file = f;
    			pt = out;
    		}

    		public void run() {
    			try {
    				Socket xfer = dataChan.accept();
    				BufferedInputStream in = new BufferedInputStream(xfer.getInputStream());
    				byte[] inputByte = new byte[4096];
    				int iLen = 0;
    				while ((iLen = in.read(inputByte)) != -1) {
    					file.write(inputByte, 0, iLen);
    				}
    				file.flush();
    				in.close();
    				xfer.close();
    				file.close();
    				System.out.println("get over");
    				pt.println("250 Requested file action okay, completed");
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	}

        public void delAllFile(String path) { 
            File file = new File(path); 
            if (!file.exists()) { 
                return; 
            } 
            if (!file.isDirectory()) { 
                return; 
            } 
            String[] tempList = file.list(); 
            File temp = null; 
            for (int i = 0; i < tempList.length; i++) { 
                if (path.endsWith(File.separator)) { 
                    temp = new File(path + tempList[i]); 
                } else { 
                    temp = new File(path + File.separator + tempList[i]); 
                } 
                if (temp.isFile()) { 
                    temp.delete(); 
                } 
                if (temp.isDirectory()) { 
                    delAllFile(path+"/"+ tempList[i]); 
                    delFolder(path+"/"+ tempList[i]);
                } 
            } 
        }
		
        public void delFolder(String folderPath) { 
            try { 
                delAllFile(folderPath); 
                String filePath = folderPath; 
                filePath = filePath.toString(); 
                java.io.File myFilePath = new java.io.File(filePath); 
                myFilePath.delete();
            } catch (Exception e) { 
                System.out.println("Error on deleting folder"); 
                e.printStackTrace(); 
            } 
        }     
    }  
	
    public FTPServer(int p) {
    	try {
    		server = new ServerSocket(p);
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    }  
  
    public void run() {   
    	while (true) {
    		System.out.println("FTPServer Begings Listening Incoming Request...");
    		try {
    			controlSocket = server.accept();
    			if(controlSocket != null) {
    				ServerThread th = new ServerThread(controlSocket);
    				th.start();
    			}
    			sleep(1000);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
    }  
  
    public static void main(String [] args) throws Exception {
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	System.out.print("Enter FTPServer's Listening Port Number: ");
    	String p = br.readLine();
    	port = Integer.parseInt(p);
    	System.out.printf("Port Number: %d\n", port);
    	if(port > 0 && port < 65536) {
    		System.out.println("Use Input Listening Port Number: " + p);
    		new FTPServer(port).start();
    	} else {
    		port = 21;
    		System.out.println("Use Default Listening Port Number 21");
    		new FTPServer(21).start();
    	}
    }  

}
