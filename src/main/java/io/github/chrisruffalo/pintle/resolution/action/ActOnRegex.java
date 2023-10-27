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

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@ApplicationScoped
public class ActOnRegex extends AbstractAct {

    @Inject
    ListController listController;

    @Override
    @RunOnVirtualThread
    @Transactional
    public Optional<ActionResult> on(final String configId, Name queryName, List<ActionList> lists) {
        if (lists.isEmpty()){
            return Optional.empty();
        }

        final List<StoredLine> lines = createQuery(configId, lists).list();
        if (lines.isEmpty()) {
            return Optional.empty();
        }

        final String queryHost = NameUtil.string(queryName);
        for (final StoredLine line : lines) {
            Pattern linePattern = Pattern.compile(line.hostname);
            if (linePattern.matcher(queryHost).matches()) {
                final ActionResult result = new ActionResult();
                result.setConfigId(configId);
                result.setListId(line.listId);
                result.setSourceId(line.sourceId);
                result.setSourceVersion(line.version);
                result.setMatchContent(line.hostname);
                result.setResolveTo(line.resolveTo);
                return Optional.of(result);
            }
        }

        return Optional.empty();
    }

    private PanacheQuery<StoredLine> createQuery(final String configId, List<ActionList> actionLists) {
        final StringBuilder builder = new StringBuilder("0=1 ");
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
        return StoredLine.find(builder.toString());
    }
}
