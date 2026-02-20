package org.lzdqesj.bungeeLog;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

public class BungeeLog extends Plugin implements Listener {

    private static BungeeLog instance;
    private File logFile;
    private PrintWriter logWriter;
    private String logFormat;
    private boolean enableConsoleMirror;
    private boolean dailyRolling;
    private String currentDate;
    private Configuration config;

    // WebAPI 相关
    private BungeeWebSocketServer webSocketServer;
    private boolean webapiEnabled;
    private String webapiAddress;
    private String webapiPassword;

    @Override
    public void onEnable() {
        instance = this;

        // 加载配置
        loadConfig();

        // 初始化日志系统
        setupLogging();

        // 注册监听器
        getProxy().getPluginManager().registerListener(this, new LogListener(this));

        // 注册命令
        getProxy().getPluginManager().registerCommand(this, new BungeeLogCommand(this));

        // 启动 WebSocket 服务器
        if (webapiEnabled) {
            startWebSocketServer();
        }

        getLogger().info("§a[BungeeLog] 插件已启用! WebAPI: " + (webapiEnabled ? "开启" : "关闭"));
    }

    @Override
    public void onDisable() {
        // 关闭 WebSocket 服务器
        if (webSocketServer != null) {
            webSocketServer.stopServer();  // 改为 stopServer()
            sendWebAPIMessage("stopped");
            webSocketServer = null;
        }

        // 关闭日志写入器
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
        }
        getLogger().info("§c[BungeeLog] 插件已禁用!");
    }

    // 加载配置
    private void loadConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                createDefaultConfig(configFile);
            }

            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);

            // 读取基本配置
            logFormat = config.getString("log-format", "[%time%] [%level%] %message%");
            enableConsoleMirror = config.getBoolean("enable-console-mirror", true);
            dailyRolling = config.getBoolean("daily-rolling", true);

            // 读取 WebAPI 配置
            webapiEnabled = config.getBoolean("webapi", false);
            webapiAddress = config.getString("waaddress", "0.0.0.0:25796");
            webapiPassword = config.getString("wapassword", "bungeelog");

        } catch (IOException e) {
            getLogger().severe("无法加载配置文件: " + e.getMessage());
            // 使用默认值
            logFormat = "[%time%] [%level%] %message%";
            enableConsoleMirror = true;
            dailyRolling = true;
            webapiEnabled = false;
            webapiAddress = "0.0.0.0:25796";
            webapiPassword = "bungeelog";
        }
    }

    // 创建默认配置文件
    private void createDefaultConfig(File configFile) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
            writer.println("# BungeeLog 配置文件");
            writer.println("# 日志格式");
            writer.println("log-format: \"[%time%] [%level%] %message%\"");
            writer.println("# 是否启用控制台镜像输出到文件");
            writer.println("enable-console-mirror: true");
            writer.println("# 是否按天分割日志文件");
            writer.println("daily-rolling: true");
            writer.println("# 日志级别");
            writer.println("log-level: \"ALL\"");
            writer.println("# 是否记录玩家连接事件");
            writer.println("log-player-connections: true");
            writer.println("# 是否记录玩家聊天消息");
            writer.println("log-player-chat: true");
            writer.println("# 是否记录命令执行");
            writer.println("log-commands: true");
            writer.println("# 是否记录服务器切换事件");
            writer.println("log-server-switches: true");
            writer.println("# 是否记录Ping请求");
            writer.println("log-pings: false");
            writer.println("# 最大日志文件数量");
            writer.println("max-log-files: 30");
            writer.println("# 日志编码");
            writer.println("encoding: \"UTF-8\"");
            writer.println("");
            writer.println("# ===== WebAPI 配置 =====");
            writer.println("# 是否启用 WebSocket API");
            writer.println("webapi: false");
            writer.println("# WebSocket 服务器地址和端口");
            writer.println("waaddress: \"0.0.0.0:25796\"");
            writer.println("# WebSocket 连接密码");
            writer.println("wapassword: \"bungeelog\"");
        } catch (IOException e) {
            getLogger().severe("无法创建默认配置文件: " + e.getMessage());
        }
    }

    // 启动 WebSocket 服务器
    private void startWebSocketServer() {
        try {
            String[] parts = webapiAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            webSocketServer = new BungeeWebSocketServer(this, host, port, webapiPassword);  // 修改这里
            webSocketServer.start();

            getLogger().info("§a[BungeeLog] WebSocket API 服务器启动在: " + webapiAddress);
            sendWebAPIMessage("started");

        } catch (Exception e) {
            getLogger().severe("§c[BungeeLog] WebSocket 服务器启动失败: " + e.getMessage());
            webapiEnabled = false;
        }
    }

    // 重新加载配置
    public void reloadLogging() {
        boolean wasWebapiEnabled = webapiEnabled;

        loadConfig();

        // 重新初始化日志系统
        if (logWriter != null) {
            logWriter.close();
        }
        setupLogging();

        // 处理 WebSocket 重启
        if (wasWebapiEnabled != webapiEnabled) {
            if (webapiEnabled) {
                startWebSocketServer();
            } else {
                if (webSocketServer != null) {
                    webSocketServer.stopServer();
                    sendWebAPIMessage("stopped");
                    webSocketServer = null;
                }
            }
        } else if (webapiEnabled && webSocketServer == null) {
            startWebSocketServer();
        }

        getLogger().info("§a[BungeeLog] 配置重载完成!");
    }

    // 重新加载配置（命令调用）
    public void reloadConfig() {
        reloadLogging();
    }

    // 获取配置
    public Configuration getConfig() {
        return config;
    }

    // 设置日志系统
    private void setupLogging() {
        try {
            // 创建 logs 文件夹
            File logsDir = new File(getDataFolder(), "logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }

            // 设置日志文件
            currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String fileName = dailyRolling ? "bungee-" + currentDate + ".log" : "bungee.log";
            logFile = new File(logsDir, fileName);

            // 初始化 PrintWriter
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);

            // 添加自定义日志处理器
            if (enableConsoleMirror) {
                Logger logger = Logger.getLogger("");
                LogHandler customHandler = new LogHandler(this);
                customHandler.setFormatter(new LogFormatter(this));
                logger.addHandler(customHandler);
            }

            // 启动日志轮换检查任务
            if (dailyRolling) {
                getProxy().getScheduler().schedule(this, () -> {
                    String nowDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                    if (!nowDate.equals(currentDate)) {
                        if (logWriter != null) {
                            logWriter.close();
                        }
                        setupLogging();
                    }
                }, 1, 1, TimeUnit.MINUTES);
            }

            // 清理旧日志文件
            if (config != null && config.getInt("max-log-files", 30) > 0) {
                cleanOldLogs();
            }

            writeLog("INFO", "BungeeLog 日志系统初始化完成");

        } catch (IOException e) {
            getLogger().severe("无法创建日志文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 清理旧日志文件
    private void cleanOldLogs() {
        try {
            File logsDir = new File(getDataFolder(), "logs");
            File[] files = logsDir.listFiles((dir, name) -> name.startsWith("bungee-") && name.endsWith(".log"));

            if (files != null && files.length > config.getInt("max-log-files", 30)) {
                java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                for (int i = config.getInt("max-log-files", 30); i < files.length; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception e) {
            getLogger().warning("清理旧日志文件失败: " + e.getMessage());
        }
    }

    // 写入日志
    public void writeLog(String level, String message) {
        try {
            String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
            String formattedMessage = logFormat
                    .replace("%time%", time)
                    .replace("%level%", level)
                    .replace("%message%", message);

            if (logWriter != null) {
                logWriter.println(formattedMessage);
                logWriter.flush();
            }

            // 同时发送到 WebSocket
            if (webapiEnabled && webSocketServer != null) {
                String jsonMessage = String.format(
                        "{\"type\":\"plugin\",\"message\":\"%s\"}",
                        escapeJson(message)
                );
                webSocketServer.broadcast(jsonMessage);
            }

        } catch (Exception e) {
            getLogger().severe("写入日志失败: " + e.getMessage());
        }
    }

    // 发送 WebAPI 状态消息
    public void sendWebAPIMessage(String status) {
        if (webapiEnabled && webSocketServer != null) {
            String jsonMessage = String.format(
                    "{\"type\":\"bungeelogwebapi\",\"message\":\"%s\"}",
                    status
            );
            webSocketServer.broadcast(jsonMessage);
        }
    }

    // 发送玩家事件到 WebSocket
    public void sendPlayerEvent(String eventType, ProxiedPlayer player, String... extra) {
        if (!webapiEnabled || webSocketServer == null) return;

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"type\":\"").append(eventType).append("\",");
        json.append("\"name\":\"").append(escapeJson(player.getName())).append("\",");
        json.append("\"uuid\":\"").append(player.getUniqueId().toString()).append("\"");

        // 添加额外字段
        if (extra.length >= 2) {
            for (int i = 0; i < extra.length; i += 2) {
                json.append(",\"").append(extra[i]).append("\":\"")
                        .append(escapeJson(extra[i + 1])).append("\"");
            }
        }

        json.append("}");
        webSocketServer.broadcast(json.toString());
    }

    // 转义 JSON 字符串
    public String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // 获取日志文件
    public File getLogFile() {
        return this.logFile;
    }

    public static BungeeLog getInstance() {
        return instance;
    }

    public boolean isWebapiEnabled() {
        return webapiEnabled;
    }

    public BungeeWebSocketServer getWebSocketServer() {  // 修改返回类型
        return webSocketServer;
    }
}
