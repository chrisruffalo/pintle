package io.github.chrisruffalo.pintle.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

public class UriUtil {

    public static Optional<URI> parse(final String uriString) {
        try {
            return Optional.of(new URI(uriString));
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

}
