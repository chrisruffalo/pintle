package io.github.chrisruffalo.pintle.resource.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.xbill.DNS.Type;

import java.io.IOException;

public class TypeStringSerializer extends JsonSerializer<Integer> {

    @Override
    public void serialize(Integer integer, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(Type.string(integer));
    }
}
