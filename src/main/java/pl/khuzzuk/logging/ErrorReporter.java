package pl.khuzzuk.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class ErrorReporter {
    private static final String LOG_FILE = "music-manager.log";
    private static final Logger LOGGER = Logger.getLogger("pl.khuzzuk");

    static {
        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.WARNING, "Cannot initialize file error log: " + LOG_FILE, e);
        }
    }

    private ErrorReporter() {
    }

    public static void log(String message, Throwable throwable) {
        LOGGER.log(Level.WARNING, message, unwrap(throwable));
    }

    public static String userMessage(Throwable throwable) {
        Throwable unwrapped = unwrap(throwable);
        String message = unwrapped.getMessage();
        if (message == null || message.isBlank()) {
            return unwrapped.getClass().getName();
        }

        return message;
    }

    public static String logFile() {
        return LOG_FILE;
    }

    public static String stackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        unwrap(throwable).printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof ExecutionException || current instanceof InvocationTargetException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
