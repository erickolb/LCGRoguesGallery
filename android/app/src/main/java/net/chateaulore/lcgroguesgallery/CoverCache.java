package net.chateaulore.lcgroguesgallery;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.LongSupplier;

/** App-private, bounded image cache. No collection titles, credentials, or cookies. */
final class CoverCache {
    interface Fetcher { byte[] fetch() throws IOException; }
    private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;
    private final File directory;
    private final long maximumBytes;
    private final int maximumFiles;
    private final LongSupplier clock;

    CoverCache(File directory) {
        this(directory, 256L * 1024 * 1024, 1000, System::currentTimeMillis);
    }

    CoverCache(File directory, long maximumBytes, int maximumFiles, LongSupplier clock) {
        this.directory = directory;
        this.maximumBytes = maximumBytes;
        this.maximumFiles = maximumFiles;
        this.clock = clock;
    }

    synchronized byte[] get(String account, String id, String url, Fetcher fetcher) throws IOException {
        File target = new File(directory, digest(account + "\n" + id + "\n" + url) + ".cover");
        if (target.isFile() && target.length() > 0 && target.length() <= MAX_IMAGE_BYTES) {
            try {
                byte[] bytes = Files.readAllBytes(target.toPath());
                target.setLastModified(clock.getAsLong());
                return bytes;
            } catch (IOException ignored) {
                target.delete();
            }
        }
        byte[] bytes = fetcher.fetch();
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) throw new IOException("Invalid cover size");
        if (bytes.length > maximumBytes) return bytes;
        File temporary = null;
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) return bytes;
            temporary = File.createTempFile("download-", ".tmp", directory);
            Files.write(temporary.toPath(), bytes);
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            target.setLastModified(clock.getAsLong());
            prune();
        } catch (IOException ignored) {
            // A full or unavailable cache must not prevent displaying a downloaded cover.
        } finally {
            if (temporary != null) temporary.delete();
        }
        return bytes;
    }

    private void prune() {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".cover"));
        if (files == null) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        long bytes = 0;
        for (File file : files) bytes += file.length();
        int count = files.length;
        for (File file : files) {
            if (bytes <= maximumBytes && count <= maximumFiles) break;
            long size = file.length();
            if (file.delete()) { bytes -= size; count--; }
        }
    }

    synchronized void remove(String account, String id, String url) {
        new File(directory, digest(account + "\n" + id + "\n" + url) + ".cover").delete();
    }

    synchronized void clear() {
        File[] files = directory.listFiles();
        if (files != null) for (File file : files) if (file.isFile()) file.delete();
    }

    static String digest(String input) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
