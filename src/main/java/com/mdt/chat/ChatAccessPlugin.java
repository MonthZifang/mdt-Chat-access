package com.mdt.chat;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Strings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import mindustry.Vars;
import mindustry.game.EventType.PlayerChatEvent;
import mindustry.game.EventType.PlayerJoin;
import mindustry.game.EventType.PlayerLeave;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

public final class ChatAccessPlugin extends Plugin {
    private static final String CONFIG_DIR_NAME = "mdt-chat-access";
    private static final String CONFIG_FILE_NAME = "chat-access.properties";
    private static final AtomicLong COUNTER = new AtomicLong();

    private volatile Config config;

    @Override
    public void init() {
        try {
            ensureDefaultResource();
            reloadConfig();
            Events.on(PlayerChatEvent.class, event -> recordChat(event.player, event.message));
            Events.on(PlayerJoin.class, event -> {
                if (config.broadcastJoin) {
                    Call.sendMessage(formatTemplate(config.joinTemplate, event.player, "", extraText(event.player)));
                }
            });
            Events.on(PlayerLeave.class, event -> {
                if (config.broadcastLeave) {
                    Call.sendMessage(formatTemplate(config.leaveTemplate, event.player, "", ""));
                }
            });
            Log.info("MDT 聊天出入 loaded.");
            Log.info("配置目录: @", resolveDataRoot().getAbsolutePath());
        } catch (IOException exception) {
            throw new RuntimeException("MDT 聊天出入初始化失败。", exception);
        }
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("chat-access-status", "查看聊天出入插件当前状态。", args -> {
            Log.info("chatList=@ writeToListData=@ joinBroadcast=@ leaveBroadcast=@",
                config.chatListName, config.writeToListData, config.broadcastJoin, config.broadcastLeave);
        });

        handler.register("chat-access-send", "<playerOrAll> <message...>", "发送一条插件自定义聊天消息。", args -> {
            String target = args[0].trim();
            String message = args[1] == null ? "" : args[1].trim();
            if ("all".equalsIgnoreCase(target)) {
                Call.sendMessage(formatSystemMessage(message));
                recordSystemMessage("all", message);
                Log.info("已广播系统消息。");
                return;
            }

            Player player = findPlayer(target);
            if (player == null) {
                Log.info("未找到玩家: @", target);
                return;
            }
            player.sendMessage(formatSystemMessage(message));
            recordSystemMessage(player.plainName(), message);
            Log.info("已向 @ 发送系统消息。", player.plainName());
        });

        handler.register("chat-access-reload", "重新加载聊天出入配置。", args -> {
            try {
                reloadConfig();
                Log.info("MDT 聊天出入已重载。chatList=@ maxRecords=@", config.chatListName, config.maxRecords);
            } catch (IOException exception) {
                Log.err("MDT 聊天出入重载失败: @", exception.getMessage());
            }
        });
    }

