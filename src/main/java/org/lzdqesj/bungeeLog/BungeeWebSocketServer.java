package org.lzdqesj.bungeeLog;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class BungeeWebSocketServer extends WebSocketServer {

    private final BungeeLog plugin;
    private final String password;
    private final ConcurrentHashMap<WebSocket, Boolean> authenticatedClients;
    private ScheduledExecutorService executor;

    public BungeeWebSocketServer(BungeeLog plugin, String host, int port, String password) {
        super(new InetSocketAddress(host, port));
        this.plugin = plugin;
        this.password = password;
        this.authenticatedClients = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        plugin.getLogger().info("WebSocket 客户端连接: " + conn.getRemoteSocketAddress());
        authenticatedClients.put(conn, false);

        // 5秒后检查是否认证
        executor.schedule(() -> {
            if (authenticatedClients.containsKey(conn) && !authenticatedClients.get(conn)) {
                plugin.getLogger().warning("WebSocket 客户端未认证，断开连接: " + conn.getRemoteSocketAddress());
                conn.close(4001, "Authentication timeout");
                authenticatedClients.remove(conn);
            }
        }, 5, TimeUnit.SECONDS);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        authenticatedClients.remove(conn);
        plugin.getLogger().info("WebSocket 客户端断开: " + conn.getRemoteSocketAddress() + " - " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        Boolean isAuth = authenticatedClients.get(conn);
        if (isAuth == null) return;

        if (!isAuth) {
            // 验证密码
            if (message.trim().equals(password)) {
                authenticatedClients.put(conn, true);
                conn.send("{\"type\":\"auth\",\"status\":\"success\"}");
                plugin.getLogger().info("WebSocket 客户端认证成功: " + conn.getRemoteSocketAddress());

                // 发送 WebAPI 已启动消息
                String startedMsg = "{\"type\":\"bungeelogwebapi\",\"message\":\"started\"}";
                conn.send(startedMsg);
            } else {
                conn.send("{\"type\":\"auth\",\"status\":\"failed\"}");
                plugin.getLogger().warning("WebSocket 客户端认证失败: " + conn.getRemoteSocketAddress());
                conn.close(4002, "Invalid password");
                authenticatedClients.remove(conn);
            }
            return;
        }

        // 已认证客户端可以发送命令等
        plugin.getLogger().info("WebSocket 消息: " + message);
        
        // 处理客户端请求
        try {
            // 简单的 JSON 解析，处理 {"type":"command","command":"占位符"} 格式
            if (message.contains("\"type\":\"command\"")) {
                // 提取 command 值
                int commandStart = message.indexOf("\"command\":\"") + ("\"command\":\"").length();
                int commandEnd = message.indexOf("\"", commandStart);
                if (commandStart > -1 && commandEnd > commandStart) {
                    String command = message.substring(commandStart, commandEnd);
                    plugin.getLogger().info("执行命令: " + command);
                    
                    // 以控制台身份执行命令
                    plugin.getProxy().getPluginManager().dispatchCommand(
                        plugin.getProxy().getConsole(), 
                        command
                    );
                    
                    // 发送执行结果
                    conn.send("{\"type\":\"command\",\"status\":\"success\",\"command\":\"" + command + "\"}");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("处理客户端请求失败: " + e.getMessage());
            conn.send("{\"type\":\"command\",\"status\":\"error\",\"message\":\"处理请求失败\"}");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        plugin.getLogger().severe("WebSocket 错误: " + ex.getMessage());
        if (conn != null) {
            authenticatedClients.remove(conn);
        }
    }

    @Override
    public void onStart() {
        plugin.getLogger().info("WebSocket 服务器启动成功");
        setConnectionLostTimeout(30);
    }

    // 广播消息到所有已认证客户端
    public void broadcast(String message) {
        for (WebSocket conn : authenticatedClients.keySet()) {
            if (authenticatedClients.get(conn) && conn.isOpen()) {
                conn.send(message);
            }
        }
    }

    // 停止服务器
    public void stopServer() {
        try {
            executor.shutdown();
            // 广播停止消息
            broadcast("{\"type\":\"bungeelogwebapi\",\"message\":\"stopped\"}");
            // 关闭所有连接
            for (WebSocket conn : authenticatedClients.keySet()) {
                conn.close();
            }
            super.stop();
        } catch (Exception e) {
            plugin.getLogger().severe("WebSocket 服务器停止失败: " + e.getMessage());
        }
    }
}