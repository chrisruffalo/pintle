package io.github.chrisruffalo.pintle.resource;

import io.github.chrisruffalo.pintle.resolution.MdnsController;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.xbill.DNS.Record;

import java.util.Map;

@Path("/api/mdns")
public class MdnsResource {

    @Inject
    MdnsController controller;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, MdnsController.MdnsCacheRecord>> get() {
        return controller.get();
    }

    @GET
    @Path("/clear")
    @Produces(MediaType.TEXT_PLAIN)
    public String clear() {
        controller.clear();
        return "ok";
    }

}
