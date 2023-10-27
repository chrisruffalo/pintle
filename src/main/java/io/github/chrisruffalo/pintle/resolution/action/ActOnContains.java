package io.github.chrisruffalo.pintle.resolution.action;

import io.github.chrisruffalo.pintle.config.ActionList;
import io.github.chrisruffalo.pintle.model.list.StoredLine;
import io.github.chrisruffalo.pintle.model.list.StoredList;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.github.chrisruffalo.pintle.resolution.ListController;
import io.github.chrisruffalo.pintle.util.NameUtil;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.xbill.DNS.Name;

import java.util.*;

@ApplicationScoped
public class ActOnContains extends AbstractAct {

    @Inject
    ListController listController;

    @Override
    @RunOnVirtualThread
    @Transactional
    public Optional<ActionResult> on(final String configId, Name queryName, List<ActionList> lists) {
        if (lists.isEmpty()){
            return Optional.empty();
        }

        final Optional<StoredLine> lineOptional = createQuery(configId, NameUtil.domains(queryName), lists).stream().findFirst();
        if (lineOptional.isEmpty()) {
            return Optional.empty();
        }

        final StoredLine line = lineOptional.get();
        final ActionResult result = new ActionResult();
        result.setListId(line.listId);
        result.setConfigId(configId);
        result.setSourceId(line.sourceId);
        result.setResolveTo(line.resolveTo);
        result.setMatchContent(line.hostname);
        result.setSourceVersion(line.version);

        return Optional.of(result);
    }

    private PanacheQuery<StoredLine> createQuery(final String configId, final Set<String> hostnames, List<ActionList> actionLists) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("hostnames", hostnames);
        final StringBuilder builder = new StringBuilder("hostname in (:hostnames) and (0=1");
        actionLists.forEach(al -> {
            final Optional<StoredList> storedList = StoredList.find("name = :name", Parameters.with("name", al.name())).firstResultOptional();
            storedList.ifPresent(list -> al.sources().forEach(s -> {
                final Optional<StoredSource> storedSourceOptional = StoredSource.find("uri = :uri", Parameters.with("uri", s)).firstResultOptional();
                storedSourceOptional.ifPresent(storedSource -> {
                    // a version of 0 means that no source was ever loaded
                    if (storedSource.version == 0) {
                        return;
                    }
                    builder.append(" or (listId =").append(list.id);
                    builder.append(" and sourceId = ").append(storedSource.id);
                    builder.append(" and version = ").append(listController.getSourceVersion(configId, storedSource.id));
                    builder.append(")");
                });
            }));
        });
        builder.append(") ORDER BY LENGTH(hostname)");

        return StoredLine.find(builder.toString(), parameters);
    }
}
