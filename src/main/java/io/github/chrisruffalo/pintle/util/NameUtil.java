package io.github.chrisruffalo.pintle.util;

import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class for handling DNS names
 * and strings that represent DNS names
 * in a standard way that produces
 * unsurprising output every time.
 */
public class NameUtil {

    /**
     * Converts a DNS name into a string. This
     * means that the ending "." is included
     * everytime making it a canonical full
     * DNS name (FQDN).
     * <br/><br/>
     * I guess we don't really care
     * about unqualified dns names.
     *
     * @param name to convert to string
     * @return name converted to string with ending "."
     */
    public static String string(final Name name) {
        if (name == null) {
            return ".";
        }
        String stringified = name.toString(false);
        if (stringified.endsWith(".")) {
            return stringified;
        }
        return stringified + ".";
    }

    /**
     * Attempt to parse a string name into a DNS name.
     *
     * @param name to parse into a DNS name
     * @return an optional with the result of the parsing if successful, an empty optional otherwise
     */
    public static Optional<Name> parse(final String name) {
        if (name == null || name.isEmpty()) {
            return Optional.of(Name.empty);
        }
        try {
            return Optional.of(new Name(name));
        } catch (TextParseException e) {
            return Optional.empty();
        }
    }

    /**
     * Takes a raw string and tries to apply
     * some DNS formatting to it creating a
     * DNS name string.
     *
     * @param name to convert to a dns name string
     * @return the standardized dns name string, with ending "."
     */
    public static String string(final String name) {
        if (name == null || name.isEmpty()) {
            return ".";
        }

        final Optional<Name> parsed = parse(name);
        return string(parsed.orElse(null));
    }

    /**
     * Given a DNS name this will return a list of strings
     * that represents the dns name and all of its subdomains.
     * <br/><br/>
     * Example:
     * <pre>
     *   input: safe.images.akamai.com
     *   output:
     *   [
     *     "safe.images.akamai.com.",
     *          "images.akamai.com.",
     *                 "akamai.com.",
     *                       ".com."
     *   ]
     * </pre>
     * @param name to convert into a list of subdomains
     * @return a list of subdomains including the domain itself
     */
    public static Set<String> domains(final Name name) {
        int labels = name.labels();
        if (labels == 0) {
            return Collections.emptySet();
        }
        final Set<String> domains = new LinkedHashSet<>();
        domains.add(string(name));
        for (int outerIndex = 1; outerIndex < labels; outerIndex++) {
            final StringBuilder builder = new StringBuilder();
            for (int innerIndex = outerIndex; innerIndex < labels; innerIndex++) {
                builder.append(name.getLabelString(innerIndex)).append(".");
            }
            if (builder.toString().equals(".")) {
                continue;
            }
            domains.add(builder.toString());
            builder.setLength(0);
            builder.trimToSize();
        }
        return domains;
    }

    /**
     * Takes a string and applies the logic of the domains() function
     * to it. If the string cannot be parsed as a domain name then
     * an empty list is returned.
     *
     * @param name to parse and create domain list for
     * @return a list of domains, an empty list if the name could not be parsed
     */
    public static Set<String> domains(final String name) {
        Optional<Name> parsed = parse(name);
        return parsed.map(NameUtil::domains).orElse(Collections.emptySet());
    }

    /**
     * Determine if there is an intersection between the question domain
     * and any of the domains in the given list.
     * <br/><br/>
     * Example:
     * <pre>
     * true:
     *   input: images.google.com
     *   domains: [google.com, att.com]
     * false:
     *   input: images.google.com
     *   domains: [att.com, oogle.com]
     * </pre>
     *
     * @param question that is being asked (google.com)
     * @param domains that it could be a subdomain of it
     * @return true if any of the domains is the question or a subdomain of it
     */
    public static boolean intersects(final Name question, final Set<String> domains) {
        if (Name.empty.equals(question)) {
            return false;
        }
        // if there are no domains set then the resolution can continue
        if (domains.isEmpty()) {
            return true;
        }
        final Set<String> processedDomains = domains.stream().map(NameUtil::string).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
        return NameUtil.domains(question).stream().anyMatch(processedDomains::contains);
    }

    /**
     * Convenience method that takes a name and checks if it intersects with
     * any of the given vararg domains (converting them to a set).
     *
     * @param question that is being asked (google.com)
     * @param domains that it could be a subdomain of it
     * @return true if any of the domains is the question or a subdomain of it
     */
    public static boolean intersects(final Name question, final String... domains) {
        Set<String> allowedDomains = Arrays.stream(domains).collect(Collectors.toSet());
        return intersects(question, allowedDomains);
    }

    /**
     * Convenience method that takes a string and checks if it intersects with
     * any of the given domains.
     *
     * @param question that is being asked (google.com)
     * @param domains that it could be a subdomain of it
     * @return true if any of the domains is the question or a subdomain of it
     */
    public static boolean intersects(final String question, final String... domains) {
        return intersects(parse(question).orElse(Name.empty), domains);
    }

}
