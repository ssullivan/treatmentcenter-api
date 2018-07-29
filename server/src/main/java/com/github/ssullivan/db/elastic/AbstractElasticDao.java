package com.github.ssullivan.db.elastic;

import com.github.ssullivan.model.SearchResults;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

public abstract class AbstractElasticDao {

  static List<Map<String, Object>> toList(final SearchHit... items) {
    return Stream.of(items)
        .map(SearchHit::getSourceAsMap)
        .collect(Collectors.toList());
  }

  static String[] prepareTerms(final Collection<String> terms) {
    return terms.stream().map(String::toLowerCase).collect(Collectors.toList())
        .toArray(new String[]{});
  }

  static boolean hasHits(final SearchResponse searchResponse) {
    return searchResponse != null && searchResponse.getHits() != null
        && searchResponse.getHits().getHits() != null
        && searchResponse.getHits().getHits().length > 0;
  }

  static Map<String, Object> firstHitSourceAsMap(final SearchResponse searchResponse) {
    return searchResponse.getHits().getHits()[0].getSourceAsMap();
  }

  static SearchResults asSearchResults(final SearchResponse searchResponse) {
    return SearchResults.searchResults(searchResponse.getHits().getTotalHits(),
        toList(searchResponse.getHits().getHits())
    );
  }
}
