/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/*
 * DatabaseAdapter - database related class - implements Create, Read, Update, Delete
 * Used for CRUD operations in the database.
 * Methods:	1. connectDB - connect to the database
 * 			2. registerAccount - register new account
 * 			3. checkDuplicateAccount - check whether the account to be registered is already in the database
 * 			4. checkPassword - check the login password is correct or not
 * 			5. getAllInfo - get all the account information
 * 			6. getItemImages - get item images path
 * 			7. addItem - add new item to the database
 * 			8. updateProfile - update profile information
 * 			9. getNearByItems - get nearby items according to current latitude and longitude
 * 			10. getUserInfo - get user's information based on ids
 * 			11. updateLike - update likes relationships in the database
 * 			12. udapteShare - update Share relationships in the database
 * 			13. search - search items according to keyword
 * 			14. updateItem - update item informations
 * 			15. deleteItem - delete an item in the database
 * 			16. getNeederInfo - get needer's information according to id
 * 			17. getTransactionList - get transaction list according to id and share relationships
 * 			18. getLikesList - get like list according to id and like relationships
 * 			19. findNameByAccount - get user's name by account's email
 * 			20. findPasswordByAccount - get account's password by account's email
 */
public class DatabaseAdapter implements Create, Read, Update, Delete {
	
	private final static String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private final static String DB_URL = "jdbc:mysql://localhost:3306/shair";
	private final static String USER = "root";
	private final static String PASSWORD = "920918";
	private Connection conn;
	
