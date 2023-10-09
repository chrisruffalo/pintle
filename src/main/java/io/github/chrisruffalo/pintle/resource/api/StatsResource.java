package io.github.chrisruffalo.pintle.resource.api;

import io.github.chrisruffalo.pintle.model.stats.Client;
import io.github.chrisruffalo.pintle.model.stats.Question;
import io.github.chrisruffalo.pintle.resolution.StatsController;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/stats")
public class StatsResource {

    @Inject
    StatsController controller;

    @GET
    @Path("/question")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public List<Question> questionStats() {
        return controller.getQuestionStats();
    }

    @GET
    @Path("/client")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public List<Client> clientStats() {
        return controller.getClientStats();
    }

}
