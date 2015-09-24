/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package Version1.Shair;

import java.io.IOException;

import webserver.WebServer;

/*
 * App - driver class for the web server
 */
public class App 
{
    public static void main( String[] args )
    {
    	WebServer server = new WebServer(80);
		try {
			server.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
