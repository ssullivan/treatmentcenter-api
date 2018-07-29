package com.github.ssullivan.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SamshaFacility {
  private String name1;
  private String name2;
  private String street1;
  private String street2;
  private String city;
  private String state;
  private String zip;
  private String zip4;
  private String county;
  private String phone;
  private String intake_prompt;
  private String intake1;
  private String intake2;
  private String website;
  private String latitude;
  private String longitude;
  private String type;
  private String lastUpdate;
  private Set<String> categoryCodes;
  private Set<String> serviceCodes;
  private Set<String> serviceNames;
  private String formattedAddress;
  private String googlePlaceId;
  private GeoPoint googleLocation;
  private GeoPoint location;
  private Map<String, Object> unknown;

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

  public String getStreet1() {
    return street1;
  }

  public void setStreet1(String street1) {
    this.street1 = street1;
  }

  public String getStreet2() {
    return street2;
  }

  public void setStreet2(String street2) {
    this.street2 = street2;
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

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getZip4() {
    return zip4;
  }

  public void setZip4(String zip4) {
    this.zip4 = zip4;
  }

  public String getCounty() {
    return county;
  }

  public void setCounty(String county) {
    this.county = county;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  @JsonProperty("intake_prompt")
  public String getIntake_prompt() {
    return intake_prompt;
  }

  public void setIntake_prompt(String intake_prompt) {
    this.intake_prompt = intake_prompt;
  }

  public String getIntake1() {
    return intake1;
  }

  public void setIntake1(String intake1) {
    this.intake1 = intake1;
  }

  public String getIntake2() {
    return intake2;
  }

  public void setIntake2(String intake2) {
    this.intake2 = intake2;
  }

  public String getWebsite() {
    return website;
  }

  public void setWebsite(String website) {
    this.website = website;
  }

  public String getLatitude() {
    return latitude;
  }

  public void setLatitude(String latitude) {
    this.latitude = latitude;
  }

  public String getLongitude() {
    return longitude;
  }

  public void setLongitude(String longitude) {
    this.longitude = longitude;
  }

  @JsonProperty("type_facility")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("last_update")
  public String getLastUpdate() {
    return lastUpdate;
  }

  public void setLastUpdate(String lastUpdate) {
    this.lastUpdate = lastUpdate;
  }

  @JsonProperty("category_codes")
  public Set<String> getCategoryCodes() {
    return categoryCodes;
  }

  public void setCategoryCodes(Set<String> categoryCodes) {
    this.categoryCodes = categoryCodes;
  }

  @JsonProperty("service_codes")
  public Set<String> getServiceCodes() {
    return serviceCodes;
  }

  public void setServiceCodes(Set<String> serviceCodes) {
    this.serviceCodes = serviceCodes;
  }

  @JsonProperty("service_names")
  public Set<String> getServiceNames() {
    return serviceNames;
  }

  public void setServiceNames(Set<String> serviceNames) {
    this.serviceNames = serviceNames;
  }

  @JsonProperty("formatted_address")
  public String getFormattedAddress() {
    return formattedAddress;
  }

  public void setFormattedAddress(String formattedAddress) {
    this.formattedAddress = formattedAddress;
  }

  @JsonProperty("google_place_id")
  public String getGooglePlaceId() {
    return googlePlaceId;
  }

  public void setGooglePlaceId(String googlePlaceId) {
    this.googlePlaceId = googlePlaceId;
  }

  @JsonProperty("google_location")
  public GeoPoint getGoogleLocation() {
    return googleLocation;
  }

  public void setGoogleLocation(GeoPoint googleLocation) {
    this.googleLocation = googleLocation;
  }

  public GeoPoint getLocation() {
    return location;
  }

  public void setLocation(GeoPoint location) {
    this.location = location;
  }

  public Map<String, Object> getUnknown() {
    return unknown;
  }

  @JsonAnySetter
  public void setProperty(final String key, final Object value) {
    if (this.unknown == null) this.unknown = new HashMap<>();
    this.unknown.put(key, value);
  }

  public void setUnknown(Map<String, Object> unknown) {
    this.unknown = unknown;
  }
}
