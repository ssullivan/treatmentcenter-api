package com.github.ssullivan.core;

import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.SortDirection;
import com.google.common.collect.Lists;
import java.util.List;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.Test;

public class FacilityComparatorTest {

  @Test
  public void testSortingByNullFacility() {
    FacilityComparator<Facility> comparator = new FacilityComparator<>("score", SortDirection.ASC);
    int result = comparator.compare(null, null);
    MatcherAssert.assertThat(result, Matchers.equalTo(0));
    int j = 0;
  }


  @Test
  public void testSortingByScoreAsc() {
    List<FacilityWithRadius> items = testData();

    items.sort(new FacilityComparator<>("score", SortDirection.ASC));

    MatcherAssert.assertThat(items.get(0).getScore(), Matchers.lessThanOrEqualTo(0.0));
    MatcherAssert.assertThat(items.get(1).getScore(), Matchers.greaterThanOrEqualTo(5.0));
  }

  @Test
  public void testSortingByScoreDesc() {
    List<FacilityWithRadius> items = testData();

    items.sort(new FacilityComparator<>("score", SortDirection.DESC));

    MatcherAssert.assertThat(items.get(0).getScore(), Matchers.greaterThanOrEqualTo(5.0));
    MatcherAssert.assertThat(items.get(1).getScore(), Matchers.lessThanOrEqualTo(0.0));
  }

  @Test
  public void testSortingByRadiusAsc() {
    List<FacilityWithRadius> items = testData();

    items.sort(new FacilityComparator<>("radius", SortDirection.ASC));

    MatcherAssert.assertThat(items.get(0).getRadius(), Matchers.lessThanOrEqualTo(0.0));
    MatcherAssert.assertThat(items.get(1).getRadius(), Matchers.greaterThanOrEqualTo(5.0));
  }

  @Test
  public void testSortingByRadiusDesc() {
    List<FacilityWithRadius> items = testData();

    items.sort(new FacilityComparator<>("radius", SortDirection.DESC));

    MatcherAssert.assertThat(items.get(0).getRadius(), Matchers.greaterThanOrEqualTo(5.0));
    MatcherAssert.assertThat(items.get(1).getRadius(), Matchers.lessThanOrEqualTo(0.0));
  }

  @Test
  public void testSortingByNameAsc() {
    List<FacilityWithRadius> items = testData();

    items.sort(new FacilityComparator<>("name1", SortDirection.ASC));

    MatcherAssert.assertThat(items.get(0).getName1(), Matchers.equalTo("aaa"));
    MatcherAssert.assertThat(items.get(1).getName1(), Matchers.equalTo("bbb"));
  }

  @Test
  public void testSortingByNameDesc() {
    List<FacilityWithRadius> items = testData();

    items.sort(new FacilityComparator<>("name1", SortDirection.DESC));

    MatcherAssert.assertThat(items.get(0).getName1(), Matchers.equalTo("bbb"));
    MatcherAssert.assertThat(items.get(1).getName1(), Matchers.equalTo("aaa"));
  }

  private List<FacilityWithRadius> testData() {
    final FacilityWithRadius f1 = new FacilityWithRadius();
    f1.setScore(5.0);
    f1.setRadius(5.0);
    f1.setName1("aaa");

    final FacilityWithRadius f2 = new FacilityWithRadius();
    f2.setScore(0.0);
    f2.setRadius(0.0);
    f2.setName1("bbb");

    List<FacilityWithRadius> items = Lists.newArrayList(f1, f2);
    return items;
  }
}
