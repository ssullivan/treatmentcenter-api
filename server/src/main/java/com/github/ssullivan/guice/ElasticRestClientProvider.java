package com.github.ssullivan.guice;

import com.github.ssullivan.ElasticConfig;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticRestClientProvider implements Provider<RestHighLevelClient> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ElasticRestClientProvider.class);


  private static int HOST_ONLY = 1;
  private static int HOST_AND_PORT = 2;

  private ElasticConfig elasticConfig;

  @Inject
  public ElasticRestClientProvider(ElasticConfig elasticConfig) {
    this.elasticConfig = elasticConfig;
  }

  @Override
  public RestHighLevelClient get() {
    return new RestHighLevelClient(
        RestClient.builder(
            getHttpHostsFromElasticConfig()
        )
    );
  }

  /**
   * Split a comma seprated string of host:port strings into an array.
   *
   * @return a non-null array of {@link HttpHost}
   */
  private HttpHost[] getHttpHostsFromElasticConfig() {
    if (elasticConfig.getHosts() != null) {
      final Splitter colonSplitter = Splitter.on(':');

      return Splitter.on(',').splitToList(elasticConfig.getHosts())
          .stream()
          .map(host -> {
            final List<String> hostPort = colonSplitter.splitToList(host);
            final int hostPortSize = hostPort.size();

            if (hostPortSize == HOST_ONLY) {
              return new HttpHost(hostPort.get(0), 9200);
            } else if (hostPortSize == HOST_AND_PORT) {
              return new HttpHost(hostPort.get(0), Integer.parseInt(hostPort.get(1), 10));
            }
            return null;
          })
          .filter(Objects::nonNull)
          .peek(it -> LOGGER.info("Configured to talk to host: {}", it.toHostString()))
          .collect(Collectors.toList())
          .toArray(new HttpHost[]{});
    }

    LOGGER.warn("No elasticsearch hosts were configured!");
    return new HttpHost[]{};
  }
}
