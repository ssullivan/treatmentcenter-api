package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiModel;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

@ApiModel
public class Facility {
  private long id;
  private String name1;
  private String name2;
  private String zip;
  private String street;
  private String city;
  private String state;
  private String googlePlaceId;
  private GeoPoint location;
  private String formattedAddress;
  private String website;
  private Set<String> phoneNumbers;
  private Set<String> categoryCodes;
  private Set<String> serviceCodes;
  private AvailableServices availableServices;

  public Facility() {

  }

  public Facility(Facility facility) {
    this.id = facility.getId();
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

    if (facility.getPhoneNumbers() != null)
      this.phoneNumbers = new HashSet<>(facility.getPhoneNumbers());
    if (facility.getCategoryCodes() != null)
      this.categoryCodes = new HashSet<>(facility.getCategoryCodes());
    if (facility.getServiceCodes() != null)
      this.serviceCodes = new HashSet<>(facility.getServiceCodes());
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
  public Facility(long id, String name1, String name2, String zip, String street,
      String city, String state, String googlePlaceId, GeoPoint location,
      String formattedAddress, String website, Set<String> phoneNumbers,
      Set<String> categoryCodes, Set<String> serviceCodes) {
    this.id = id;
    this.name1 = name1;
    this.name2 = name2;
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

  public long getId() {
    return id;
  }

  public void setId(long id) {
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

  @JsonIgnore
  public boolean hasService(final String serviceCode) {
    if (null == serviceCode || serviceCode.isEmpty()) return false;
    return this.getServiceCodes().contains(serviceCode);
  }

  @JsonIgnore
  public boolean hasAllOf(final String... services) {
    if (null == services || services.length <= 0) return false;
    return Stream.of(services)
        .allMatch(it -> this.getServiceCodes().contains(it));
  }

  @JsonIgnore
  public boolean hasAnyOf(final Set<String> services) {
    if (null == services || services.isEmpty()) return false;
    return !Sets.intersection(services, this.serviceCodes).isEmpty();
  }

  @JsonIgnore
  public boolean hasAnyOf(final String... services) {
    if (null == services || services.length <= 0) return false;
    return Stream.of(services)
        .anyMatch(it -> this.getServiceCodes().contains(it));
  }

  @Override
  public String toString() {
    return "Facility{" +
        "id=" + id +
        ", name1='" + name1 + '\'' +
        '}';
  }
}
