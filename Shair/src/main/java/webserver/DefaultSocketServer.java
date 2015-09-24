/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package webserver;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import database.DatabaseAdapter;
import email.SendEmail;

/*
 * DefaultSocketServer - socket server class - implements Thread
 * Methods: 1. run - every time a request is sent in, start a new thread
 * 			2. openConnection - initialize ObjectInputStream, ObjectOutputStream
 * 			3. handle - handle different requests from the client
 * 			4. 
 */
public class DefaultSocketServer extends Thread{
	
	private Socket connSocket;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	// constructor - initialize with socket
	public DefaultSocketServer(Socket socket){
		this.connSocket = socket;
	}
	
	public void run(){
		if(openConnection()){
			// loop to handle different operations request by clients
			handle();
		}
	}
	
	public boolean openConnection(){
		try{
			oos = new ObjectOutputStream(connSocket.getOutputStream());
			ois = new ObjectInputStream(connSocket.getInputStream());
		} catch(IOException socketError){
			socketError.printStackTrace();
			System.out.println("Connection failed.");
			return false;
		}
		return true;
	}

	public void handle(){
		DatabaseAdapter dbAdapter = new DatabaseAdapter();
		try {
			// get query type
			int queryType = (Integer) ois.readObject();
			switch(queryType){
			// register new account
			case 1 : 
				String registrationStr = (String) ois.readObject();
				JsonObject registrationJson = new JsonParser().parse(registrationStr).getAsJsonObject();
				if(dbAdapter.checkDuplicateAccount(registrationJson)){
					oos.writeObject(0);
				}else{
					oos.writeObject(1);
					System.out.println(registrationJson.toString());
					String account = registrationJson.get("account").getAsString();
					String password = registrationJson.get("password").getAsString();
					String name = registrationJson.get("user").getAsJsonObject().get("name").getAsString();
					new SendEmail(account,name,account,password).start();
					dbAdapter.registerAccount(registrationJson);
				}
				break;
			// check login password
			case 2:
				String keyPairStr = (String) ois.readObject();
				JsonObject keyPairJson = new JsonParser().parse(keyPairStr).getAsJsonObject();
				if(dbAdapter.checkPassword(keyPairJson)){
					oos.writeObject(1);
					oos.writeObject(dbAdapter.getAllInfo(keyPairJson.get("key").getAsString()));
					System.out.println(dbAdapter.getAllInfo(keyPairJson.get("key").getAsString()));
				}else{
					oos.writeObject(0);
					System.out.println("wrong password");
				}
				break;
			// receive new item and add it to the database
			case 3:
				String uploadedItemInfo = (String) ois.readObject();
				System.out.println(uploadedItemInfo);
				JsonObject uploadedItemJson = new JsonParser().parse(uploadedItemInfo).getAsJsonObject();
				dbAdapter.addItem(uploadedItemJson);
				oos.writeObject(1);
				break;
			// get nearby items according to longitude and latitude
			case 4:
				String locationStr = (String) ois.readObject();
				System.out.println(locationStr);
				JsonObject locationJson = new JsonParser().parse(locationStr).getAsJsonObject();
				JsonArray itemArray = dbAdapter.getNearByItems(locationJson);
				oos.writeObject(itemArray.toString());
				break;
			// update profile information
			case 5:
				String editProfileStr = (String) ois.readObject();
				System.out.println(editProfileStr);
				JsonObject editProfileJson = new JsonParser().parse(editProfileStr).getAsJsonObject();
				dbAdapter.updateProfile(editProfileJson);
				break;
			// get user's information according to item id
			case 6:
				String userItemStr = (String) ois.readObject();
				System.out.println(userItemStr);
				JsonObject userItemJson = new JsonParser().parse(userItemStr).getAsJsonObject();
				oos.writeObject(dbAdapter.getUserInfo(userItemJson).toString());
				break;
			// update like table
			case 7:
				String likeStr = (String) ois.readObject();
				System.out.println(likeStr);
				JsonObject likeJson = new JsonParser().parse(likeStr).getAsJsonObject();
				dbAdapter.updateLike(likeJson);
				break;
			// update item table with newly built share relationships
			case 8:
				String updateShareStr = (String) ois.readObject();
				System.out.println(updateShareStr);
				JsonObject updateShareJson = new JsonParser().parse(updateShareStr).getAsJsonObject();
				dbAdapter.updateShare(updateShareJson);
				break;
			// search for items according to the keyword
			case 9:
				String keywordStr = (String) ois.readObject();
				System.out.println(keywordStr);
				JsonObject keywordJson = new JsonParser().parse(keywordStr).getAsJsonObject();
				oos.writeObject(dbAdapter.search(keywordJson).toString());
				break;
			// update item information in the item table
			case 10:
				String updatedItemStr = (String) ois.readObject();
				System.out.println(updatedItemStr);
				JsonObject updatedItemJson = new JsonParser().parse(updatedItemStr).getAsJsonObject();
				dbAdapter.updateItem(updatedItemJson);
				break;
			// delete an item from the table
			case 11:
				String itemInfoStr = (String) ois.readObject();
				System.out.println(itemInfoStr);
				JsonObject itemInfoJson = new JsonParser().parse(itemInfoStr).getAsJsonObject();
				dbAdapter.deleteItem(itemInfoJson);
				break;
			// get share relationship list
			case 12:
				String userIdStr = (String) ois.readObject();
				System.out.println(userIdStr);
				JsonObject userIdJson = new JsonParser().parse(userIdStr).getAsJsonObject();
				oos.writeObject(dbAdapter.getTransactionList(userIdJson).toString());
				break;
			// get like relationship list
			case 13:
				String userIDStr = (String) ois.readObject();
				System.out.println(userIDStr);
				JsonObject userIDJson = new JsonParser().parse(userIDStr).getAsJsonObject();
				oos.writeObject(dbAdapter.getLikesList(userIDJson).toString());
				break;
			// get needer's information according to item id
			case 14:
				String itemIdStr = (String) ois.readObject();
				System.out.println(itemIdStr);
				JsonObject itemIdJson = new JsonParser().parse(itemIdStr).getAsJsonObject();
				oos.writeObject(dbAdapter.getNeederInfo(itemIdJson).toString());
				break;
			// find password and send email to specific email address
			case 15:
				String emailStr = (String) ois.readObject();
				System.out.println(emailStr);
				JsonObject emailJson = new JsonParser().parse(emailStr).getAsJsonObject();
				String address = emailJson.get("account").getAsString();
				if(dbAdapter.checkDuplicateAccount(emailJson)){
					String name = dbAdapter.findNameByAccount(address);
					String password = dbAdapter.findPasswordByAccount(address);
					new SendEmail(address, name, address, password).start();
				}
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}
