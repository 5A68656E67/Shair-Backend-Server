/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * WebServer - wrapper class for the DefaultSocketServer
 */
public class WebServer {
	private ServerSocket serverSocket;
	// constructor - new ServerSocket with private IP of the AWS instance
	public WebServer(int port){
		try {
			this.serverSocket = new ServerSocket(port);
			System.out.println("Server starts.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	 * run() - start the server
	 * Using infinite loop to keep server running
	 */
	public void run() throws IOException{
		while(true){
			// the method blocks until a connection is made, so only when a new request is sent in
			// the server starts a new thread
			Socket socket = serverSocket.accept();
			DefaultSocketServer server = new DefaultSocketServer(socket);
			server.start();
		}
	}
}

