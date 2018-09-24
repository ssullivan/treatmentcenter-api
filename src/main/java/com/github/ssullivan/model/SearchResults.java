package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel
@AutoValue
public abstract class SearchResults<T> {

  /**
   * Utility method to create a new instance of {@link SearchResults}.
   *
   * @param totalHits the total hits in the db
   * @param hits the number of hits in this batch
   * @return a non-null instance of {@link SearchResults}
   */
  @JsonCreator
  public static <E> SearchResults<E> searchResults(@JsonProperty("totalHits") final long totalHits,
      @JsonProperty("hits") final List<E> hits) {
    final ImmutableList<E> listT =
        hits != null ? ImmutableList.copyOf(hits) : ImmutableList.of();

    return new AutoValue_SearchResults<>(totalHits, listT);
  }


  /**
   * Utility method to create a new instance of {@link SearchResults}.
   *
   * @param totalHits the total hits in the db
   * @param hits the number of hits in this batch
   * @return a non-null instance of {@link SearchResults}
   */
  public static <E> SearchResults<E> searchResults(@JsonProperty("totalHits") final long totalHits,
      final E... hits) {
    final ImmutableList<E> listT =
        hits != null ? ImmutableList.copyOf(hits) : ImmutableList.of();

    return new AutoValue_SearchResults<>(totalHits, listT);
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
  @ApiModelProperty(value = "The total number of results found. This is the same as the number of hits returned")
  @JsonProperty("totalHits")
  public abstract long totalHits();

  /**
   * The results in this batch of the query.
   *
   * @return the hits
   */
  @ApiModelProperty(value = "The results of the search operation")
  @JsonProperty("hits")
  public abstract ImmutableList<T> hits();
}
