package org.lzdqesj.bungeeLog;

import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class LogListener implements Listener {

    private final BungeeLog plugin;

    public LogListener(BungeeLog plugin) {
        this.plugin = plugin;
    }

    // 1. 玩家加入事件
    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // 写入日志
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-player-connections", true)) {
            plugin.writeLog("INFO", String.format("[玩家加入] %s (%s)",
                    player.getName(),
                    player.getAddress().getAddress().getHostAddress()
            ));
        }

        // WebSocket 事件
        if (plugin.isWebapiEnabled()) {
            plugin.sendPlayerEvent("playerjoin", player);
        }
    }

    // 2. 玩家离开事件
    @EventHandler
    public void onPlayerQuit(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        // 写入日志
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-player-connections", true)) {
            plugin.writeLog("INFO", String.format("[玩家离开] %s",
                    player.getName()
            ));
        }

        // WebSocket 事件
        if (plugin.isWebapiEnabled()) {
            plugin.sendPlayerEvent("playerquit", player);
        }
    }

    // 3. 服务器连接事件
    @EventHandler
    public void onServerConnect(ServerConnectEvent event) {
        if (event.isCancelled()) return;

        ProxiedPlayer player = event.getPlayer();
        String targetServer = event.getTarget().getName();

        // 写入日志
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-server-switches", true)) {
            plugin.writeLog("INFO", String.format("[服务器连接] %s -> %s | 原因: %s",
                    player.getName(),
                    targetServer,
                    event.getReason().toString().toLowerCase()
            ));
        }
    }

    // 4. 服务器连接成功事件
    @EventHandler
    public void onServerConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String serverName = event.getServer().getInfo().getName();

        // 写入日志
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-server-switches", true)) {
            plugin.writeLog("INFO", String.format("[服务器连接成功] %s 已连接到 %s",
                    player.getName(),
                    serverName
            ));
        }

        // WebSocket 事件 - 玩家连接到子服
        if (plugin.isWebapiEnabled()) {
            plugin.sendPlayerEvent("playergotoserver", player,
                    "server", serverName,
                    "from", event.getPlayer().getServer() != null ?
                            event.getPlayer().getServer().getInfo().getName() : "none"
            );
        }
    }

    // 5. 服务器断开事件
    @EventHandler
    public void onServerDisconnect(ServerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        String serverName = event.getTarget().getName();

        // 写入日志
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-server-switches", true)) {
            plugin.writeLog("INFO", String.format("[服务器断开] %s 从 %s 断开",
                    player.getName(),
                    serverName
            ));
        }

        // WebSocket 事件 - 玩家离开子服
        if (plugin.isWebapiEnabled()) {
            plugin.sendPlayerEvent("playerleaveserver", player,
                    "server", serverName,
                    "to", player.getServer() != null ?
                            player.getServer().getInfo().getName() : "none"
            );
        }
    }

    // 6. 玩家被踢事件
    @EventHandler
    public void onPlayerKick(ServerKickEvent event) {
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-server-switches", true)) {
            plugin.writeLog("WARNING", String.format("[玩家被踢] %s 从 %s 被踢出 | 原因: %s",
                    event.getPlayer().getName(),
                    event.getKickedFrom().getName(),
                    event.getKickReasonComponent().toString()
            ));
        }
    }

    // 7. 聊天/命令事件
    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }

        ProxiedPlayer player = (ProxiedPlayer) event.getSender();
        String message = event.getMessage();

        if (event.isCommand()) {
            if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-commands", true)) {
                plugin.writeLog("INFO", String.format("[命令] %s: %s",
                        player.getName(), message
                ));
            }
        } else {
            if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-player-chat", true)) {
                plugin.writeLog("INFO", String.format("[聊天] %s: %s",
                        player.getName(), message
                ));
            }
        }
    }

    // 8. Ping 事件
    @EventHandler
    public void onPing(ProxyPingEvent event) {
        if (plugin.getConfig() != null && plugin.getConfig().getBoolean("log-pings", false)) {
            plugin.writeLog("INFO", String.format("[Ping] %s",
                    event.getConnection().getAddress().getAddress().getHostAddress()
            ));
        }
    }
}