package io.github.chrisruffalo.pintle.resource.api;

import io.github.chrisruffalo.pintle.model.QueryResult;
import io.github.chrisruffalo.pintle.model.log.LogItem;
import io.github.chrisruffalo.pintle.resource.dto.DataList;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Path("/api/logs")
public class LogsResource {

    private static final Set<String> SAFE_COLUMNS = new HashSet<>() {{
       add("service");
       add("clientIp");
       add("type");
       add("hostname");
       add("result");
       add("responseCode");
       add("start");
       add("elapsedTime");
    }};

    private static final int MAX_PAGE_SIZE = 250;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @RunOnVirtualThread
    public DataList<LogItem> logs(
        @QueryParam("hostname") @DefaultValue("") String hostname,
        @QueryParam("result") @DefaultValue("") String result,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("pageSize") @DefaultValue("100") int pageSize,
        @QueryParam("order") @DefaultValue("") String order,
        @QueryParam("direction") @DefaultValue("") String direction
    ) {
        final StringBuilder query = new StringBuilder("where 1=1");
        final Parameters params = new Parameters();
        if (result != null && !result.isEmpty()) {
            if (result.startsWith("!")) {
                final String notResult = result.substring(1);
                final QueryResult notResultEnum = QueryResult.fromString(notResult);
                if (notResultEnum != null) {
                    query.append(" and result != :result");
                    params.and("result", notResultEnum);
                }
            } else if (QueryResult.fromString(result) != null) {
                query.append(" and result = :result");
                params.and("result", QueryResult.fromString(result));
            }
        }
        if (hostname != null && !hostname.isEmpty()) {
            if (hostname.endsWith(".")) {
                query.append(" and hostname = :hostname");
                params.and("hostname", hostname);
            } else {
                query.append(" and (hostname = :hostname or hostname = :hostname_with_dot)");
                params.and("hostname", hostname);
                params.and("hostname_with_dot", hostname + ".");
            }
        }
        if (page < 0) {
            page = 0;
        }

        if (pageSize < 0) {
            pageSize = 100;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        final DataList<LogItem> container = new DataList<>();

        PanacheQuery<LogItem> baseQuery = LogItem.find(query.toString(), params);
        baseQuery.page(new Page(page, pageSize));
        container.setTotalCount(baseQuery.count());
        container.setPageIndex(baseQuery.page().index);
        container.setPageSize(baseQuery.page().size);
        container.setTotalPages(baseQuery.pageCount());

        // order by
        if (order != null || !order.trim().isEmpty()) {
            final List<String> orderBy = Arrays.stream(order.split(",")).filter(SAFE_COLUMNS::contains).toList();
            if (!orderBy.isEmpty()) {
                query.append("ORDER BY ").append(String.join(",", orderBy));
            }
        }
        baseQuery = LogItem.find(query.toString(), params);
        baseQuery.page(new Page(page, pageSize));
        container.setData(baseQuery.list());

        return container;
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @RunOnVirtualThread
    public LogItem item(@PathParam("id") final long id) {
        return LogItem.findById(id);
    }

    @GET
    @Path("/count")
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    @RunOnVirtualThread
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
