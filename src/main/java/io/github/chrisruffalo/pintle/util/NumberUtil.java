package io.github.chrisruffalo.pintle.util;

import java.util.Optional;

public class NumberUtil {

    public static Optional<Integer> safeInt(final String integer) {
        try {
            return Optional.of(Integer.parseInt(integer));
        } catch (Exception ex) {
            // no-op
        }
        return Optional.empty();
    }

    public static int safeInt(final String integer, int safeValue) {
        return safeInt(integer).orElse(safeValue);
    }

}
