package io.github.chrisruffalo.pintle.model;

import io.github.chrisruffalo.pintle.resolution.responder.Responder;
import io.opentelemetry.api.trace.Span;
import org.xbill.DNS.Message;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

/**
 * Handles the accumulation of data throughout the process
 * of resolution
 */
public class QueryContext {

    private final String traceId;

    private final ZonedDateTime started;

    private ZonedDateTime responded;

    private final Responder responder;

    private Message question;

    private Message answer;

    private final List<Throwable> exceptions = new LinkedList<>();

    private QueryResult result;

    private Span span;

    /**
     * When true sets that the source was a cached entry. Cached entries
     * should not re-update the cache.
     */
    private boolean cached;

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
}
