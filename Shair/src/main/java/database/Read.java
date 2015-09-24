/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/*
 * Read - interface
 * Methods Declaration:
 * 1. public boolean checkPassword(JsonObject jsonObject);
 * 2. public String getAllInfo(String account);
 * 3. public JsonArray getNearByItems(JsonObject jsonObject);
 * 4. public JsonObject getUserInfo(JsonObject jsonObject);
 * 5. public JsonArray search(JsonObject jsonObject);
 * 6. public JsonObject getNeederInfo(JsonObject jsonObject);
 * 7. public JsonArray getTransactionList(JsonObject jsonObject);
 * 8. public JsonArray getLikesList(JsonObject jsonObject);
 * 9. public String findNameByAccount(String account);
 * 10.public String findPasswordByAccount(String account);
 */
public interface Read {
	public boolean checkPassword(JsonObject jsonObject);
	public String getAllInfo(String account);
	public JsonArray getNearByItems(JsonObject jsonObject);
	public JsonObject getUserInfo(JsonObject jsonObject);
	public JsonArray search(JsonObject jsonObject);
	public JsonObject getNeederInfo(JsonObject jsonObject);
	public JsonArray getTransactionList(JsonObject jsonObject);
	public JsonArray getLikesList(JsonObject jsonObject);
	public String findNameByAccount(String account);
	public String findPasswordByAccount(String account);
}
