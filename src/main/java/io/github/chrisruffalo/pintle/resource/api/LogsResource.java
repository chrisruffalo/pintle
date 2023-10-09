package io.github.chrisruffalo.pintle.resource.api;

import io.github.chrisruffalo.pintle.model.log.LogItem;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/logs")
public class LogsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public List<LogItem> logs() {
        return LogItem.listAll();
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public String count() {
        return String.valueOf(LogItem.count());
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @RunOnVirtualThread
    public String clear() {
        LogItem.deleteAll();
        return "ok";
    }

}
