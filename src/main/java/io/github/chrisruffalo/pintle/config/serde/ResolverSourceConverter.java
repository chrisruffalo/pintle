package io.github.chrisruffalo.pintle.config.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.github.chrisruffalo.pintle.config.ResolverSource;
import io.github.chrisruffalo.pintle.config.resolver.ResolverSourceFactory;
import org.eclipse.microprofile.config.spi.Converter;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Converter to convert json that is used internally by smallrye configuration
 * to ResolverSource objects. This has the special property that it can parse
 * different object types from the differentiated elements _or_ that it can
 * parse a simple string into a given resolver source.
 * <br/><br/>
 * Example
 * <pre>
 * # the following yaml fragment contains different configuration that means the same thing
 * sources:
 *   # the following four are all ways to configure udp. udp is the implicit/default type.
 *   - 8.8.8.8
 *   - udp://8.8.8.8:53
 *   - uri: 8.8.8.8:53
 *   - type: udp
 *     uri: 8.8.8.8:53
 *   # and the non-default type tcp can be configured like the following lines:
 *   - tcp://8.8.8.8:53
 *   - type: tcp
 *     uri: 8.8.8.8:53
 * </pre>

 */
public class ResolverSourceConverter implements Converter<List<ResolverSource>> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<ResolverSource> convert(String s) throws IllegalArgumentException, NullPointerException {
        if (s == null || s.isEmpty()) {
            return Collections.emptyList();
        }
        // parse to yaml
        try {
            // this is a bit of a rigamarole but it goes through the process of finding out
            // if the tree has the desired intermediary list (`sources`) and then if it does
            // and the list has content...
            final JsonNode root = mapper.readTree(s);
            if (root.isEmpty() || root.get("sources") == null || root.get("sources").isEmpty()) {
                return Collections.emptyList();
            }
            final JsonNode list = root.get("sources");
            final List<ResolverSource> resolverSources = new LinkedList<>();
            // ... it will go through each element of the list and try and determine ...
            for (final JsonNode element : list) {
                // ... if it is a text node (plain string) that it can parse like `udp://8.8.8.8:53`
                if(element instanceof final TextNode textNode) {
                    final ResolverSource created = ResolverSourceFactory.create(textNode.textValue());
                    if (created != null) {
                        resolverSources.add(created);
                    }
                } else if (element instanceof final ObjectNode objectNode) {
                    // ... or if it is a full-fledged JsonObject that it can straight convert to the type
                    try {
                        resolverSources.add(mapper.readValue(element.toPrettyString(), ResolverSource.class));
                    } catch (JsonProcessingException jpe) {
                        Logger.getLogger(this.getClass()).errorf("could not deserialize string: %s", element.toString());
                    }
                }
            }
            return resolverSources;
        } catch (JsonProcessingException e) {
            Logger.getLogger(this.getClass()).errorf(e, "could not deserialize string %s", s);
        }
        return Collections.emptyList();
    }
}
