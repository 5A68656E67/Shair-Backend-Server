/*
 * 18641 java smart phone development - final project - Shair - Web Server
 * Zheng Lei(zlei), Sen Yue(seny)
 */

package database;

import com.google.gson.JsonObject;

/*
 * Create - interface
 * Methods declaration:
 * 1. public void registerAccount(JsonObject jsonObject);
 * 2. public boolean checkDuplicateAccount(JsonObject jsonObject);
 */
public interface Create {
	public void registerAccount(JsonObject jsonObject);
	public boolean checkDuplicateAccount(JsonObject jsonObject);
}
