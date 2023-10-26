package io.github.chrisruffalo.pintle.resolution;

import io.github.chrisruffalo.pintle.config.Mdns;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.resolution.server.ListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.MdnsListenerHolder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

@ApplicationScoped
public class MdnsController extends AbstractListenerController {

    private static final String MDNS_LISTEN_ADDRESS = "224.0.0.251";

    private static final int MDNS_PORT = 5353;

    private final Map<String, Map<String, MdnsCacheRecord>> RECORDS = new HashMap<>();

    @Inject
    Logger logger;

    @Override
    protected Logger logger() {
        return logger;
    }

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

    @WithSpan("configure mdns")
    @ConsumeEvent(value = Bus.CONFIG_UPDATE_MDNS, ordered = true)
    public void configure(ConfigUpdate event) {
        config = configProducer.get(event.getId());
        final Mdns mdns = config.mdns();
        if (mdns.enabled()) {
            if(mdns.interfaces().stream().anyMatch("all"::equalsIgnoreCase)) {
                try {
                    Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
                    for(final NetworkInterface ne : Collections.list(interfaceEnumeration)) {
                        listeners.add(startMdnsUdpServer(ne.getName()));
                    }
                } catch (SocketException e) {
                    logger.errorf("could not enumerate 'all' interfaces for mdns: %s", e.getMessage());
                }
            } else {
                for (String i : mdns.interfaces()) {
                    listeners.add(startMdnsUdpServer(i));
                }
            }
        }
    }

    private ListenerHolder startMdnsUdpServer(final String mdnsInterface) {
        final DatagramSocket mdnsServer = vertx.createDatagramSocket(new DatagramSocketOptions().setMulticastNetworkInterface(mdnsInterface).setIpV6(false).setReuseAddress(true));
        mdnsServer.listen(MDNS_PORT, MDNS_LISTEN_ADDRESS, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[MDNS] on interface=%s listening on %s:%d", mdnsInterface, MDNS_LISTEN_ADDRESS, MDNS_PORT);

                mdnsServer.listenMulticastGroup(MDNS_LISTEN_ADDRESS, handler -> {
                    mdnsServer.handler(packet -> {

                        byte[] questionBytes = packet.data().getBytes();
                        try {
                            final Message message = new Message(questionBytes);
                            if (message.getHeader().getFlag(Flags.QR)) {
                                final Span span = tracer.spanBuilder("mdns-store").startSpan();
                                final String traceId = span.getSpanContext().getTraceId();
                                final QueryContext context = new QueryContext(traceId, null, message);
                                context.setListenerName("mdns-" + mdnsInterface);
                                eventBus.send(Bus.STORE_MDNS, context);
                            } else {
                                logger.debugf("[MDNS] multicast message received query: %s", message.getQuestion().getName().toString(true));
                            }
                        } catch (IOException e) {
                            // nothing, we don't care about non-dns traffic on this address at all
                        }
                    });
                });
            } else {
                logger.errorf("[MDNS] Server listen failed on %s:%d - %s", MDNS_LISTEN_ADDRESS, MDNS_PORT, asyncResult.cause());
            }
        });
        return new MdnsListenerHolder("mdns-" + mdnsInterface, mdnsServer);
    }

    @WithSpan("store mdns")
    @ConsumeEvent(Bus.STORE_MDNS)
    @RunOnVirtualThread
    public void store(QueryContext context) {
        if (context == null) {
            return;
        }
        final Message question = context.getQuestion();
        if (question != null) {
            int[] sections = new int[]{
                Section.ANSWER,
                Section.ADDITIONAL
            };
            for(int section : sections) {
                if (question.getSection(section) != null && !question.getSection(section).isEmpty()) {
                    context.getQuestion().getSection(section).forEach(r -> {
                        final MdnsCacheRecord cacheRecord = translate(r);
                        final String key = Type.string(r.getType());
                        if (!RECORDS.containsKey(key)) {
                            RECORDS.put(key, new HashMap<>());
                        }
                        RECORDS.get(key).put(cacheRecord.name, cacheRecord);
                    });
                }
            }
        }
        Optional.ofNullable(context.getSpan()).ifPresent(Span::end);
    }

    private MdnsCacheRecord translate(Record r) {
        final MdnsCacheRecord cacheRecord = new MdnsCacheRecord();
        cacheRecord.name = r.getName().toString(false);
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
