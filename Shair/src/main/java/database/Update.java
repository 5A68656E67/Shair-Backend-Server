/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package database;

import com.google.gson.JsonObject;

/*
 * Update - interface
 * Methods Declaration:
 * 1. public void addItem(JsonObject jsonObject);
 * 2. public void updateProfile(JsonObject jsonObject);
 * 3. public void updateLike(JsonObject jsonObject);
 * 4. public void updateShare(JsonObject jsonObject);
 * 5. public void updateItem(JsonObject jsonObject);
 */
public interface Update {
	public void addItem(JsonObject jsonObject);
	public void updateProfile(JsonObject jsonObject);
	public void updateLike(JsonObject jsonObject);
	public void updateShare(JsonObject jsonObject);
	public void updateItem(JsonObject jsonObject);
}
