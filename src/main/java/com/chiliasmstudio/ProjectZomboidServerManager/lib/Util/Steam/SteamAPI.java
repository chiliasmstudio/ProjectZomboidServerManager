/*
 * < ProjectZomboidServerMannger - Project Zomboid server manage software >
 *     Copyright (C) 2022-2024 chiliasmstudio
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.chiliasmstudio.ProjectZomboidServerManager.lib.Util.Steam;

import com.chiliasmstudio.ProjectZomboidServerManager.Config;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SteamAPI {
    private final static Logger SteamApiLogger = LogManager.getLogger("SteamAPI");

    /**
     * 請求失敗後的重試次數。
     */
    private static final int RETRY_COUNT = 3;

    /**
     * 每次重試前的等待時間 (毫秒)。
     */
    private static final long RETRY_INTERVAL_MS = 30 * 1000L;

    /**
     * 取得合集內的模組 ID，失敗時每 30 秒重試，共重試 3 次。
     *
     * @return 模組 ID 清單；全部失敗時回傳 null。
     */
    public static ArrayList<Long> getCollectionDetail(long collectionID) throws InterruptedException {
        return withRetry("GetCollectionDetails", () -> requestCollectionDetail(collectionID));
    }

    /**
     * 一次查詢所有模組的資訊，失敗時每 30 秒重試，共重試 3 次。
     *
     * @return 各模組的 publishedfiledetails；全部失敗時回傳 null。
     */
    public static JSONArray getPublishedFileDetails(List<Long> itemList) throws InterruptedException {
        if (itemList == null || itemList.isEmpty())
            return new JSONArray();
        return withRetry("GetPublishedFileDetails", () -> requestPublishedFileDetails(itemList));
    }

    /**
     * 執行請求，回傳 null 視為失敗並在間隔後重試。
     */
    private static <T> T withRetry(String apiName, Supplier<T> request) throws InterruptedException {
        T result = request.get();
        for (int retry = 1; result == null && retry <= RETRY_COUNT; retry++) {
            SteamApiLogger.warn(apiName + " failed, retry " + retry + "/" + RETRY_COUNT + " in " + RETRY_INTERVAL_MS / 1000 + " seconds.");
            Thread.sleep(RETRY_INTERVAL_MS);
            result = request.get();
        }
        if (result == null)
            SteamApiLogger.error(apiName + " failed after " + RETRY_COUNT + " retries, wait for next check.");
        return result;
    }

    /**
     * Get and return workshop item ids in collection.
     */
    private static ArrayList<Long> requestCollectionDetail(long collectionID) {
        HttpPost httpPost = new HttpPost("https://api.steampowered.com/ISteamRemoteStorage/GetCollectionDetails/v1");
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("key", Config.steamKey));
        nvps.add(new BasicNameValuePair("format", "json"));
        nvps.add(new BasicNameValuePair("collectioncount", "1"));
        nvps.add(new BasicNameValuePair("publishedfileids[0]", String.valueOf(collectionID)));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        ArrayList<Long> itemList = new ArrayList<Long>();
        
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httpPost)) {
            //System.out.println("GetCollectionDetail with " + response.getCode() + " " + response.getReasonPhrase());
            HttpEntity entity = response.getEntity();
            // Response form steam as string.
            String responseBody = EntityUtils.toString(entity, "UTF-8");

            // Return item ids in collection.
            JSONObject obj = new JSONObject(responseBody);
            JSONArray collectiondetails = obj
                    .getJSONObject("response")
                    .getJSONArray("collectiondetails")
                    .getJSONObject(0)
                    .getJSONArray("children");
            //System.out.println(collectionID + " include " + collectiondetails.length() + " items");
            // 單一項目格式錯誤時只略過該項目，不影響整份清單
            for (int i = 0; i < collectiondetails.length(); i++) {
                try {
                    itemList.add(Long.parseLong(collectiondetails.getJSONObject(i).getString("publishedfileid")));
                } catch (Exception e) {
                    SteamApiLogger.warn("Skip invalid item #" + i + " in collection " + collectionID + ": " + e.getMessage());
                }
            }
            return itemList;
        } catch (Exception e) {
            SteamApiLogger.warn("Failed to get collection details for collectionID " + collectionID + ": " + e);
            return null;
        }
    }

    /**
     * Get and return workshop item info in json.
     */
    private static JSONArray requestPublishedFileDetails(List<Long> itemList) {
        HttpPost httpPost = new HttpPost("https://api.steampowered.com/ISteamRemoteStorage/GetPublishedFileDetails/v1/?");
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("key", Config.steamKey));
        nvps.add(new BasicNameValuePair("format", "json"));
        nvps.add(new BasicNameValuePair("itemcount", String.valueOf(itemList.size())));
        for (int i = 0; i < itemList.size(); i++)
            nvps.add(new BasicNameValuePair("publishedfileids[" + i + "]", itemList.get(i).toString()));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));

        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(httpPost)) {
            //System.out.println("GetPublishedFileDetails with " + response.getCode() + " " + response.getReasonPhrase());
            HttpEntity entity = response.getEntity();

            // Response form steam as string.
            String responseBody = EntityUtils.toString(entity, "UTF-8");
            JSONObject obj = new JSONObject(responseBody);
            JSONArray publishedfiledetails = obj
                    .getJSONObject("response")
                    .getJSONArray("publishedfiledetails");

            return publishedfiledetails;
        } catch (Exception e) {
            SteamApiLogger.warn("Failed to get published file details for " + itemList.size() + " items: " + e);
            return null;
        }
    }
}
