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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 記錄每個工作坊模組最後一次看到的 time_updated，並持久化到 JSON 檔。
 * 以 Steam 回傳的時間互相比較，不受本機時鐘誤差影響；管理程式重啟後也不會漏掉期間的更新。
 */
public class UpdateState {
    private final Path file;
    private final Map<Long, Long> itemTimes = new HashMap<>();

    public UpdateState(Path file) {
        this.file = file;
    }

    /**
     * 是否尚未建立過基準 (首次執行)。
     */
    public boolean isEmpty() {
        return itemTimes.isEmpty();
    }

    /**
     * 從檔案載入狀態，檔案不存在時維持空狀態。
     */
    public void load() throws IOException {
        itemTimes.clear();
        if (!Files.exists(file))
            return;
        JSONObject items = new JSONObject(Files.readString(file, StandardCharsets.UTF_8)).optJSONObject("items");
        if (items == null)
            return;
        for (String key : items.keySet())
            itemTimes.put(Long.parseLong(key), items.getLong(key));
    }

    /**
     * 先寫入暫存檔再取代，避免寫到一半中斷造成檔案損毀。
     */
    public void save() throws IOException {
        JSONObject items = new JSONObject();
        itemTimes.forEach((id, time) -> items.put(id.toString(), time));
        String content = new JSONObject().put("items", items).toString(2);

        if (file.getParent() != null)
            Files.createDirectories(file.getParent());
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, content, StandardCharsets.UTF_8);
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * 找出 time_updated 比已記錄值更新的模組。
     * 尚未記錄過的模組 (首次執行或新加入合集) 只建立基準，不視為需要更新。
     *
     * @param details GetPublishedFileDetails 回傳的 publishedfiledetails。
     * @return 需要更新的模組。
     */
    public List<JSONObject> findUpdatedItems(JSONArray details) {
        List<JSONObject> updated = new ArrayList<>();
        for (int i = 0; i < details.length(); i++) {
            JSONObject item = details.optJSONObject(i);
            if (item == null || !item.has("time_updated"))
                continue;
            Long known = itemTimes.get(item.optLong("publishedfileid"));
            if (known != null && item.getLong("time_updated") > known)
                updated.add(item);
        }
        return updated;
    }

    /**
     * 以本次查詢結果更新記錄。
     *
     * @param details GetPublishedFileDetails 回傳的 publishedfiledetails。
     */
    public void record(JSONArray details) {
        for (int i = 0; i < details.length(); i++) {
            JSONObject item = details.optJSONObject(i);
            if (item == null || !item.has("time_updated"))
                continue;
            itemTimes.put(item.optLong("publishedfileid"), item.getLong("time_updated"));
        }
    }

    /**
     * 回傳無法取得 time_updated 的模組數量 (例如已下架或設為私人)。
     */
    public static int countInvalidItems(JSONArray details) {
        int invalid = 0;
        for (int i = 0; i < details.length(); i++) {
            JSONObject item = details.optJSONObject(i);
            if (item == null || !item.has("time_updated"))
                invalid++;
        }
        return invalid;
    }
}
