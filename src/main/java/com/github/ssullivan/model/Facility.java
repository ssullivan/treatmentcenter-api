package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiModel;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@ApiModel
public class Facility {

  private String id;
  private String feedId;
  private String name1;
  private String name2;
  private String zip;
  private String street;
  private String city;
  private String state;
  private String county;
  private String googlePlaceId;
  private GeoPoint location;
  private String formattedAddress;
  private String website;
  private Set<String> phoneNumbers;
  private Set<String> categoryCodes;
  private Set<String> serviceCodes;
  private AvailableServices availableServices;
  private double score = 0.0;

  public Facility() {

  }

  /**
   * Copy constructor for {@link Facility}.
   *
   * @param facility another instance of Facility
   */
  public Facility(Facility facility) {
    this.id = facility.getId();
    this.feedId = facility.getFeedId();
    this.name1 = facility.getName1();
    this.name2 = facility.getName2();
    this.zip = facility.getZip();
    this.street = facility.getStreet();
    this.city = facility.getCity();
    this.state = facility.getState();
    this.googlePlaceId = facility.getGooglePlaceId();
    this.location = facility.getLocation();
    this.formattedAddress = facility.getFormattedAddress();
    this.website = facility.getWebsite();
    this.county = facility.getCounty();
    this.score = facility.score;

    if (facility.getPhoneNumbers() != null) {
      this.phoneNumbers = new HashSet<>(facility.getPhoneNumbers());
    }
    if (facility.getCategoryCodes() != null) {
      this.categoryCodes = new HashSet<>(facility.getCategoryCodes());
    }
    if (facility.getServiceCodes() != null) {
      this.serviceCodes = new HashSet<>(facility.getServiceCodes());
    }
    if (facility.getAvailableServices() != null) {
      this.availableServices = facility.getAvailableServices();
    }
  }

  /**
   * Creates a new instance of {@link Facility}.
   *
   * @param id the primary key >= 0 (if 0 one will be generated)
   * @param name1 the primary name for this facility
   * @param name2 a secondary name for this facility
   * @param zip zipcode of the facility
   * @param street street part of the address for the facility
   * @param city city part of the address for the facility
   * @param state state part of the address for the facility
   * @param googlePlaceId the google place id for the location
   * @param location the geo location of the facility
   * @param formattedAddress the clean formatted address for the location
   * @param website the website for the facility
   * @param phoneNumbers phone numbers associated with the facility
   * @param categoryCodes categories of services offered by the facility
   * @param serviceCodes services offered by the facility
   */
  public Facility(String id, String feedId, String name1, String name2, String zip, String street,
      String city, String state, String county, String googlePlaceId, GeoPoint location,
      String formattedAddress, String website, Set<String> phoneNumbers,
      Set<String> categoryCodes, Set<String> serviceCodes) {
    this.id = id;
    this.feedId = feedId;
    this.name1 = name1;
    this.name2 = name2;
    this.county = county;
    this.zip = zip;
    this.street = street;
    this.city = city;
    this.state = state;
    this.googlePlaceId = googlePlaceId;
    this.location = location;
    this.formattedAddress = formattedAddress;
    this.website = website;
    this.phoneNumbers = phoneNumbers;
    this.categoryCodes = categoryCodes;
    this.serviceCodes = serviceCodes;
  }

  /**
   * This is the uniq identifier for the locator spreadsheet that was downloaded via {@link
   * com.github.ssullivan.tasks.feeds.SamshaLocatorEtl}
   *
   * @return the feed id
   */
  public String getFeedId() {
    return feedId;
  }

  public void setFeedId(String feedId) {
    this.feedId = feedId;
  }

  /**
   * This is the county that the location is in.
   *
   * @return the county
   */
  public String getCounty() {
    return county;
  }

  public void setCounty(String county) {
    this.county = county;
  }

  /**
   * This is the street that the location is on.
   *
   * @return the street
   */
  public String getStreet() {
    return street;
  }

  public void setStreet(String street) {
    this.street = street;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName1() {
    return name1;
  }

  public void setName1(String name1) {
    this.name1 = name1;
  }

  public String getName2() {
    return name2;
  }

  public void setName2(String name2) {
    this.name2 = name2;
  }

  public String getGooglePlaceId() {
    return googlePlaceId;
  }

  public void setGooglePlaceId(String googlePlaceId) {
    this.googlePlaceId = googlePlaceId;
  }

  public GeoPoint getLocation() {
    return location;
  }

  public void setLocation(GeoPoint location) {
    this.location = location;
  }

  public String getFormattedAddress() {
    return formattedAddress;
  }

  public void setFormattedAddress(String formattedAddress) {
    this.formattedAddress = formattedAddress;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public Set<String> getPhoneNumbers() {
    return phoneNumbers;
  }

  public void setPhoneNumbers(Set<String> phoneNumbers) {
    this.phoneNumbers = phoneNumbers;
  }

  public Set<String> getCategoryCodes() {
    return categoryCodes;
  }

  public void setCategoryCodes(Set<String> categoryCodes) {
    this.categoryCodes = categoryCodes;
  }

  public Set<String> getServiceCodes() {
    return serviceCodes;
  }

  public void setServiceCodes(Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;
  }

  @JsonProperty("available")
  public AvailableServices getAvailableServices() {
    return availableServices;
  }

  public void setAvailableServices(AvailableServices availableServices) {
    this.availableServices = availableServices;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  /**
   * Checks if the facility has the specified service code.
   *
   * @param serviceCode a service code
   * @return true if the facility has the service code, false otherwise
   */
  @JsonIgnore
  public boolean hasService(final String serviceCode) {
    if (null == serviceCode || serviceCode.isEmpty()) {
      return false;
    }
    return this.getServiceCodes().contains(serviceCode);
  }

  /**
   * Checks if the facility has all of the provided service codes.
   *
   * @param services 1 or more service codes
   * @return true if the facility has all of the service codes, false otherwise
   */
  @JsonIgnore
  public boolean hasAllOf(final String... services) {
    if (null == services || services.length <= 0) {
      return false;
    }
    final Set<String> uniq = ImmutableSet.copyOf(services);
    return uniq.size() == Sets.intersection(uniq, this.serviceCodes).size();
  }

  @JsonIgnore
  public boolean hasAnyOf(final Set<String> services) {
    if (null == services || services.isEmpty()) {
      return false;
    }
    return !Sets.intersection(services, this.serviceCodes).isEmpty();
  }

  @JsonIgnore
  public boolean hasAnyOf(final String... services) {
    if (null == services || services.length <= 0) {
      return false;
    }
    return Stream.of(services)
        .anyMatch(it -> this.getServiceCodes() != null && this.getServiceCodes().contains(it));
  }

  @Override
  public String toString() {
    return "Facility{"
        + "id="
        + id
        + ", name1='"
        + name1
        + '\''
        + '}';
  }
}
