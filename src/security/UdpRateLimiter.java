package security;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class UdpRateLimiter {

    private static final int MAX_PACKETS_PER_WINDOW = 10;
    private static final long WINDOW_MILLIS = 1000;

    private final Map<String, Counter> counters = new HashMap<>();

    public synchronized boolean allow(String ip) {
        long now = Instant.now().toEpochMilli();

        Counter counter = counters.get(ip);

        if (counter == null || now - counter.windowStart > WINDOW_MILLIS) {
            counters.put(ip, new Counter(now, 1));
            return true;
        }

        if (counter.count >= MAX_PACKETS_PER_WINDOW) {
            return false;
        }

        counter.count++;
        return true;
    }

    private static class Counter {
        long windowStart;
        int count;

        Counter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}