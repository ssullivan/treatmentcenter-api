package com.github.ssullivan.db;

import com.github.ssullivan.model.SearchResults;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.Map;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;

public class ElasticServiceCodesDao extends AbstractElasticDao implements IServiceCodesDao {

  private RestHighLevelClient esClient;

  @Override
  public Map<String, Object> get(final String id) throws IOException {

    final GetRequest getRequest = new GetRequest();
    getRequest
        .index("service_codes")
        .type("_doc")
        .id(id)
        .fetchSourceContext(FetchSourceContext.FETCH_SOURCE);

    final GetResponse getResponse = this.esClient.get(getRequest);
    return getResponse.getSource();
  }

  @Override
  public Map<String, Object> getByServiceCode(final String serviceCode) throws IOException {
    Preconditions.checkNotNull(serviceCode, "The serviceCode must not be null");

    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    searchSourceBuilder.query(QueryBuilders.constantScoreQuery(
        QueryBuilders.termsQuery("service_code", serviceCode, serviceCode.toLowerCase())
    ))
        .from(0)
        .size(1)
        .fetchSource(true);

    final SearchResponse searchResponse = this.esClient.search(new SearchRequest()
        .indices("service_codes")
        .types("_doc")
        .source(searchSourceBuilder)
    );

    if (hasHits(searchResponse)) {
      return firstHitSourceAsMap(searchResponse);
    }

    // No documents/records were found
    return null;
  }

  @Override
  public SearchResults listServiceCodes() throws IOException {

    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    searchSourceBuilder.query(QueryBuilders.constantScoreQuery(
        QueryBuilders.matchAllQuery()
    )).from(0).size(9999).fetchSource(true);

    final SearchResponse searchResponse = this.esClient.search(new SearchRequest()
        .indices("service_codes")
        .types("_doc")
        .source(searchSourceBuilder)
    );

    return asSearchResults(searchResponse);
  }

  @Override
  public SearchResults listServiceCodesInCategory(String categoryCode) throws IOException {
    Preconditions.checkNotNull(categoryCode, "The categoryCode must not be null");

    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

    searchSourceBuilder.query(QueryBuilders.constantScoreQuery(
        QueryBuilders.termsQuery("category_code", categoryCode, categoryCode.toLowerCase())
    ))
        .from(0)
        .size(9999)
        .fetchSource(true);

    final SearchResponse searchResponse = this.esClient.search(new SearchRequest()
        .indices("category_code")
        .types("_doc")
        .source(searchSourceBuilder)
    );

    return asSearchResults(searchResponse);
  }
}
