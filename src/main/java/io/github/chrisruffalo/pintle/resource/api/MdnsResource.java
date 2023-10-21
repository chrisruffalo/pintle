package io.github.chrisruffalo.pintle.resource.api;

import io.github.chrisruffalo.pintle.resolution.MdnsController;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/api/mdns")
public class MdnsResource {

    @Inject
    MdnsController controller;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public Map<String, Map<String, MdnsController.MdnsCacheRecord>> get() {
        return controller.get();
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
