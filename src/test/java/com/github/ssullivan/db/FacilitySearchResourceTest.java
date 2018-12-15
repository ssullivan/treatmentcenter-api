package com.github.ssullivan.db;

import com.github.ssullivan.api.IPostalcodeService;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.FacilityWithRadius;
import com.github.ssullivan.model.GeoPoint;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchResults;
import com.github.ssullivan.resources.FacilitySearchResource;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.hamcrest.Matchers;
import org.hamcrest.junit.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(DropwizardExtensionsSupport.class)
@TestInstance(Lifecycle.PER_CLASS)
public class FacilitySearchResourceTest {
  private static final GenericType<SearchResults<Facility>> SEARCH_RESULTS_GENERIC_TYPE =
      new GenericType<SearchResults<Facility>>(){

      };

  private static final IFacilityDao dao = Mockito.mock(IFacilityDao.class);
  private static final IPostalcodeService postalCodeService = Mockito.mock(IPostalcodeService.class);

  private static class FacilitySearchResults {
    private long totalHits;
    private List<FacilityWithRadius> hits;

    public long getTotalHits() {
      return totalHits;
    }

    public void setTotalHits(long totalHits) {
      this.totalHits = totalHits;
    }

    public List<FacilityWithRadius> getHits() {
      return hits;
    }

    public void setHits(List<FacilityWithRadius> hits) {
      this.hits = hits;
    }
  }

  public static final ResourceExtension resources = ResourceExtension.builder()
      .addResource(new FacilitySearchResource(dao, postalCodeService))
      .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
      .build();

  private static final Facility facility = new Facility(1L,
      "test", "test", "1234", "test",
      "test", "test", "test", GeoPoint.geoPoint(30.0, 30.0),
      "test", "http://test.com",
      ImmutableSet.of("123"), ImmutableSet.of("FOO"), ImmutableSet.of("BAR"));



  @BeforeAll
  public void setup() throws IOException {

    Mockito.when(dao.findByServiceCodes(Mockito.eq(Lists.newArrayList("BAR")),
        Mockito.eq(Page.page())))
        .thenReturn(SearchResults.searchResults(1L, facility));
    Mockito.when(dao.findByServiceCodesWithin(
        Mockito.anyList(),
        Mockito.anyDouble(),
        Mockito.anyDouble(),
        Mockito.anyDouble(),
        Mockito.eq("mi"),
        Mockito.any()))
        .thenReturn(SearchResults.searchResults(1L, new FacilityWithRadius(facility, 1.0)));

    Mockito.when(postalCodeService.fetchGeos(Mockito.anyString()))
        .thenReturn(ImmutableList.of(GeoPoint.geoPoint(33,33)));
  }

  @AfterAll
  public void teardown() {
    Mockito.reset(dao);
  }

  @Test
  public void testSearchingFacilities() {
    final FacilitySearchResults searchResults =
        resources.target("facilities").path("search")
            .queryParam("serviceCode", "BAR")
            .queryParam("lat", "30.0")
            .queryParam("lon", "30.0")
            .request().get(FacilitySearchResults.class);

    MatcherAssert.assertThat(searchResults, Matchers.notNullValue());
    MatcherAssert.assertThat(searchResults.getHits(), Matchers.notNullValue());
    MatcherAssert.assertThat(searchResults.getHits().size(), Matchers.equalTo(1));

    final Facility firstResult = searchResults.getHits().get(0);
    MatcherAssert.assertThat(firstResult.getCity(), Matchers.equalTo(facility.getCity()));
    MatcherAssert.assertThat(firstResult.getName1(), Matchers.equalTo(facility.getName1()));
    MatcherAssert.assertThat(firstResult.getName2(), Matchers.equalTo(facility.getName2()));
    MatcherAssert.assertThat(firstResult.getCategoryCodes(), Matchers.containsInAnyOrder("FOO"));
    MatcherAssert.assertThat(firstResult.getServiceCodes(), Matchers.containsInAnyOrder("BAR"));
    MatcherAssert.assertThat(firstResult.getLocation().lat(),
        Matchers.allOf(Matchers.greaterThanOrEqualTo(30.0),
            Matchers.lessThanOrEqualTo(30.1)));

    MatcherAssert.assertThat(firstResult.getLocation().lon(),
        Matchers.allOf(Matchers.greaterThanOrEqualTo(30.0),
            Matchers.lessThanOrEqualTo(30.1)));
  }

  @Test
  public void testSearchingByPostalCode() {
    final FacilitySearchResults searchResults =
        resources.target("facilities").path("search")
            .queryParam("serviceCode", "BAR")
            .queryParam("postalCode", "123456")
            .request().get(FacilitySearchResults.class);

    MatcherAssert.assertThat(searchResults, Matchers.notNullValue());
    MatcherAssert.assertThat(searchResults.getHits(), Matchers.notNullValue());
    MatcherAssert.assertThat(searchResults.getHits().size(), Matchers.equalTo(1));

    final Facility firstResult = searchResults.getHits().get(0);
    MatcherAssert.assertThat(firstResult.getCity(), Matchers.equalTo(facility.getCity()));
    MatcherAssert.assertThat(firstResult.getName1(), Matchers.equalTo(facility.getName1()));
    MatcherAssert.assertThat(firstResult.getName2(), Matchers.equalTo(facility.getName2()));
    MatcherAssert.assertThat(firstResult.getCategoryCodes(), Matchers.containsInAnyOrder("FOO"));
    MatcherAssert.assertThat(firstResult.getServiceCodes(), Matchers.containsInAnyOrder("BAR"));
    MatcherAssert.assertThat(firstResult.getLocation().lat(),
        Matchers.allOf(Matchers.greaterThanOrEqualTo(30.0),
            Matchers.lessThanOrEqualTo(30.1)));

    MatcherAssert.assertThat(firstResult.getLocation().lon(),
        Matchers.allOf(Matchers.greaterThanOrEqualTo(30.0),
            Matchers.lessThanOrEqualTo(30.1)));
  }

  @Test
  public void testInvalidPostalCode() {
    Response response =
        resources.target("facilities").path("search")
            .queryParam("serviceCode", "BAR")
            .queryParam("postalCode", "12345600000000000000000000000")
            .request()
        .get();
    MatcherAssert.assertThat(response.getStatus(), Matchers.equalTo(400));
  }
}
