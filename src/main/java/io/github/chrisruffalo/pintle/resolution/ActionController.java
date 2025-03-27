package io.github.chrisruffalo.pintle.resolution;

import com.beust.jcommander.Strings;
import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.config.Group;
import io.github.chrisruffalo.pintle.config.PintleConfig;
import io.github.chrisruffalo.pintle.config.producer.ConfigProducer;
import io.github.chrisruffalo.pintle.event.Bus;
import io.github.chrisruffalo.pintle.model.QueryContext;
import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.resolution.action.Act;
import io.github.chrisruffalo.pintle.resolution.action.ActOnContains;
import io.github.chrisruffalo.pintle.resolution.action.ActOnRegex;
import io.github.chrisruffalo.pintle.resolution.action.ActionResult;
import io.github.chrisruffalo.pintle.telemetry.SpanController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.xbill.DNS.Name;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Uses the state of the query context
 * to look up applicable lists and then
 * executes the rules from those lists
 */
@ApplicationScoped
public class ActionController {

    @Inject
    Logger logger;

    @Inject
    ConfigProducer configProducer;

    @Inject
    ListController listController;

    @Inject
    ActOnRegex actOnRegex;

    @Inject
    ActOnContains actOnContains;

    @Inject
    EventBus bus;

    @Inject
    Tracer tracer;

    @Inject
    SpanController spanController;

    private static final ActionList.Type[] TYPE_ORDER = new ActionList.Type[]{
        ActionList.Type.HOSTFILE,
        ActionList.Type.REGEX
    };

    @ConsumeEvent(Bus.HANDLE_ACTION_LISTS)
    @RunOnVirtualThread
    void handleActions(final QueryContext context){
        try (final Scope outer = spanController.startAsCurrent(context)) {
            final Span span = tracer.spanBuilder("take-actions").startSpan();
            try (final Scope scope = span.makeCurrent()) {

                // get the configuration from the context
                final PintleConfig config = configProducer.get(context.getConfigId());

                // we need to get the set of lists we will be applying, in order, by visiting
                // each group (again: in order) and collecting all the names of the action
                // lists configured for that group (in order). In order.
                final List<ActionList> toApply;
                final Span collectSpan = tracer.spanBuilder("collect-actions").startSpan();
                try (final Scope collectScope = collectSpan.makeCurrent()) {
                    toApply = new LinkedList<>(context.getGroups().stream().mapMulti((group, consumer) -> {
                            if (group.lists().isEmpty()) {
                                return;
                            }
                            for (final String listName : group.lists().get()) {
                                Optional<ActionList> actionListOptional = config.list(listName);
                                actionListOptional.ifPresent(consumer);
                            }
                        })
                        .map(o -> (ActionList) o)
                        .toList());
                } finally {
                    collectSpan.end();
                }

                // no lists to apply, continue
                if (toApply.isEmpty()) {
                    logger.debugf("no action lists to apply for group(s) [%s]", String.join(", ", context.getGroups().stream().map(Group::name).toList()));
                    bus.send(Bus.CHECK_CACHE, context);
                    return;
                }

                logger.debugf("selected lists: %s", Strings.join(", ", toApply.stream().map(ActionList::name).toList()));

                final Name domain = context.getQuestion().getQuestion().getName();

                final Span processSpan = tracer.spanBuilder("process-actions").startSpan();
                try (final Scope processScope = processSpan.makeCurrent()) {
                    // allow lists are applied first
                    final Span allowSpan = tracer.spanBuilder("process-allow-actions").startSpan();
                    try (final Scope allowScope = allowSpan.makeCurrent()) {
                        final List<ActionList> allow = toApply.stream().filter(actionList -> ActionList.Action.ALLOW.equals(actionList.action())).toList();
                        final Optional<ActionResult> result = process(context.getConfigId(), domain, allow);
                        if (result.isPresent()) {
                            // todo: log/save

                            // forward
                            bus.send(Bus.CHECK_CACHE, context);
                            return;
                        }
                    } finally {
                        allowSpan.end();
                    }

                    // warn lists are applied second
                    final Span warnSpan = tracer.spanBuilder("process-warn-actions").startSpan();
                    try (final Scope warnScope = warnSpan.makeCurrent()) {
                        final List<ActionList> warn = toApply.stream().filter(actionList -> ActionList.Action.WARN.equals(actionList.action())).toList();
                        final Optional<ActionResult> result = process(context.getConfigId(), domain, warn);
                        if (result.isPresent()) {
                            // todo: log/save

                            // forward
                            bus.send(Bus.CHECK_CACHE, context);
                            return;
                        }
                    } finally {
                        warnSpan.end();
                    }

                    // followed by block lists
                    final Span blockSpan = tracer.spanBuilder("process-block-actions").startSpan();
                    try (final Scope blockScope = blockSpan.makeCurrent()) {
                        final List<ActionList> block = toApply.stream().filter(actionList -> ActionList.Action.BLOCK.equals(actionList.action())).toList();
                        final Optional<ActionResult> result = process(context.getConfigId(), domain, block);
                        if (result.isPresent()) {
                            logger.warnf("blocked domain %s", domain.toString(false));
                            context.setResult(QueryResult.BLOCKED);

                            // forward
                            bus.send(Bus.RESPOND, context);
                        }
                    } finally {
                        blockSpan.end();
                    }

                    bus.send(Bus.CHECK_CACHE, context);
                } finally {
                    processSpan.end();
                }
            } finally {
                span.end();
            }
        }
    }

    private Optional<ActionResult> process(final String configId, final Name queryName, final List<ActionList> lists) {
        for (ActionList.Type type : TYPE_ORDER) {
            final Act action = action(type);
            Optional<ActionResult> result = action.on(configId, queryName, lists.stream().filter(l -> type.equals(l.type())).toList());
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    private Act action(ActionList.Type type) {
        if (ActionList.Type.REGEX.equals(type)) {
            return actOnRegex;
        }
        return actOnContains;
    }

}
