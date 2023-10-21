package io.github.chrisruffalo.pintle.util;

import org.jboss.logging.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class PathUtil {

    /**
     * Tries to return the real path, otherwise
     * returns the absolute path.
     *
     * @param path to expand to real or absolute
     * @return the real path if it can, the absolute path if it cannot
     */
    public static Path real(final Path path) {
        try {
            return path.normalize().toRealPath().normalize();
        } catch (Exception ex) {
            return path.normalize().toAbsolutePath().normalize();
        }
    }

    /**
     * Given a string convert it to path and
     * then return the real path if possible.
     *
     * @param path as a string value
     * @return the real path if possible, the absolute path if not
     */
    public static Path real(final String path) {
        return real(Paths.get(path));
    }

    /**
     * Uses the list of potential parent paths and then
     * the user.home, and user.dir directores
     * to try and find the relative file.
     *
     * @param input
     * @param potentialParents
     * @return
     */
    public static Optional<Path> find(final String input, Path... potentialParents) {
       return find(real(input), potentialParents);
    }

    /**
     * Uses the list of potential parent paths and then
     * the user.home, and user.dir directores
     * to try and find the relative file.
     *
     * @param input
     * @param potentialParents
     * @return
     */
    public static Optional<Path> find(final String input, String... potentialParents) {
        return find(real(input), Arrays.stream(potentialParents).map(PathUtil::real).toArray(Path[]::new));
    }

    /**
     * Uses the list of potential parent paths and then
     * the user.home, and user.dir directores
     * to try and find the relative file.
     *
     * @param input
     * @param potentialParents
     * @return
     */
    public static Optional<Path> find(Path input, Path... potentialParents) {
        if (input.isAbsolute()) {
            if (Files.exists(input)) {
                return Optional.of(real(input));
            }
        } else {
            final List<Path> paths = new LinkedList<>(Arrays.stream(potentialParents).distinct().toList());
            paths.add(real(System.getProperty("user.home")));
            paths.add(real(System.getProperty("user.dir")));
            for (final Path findRoot : paths) {
                final Path candidate = real(findRoot.resolve(input));
                if (Files.exists(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Path> create(final String name, final Path to, final Logger logger) {
        // show the home directory
        final Path realTo = PathUtil.real(to);
        if (!realTo.equals(to)) {
            if (logger != null) {
                logger.infof("%s: %s (%s)%n", name, to, realTo);
            } else {
                System.out.printf("%s: %s (%s)%n", name, to, realTo);
            }
        } else {
            if (logger != null) {
                logger.infof("%s: %s (%s)%n", name, to, realTo);
            } else {
                System.out.printf("%s: %s%n", name, realTo);
            }
        }
        if (Files.exists(realTo) && !Files.isDirectory(realTo)) {
            if (logger != null) {
                logger.errorf("%s must be a directory if it exists already%n", name);
            } else {
                System.err.printf("%s must be a directory if it exists already%n", name);
            }
            return Optional.empty();
        }
        if (!Files.exists(realTo)) {
            try {
                Files.createDirectories(realTo);
            } catch (Exception ex) {
                if (logger != null) {
                    logger.errorf("could not %s: %s%n", name, ex.getMessage());
                } else {
                    System.err.printf("could not create %s: %s%n", name, ex.getMessage());
                }
                return Optional.empty();
            }
        }
        return Optional.of(realTo);
    }

}
