package com.chiliasmstudio.ProjectZomboidServerManager.function.projectzomboid;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateStateTest {
    private static final long START = 1000;

    private static JSONObject item(long id, long timeUpdated) {
        return new JSONObject().put("publishedfileid", String.valueOf(id)).put("result", 1).put("time_updated", timeUpdated);
    }

    private static JSONObject failed(long id) {
        return new JSONObject().put("publishedfileid", String.valueOf(id)).put("result", 9);
    }

    @Test
    void 未記錄的模組以啟動時間為基準() {
        UpdateState state = new UpdateState(START);
        List<JSONObject> updated = state.findUpdatedItems(new JSONArray().put(item(1, 900)).put(item(2, 1100)));

        assertEquals(1, updated.size());
        assertEquals(2L, updated.get(0).optLong("publishedfileid"));
    }

    @Test
    void 記錄後只回傳時間變新的模組() {
        UpdateState state = new UpdateState(START);
        state.record(new JSONArray().put(item(1, 1100)).put(item(2, 1200)));

        List<JSONObject> updated = state.findUpdatedItems(new JSONArray().put(item(1, 1150)).put(item(2, 1200)));
        assertEquals(1, updated.size());
        assertEquals(1L, updated.get(0).optLong("publishedfileid"));
    }

    @Test
    void 查詢失敗的模組被略過且保留原記錄() {
        UpdateState state = new UpdateState(START);
        state.record(new JSONArray().put(item(1, 1100)));

        // 本輪模組 1 查詢失敗：不視為更新，也不覆蓋記錄
        JSONArray withFailure = new JSONArray().put(failed(1)).put(item(2, 900));
        assertTrue(state.findUpdatedItems(withFailure).isEmpty());
        state.record(withFailure);

        // 下一輪成功時仍能偵測到期間的更新
        assertEquals(1, state.findUpdatedItems(new JSONArray().put(item(1, 1300))).size());
    }

    @Test
    void 判斷模組是否有效() {
        assertTrue(UpdateState.isValid(item(1, 1)));
        assertFalse(UpdateState.isValid(failed(1)));
        assertFalse(UpdateState.isValid(null));
    }
}
