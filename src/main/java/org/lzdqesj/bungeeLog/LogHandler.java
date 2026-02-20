package org.lzdqesj.bungeeLog;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class LogHandler extends Handler {

    private final BungeeLog plugin;

    public LogHandler(BungeeLog plugin) {
        this.plugin = plugin;
    }

    @Override
    public void publish(LogRecord record) {
        String message = getFormatter().format(record);
        plugin.writeLog(record.getLevel().getName(), record.getMessage());
    }

    @Override
    public void flush() {
        // 不需要额外操作
    }

    @Override
    public void close() throws SecurityException {
        // 清理资源
    }
}