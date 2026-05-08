package com.mdt.chat;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.gen.Player;
import mindustry.mod.Plugin;

public final class ChatAccessPlugin extends Plugin {
    @Override
    public void init() {
        Log.info("MDT 聊天出入 loaded.");
        Log.info("配置目录建议: config/mods/config/mdt-chat-access");
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.register("chat-access-status", "查看聊天出入插件当前状态。", args -> {
            Log.info("MDT 聊天出入 命令占位已触发: chat-access-status");
        });

        handler.register("chat-access-send", "<playerOrAll> <message...>", "发送一条插件自定义聊天消息。", args -> {
            Log.info("MDT 聊天出入 命令占位已触发: chat-access-send");
        });

        handler.register("chat-access-reload", "重新加载聊天出入配置。", args -> {
            Log.info("MDT 聊天出入 命令占位已触发: chat-access-reload");
        });

    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        // No client commands are defined yet.
    }
}
