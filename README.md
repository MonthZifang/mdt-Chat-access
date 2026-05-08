<div align="center">
  <a href="https://github.com/MonthZifang/YUEYUEDAO-TECH">
    <img src="./md/logo.png" alt="YUEYUEDAO TECH Logo" width="720" />
  </a>

  <p><strong>YUEYUEDAO TECH 维护 MDT 聊天出入</strong></p>

  <p>
    <a href="https://github.com/MonthZifang/YUEYUEDAO-TECH"><strong>查看月月岛科技详情</strong></a>
  </p>
</div>

# MDT 聊天出入

将聊天写入全局玩家聊天信息，也可在玩家进服时以参数形式发送包含等级等信息的聊天内容，并把全局聊天记录控制在最近 200 条。

## 市场固定识别文件

仓库根目录固定提供以下文件，供插件市场识别：

```text
market.plugin.json
plugin.json
```

## 依赖

- `mdt-list-data-system`
- 可选依赖：`mdt-player-level-system`
- 可选依赖：`mdt-bound-unbound`

## 配置文件

首次启动后建议维护以下配置文件：

```text
config/mods/config/mdt-chat-access/chat-access.properties
```

- 支持把普通聊天、玩家进入消息和系统消息统一写入全局列表。
- 支持只保留最近 200 条聊天记录，超过后删除最早的一条。
- 支持通过参数插入玩家等级、绑定状态等文本。
- 支持其他插件按玩家或全服发送自定义聊天消息。

## 功能说明

- 聊天消息统一落到列表数据系统的全局聊天列表。
- 支持玩家进入服务器时追加插件生成的信息。
- 支持系统消息、玩家消息和自定义参数模板。
- 支持作为其他插件的聊天输出依赖。

## 数据与写入说明

- 建议默认聊天列表名使用 `global_chat`。
- 建议单条聊天对象包含 `time`、`player`、`message`、`type`、`extra` 字段。

## 命令

- `chat-access-status`：查看聊天出入插件当前状态。
- `chat-access-send <playerOrAll> <message...>`：发送一条插件自定义聊天消息。
- `chat-access-reload`：重新加载聊天出入配置。

## Help 注册备注

- `help mdt-chat-access`：查看 MDT 聊天出入 的独立命令说明。
- 中文备注建议写为“聊天状态、系统消息发送、聊天模板重载”。

## 插件入口

```text
com.mdt.chat.ChatAccessPlugin
```

## 版本规则

- 当前插件版本：`v1`
- 当前需求市场版本：`v1`
