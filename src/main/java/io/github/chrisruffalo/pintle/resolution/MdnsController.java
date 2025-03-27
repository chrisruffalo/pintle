package io.github.chrisruffalo.pintle.resolution;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.chrisruffalo.pintle.config.Mdns;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.event.ConfigUpdate;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.resolution.server.ListenerHolder;
import io.github.chrisruffalo.pintle.resolution.server.MdnsListenerHolder;
import io.github.chrisruffalo.pintle.resource.serde.TypeStringSerializer;
import io.github.chrisruffalo.pintle.util.NameUtil;
import io.opentelemetry.api.trace.Span;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for _part_ of the client name resolution system
 * this class will periodically send out a MDNS question for
 * services on the network and listen for responses.
 *
 * This is _almost_ a direct port of https://github.com/chrisruffalo/gudgeon/blob/master/engine/mdns.go.
 *
 */
@ApplicationScoped
public class MdnsController extends AbstractListenerController {

    private static final String MDNS_LISTEN_ADDRESS_V4 = "224.0.0.251";

    private static final String MDNS_LISTEN_ADDRESS_V6 = "ff02::fb";

    private static final int MDNS_PORT = 5353;

    private static final Optional<Name> QUESTION_NAME_OPTIONAL = NameUtil.parse("_services._dns-sd._udp.local.");

    private static final Set<String> SKIP_INTERFACES = new HashSet<>() {{
       add("lo");
    }};

    private static final String[] HOSTNAME_READ_ORDER = new String[]{
        "txt:fn", "txt:f", "txt:md", "name", "name6", "hostname", "hostname6"
    };

    private final Map<String, Map<String, MdnsCacheRecord>> RECORDS = new ConcurrentHashMap<>();

    @Inject
    Logger logger;

    @Override
    protected Logger logger() {
        return logger;
    }

    public static class MdnsCacheRecord {
        private String name;
        private byte[] data;

        private String dataString;

        private long ttl;
        private String type;

        private int dclass;

        @JsonSerialize(using = TypeStringSerializer.class)
        private int rsetType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }

        public String getDataString() {
            return dataString;
        }

