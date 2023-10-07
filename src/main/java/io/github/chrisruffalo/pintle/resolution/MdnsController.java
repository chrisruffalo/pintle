package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Message;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class MdnsController {

    private final Map<String, Map<String, MdnsCacheRecord>> RECORDS = new HashMap<>(){{
        put("A", new HashMap<>());
        put("AAAA", new HashMap<>());
    }};

    public static class MdnsCacheRecord {
        private String name;
        private String data;
        private long ttl;
        private String type;
        private int rsetType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public long getTtl() {
            return ttl;
        }

        public void setTtl(long ttl) {
            this.ttl = ttl;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getRsetType() {
            return rsetType;
        }

        public void setRsetType(int rsetType) {
            this.rsetType = rsetType;
        }
    }

    @WithSpan("store mdns")
    @ConsumeEvent(Bus.STORE_MDNS)
    public void store(QueryContext context) {
        if (context == null) {
            return;
        }
        final Message question = context.getQuestion();
        if (question != null && question.getSection(Section.ANSWER) != null && !question.getSection(Section.ANSWER).isEmpty()) {
            context.getQuestion().getSection(Section.ANSWER).forEach(r -> {
                final MdnsCacheRecord cacheRecord = translate(r);
                RECORDS.getOrDefault(Type.string(r.getType()), new HashMap<>()).put(cacheRecord.name, cacheRecord);
            });
        }
        if (question != null && question.getSection(Section.ADDITIONAL) != null && !question.getSection(Section.ADDITIONAL).isEmpty()) {
            context.getQuestion().getSection(Section.ADDITIONAL).forEach(r -> {
                final MdnsCacheRecord cacheRecord = translate(r);
                RECORDS.getOrDefault(Type.string(r.getType()), new HashMap<>()).put(cacheRecord.name, cacheRecord);
            });
        }
        Optional.ofNullable(context.getSpan()).ifPresent(Span::end);
    }

    private MdnsCacheRecord translate(Record r) {
        final MdnsCacheRecord cacheRecord = new MdnsCacheRecord();
        cacheRecord.name = r.getName().toString(true);
        cacheRecord.data = r.rdataToString();
        cacheRecord.rsetType = r.getRRsetType();
        cacheRecord.ttl = r.getTTL();
        cacheRecord.type = Type.string(r.getType());
        return cacheRecord;
    }

    public Map<String, Map<String, MdnsCacheRecord>> get() {
        return Collections.unmodifiableMap(RECORDS);
    }

    public void clear() {
        RECORDS.forEach((key, value) -> value.clear());
    }

}
