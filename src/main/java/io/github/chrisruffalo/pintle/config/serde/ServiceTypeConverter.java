package io.github.chrisruffalo.pintle.config.serde;

import io.github.chrisruffalo.pintle.model.ServiceType;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * Helps convert service types to uppercase in the event the
 * configuration serializer cannot.
 */
public class ServiceTypeConverter implements Converter<ServiceType> {

    @Override
    public ServiceType convert(String s) throws IllegalArgumentException, NullPointerException {
        // default to udp
        if (s == null || s.isEmpty()) {
            return ServiceType.UDP;
        }
        return ServiceType.valueOf(s.toUpperCase());
    }
}
