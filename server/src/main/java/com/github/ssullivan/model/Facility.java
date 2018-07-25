package com.github.ssullivan.model;

import java.util.Set;

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
}
