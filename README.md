# Project Zomboid Server Manager

自動偵測 Steam 工作坊模組更新，並在更新時透過 Rcon 廣播倒數、關閉 Project Zomboid 伺服器，同時發送 Discord 通知。

> 本程式只負責**關閉**伺服器。請以 systemd、Docker restart policy 等外部機制，讓伺服器在關閉後自動重新啟動並下載最新模組。

## 需求

- Java 25
- Discord Bot Token
- [Steam Web API 金鑰](https://steamcommunity.com/dev/apikey)
- 已開啟 Rcon 的 Project Zomboid 伺服器

## 建置

```bash
mvn package
```

產物位於 `target/ProjectZomboidServerManager-<版本>.jar`。

## 使用方式

1. 首次執行會在工作目錄建立 `config/config.properties` 與 `config/servers/server_example.properties`。
2. 在 `config/config.properties` 填入 `DiscordToken`、`SteamKey`，並視需要開關 Discord 指令。
3. 複製 `config/servers/server_example.properties` 為新檔 (例如 `myserver.properties`)，每台伺服器一個檔案。
4. 再次執行：

```bash
java -jar ProjectZomboidServerManager.jar
```

可加上 `-debug` 或 `-trace` 參數調整 log 等級。

## 主設定 `config/config.properties`

| 鍵 | 說明 |
|---|---|
| `DiscordToken` | Discord Bot Token (必填) |
| `SteamKey` | Steam Web API 金鑰 (必填) |
| `LuCat`、`HelloWorld`、`TestCommand`、`TestRestartCommand` | 以指令類別名稱開關 Discord 指令，`true` 啟用；未設定視為停用 |

`HelloWorld`、`TestCommand`、`TestRestartCommand` 為文字指令，啟用時需在 Discord Developer Portal 為 Bot 開啟 **Message Content Intent**。

## 伺服器設定 `config/servers/*.properties`

| 鍵 | 說明 |
|---|---|
| `ServerName` | 伺服器名稱 (用於 log 與通知) |
| `SteamCollections` | 伺服器使用的工作坊合集 ID |
| `TimeZone` | log 顯示時區，`auto` 或 TZ 名稱 (例如 `Asia/Taipei`) |
| `CheckFrequency` | 檢查間隔 (秒)，從上一輪結束後開始計算 |
| `RconIP`、`RconPort`、`RconPassword` | Rcon 連線資訊 |
| `DiscordChannel` | 通知頻道 ID，`0` 表示不發送 |
| `debugSkipRestartDelay` | `true` 時略過 10 分鐘倒數，10 秒後直接重啟 (除錯用) |
| `StartTime`、`StartServerByCommand`、`StartScriptDir` | 尚未支援 |

## 運作方式

- 每台伺服器各自依 `CheckFrequency` 排程檢查。
- 每輪檢查向 Steam 發出：1 次 `GetCollectionDetails` 取得合集清單，再以每批最多 100 個模組呼叫 `GetPublishedFileDetails` 取得更新時間 (不是每個模組各一次請求)。
- 各模組最後一次看到的 `time_updated` 只保存在記憶體，不寫入檔案；尚未記錄的模組以管理程式啟動時間為基準。管理程式停機期間的更新不會被偵測。
- 取得合集清單失敗時整輪略過；個別模組 (或某一批) 查詢失敗、已下架或設為私人時只略過該模組，並保留原記錄，下一輪成功時仍能偵測到。
- 偵測到模組更新時：發送 Discord 卡片 → 遊戲內倒數 10 分鐘 (5 分、1 分、30 秒、10 秒提醒) → 透過 Rcon 送出 `quit`。
- 只有重啟流程完成後才會更新記錄；途中中斷時下一輪會重新偵測。

## 授權

GNU General Public License v3.0，詳見 [LICENSE](LICENSE)。
