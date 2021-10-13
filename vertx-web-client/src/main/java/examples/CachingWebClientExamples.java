package examples;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.CachingWebClient;
import io.vertx.ext.web.client.CachingWebClientOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientSession;
import io.vertx.ext.web.client.spi.CacheStore;
import io.vertx.ext.web.client.impl.cache.NoOpCacheStore;

public class CachingWebClientExamples {

  public void create(Vertx vertx) {
    WebClient client = WebClient.create(vertx);
    WebClient cachingWebClient = CachingWebClient.create(client);
  }

  public void createWithOptions(Vertx vertx) {
    CachingWebClientOptions options = new CachingWebClientOptions()
      .addCachedMethod(HttpMethod.HEAD)
      .removeCachedStatusCode(301)
      .setEnableVaryCaching(true);

    WebClient client = WebClient.create(vertx);
    WebClient cachingWebClient = CachingWebClient.create(client, options);
  }

  public void createWithCustomStore(Vertx vertx) {
    WebClient client = WebClient.create(vertx);
    CacheStore store = new NoOpCacheStore(); // or any store you like
    WebClient cachingWebClient = CachingWebClient.create(client, store);
  }

  public void createWithSession(Vertx vertx) {
    WebClient client = WebClient.create(vertx);
    WebClient cachingWebClient = CachingWebClient.create(client);
    WebClient sessionClient = WebClientSession.create(cachingWebClient);
  }

  public void simpleGetWithCaching(Vertx vertx) {
    WebClient client = WebClient.create(vertx);
    WebClient cachingWebClient = CachingWebClient.create(client);

    cachingWebClient
      .get(8080, "myserver.mycompany.com", "/some-uri")
      .send()
      .onSuccess(response -> System.out
        .println("Received response with age" + response.headers().get(HttpHeaders.AGE)))
      .onFailure(err ->
        System.out.println("Something went wrong " + err.getMessage()));
  }
}
