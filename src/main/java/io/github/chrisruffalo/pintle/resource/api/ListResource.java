package io.github.chrisruffalo.pintle.resource.api;

import io.github.chrisruffalo.pintle.model.list.StoredLine;
import io.github.chrisruffalo.pintle.model.list.StoredList;
import io.github.chrisruffalo.pintle.model.list.StoredSource;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api")
public class ListResource {

    @Path("/lists")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @RunOnVirtualThread
    public List<StoredList> lists() {
        final List<StoredList> lists = StoredList.listAll();
        lists.forEach(each -> {
            try {
                each.entries = StoredLine.count("#line.byList", Parameters.with("listId", each.id));
            } catch (NoResultException noResultException) {
                each.entries = 0;
            }
        });
        return lists;
    }

    @Path("/sources")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @RunOnVirtualThread
    public List<StoredSource> sources() {
        return StoredSource.listAll();
    }

}
