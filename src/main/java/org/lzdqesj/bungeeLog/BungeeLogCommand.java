package org.lzdqesj.bungeeLog;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.chat.TextComponent;

public class BungeeLogCommand extends Command {

    private final BungeeLog plugin;

    public BungeeLogCommand(BungeeLog plugin) {
        super("bungeelog", "bungeelog.admin", "blog", "bclog", "bl");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
            case "rl":
                plugin.reloadConfig();
                sender.sendMessage(new TextComponent("§a[BungeeLog] 配置重载完成!"));
                break;

            case "status":
            case "info":
                showStatus(sender);
                break;

            case "webapi":
                handleWebAPI(sender, args);
                break;

            case "test":
                plugin.writeLog("INFO", "这是来自命令的测试日志消息");
                if (plugin.isWebapiEnabled()) {
                    plugin.sendWebAPIMessage("test");
                }
                sender.sendMessage(new TextComponent("§a[BungeeLog] 测试日志已写入!"));
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                sender.sendMessage(new TextComponent("§c未知命令! 使用 /bungeelog help 查看帮助"));
                break;
        }
    }

    private void handleWebAPI(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bungeelog.admin")) {
            sender.sendMessage(new TextComponent("§c你没有权限执行此命令!"));
            return;
        }

        if (args.length == 1) {
            sender.sendMessage(new TextComponent("§6=== WebAPI 状态 ==="));
            sender.sendMessage(new TextComponent("§e启用状态: §f" + (plugin.isWebapiEnabled() ? "§a开启" : "§c关闭")));
            if (plugin.isWebapiEnabled()) {
                sender.sendMessage(new TextComponent("§e地址: §f" + plugin.getConfig().getString("waaddress")));
                sender.sendMessage(new TextComponent("§e密码: §f" + plugin.getConfig().getString("wapassword")));
                if (plugin.getWebSocketServer() != null) {
                    sender.sendMessage(new TextComponent("§e客户端数: §f" +
                            plugin.getWebSocketServer().getConnections().size()));
                }
            }
            return;
        }

        switch (args[1].toLowerCase()) {
            case "restart":
                if (plugin.isWebapiEnabled()) {
                    plugin.reloadConfig();
                    sender.sendMessage(new TextComponent("§a[BungeeLog] WebAPI 已重启!"));
                } else {
                    sender.sendMessage(new TextComponent("§c[BungeeLog] WebAPI 未启用!"));
                }
                break;

            case "broadcast":
                if (args.length > 2 && plugin.getWebSocketServer() != null) {
                    String message = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    String jsonMsg = String.format("{\"type\":\"plugin\",\"message\":\"%s\"}",
                            plugin.escapeJson(message));
                    plugin.getWebSocketServer().broadcast(jsonMsg);
                    sender.sendMessage(new TextComponent("§a[BungeeLog] 已广播消息!"));
                }
                break;
        }
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(new TextComponent("§6=== BungeeLog 状态 ==="));
        try {
            sender.sendMessage(new TextComponent("§e日志文件: §f" + plugin.getLogFile().getAbsolutePath()));
            sender.sendMessage(new TextComponent("§e日志格式: §f" + plugin.getConfig().getString("log-format")));
            sender.sendMessage(new TextComponent("§e每日分割: §f" + plugin.getConfig().getBoolean("daily-rolling")));
            sender.sendMessage(new TextComponent("§e控制台镜像: §f" + plugin.getConfig().getBoolean("enable-console-mirror")));
            sender.sendMessage(new TextComponent("§eWebAPI: §f" + (plugin.isWebapiEnabled() ? "§a开启" : "§c关闭")));

            if (plugin.isWebapiEnabled()) {
                sender.sendMessage(new TextComponent("§e  └ 地址: §f" + plugin.getConfig().getString("waaddress")));
                if (plugin.getWebSocketServer() != null) {
                    sender.sendMessage(new TextComponent("§e  └ 客户端: §f" +
                            plugin.getWebSocketServer().getConnections().size()));
                }
            }
        } catch (Exception e) {
            sender.sendMessage(new TextComponent("§c获取状态失败: " + e.getMessage()));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent("§6=== BungeeLog 帮助 ==="));
        sender.sendMessage(new TextComponent("§e/bungeelog reload §7- 重载配置文件"));
        sender.sendMessage(new TextComponent("§e/bungeelog status §7- 查看插件状态"));
        sender.sendMessage(new TextComponent("§e/bungeelog webapi §7- 查看WebAPI状态"));
        sender.sendMessage(new TextComponent("§e/bungeelog webapi restart §7- 重启WebAPI"));
        sender.sendMessage(new TextComponent("§e/bungeelog webapi broadcast <msg> §7- 广播消息"));
        sender.sendMessage(new TextComponent("§e/bungeelog test §7- 测试日志写入"));
        sender.sendMessage(new TextComponent("§e/bungeelog help §7- 显示帮助"));
    }
}