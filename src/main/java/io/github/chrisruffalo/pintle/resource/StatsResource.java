package io.github.chrisruffalo.pintle.resource;

import io.github.chrisruffalo.pintle.model.stats.Client;
import io.github.chrisruffalo.pintle.model.stats.Question;
import io.github.chrisruffalo.pintle.resolution.StatsController;
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
    public Uni<List<Question>> questionStats() {
        return Uni.createFrom().voidItem().emitOn(Infrastructure.getDefaultExecutor()).map(v -> controller.getQuestionStats());
    }

    @GET
    @Path("/client")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<Client>> clientStats() {
        return Uni.createFrom().voidItem().emitOn(Infrastructure.getDefaultExecutor()).map(v -> controller.getClientStats());
    }

}
