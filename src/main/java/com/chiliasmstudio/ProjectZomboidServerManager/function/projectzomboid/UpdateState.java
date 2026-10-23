/*
 * < ProjectZomboidServerMannger - Project Zomboid server manage software >
 *     Copyright (C) 2022-2026 chiliasmstudio
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

package com.chiliasmstudio.ProjectZomboidServerManager.function.projectzomboid;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在記憶體中記錄每個工作坊模組最後一次看到的 time_updated，不寫入檔案。
 * 尚未成功檢查過的模組以管理程式啟動時間為基準，因此首次檢查失敗也不會漏掉啟動後的更新。
 */
public class UpdateState {
    private final long startTime;
    private final Map<Long, Long> itemTimes = new HashMap<>();

    /**
     * @param startTime 管理程式啟動時間 (Unix 秒)，作為尚未記錄模組的比較基準。
     */
    public UpdateState(long startTime) {
        this.startTime = startTime;
    }

    /**
     * 模組是否成功取得資料。Steam 對已下架、私人或查詢失敗的模組會回傳 result != 1 且沒有 time_updated。
     */
    public static boolean isValid(JSONObject item) {
        return item != null && item.optInt("result", 1) == 1 && item.has("time_updated");
    }

    /**
     * 找出 time_updated 比基準更新的模組，無效的模組會被略過。
     *
     * @param details GetPublishedFileDetails 回傳的 publishedfiledetails。
     * @return 需要更新的模組。
     */
    public List<JSONObject> findUpdatedItems(JSONArray details) {
        List<JSONObject> updated = new ArrayList<>();
        for (int i = 0; i < details.length(); i++) {
            JSONObject item = details.optJSONObject(i);
            if (!isValid(item))
                continue;
            long known = itemTimes.getOrDefault(item.optLong("publishedfileid"), startTime);
            if (item.getLong("time_updated") > known)
                updated.add(item);
        }
        return updated;
    }

    /**
     * 以本次查詢結果更新記錄。無效的模組保留原記錄，下一輪成功時仍能偵測到期間的更新。
     *
     * @param details GetPublishedFileDetails 回傳的 publishedfiledetails。
     */
    public void record(JSONArray details) {
        for (int i = 0; i < details.length(); i++) {
            JSONObject item = details.optJSONObject(i);
            if (isValid(item))
                itemTimes.put(item.optLong("publishedfileid"), item.getLong("time_updated"));
        }
    }
}
