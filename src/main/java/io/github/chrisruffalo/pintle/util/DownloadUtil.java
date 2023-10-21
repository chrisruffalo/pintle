package io.github.chrisruffalo.pintle.util;

import io.github.chrisruffalo.pintle.model.download.DownloadResponse;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.logging.Logger;

import java.net.URI;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class DownloadUtil {

    private static final Logger LOGGER = Logger.getLogger(DownloadResponse.class);


    public static CompletionStage<DownloadResponse> download(URI remote, Path cache, String etag) {

        final HttpClient client = Vertx.currentContext().owner().createHttpClient();
        final RequestOptions options = new RequestOptions();
        options.setMethod(HttpMethod.GET);
        options.setFollowRedirects(true);
        options.setAbsoluteURI(remote.toString()); // why would it be called 'setAbsoluteURI' and not take a URI?
        if (etag != null && !etag.isEmpty()) {
            options.addHeader("If-None-Match", etag);
        } else {
            options.addHeader("If-Match", "*");
        }
        options.addHeader("Cache-control", "max-age=0");
        options.addHeader("Accept-Encoding", "gzip");
        options.addHeader("Dnt", "1");
        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setTryUseCompression(true);
        webClientOptions.setFollowRedirects(true);

        final WebClient webClient = WebClient.wrap(client, webClientOptions);
        return webClient.request(HttpMethod.GET, options).send().flatMap(response -> {
            final DownloadResponse dr = new DownloadResponse();
            if (response.statusCode() == 304) {
                LOGGER.debugf("no need to download, file already exists");
                dr.setDownloaded(true);
                return Future.succeededFuture(dr);
            } else if (response.statusCode() != 200) {
                LOGGER.errorf("get %s status=%d",remote, response.statusCode());
                dr.setDownloaded(false);
                return Future.succeededFuture(dr);
            } else {
                LOGGER.debugf("got %d status code while downloading", response.statusCode());
            }

            // let's see if we can get an etag
            String cacheName = null;
            if (response.headers().contains("etag")) {
                String newEtag = response.headers().get("etag");
                String cacheNameEtag = newEtag;
                if (cacheNameEtag.toLowerCase().startsWith("w/")) {
                    cacheNameEtag = cacheNameEtag.substring(2);
                }
                if (cacheNameEtag.startsWith("\"")) {
                    cacheNameEtag = cacheNameEtag.substring(1);
                }
                if (cacheNameEtag.endsWith("\"")) {
                    cacheNameEtag = cacheNameEtag.substring(0, cacheNameEtag.length() - 1);
                }
                dr.setEtag(newEtag);
                cacheName = cacheNameEtag;
                LOGGER.debugf("download response with etag value: %s", newEtag);
            } else {
                // this is pretty cursed but it should allow us to send an etag with the same
                // logic if the download already exists
                cacheName = UUID.randomUUID().toString().replace("-", "");
                dr.setEtag(cacheName);
            }

            if (response.headers().contains("Cache-control")) {
                final String cacheControl = response.headers().get("cache-control");
                LOGGER.debugf("cache: %s", cacheControl);
                final Map<String, String> kvp = getKeysAndValuesFromString(cacheControl);
                if (kvp.containsKey("max-age")) {
                    NumberUtil.safeInt(kvp.get("max-age")).ifPresent(value -> {
                        final ZonedDateTime future = ZonedDateTime.now().plusSeconds(value);
                        dr.setCacheUntil(future);
                    });
                }
            }

            final OpenOptions openOptions = new OpenOptions();
            openOptions.setCreate(true);
            openOptions.setWrite(true);
            openOptions.setTruncateExisting(true);

            cacheName = cacheName + ".list";

            if (response.headers().contains("content-encoding")) {
                dr.setCompression(response.headers().get("content-encoding"));
                cacheName = cacheName + "." + dr.getCompression();
            }

            // use as final before return
            final Path downloadTarget = PathUtil.real(cache.resolve(cacheName));
            dr.setDownloadPath(downloadTarget.toString());

            return Vertx.vertx().fileSystem().openBlocking(downloadTarget.toString(), openOptions).write(response.body())
                .map(handler -> {
                    dr.setDownloaded(true);
                    return dr;
                });
        }).recover(throwable -> {
            LOGGER.errorf("failed to download: %s", throwable.getMessage());
            final DownloadResponse dr = new DownloadResponse();
            dr.setDownloaded(false);

            return Future.succeededFuture(dr);
        }).toCompletionStage();
    }

    static Map<String, String> getKeysAndValuesFromString(String keyValueString) {
        String[] pairs = keyValueString.split(" ");
        Map<String, String> kvp = new HashMap<>();
        for (final String pair : pairs) {
            String[] split = pair.split("=");
            if (split.length != 2) {
                continue;
            }
            if (split[0] == null || split[0].isEmpty()) {
                continue;
            }
            kvp.put(split[0].toLowerCase(), split[1]);
        }
        return kvp;
    }

}
