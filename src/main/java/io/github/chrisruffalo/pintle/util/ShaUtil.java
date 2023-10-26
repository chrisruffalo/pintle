package io.github.chrisruffalo.pintle.util;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class ShaUtil {

    private static final int BUFFER_SIZE = 16 * 1024; //16k

    private static final Logger LOGGER = Logger.getLogger(ShaUtil.class);

    /**
     * Algorithms to try, in order, to get the hash
     */
    public static String[] ALGS = new String[]{
      "SHA3-256",
      "SHA-256"
    };

    public static Optional<String> hash(final Path toFile) {
        if (!Files.exists(toFile)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(toFile)) {
            return Optional.empty();
        }
        final MessageDigest messageDigest = getMessageDigest();
        try (
            final InputStream reader = Files.newInputStream(toFile);
        ) {
            hash(messageDigest, reader);
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(HexFormat.of().formatHex(messageDigest.digest()));
    }

    public static Optional<String> hash(final Path toFile, final String compressionFormat) {
        if (!Files.exists(toFile)) {
            return Optional.empty();
        }
        if (!Files.isRegularFile(toFile)) {
            return Optional.empty();
        }
        if (compressionFormat == null || compressionFormat.isEmpty()) {
            return hash(toFile);
        }

        final MessageDigest messageDigest = getMessageDigest();

        try (
            final InputStream reader = Files.newInputStream(toFile);
            final InputStream decompressed = getDecompressionStream(reader, compressionFormat);
        ) {
            hash(messageDigest, decompressed);
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(HexFormat.of().formatHex(messageDigest.digest()));
    }

    private static void hash(final MessageDigest messageDigest, final InputStream source) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        while(source.read(buffer) >= 0) {
            messageDigest.update(buffer);
        }
    }

    private static MessageDigest getMessageDigest() {
        MessageDigest messageDigest = null;
        for (String alg : ALGS) {
            try {
                messageDigest = MessageDigest.getInstance(alg);
                LOGGER.debugf("using sha algorithm %s", alg);
                break;
            } catch (Exception ex) {
                // no-op continue
            }
        }
        if (messageDigest == null) {
            throw new RuntimeException(String.format("Could not find a valid message digest algorithm among %s", String.join(",", ALGS)));
        }
        return messageDigest;
    }

    private static InputStream getDecompressionStream(final InputStream source, final String compressionFormat) throws IOException {
        InputStream target = null;
        if ("gzip".equalsIgnoreCase(compressionFormat)) {
            target = new GZIPInputStream(source);
        }
        if (target == null) {
            return source;
        }
        return target;
    }

}
