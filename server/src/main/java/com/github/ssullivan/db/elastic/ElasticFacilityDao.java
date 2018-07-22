package com.github.ssullivan.db.elastic;

import com.github.ssullivan.db.IFacilityDao;
import com.github.ssullivan.db.elastic.AbstractElasticDao;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import javax.inject.Inject;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class ElasticFacilityDao extends AbstractElasticDao implements IFacilityDao {

  private final RestHighLevelClient elasticClient;

  @Inject
  public ElasticFacilityDao(final RestHighLevelClient elasticClient) {
    this.elasticClient = elasticClient;
  }

  @Override
  public SearchResults findByServiceCodes(final ImmutableSet<String> mustServiceCodes,
      final Page page) throws IOException {

    final SearchResponse searchResponse = this.elasticClient
        .search(new SearchRequest("treatment_facilities")

            .source(SearchSourceBuilder.searchSource()
                .query(QueryBuilders.boolQuery()
                    .should(
                        QueryBuilders.termsQuery("service_codes", prepareTerms(mustServiceCodes)))
                    .minimumShouldMatch(mustServiceCodes.size())
                )
                .from(page.offset())
                .size(page.size())
                .sort("_doc", SortOrder.ASC)
                .fetchSource(true)
            )
        );

    return asSearchResults(searchResponse);
  }


}
