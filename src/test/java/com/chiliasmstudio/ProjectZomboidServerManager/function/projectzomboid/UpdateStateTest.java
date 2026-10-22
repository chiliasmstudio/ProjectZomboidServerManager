package com.chiliasmstudio.ProjectZomboidServerManager.function.projectzomboid;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateStateTest {

    private static JSONObject item(long id, long timeUpdated) {
        return new JSONObject().put("publishedfileid", String.valueOf(id)).put("time_updated", timeUpdated);
    }

    @Test
    void 首次記錄不視為更新(@TempDir Path dir) {
        UpdateState state = new UpdateState(dir.resolve("s.json"));
        JSONArray details = new JSONArray().put(item(1, 100)).put(item(2, 200));

        assertTrue(state.findUpdatedItems(details).isEmpty());
        state.record(details);
        assertFalse(state.isEmpty());
    }

    @Test
    void 只回傳時間變新的模組(@TempDir Path dir) {
        UpdateState state = new UpdateState(dir.resolve("s.json"));
        state.record(new JSONArray().put(item(1, 100)).put(item(2, 200)));

        JSONArray next = new JSONArray().put(item(1, 150)).put(item(2, 200)).put(item(3, 999));
        List<JSONObject> updated = state.findUpdatedItems(next);

        assertEquals(1, updated.size());
        assertEquals(1L, updated.get(0).optLong("publishedfileid"));
    }

    @Test
    void 無效模組不影響比對且會被計數(@TempDir Path dir) {
        UpdateState state = new UpdateState(dir.resolve("s.json"));
        JSONArray details = new JSONArray().put(item(1, 100))
                .put(new JSONObject().put("publishedfileid", "2").put("result", 9));

        state.record(details);
        assertEquals(1, UpdateState.countInvalidItems(details));
        assertTrue(state.findUpdatedItems(details).isEmpty());
    }

    @Test
    void 儲存後可重新載入(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("sub").resolve("s.json");
        UpdateState state = new UpdateState(file);
        state.record(new JSONArray().put(item(1, 100)));
        state.save();

        UpdateState reloaded = new UpdateState(file);
        reloaded.load();
        assertEquals(1, reloaded.findUpdatedItems(new JSONArray().put(item(1, 101))).size());
        assertTrue(reloaded.findUpdatedItems(new JSONArray().put(item(1, 100))).isEmpty());
    }
}
