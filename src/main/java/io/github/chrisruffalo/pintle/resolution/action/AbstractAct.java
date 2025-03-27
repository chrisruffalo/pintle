package io.github.chrisruffalo.pintle.resolution.action;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.inject.Inject;
import org.xbill.DNS.Name;

import java.util.List;
import java.util.Optional;

public abstract class AbstractAct implements Act {

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    protected abstract Optional<ActionResult> getResult(String configId, Name queryName, List<ActionList> lists);

    @Override
    public Optional<ActionResult> on(String configId, Name queryName, List<ActionList> lists) {
        final Span onSpan = tracer.spanBuilder("act.on").startSpan();
        try (final Scope scope = onSpan.makeCurrent()) {
            return getResult(configId, queryName, lists);
        } finally {
            onSpan.end();
        }
    }
}
