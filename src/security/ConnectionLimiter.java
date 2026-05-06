package security;

import java.util.HashMap;
import java.util.Map;

public class ConnectionLimiter {

    private static final int MAX_CONNECTIONS_PER_IP = 5;

    private final Map<String, Integer> activeConnections = new HashMap<>();

    public synchronized boolean allow(String ip) {
        int count = activeConnections.getOrDefault(ip, 0);

        if (count >= MAX_CONNECTIONS_PER_IP) {
            return false;
        }

        activeConnections.put(ip, count + 1);
        return true;
    }

    public synchronized void release(String ip) {
        int count = activeConnections.getOrDefault(ip, 0);

        if (count <= 1) {
            activeConnections.remove(ip);
        } else {
            activeConnections.put(ip, count - 1);
        }
    }
}