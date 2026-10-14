package net.chateaulore.roguesgallery;

/** Monotonic active-playback time; paused/background time does not count. */
final class RefreshSchedule {
    static final long INTERVAL = 12L * 60 * 60 * 1000;
    static final long RETRY = 15L * 60 * 1000;
    private long accumulated;
    private long runningSince = -1;
    private long nextRefresh = INTERVAL;

    private long elapsed(long now) {
        return accumulated + (runningSince < 0 ? 0 : Math.max(0, now - runningSince));
    }

    void running(long now, boolean running) {
        accumulated = elapsed(now);
        runningSince = running ? now : -1;
    }

    long delay(long now) { return Math.max(0, nextRefresh - elapsed(now)); }

    void refreshed(long now) {
        accumulated = 0;
        if (runningSince >= 0) runningSince = now;
        nextRefresh = INTERVAL;
    }

    void failed(long now) { nextRefresh = elapsed(now) + RETRY; }
}
