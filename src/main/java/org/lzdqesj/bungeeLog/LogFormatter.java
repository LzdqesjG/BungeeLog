package org.lzdqesj.bungeeLog;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogFormatter extends Formatter {

    private final BungeeLog plugin;

    public LogFormatter(BungeeLog plugin) {
        this.plugin = plugin;
    }

    @Override
    public String format(LogRecord record) {
        return record.getMessage() + "\n";
    }
}