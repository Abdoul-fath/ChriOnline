package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AppLogger {

    public static final Logger SERVER = LogManager.getLogger("SERVER");
    public static final Logger SECURITY = LogManager.getLogger("SECURITY");
    public static final Logger ADMIN = LogManager.getLogger("ADMIN");

    private AppLogger() {
    }
}