        public void setDataString(String dataString) {
            this.dataString = dataString;
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

        public int getDclass() {
            return dclass;
        }

        public void setDclass(int dclass) {
            this.dclass = dclass;
        }
    }

    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP, delay = 2, delayUnit = TimeUnit.SECONDS)
    public CompletionStage<Void> beacon() {
        Future<Void> stage = null;

        if (listeners.isEmpty()) {
            return null;
        }

        if (QUESTION_NAME_OPTIONAL.isEmpty()) {
            return null;
        }

        for (ListenerHolder<?> holder : this.listeners) {
            final Object instance = holder.get();
            if (instance instanceof final DatagramSocket datagramSocket) {
                final Name questionName = QUESTION_NAME_OPTIONAL.get();
                final Message question = new Message();
                question.getHeader().unsetFlag(Flags.RD);
                question.addRecord(Record.newRecord(questionName, Type.SRV, DClass.IN), Section.QUESTION);
                question.addRecord(Record.newRecord(questionName, Type.TXT, DClass.IN), Section.QUESTION);
                if (stage == null) {
                    stage = datagramSocket.send(Buffer.buffer(question.toWire()), MDNS_PORT, holder.address()).andThen(result -> {
                        if (result.succeeded()) {
                            logger.debugf("sent question multicast out interface %s", holder.name());
                        } else {
                            logger.errorf("could not send question multicast out interface %s (%s)", holder.name(), holder.address());
                        }
                    });
                } else {
                    stage.andThen(handler -> {
                        logger.debugf("sending next in chain...");
                        datagramSocket.send(Buffer.buffer(question.toWire()), MDNS_PORT, holder.address()).andThen(result -> {
                            if (result.succeeded()) {
                                logger.debugf("sent question multicast out interface %s", holder.name());
                            } else {
                                logger.errorf("could not send question multicast out interface %s (%s)", holder.name(), holder.address());
                            }
                        });
                    });
                }
            }
        }

        if (stage == null) {
            return null;
        }
        return stage.toCompletionStage();
    }

    @ConsumeEvent(value = Bus.CONFIG_UPDATE_MDNS, ordered = true)
    public void configure(ConfigUpdate event) {
        config = configProducer.get(event.getId());
        final Mdns mdns = config.mdns();

        // if mdns names are specified then go through them to
        // listen on the target interfaces for mdns messages
        if (mdns.enabled() && !mdns.interfaces().isEmpty()) {
            final Set<String> interfaceNames = new HashSet<>(mdns.interfaces());
            if (interfaceNames.stream().anyMatch("all"::equalsIgnoreCase)) {
                interfaceNames.clear();
                try {
                    final Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();;
                    for(final NetworkInterface ne : Collections.list(interfaceEnumeration)) {
                        if (SKIP_INTERFACES.contains(ne.getName().toLowerCase())) {
                            continue;
                        }
                        interfaceNames.add(ne.getName());
                    }
                } catch (SocketException e) {
                    if (logger.isDebugEnabled()) {
                        logger.errorf(e, "could not enumerate interface names for 'all' interfaces, skipping MDNS configuration");
                    } else {
                        logger.errorf("could not enumerate interface names for 'all' interfaces, skipping MDNS configuration");
                    }
                    return;
                }
            }

            // add interfaces by name
            for (String i : interfaceNames) {
                if (i == null || SKIP_INTERFACES.contains(i.toLowerCase())) {
                    continue;
                }
                listeners.add(startMdnsUdpServer(i, MDNS_LISTEN_ADDRESS_V4));
            }
        }
    }

    private ListenerHolder<DatagramSocket> startMdnsUdpServer(final String mdnsInterface, final String address) {
        final DatagramSocketOptions options = new DatagramSocketOptions()
            .setMulticastNetworkInterface(mdnsInterface)
            .setReuseAddress(true)
            ;

        final DatagramSocket mdnsServer = vertx.createDatagramSocket(options);
        mdnsServer.listen(MDNS_PORT, address, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.infof("[MDNS] on interface=%s listening on %s:%d", mdnsInterface, address, MDNS_PORT);

                mdnsServer.listenMulticastGroup(address, handler -> {
                    mdnsServer.handler(packet -> {
                        byte[] questionBytes = packet.data().getBytes();
                        try {
                            final Message message = new Message(questionBytes);
                            final Span span = tracer.spanBuilder("mdns-store").startSpan();
                            final String traceId = span.getSpanContext().getTraceId();
                            final QueryContext context = new QueryContext(traceId, null, message);
                            context.setListenerName("mdns-" + mdnsInterface);
                            eventBus.send(Bus.STORE_MDNS, context);
                        } catch (IOException e) {
                            // nothing, we don't care about non-dns traffic on this address at all
                            logger.errorf(e, "could not parse mdns message: %s", e.getMessage());
                        }
                    });
                });
            } else {
                logger.errorf("[MDNS] Server listen failed on %s:%d - %s", address, MDNS_PORT, asyncResult.cause());
            }
        });
        return new MdnsListenerHolder("mdns-" + mdnsInterface, mdnsServer, address);
    }

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
                Section.ADDITIONAL,
            };
            for(int section : sections) {
                if (question.getSection(section) != null && !question.getSection(section).isEmpty()) {
                    question.getSection(section).forEach(r -> {
                        // if the type of the record isn't A, AAAA, or TXT then there won't be usable data
                        if (Type.A != r.getType() && Type.AAAA != r.getType() && Type.TXT != r.getType()) {
                            return;
                        }
                        final MdnsCacheRecord cacheRecord = translate(r);
                        final String key = Type.string(r.getType());
                        RECORDS.computeIfAbsent(key, k -> new ConcurrentHashMap<>()).put(cacheRecord.name, cacheRecord);
                    });
                }
            }
        }
        Optional.ofNullable(context.getSpan()).ifPresent(Span::end);
    }

    private MdnsCacheRecord translate(Record r) {
        final MdnsCacheRecord cacheRecord = new MdnsCacheRecord();
        cacheRecord.name = NameUtil.string(r.getName());
        cacheRecord.data = r.rdataToWireCanonical();
        cacheRecord.dataString = r.rdataToString();
        cacheRecord.rsetType = r.getRRsetType();
        cacheRecord.ttl = r.getTTL();
        cacheRecord.type = Type.string(r.getType());
        cacheRecord.dclass = r.getDClass();
        return cacheRecord;
    }

    public Map<String, Map<String, MdnsCacheRecord>> get() {
        return Collections.unmodifiableMap(RECORDS);
    }

    public Optional<Message> query(final Message question) {
        final String domain = NameUtil.string(question.getQuestion().getName());
        final Name domainName = NameUtil.parse(domain).orElse(null);
        if (domain.isEmpty() || domainName == null) {
            return Optional.empty();
        }
        final String type = Type.string(question.getQuestion().getType());
        final Map<String, MdnsCacheRecord> typeRecords = RECORDS.get(type);
        if (typeRecords != null) {
            final MdnsCacheRecord cacheRecord = typeRecords.get(domain);
            if(cacheRecord != null) {
                final Message response = new Message(question.getHeader().getID());
                response.getHeader().setFlag(Flags.QR);
                final Record r = Record.newRecord(
                    domainName,
                    Type.value(cacheRecord.type),
                    cacheRecord.dclass > DClass.ANY ? DClass.IN : cacheRecord.dclass,
                    cacheRecord.ttl,
                    cacheRecord.data
                );
                response.addRecord(r, Section.ANSWER);
                return Optional.of(response);
            }
        }
        return Optional.empty();
    }

    public void clear() {
        RECORDS.forEach((key, value) -> value.clear());
    }

}