	private void connectDB(){
		try{
			Class.forName(JDBC_DRIVER);
			conn = DriverManager.getConnection(DB_URL,USER,PASSWORD);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void registerAccount(JsonObject jsonObject){
		String account = jsonObject.get("account").getAsString();
		String password = jsonObject.get("password").getAsString();
		String name = jsonObject.get("user").getAsJsonObject().get("name").getAsString();
		connectDB();
		try {
			PreparedStatement stmt1 = conn.prepareStatement("INSERT INTO `account` (`account`, `password`) VALUES (?, ?);");
			Statement stmt2 = conn.createStatement();
			PreparedStatement stmt3 = conn.prepareStatement("INSERT INTO `user` (`name`,`account_id`) VALUES (?,?);");
			stmt1.setString(1,account);
			stmt1.setString(2, password);
			stmt1.executeUpdate();
			ResultSet set = stmt2.executeQuery("SELECT LAST_INSERT_ID();");
			set.next();
			int accountId = set.getInt(1);
			stmt3.setString(1, name);
			stmt3.setInt(2, accountId);
			stmt3.executeUpdate();
			stmt1.close();
			stmt3.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean checkDuplicateAccount(JsonObject jsonObject){
		String account = jsonObject.get("account").getAsString();
		connectDB();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM `account` WHERE `account` = ?");
			stmt.setString(1, account);
			ResultSet set = stmt.executeQuery();
			set.next();
			int count = set.getInt(1);
			if(count == 0){
				return false;
			}
			set.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public boolean checkPassword(JsonObject jsonObject){
		String account = jsonObject.get("key").getAsString();
		String password = jsonObject.get("value").getAsString();
		connectDB();
		try {
			PreparedStatement stmt = conn.prepareStatement("SELECT password from account where account = ?;");
			stmt.setString(1, account);
			ResultSet set = stmt.executeQuery();
			// no rows
			if(!set.isBeforeFirst()){
				set.close();
				stmt.close();
				conn.close();
				return false;
			}
			// has rows
			set.next();
			String realPassword = set.getString(1);
			if(!password.equals(realPassword)){
				set.close();
				stmt.close();
				conn.close();
				return false;
			}
			set.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public String getAllInfo(String account){
		connectDB();
		JsonObject accountJson = new JsonObject();
		JsonObject userJson = new JsonObject();
		try{
			PreparedStatement stmt1 = conn.prepareStatement("select account_id, password from account where account = ?;");
			stmt1.setString(1, account);
			ResultSet set1 = stmt1.executeQuery();
			set1.next();
			int accountID = set1.getInt(1);
			String password = set1.getString(2);
			set1.close();
			stmt1.close();
			accountJson.addProperty("account", account);
			accountJson.addProperty("password",password);
			
			PreparedStatement stmt2 = conn.prepareStatement("select * from user where account_id = ?;");
			stmt2.setInt(1, accountID);
			ResultSet set2 = stmt2.executeQuery();
			set2.next();
			int userID = set2.getInt(1);
			String userName = set2.getString(2);
			// 0 if null in database
			int birthdate = set2.getInt(3);
			// null if null in database
			String email = set2.getString(4);
			String phone = set2.getString(5);
			String address = set2.getString(6);
			set2.close();
			stmt2.close();
			userJson.addProperty("id", userID);
			userJson.addProperty("name",userName);
			userJson.addProperty("birthdate",birthdate);
			userJson.addProperty("email",email);
			userJson.addProperty("phone",phone);
			userJson.addProperty("address", address);
			
			PreparedStatement stmt2Image = conn.prepareStatement("select path from profile_image where user_id = ?;");
			stmt2Image.setInt(1, userID);
			ResultSet set2Image = stmt2Image.executeQuery();
			if(!set2Image.isBeforeFirst()){
				// no profile images
				userJson.addProperty("profile_image",(String) null); 
			}else{
				set2Image.next();
				userJson.addProperty("profile_image", set2Image.getString(1));
			}
			set2Image.close();
			stmt2Image.close();
			
			PreparedStatement stmt3 = conn.prepareStatement("select * from item where sharer_id = ? and needer_id = 0;");
			stmt3.setInt(1, userID);
			ResultSet set3 = stmt3.executeQuery();
			JsonArray postedItemsArray = new JsonArray();
			if(set3.isBeforeFirst()){
				while(set3.next()){
					JsonObject tempPostedItemJson = new JsonObject();
					int itemID = set3.getInt(1);
					tempPostedItemJson.addProperty("id", itemID);
					tempPostedItemJson.addProperty("name",set3.getString(2));
					tempPostedItemJson.addProperty("description",set3.getString(3));
					tempPostedItemJson.addProperty("new_degree",set3.getInt(4));
					tempPostedItemJson.addProperty("price",set3.getDouble(5));
					tempPostedItemJson.addProperty("duration",set3.getInt(6));
					tempPostedItemJson.addProperty("discuss",set3.getInt(7));
					tempPostedItemJson.addProperty("security_deposit",set3.getDouble(8));
					tempPostedItemJson.addProperty("start_date",set3.getInt(9));
					tempPostedItemJson.addProperty("deadline",set3.getInt(10));
					tempPostedItemJson.addProperty("longitude",set3.getDouble(11));
					tempPostedItemJson.addProperty("latitude",set3.getDouble(12));
					tempPostedItemJson.addProperty("needer_id",set3.getInt(13));
					tempPostedItemJson.addProperty("sharer_id",set3.getInt(14));
					tempPostedItemJson.add("images",getItemImages(itemID));
					postedItemsArray.add(tempPostedItemJson);
				}
			}
			userJson.add("posted_items", postedItemsArray);
			set3.close();
			stmt3.close();
			
			
			PreparedStatement stmt4 = conn.prepareStatement("select * from item where sharer_id = ? and needer_id > 0;");
			stmt4.setInt(1, userID);
			ResultSet set4 = stmt4.executeQuery();
			JsonArray sharedItemsArray = new JsonArray();
			if(set4.isBeforeFirst()){
				while(set4.next()){
					JsonObject tempSharedItemJson = new JsonObject();
					int itemID = set4.getInt(1);
					tempSharedItemJson.addProperty("id", itemID);
					tempSharedItemJson.addProperty("name",set4.getString(2));
					tempSharedItemJson.addProperty("description",set4.getString(3));
					tempSharedItemJson.addProperty("new_degree",set4.getInt(4));
					tempSharedItemJson.addProperty("price",set4.getDouble(5));
					tempSharedItemJson.addProperty("duration",set4.getInt(6));
					tempSharedItemJson.addProperty("discuss",set4.getInt(7));
					tempSharedItemJson.addProperty("security_deposit",set4.getDouble(8));
					tempSharedItemJson.addProperty("start_date",set4.getInt(9));
					tempSharedItemJson.addProperty("deadline",set4.getInt(10));
					tempSharedItemJson.addProperty("longitude",set4.getDouble(11));
					tempSharedItemJson.addProperty("latitude",set4.getDouble(12));
					tempSharedItemJson.addProperty("needer_id",set4.getInt(13));
					tempSharedItemJson.addProperty("sharer_id",set4.getInt(14));
					tempSharedItemJson.add("images",getItemImages(itemID));
					sharedItemsArray.add(tempSharedItemJson);
				}
			}
			userJson.add("shared_items", sharedItemsArray);
			set4.close();
			stmt4.close();
			
			
			PreparedStatement stmt5 = conn.prepareStatement("select * from item where needer_id = ?;");
			stmt5.setInt(1, userID);
			ResultSet set5 = stmt5.executeQuery();
			JsonArray borrowedItemsArray = new JsonArray();
			if(set5.isBeforeFirst()){
				while(set5.next()){
					JsonObject tempBorrowedItemJson = new JsonObject();
					int itemID = set5.getInt(1);
					tempBorrowedItemJson.addProperty("id", itemID);
					tempBorrowedItemJson.addProperty("name",set5.getString(2));
					tempBorrowedItemJson.addProperty("description",set5.getString(3));
					tempBorrowedItemJson.addProperty("new_degree",set5.getInt(4));
					tempBorrowedItemJson.addProperty("price",set5.getDouble(5));
					tempBorrowedItemJson.addProperty("duration",set5.getInt(6));
					tempBorrowedItemJson.addProperty("discuss",set5.getInt(7));
					tempBorrowedItemJson.addProperty("security_deposit",set5.getDouble(8));
					tempBorrowedItemJson.addProperty("start_date",set5.getInt(9));
					tempBorrowedItemJson.addProperty("deadline",set5.getInt(10));
					tempBorrowedItemJson.addProperty("longitude",set5.getDouble(11));
					tempBorrowedItemJson.addProperty("latitude",set5.getDouble(12));
					tempBorrowedItemJson.addProperty("needer_id",set5.getInt(13));
					tempBorrowedItemJson.addProperty("sharer_id",set5.getInt(14));
					tempBorrowedItemJson.add("images",getItemImages(itemID));
					borrowedItemsArray.add(tempBorrowedItemJson);
				}
			}
			userJson.add("borrowed_items", borrowedItemsArray);
			set5.close();
			stmt5.close();
			conn.close();
			accountJson.add("user", userJson);
		}catch(Exception e){
			e.printStackTrace();
		}
		return accountJson.toString();
	}
	
	private JsonArray getItemImages(int itemID){
		JsonArray itemImagesArray = new JsonArray();
		try{
			PreparedStatement stmtImage = conn.prepareStatement("select path from item_image where item_id = ?;");
			stmtImage.setInt(1, itemID);
			ResultSet setImage = stmtImage.executeQuery();
			if(setImage.isBeforeFirst()){
				while(setImage.next()){
					JsonObject imageJson = new JsonObject();
					imageJson.addProperty("path", setImage.getString(1));
					itemImagesArray.add(imageJson);
				}
			}
			setImage.close();
			stmtImage.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return itemImagesArray;
	}
	
	public void addItem(JsonObject jsonObject){
		connectDB();
		int userID = jsonObject.get("user_id").getAsInt();
		JsonObject itemJson = jsonObject.get("item").getAsJsonObject();
		String name = itemJson.get("name").getAsString();
		double price = itemJson.get("price").getAsDouble();
		int duration = itemJson.get("duration").getAsInt();
		boolean discuss = itemJson.get("discuss").getAsBoolean();
		int newDegree = itemJson.get("new_degree").getAsInt();
		double latitude = itemJson.get("latitude").getAsDouble();
		double longitude = itemJson.get("longitude").getAsDouble();
		double securityDeposit = itemJson.get("security_deposit").getAsDouble();
		int startDate = 0;
		int deadline = itemJson.get("deadline").getAsInt();
		String description = itemJson.get("description").getAsString();
		int sharerID = userID;
		int neederID = 0;
		JsonArray imagesArray = itemJson.get("images").getAsJsonArray();
		ArrayList<String> imagesPath = new ArrayList<String>();
		for(int i = 0; i < imagesArray.size(); i++){
			imagesPath.add(imagesArray.get(i).getAsJsonObject().get("path").getAsString());
		}
		try{
			PreparedStatement stmt1 = conn.prepareStatement("insert into `item` (`name`,`description`,`new_degree`,"
					+ "`price`,`duration`,`discuss`,`security_deposit`,`start_date`,`deadline`,`longitude`,`latitude`,"
					+ "`needer_id`,`sharer_id`) values (?,?,?,?,?,?,?,?,?,?,?,?,?)");
			Statement stmt2 = conn.createStatement();
			stmt1.setString(1, name);
			stmt1.setString(2, description);
			stmt1.setInt(3,newDegree);
			stmt1.setDouble(4,price);
			stmt1.setInt(5,duration);
			stmt1.setBoolean(6,discuss);
			stmt1.setDouble(7,securityDeposit);
			stmt1.setInt(8,startDate);
			stmt1.setInt(9,deadline);
			stmt1.setDouble(10,longitude);
			stmt1.setDouble(11,latitude);
			stmt1.setInt(12, neederID);
			stmt1.setInt(13,sharerID);
			stmt1.executeUpdate();
			stmt1.close();
			
			ResultSet set = stmt2.executeQuery("SELECT LAST_INSERT_ID();");
			set.next();
			int itemID = set.getInt(1);
			set.close();
			stmt2.close();
			
			PreparedStatement stmt3 = conn.prepareStatement("insert into `item_image` (`path`,`item_id`) values (?,?);");
			for(String path : imagesPath){
				stmt3.setString(1,path);
				stmt3.setInt(2,itemID);
				stmt3.executeUpdate();
			}
			stmt3.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void updateProfile(JsonObject jsonObject){
		int id = jsonObject.get("id").getAsInt();
		String name = jsonObject.get("name").getAsString();
		int birthdate = jsonObject.get("birthdate").getAsInt();
		String email = jsonObject.get("email").getAsString();
		String phone = jsonObject.get("phone").getAsString();
		String location = jsonObject.get("location").getAsString();
		String imagePath = jsonObject.get("image_path").isJsonNull() ? null : jsonObject.get("image_path").getAsString();
		connectDB();
		try{
			PreparedStatement stmt1 = conn.prepareStatement("update `user` set `name` = ?, `birthdate` = ?, "
					+ "`email` = ?, `phone` = ?, `address` = ? where `user_id` = ?");
			
			stmt1.setString(1, name);
			stmt1.setInt(2,birthdate);
			stmt1.setString(3, email);
			stmt1.setString(4, phone);
			stmt1.setString(5, location);
			stmt1.setInt(6, id);
			stmt1.executeUpdate();
			stmt1.close();
			if(imagePath != null){
				PreparedStatement stmt2 = conn.prepareStatement("select * from profile_image where `user_id` = ?;");
				stmt2.setInt(1, id);
				ResultSet set = stmt2.executeQuery();
				if(!set.isBeforeFirst()){
					PreparedStatement stmt3 = conn.prepareStatement("insert into `profile_image` (`path`,`user_id`) values (?,?);");
					stmt3.setString(1, imagePath);
					stmt3.setInt(2,id);
					stmt3.executeUpdate();
					stmt3.close();
				}
				set.close();
				stmt2.close();
			}
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public JsonArray getNearByItems(JsonObject jsonObject){
		double latitude = jsonObject.get("latitude").getAsDouble();
		double longitude = jsonObject.get("longitude").getAsDouble();
		int userID = jsonObject.get("user_id").getAsInt();
		int start = jsonObject.get("start").getAsInt();
		double lowerLatitude = latitude - 0.1;
		double higherLatitude = latitude + 0.1;
		double lowerLongitude = longitude - 0.1;
		double higherLongitude = longitude + 0.1;
		int today = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(new Date()));
		connectDB();
		JsonArray itemsArray = new JsonArray();
		try{
			// get not self's, not shared, not due items in distance range
			PreparedStatement stmt = conn.prepareStatement("select * from `item` where `sharer_id` != ? "
					+ "and `needer_id` = 0 and `deadline` >= ? and `latitude` between ? and ? and `longitude` "
					+ "between ? and ? order by (price/duration) asc limit ?,?;");
			stmt.setInt(1,userID);
			stmt.setInt(2, today);
			stmt.setDouble(3, lowerLatitude);
			stmt.setDouble(4, higherLatitude);
			stmt.setDouble(5, lowerLongitude);
			stmt.setDouble(6, higherLongitude);
			stmt.setInt(7, start);
			stmt.setInt(8, start + 10);
			ResultSet set = stmt.executeQuery();
			if(set.isBeforeFirst()){
				while(set.next()){
					JsonObject item = new JsonObject();
					int itemID = set.getInt(1);
					item.addProperty("id",itemID);
					item.addProperty("name",set.getString(2));
					item.addProperty("description", set.getString(3));
					item.addProperty("new_degree",set.getInt(4));
					item.addProperty("price", set.getDouble(5));
					item.addProperty("duration",set.getDouble(6));
					item.addProperty("discuss",set.getBoolean(7));
					item.addProperty("security_deposit",set.getDouble(8));
					item.addProperty("deadline",set.getInt(10));
					item.addProperty("longitude", set.getDouble(11));
					item.addProperty("latitude", set.getDouble(12));
					item.addProperty("sharer_id", set.getInt(14));
					item.add("images",getItemImages(itemID));
					itemsArray.add(item);
				}
			}
			set.close();
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return itemsArray;
	}
	
	public JsonObject getUserInfo(JsonObject jsonObject){
		int userID = jsonObject.get("sharer_id").getAsInt();
		int itemID = jsonObject.get("item_id").getAsInt();
		int likerID = jsonObject.get("user_id").getAsInt();
		JsonObject userInfo = new JsonObject();
		connectDB();
		try{
			PreparedStatement stmt1 = conn.prepareStatement("select `name`,`email`,`phone` from `user` where `user_id` = ?;");
			stmt1.setInt(1, userID);
			ResultSet set1 = stmt1.executeQuery();
			set1.next();
			userInfo.addProperty("name", set1.getString(1));
			userInfo.addProperty("email",set1.getString(2));
			userInfo.addProperty("phone", set1.getString(3));
			set1.close();
			stmt1.close();
			PreparedStatement stmt2 = conn.prepareStatement("select `path` from `profile_image` where `user_id` = ?;");
			stmt2.setInt(1, userID);
			ResultSet set2 = stmt2.executeQuery();
			if(set2.isBeforeFirst()){
				set2.next();
				userInfo.addProperty("profile_image",set2.getString(1));
			}else{
				userInfo.addProperty("profile_image",(String) null);
			}
			set2.close();
			stmt2.close();
			PreparedStatement stmt3 = conn.prepareStatement("select * from `like` where `item_id` = ? and `liker_id` = ?;");
			stmt3.setInt(1, itemID);
			stmt3.setInt(2, likerID);
			ResultSet set3 = stmt3.executeQuery();
			if(set3.isBeforeFirst()){
				userInfo.addProperty("like_item", 1);
			}else{
				userInfo.addProperty("like_item", 0);
			}
			set3.close();
			stmt3.close();
			PreparedStatement stmt4 = conn.prepareStatement("select `needer_id` from `item` where `item_id` = ?;");
			stmt4.setInt(1, itemID);
			ResultSet set4 = stmt4.executeQuery();
			set4.next();
			userInfo.addProperty("needer_id", set4.getInt(1));
			set4.close();
			stmt4.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return userInfo;
	}
	
	public void updateLike(JsonObject jsonObject){
		int itemID = jsonObject.get("item_id").getAsInt();
		int userID = jsonObject.get("liker_id").getAsInt();
		int type = jsonObject.get("type").getAsInt();
		connectDB();
		try{
			if(type == 0){
				PreparedStatement stmt1 = conn.prepareStatement("delete from `like` where `item_id` = ? and `liker_id` = ?;");
				stmt1.setInt(1, itemID);
				stmt1.setInt(2, userID);
				stmt1.executeUpdate();
				stmt1.close();
			}else{
				PreparedStatement stmt2 = conn.prepareStatement("select * from `like` where `item_id` = ? and `liker_id` = ?;");
				stmt2.setInt(1, itemID);
				stmt2.setInt(2, userID);
				ResultSet set = stmt2.executeQuery();
				if(!set.isBeforeFirst()){
					PreparedStatement stmt3 = conn.prepareStatement("insert into `like` (`item_id`,`liker_id`) values (?,?);");
					stmt3.setInt(1, itemID);
					stmt3.setInt(2, userID);
					stmt3.executeUpdate();
					stmt3.close();
				}
				set.close();
				stmt2.close();
			}
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void updateShare(JsonObject jsonObject){
		int itemID = jsonObject.get("item_id").getAsInt();
		int neederID = jsonObject.get("needer_id").getAsInt();
		int startdate = jsonObject.get("startdate").getAsInt();
		connectDB();
		try{
			PreparedStatement stmt = conn.prepareStatement("update `item` set `needer_id` = ?, `start_date` = ? where `item_id` = ?;");
			stmt.setInt(1, neederID);
			stmt.setInt(2, startdate);
			stmt.setInt(3, itemID);
			stmt.executeUpdate();
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public JsonArray search(JsonObject jsonObject){
		String keyword = jsonObject.get("keyword").getAsString();
		int userID = jsonObject.get("user_id").getAsInt();
		int start = jsonObject.get("start").getAsInt();
		connectDB();
		JsonArray itemsArray = new JsonArray();
		try{
			PreparedStatement stmt = conn.prepareStatement("select * from item where (`name` like ? or `description` like ?) and `needer_id` = 0 and `sharer_id` != ? limit ?,?;");
			stmt.setString(1, "%" + keyword + "%");
			stmt.setString(2, "%" + keyword + "%");
			stmt.setInt(3, userID);
			stmt.setInt(4, start);
			stmt.setInt(5, start + 10);
			ResultSet set = stmt.executeQuery();
			if(set.isBeforeFirst()){
				while(set.next()){
					JsonObject item = new JsonObject();
					int itemID = set.getInt(1);
					item.addProperty("id",itemID);
					item.addProperty("name",set.getString(2));
					item.addProperty("description", set.getString(3));
					item.addProperty("new_degree",set.getInt(4));
					item.addProperty("price", set.getDouble(5));
					item.addProperty("duration",set.getDouble(6));
					item.addProperty("discuss",set.getBoolean(7));
					item.addProperty("security_deposit",set.getDouble(8));
					item.addProperty("deadline",set.getInt(10));
					item.addProperty("longitude", set.getDouble(11));
					item.addProperty("latitude", set.getDouble(12));
					item.addProperty("sharer_id", set.getInt(14));
					item.add("images",getItemImages(itemID));
					itemsArray.add(item);
				}
			}
			set.close();
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return itemsArray;
	}

	public void updateItem(JsonObject jsonObject){
		int itemID = jsonObject.get("id").getAsInt();
		String itemName = jsonObject.get("name").getAsString();
		String itemDescription = jsonObject.get("description").getAsString();
		int itemNewDegree = jsonObject.get("new_degree").getAsInt();
		double itemPrice = jsonObject.get("price").getAsDouble();
		int itemDuration = jsonObject.get("duration").getAsInt();
		boolean itemDiscuss = jsonObject.get("discuss").getAsBoolean();
		double itemSecurityDeposit = jsonObject.get("security_deposit").getAsDouble();
		int itemDeadline = jsonObject.get("deadline").getAsInt();
		connectDB();
		try{
			PreparedStatement stmt = conn.prepareStatement("update `item` set `name` = ?, `description` = ?, "
					+ "`new_degree` = ?, `price` = ?, `duration` = ?, `discuss` = ?, `security_deposit` = ?, "
					+ "`deadline` = ? where `item_id` = ?;");
			stmt.setString(1, itemName);
			stmt.setString(2, itemDescription);
			stmt.setInt(3, itemNewDegree);
			stmt.setDouble(4,itemPrice);
			stmt.setInt(5, itemDuration);
			stmt.setBoolean(6, itemDiscuss);
			stmt.setDouble(7, itemSecurityDeposit);
			stmt.setInt(8,itemDeadline);
			stmt.setInt(9,itemID);
			stmt.executeUpdate();
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void deleteItem(JsonObject jsonObject){
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials("<Enter Your Access Key Id here>","<Enter Your Secret Key here>");
		AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
		int itemID = jsonObject.get("id").getAsInt();
		connectDB();
		try{
			PreparedStatement stmt1 = conn.prepareStatement("select `path` from `item_image` where `item_id` = ?;");
			stmt1.setInt(1, itemID);
			ResultSet set1 = stmt1.executeQuery();
			if(set1.isBeforeFirst()){
				while(set1.next()){
					String path = set1.getString(1);
					DeleteObjectRequest deleteReq = new DeleteObjectRequest("shair-application-image","item-image" + path.substring(path.lastIndexOf("/")));
					System.out.println("Bucket: shair-application-image");
					System.out.println("profile-image" + path.substring(path.lastIndexOf("/")));
					s3Client.deleteObject(deleteReq);
				}
			}
			set1.close();
			stmt1.close();
			PreparedStatement stmt2 = conn.prepareStatement("delete from `item` where `item_id` = ?;");
			stmt2.setInt(1, itemID);
			stmt2.executeUpdate();
			stmt2.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public JsonObject getNeederInfo(JsonObject jsonObject){
		int id = jsonObject.get("needer_id").getAsInt();
		JsonObject neederJson = new JsonObject();
		connectDB();
		try{
			PreparedStatement stmt = conn.prepareStatement("select `name`,`email`,`phone` from `user` where `user_id` = ?;");
			stmt.setInt(1, id);
			ResultSet set = stmt.executeQuery();
			set.next();
			neederJson.addProperty("name", set.getString(1));
			neederJson.addProperty("email", set.getString(2));
			neederJson.addProperty("phone", set.getString(3));
			set.close();
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return neederJson;
	}
	
	public JsonArray getTransactionList(JsonObject jsonObject){
		int userId = jsonObject.get("user_id").getAsInt();
		JsonArray transactionList = new JsonArray();
		connectDB();
		try{
			PreparedStatement stmt1 = conn.prepareStatement("select `needer_id`,`item_id`,`name` from `item` where `sharer_id` = ? and `needer_id` != 0;");
			stmt1.setInt(1, userId);
			ResultSet set1 = stmt1.executeQuery();
			if(set1.isBeforeFirst()){
				while(set1.next()){
					JsonObject singleTransaction = new JsonObject();
					int neederId = set1.getInt(1);
					int itemId = set1.getInt(2);
					String itemName = set1.getString(3);
					singleTransaction.addProperty("item_name", itemName);
					PreparedStatement stmt2 = conn.prepareStatement("select `name` from `user` where `user_id` = ?;");
					stmt2.setInt(1, neederId);
					ResultSet set2 = stmt2.executeQuery();
					set2.next();
					singleTransaction.addProperty("needer_name",set2.getString(1));
					set2.close();
					stmt2.close();
					PreparedStatement stmt3 = conn.prepareStatement("select `path` from `item_image` where `item_id` = ? limit 0,1;");
					stmt3.setInt(1, itemId);
					ResultSet set3 = stmt3.executeQuery();
					if(set3.isBeforeFirst()){
						set3.next();
						singleTransaction.addProperty("item_img",set3.getString(1));
					}else{
						singleTransaction.addProperty("item_img", (String) null);
					}
					set3.close();
					stmt3.close();
					PreparedStatement stmt4 = conn.prepareStatement("select `path` from `profile_image` where `user_id` = ?;");
					stmt4.setInt(1, neederId);
					ResultSet set4 = stmt4.executeQuery();
					if(set4.isBeforeFirst()){
						set4.next();
						singleTransaction.addProperty("needer_img", set4.getString(1));
					}else{
						singleTransaction.addProperty("needer_img", (String) null);
					}
					set4.close();
					stmt4.close();
					transactionList.add(singleTransaction);
				}
			}
			set1.close();
			stmt1.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return transactionList;
	}
	
	public JsonArray getLikesList(JsonObject jsonObject){
		int userId = jsonObject.get("user_id").getAsInt();
		JsonArray likesList = new JsonArray();
		connectDB();
		try{
			PreparedStatement stmt1 = conn.prepareStatement("select `like`.`item_id`,`like`.`liker_id`,`item`.`name` "
					+ "from `like` inner join `item` on `like`.`item_id` = `item`.`item_id` and `item`.`sharer_id` = ?;");
			stmt1.setInt(1, userId);
			ResultSet set1 = stmt1.executeQuery();
			if(set1.isBeforeFirst()){
				while(set1.next()){
					JsonObject singleLike = new JsonObject();
					int itemId = set1.getInt(1);
					int likerId = set1.getInt(2);
					singleLike.addProperty("item_name", set1.getString(3));
					PreparedStatement stmt2 = conn.prepareStatement("select `name` from `user` where `user_id` = ?;");
					stmt2.setInt(1, likerId);
					ResultSet set2 = stmt2.executeQuery();
					set2.next();
					singleLike.addProperty("needer_name", set2.getString(1));
					set2.close();
					stmt2.close();
					PreparedStatement stmt3 = conn.prepareStatement("select `path` from `profile_image` where `user_id` = ?;");
					stmt3.setInt(1, likerId);
					ResultSet set3 = stmt3.executeQuery();
					if(set3.isBeforeFirst()){
						set3.next();
						singleLike.addProperty("needer_img", set3.getString(1));
					}else{
						singleLike.addProperty("needer_img", (String) null);
					}
					set3.close();
					stmt3.close();
					PreparedStatement stmt4 = conn.prepareStatement("select `path` from `item_image` where `item_id` = ? limit 0,1;");
					stmt4.setInt(1, itemId);
					ResultSet set4 = stmt4.executeQuery();
					if(set4.isBeforeFirst()){
						set4.next();
						singleLike.addProperty("item_img",set4.getString(1));
					}else{
						singleLike.addProperty("item_img", (String) null);
					}
					set4.close();
					stmt4.close();
					likesList.add(singleLike);
				}
			}
			set1.close();
			stmt1.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return likesList;
	}
	
	public String findNameByAccount(String account){
		String name = null;
		connectDB();
		try{
			PreparedStatement stmt = conn.prepareStatement("select `user`.`name` from `user` inner join `account` on `account`.`account_id` = `user`.`account_id` and `account`.`account` = ?;");
			stmt.setString(1, account);
			ResultSet set = stmt.executeQuery();
			set.next();
			name = set.getString(1);
			set.close();
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return name;
	}
	
	public String findPasswordByAccount(String account){
		String password = null;
		connectDB();
		try{
			PreparedStatement stmt = conn.prepareStatement("select `password` from `account` where `account` = ?;");
			stmt.setString(1, account);
			ResultSet set = stmt.executeQuery();
			set.next();
			password = set.getString(1);
			set.close();
			stmt.close();
			conn.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return password;
	}
}
