package io.github.chrisruffalo.pintle.resolution.dto;

import org.xbill.DNS.Message;

import java.util.LinkedList;
import java.util.List;

/**
 * Handles the accumulation of data throughout the process
 * of resolution
 */
public class QueryContext {

    private final Responder responder;

    private Message question;

    private Message answer;

    private final List<Throwable> exceptions = new LinkedList<>();

    /**
     * When true sets that the source was a cached entry. Cached entries
     * should not re-update the cache.
     */
    private boolean cached;

    public QueryContext(Responder responder, Message question) {
        this.responder = responder;
        this.question = question;
    }

    public QueryContext(Responder responder, Throwable ex) {
        this.responder = responder;
        this.exceptions.add(ex);
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

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }
}
