package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class SearchResults {

  /**
   * Utility method to create a new instance of {@link SearchResults}.
   *
   * @param totalHits the total hits in the db
   * @param hits the number of hits in this batch
   * @return a non-null instance of {@link SearchResults}
   */
  @JsonCreator
  public static SearchResults searchResults(@JsonProperty("totalHits") final long totalHits,
      @JsonProperty("hits") final List<Map<String, Object>> hits) {
    final ImmutableList<Map<String, Object>> listT =
        hits != null ? ImmutableList.copyOf(hits) : ImmutableList.of();

    return new AutoValue_SearchResults(totalHits, listT);
  }

  /**
   * Utility method to create a new instance of {@link SearchResults}.
   *
   * @param totalHits the total hits in the db
   * @param hits the number of hits in this batch
   * @return a non-null instance of {@link SearchResults}
   */
  public static SearchResults searchResults(@JsonProperty("totalHits") final long totalHits,
      final Map<String, Object>... hits) {
    final ImmutableList<Map<String, Object>> listT =
        hits != null ? ImmutableList.copyOf(hits) : ImmutableList.of();

    return new AutoValue_SearchResults(totalHits, listT);
  }

  /**
   * Utility method to create a new instance of {@link SearchResults}.
   *
   * @return a non-null instance of {@link SearchResults}
   */
  public static SearchResults empty() {
    return searchResults(0, ImmutableList.of());
  }

  /**
   * The total number of hits that matched the query.
   *
   * @return the total hits
   */
  @JsonProperty("totalHits")
  public abstract long totalHits();

  /**
   * The results in this batch of the query.
   *
   * @return the hits
   */
  @JsonProperty("hits")
  public abstract ImmutableList<Map<String, Object>> hits();
}