    private void recordChat(Player player, String message) {
        if (!config.writeToListData || player == null || message == null) {
            return;
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        values.put("type", "chat");
        values.put("name", player.plainName());
        values.put("uuid", player.uuid());
        values.put("comid", resolveComId(player.uuid()));
        values.put("message", message);
        values.put("time", nowText());
        listDataPutObject(config.chatListName, nextRecordKey(), values);
    }

    private void recordSystemMessage(String target, String message) {
        if (!config.writeToListData) {
            return;
        }
        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        values.put("type", "system");
        values.put("target", target);
        values.put("message", message);
        values.put("time", nowText());
        listDataPutObject(config.chatListName, nextRecordKey(), values);
    }

    private String formatSystemMessage(String message) {
        return config.systemTemplate.replace("{message}", message);
    }

    private String formatTemplate(String template, Player player, String message, String extra) {
        String result = template;
        result = result.replace("{name}", player == null ? "<unknown>" : player.plainName());
        result = result.replace("{message}", message == null ? "" : message);
        result = result.replace("{extra}", extra == null ? "" : extra);
        return result;
    }

    private String extraText(Player player) {
        String comId = resolveComId(player.uuid());
        if (comId == null) {
            return "";
        }
        return "(comid: " + comId + ")";
    }

    private Player findPlayer(String value) {
        String normalized = Strings.stripColors(value).trim();
        return Groups.player.find(player ->
            player.plainName().equalsIgnoreCase(normalized)
                || Strings.stripColors(player.name).equalsIgnoreCase(normalized)
                || player.uuid().equalsIgnoreCase(normalized)
        );
    }

    private void reloadConfig() throws IOException {
        Properties properties = new Properties();
        InputStreamReader reader = new InputStreamReader(new FileInputStream(new File(resolveDataRoot(), CONFIG_FILE_NAME)), StandardCharsets.UTF_8);
        try {
            properties.load(reader);
        } finally {
            reader.close();
        }
        config = new Config(
            readString(properties, "chat.list.name", "global_chat"),
            readBoolean(properties, "chat.writeToListData", true),
            readInt(properties, "chat.maxRecords", 200),
            readBoolean(properties, "chat.broadcastJoin", true),
            readBoolean(properties, "chat.broadcastLeave", true),
            readString(properties, "template.join", "[lime]{name}[] 进入了服务器 {extra}"),
            readString(properties, "template.leave", "[scarlet]{name}[] 离开了服务器"),
            readString(properties, "template.system", "[accent]{message}[]")
        );
    }

    private String readString(Properties properties, String englishKey, String fallback) {
        String value = properties.getProperty(englishKey);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private boolean readBoolean(Properties properties, String englishKey, boolean fallback) {
        String value = properties.getProperty(englishKey);
        return value == null || value.trim().isEmpty() ? fallback : Boolean.parseBoolean(value.trim());
    }

    private int readInt(Properties properties, String englishKey, int fallback) {
        String value = properties.getProperty(englishKey);
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private void ensureDefaultResource() throws IOException {
        File dataRoot = resolveDataRoot();
        if (!dataRoot.exists() && !dataRoot.mkdirs() && !dataRoot.isDirectory()) {
            throw new IOException("无法创建配置目录: " + dataRoot.getAbsolutePath());
        }
        File configFile = new File(dataRoot, CONFIG_FILE_NAME);
        if (configFile.exists()) {
            return;
        }
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
        if (inputStream == null) {
            throw new IOException("缺少默认资源: " + CONFIG_FILE_NAME);
        }
        try {
            Files.copy(inputStream, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            inputStream.close();
        }
    }

    private File resolveDataRoot() {
        File modsRoot = new File(Vars.dataDirectory.absolutePath(), "mods");
        return new File(new File(modsRoot, "config"), CONFIG_DIR_NAME);
    }

    private void listDataPutObject(String listName, String key, Map<String, String> values) {
        try {
            Class<?> listDataClass = Class.forName("com.mdt.listdata.ListDataSystemPlugin");
            Method method = listDataClass.getMethod("putObject", String.class, String.class, Map.class);
            method.invoke(null, listName, key, values);
        } catch (Exception exception) {
            Log.err("写入聊天记录失败: @", exception.getMessage());
        }
    }

    private String resolveComId(String uuid) {
        try {
            Class<?> jumpPluginClass = Class.forName("com.mdt.jump.JumpComIdPlugin");
            Object api = jumpPluginClass.getMethod("getApi").invoke(null);
            if (api == null) return "";
            Object record = api.getClass().getMethod("getOrCreate", String.class).invoke(api, uuid);
            if (record == null) return "";
            Object value = record.getClass().getMethod("getComId").invoke(record);
            return value == null ? "" : value.toString();
        } catch (Exception exception) {
            return "";
        }
    }

    private String nextRecordKey() {
        return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + "-" + COUNTER.incrementAndGet();
    }

    private String nowText() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    private static final class Config {
        private final String chatListName;
        private final boolean writeToListData;
        private final int maxRecords;
        private final boolean broadcastJoin;
        private final boolean broadcastLeave;
        private final String joinTemplate;
        private final String leaveTemplate;
        private final String systemTemplate;

        private Config(
            String chatListName,
            boolean writeToListData,
            int maxRecords,
            boolean broadcastJoin,
            boolean broadcastLeave,
            String joinTemplate,
            String leaveTemplate,
            String systemTemplate
        ) {
            this.chatListName = chatListName;
            this.writeToListData = writeToListData;
            this.maxRecords = maxRecords;
            this.broadcastJoin = broadcastJoin;
            this.broadcastLeave = broadcastLeave;
            this.joinTemplate = joinTemplate;
            this.leaveTemplate = leaveTemplate;
            this.systemTemplate = systemTemplate;
        }
    }
}
