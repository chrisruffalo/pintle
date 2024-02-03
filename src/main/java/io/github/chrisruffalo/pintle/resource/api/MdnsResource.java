package io.github.chrisruffalo.pintle.resource.api;

import io.github.chrisruffalo.pintle.resolution.MdnsController;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/mdns")
public class MdnsResource {

    @Inject
    MdnsController controller;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Response get(
        @QueryParam("type") @DefaultValue("") final String recordType
    ) {
        if (recordType != null && !recordType.trim().isEmpty() && controller.get().containsKey(recordType)) {
            return Response.ok(controller.get().get(recordType)).build();
        }
        return Response.ok(controller.get()).build();
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.TEXT_PLAIN)
    @RunOnVirtualThread
    public String clear() {
        controller.clear();
        return "ok";
    }

}
