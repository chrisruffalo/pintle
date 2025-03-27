package io.github.chrisruffalo.pintle.telemetry;

import io.github.chrisruffalo.pintle.model.QueryContext;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SpanController {


    public Scope startAsCurrent(final QueryContext context) {
        return context.getSpan().makeCurrent();
    }

}
