package io.github.chrisruffalo.pintle.util;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

public class ShaUtil {

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
        byte[] buffer = new byte[8196];
        try (
            final InputStream reader = Files.newInputStream(toFile);
        ) {
            while(reader.read(buffer) >= 0) {
                messageDigest.update(buffer);
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(HexFormat.of().formatHex(messageDigest.digest()));
    }

}
