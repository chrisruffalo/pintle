package io.github.chrisruffalo.pintle.model;

import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.opentelemetry.api.trace.Span;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.xbill.DNS.Message;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles the accumulation of data throughout the process
 * of resolution
 */
@RegisterForReflection
public class QueryContext {

    /**
     * Unique identifier for every request in the
     * system that ties back to opentelemetry
     */
    private final String traceId;

    /**
     * The configuration id that was current at the time
     * that the request was serviced. Used mainly in the
     * blocking step to ensure that the mappings/groups
     * that were active at the time the listener
     * handled the query are the ones that are applied
     */
    private String configId;

    /**
     * The first available moment that the application
     * started handling the request.
     */
    private final ZonedDateTime started;

    /**
     * The time that the request was responded to, meaning
     * after the future completed.
     */
    private ZonedDateTime responded;

    /**
     * The class that encapsulates how the client is responded
     * to.
     */
    private final Responder responder;

    /**
     * The DNS message that represents the query sent to the
     * listener.
     */
    private Message question;

    /**
     * The DNS message that serves as the response that will be
     * sent back out the responder.
     */
    private Message answer;

    /**
     * Any exceptions collected during the lifetime of the context
     */
    private final List<Throwable> exceptions = new LinkedList<>();

    /**
     * The enumerated type that represents the end-state/result of
     * the resolution.
     */
    private QueryResult result;

    /**
     * The span created at the beginning and tracked with opentelemtry
     * through the execution of the query.
     */
    private Span span;

    /**
     * The group that matched first to the query.
     */
    private Group group;

    /**
     * When true sets that the source was a cached entry. Cached entries
     * should not re-update the cache.
     */
    private boolean cached;

    /**
     * The name of the listener that the query was ingested through
     */
    private String listenerName;

    QueryContext(String traceId, Responder responder) {
        this.traceId = traceId;
        this.started = ZonedDateTime.now();
        this.responder = responder;
    }

    public QueryContext(String traceId, Responder responder, Message question) {
        this(traceId, responder);
        this.question = question;
    }

    public QueryContext(String traceId, Span span, Responder responder, Message question) {
        this(traceId, responder, question);
        this.span = span;
    }

    public QueryContext(String traceId, Responder responder, Throwable ex) {
        this(traceId, responder);
        this.exceptions.add(ex);
    }

    public QueryContext(String traceId, Span span, Responder responder, Throwable ex) {
        this(traceId, responder, ex);
        this.span = span;
    }


    public String getListenerName() {
        return listenerName;
    }

    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

    public Responder getResponder() {
        return responder;
    }

    public Message getQuestion() {
        return question;
    }

    public Message getAnswer() {
        return answer;
    }

    public void setAnswer(Message answer) {
        this.answer = answer;
    }

    public List<Throwable> getExceptions() {
        return exceptions;
    }

    public QueryResult getResult() {
        return result;
    }

    public void setResult(QueryResult result) {
        this.result = result;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public ZonedDateTime getStarted() {
        return started;
    }

    public ZonedDateTime getResponded() {
        return responded;
    }

    public void setResponded(ZonedDateTime responded) {
        this.responded = responded;
    }

    public int getElapsedMs() {
        ZonedDateTime end = this.responded;
        if (end == null) {
            end = ZonedDateTime.now();
        }
        return (int)(end.toInstant().toEpochMilli() - started.toInstant().toEpochMilli());
    }

    public String getTraceId() {
        return this.traceId;
    }

    public void setQuestion(Message question) {
        this.question = question;
    }

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
        this.span = span;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getConfigId() {
        return configId;
    }

    public void setConfigId(String configId) {
        this.configId = configId;
    }
}
