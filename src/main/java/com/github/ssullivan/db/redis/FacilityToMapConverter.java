package com.github.ssullivan.db.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.ssullivan.model.Facility;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacilityToMapConverter implements Function<Facility, Map<String, String>> {
  private static final Logger LOGGER = LoggerFactory.getLogger(FacilityToMapConverter.class);
  private final ObjectMapper objectMapper;
  private final ObjectWriter objectWriter;

  @Inject
  public FacilityToMapConverter(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.objectWriter = objectMapper.writerFor(Facility.class);
  }

  @Override
  public Map<String, String> apply(Facility facility) {
    try {
      final Map<String, String> toReturn = new HashMap<>();
      toReturn.put("id", "" + facility.getId());
      toReturn.put("name1", facility.getName1());
      toReturn.put("name2", facility.getName2());
      toReturn.put("zip", facility.getZip());
      toReturn.put("street", facility.getStreet());
      toReturn.put("city", facility.getCity());
      toReturn.put("state", facility.getState());
      toReturn.put("county", facility.getCounty());
      toReturn.put("googlePlaceId", facility.getGooglePlaceId());
      toReturn.put("formattedAddress", facility.getFormattedAddress());
      toReturn.put("website", facility.getWebsite());
      facility.getCategoryCodes()
          .forEach(code -> toReturn.put("c:" + code, "1"));
      facility.getServiceCodes()
          .forEach(code -> toReturn.put("s:" + code, "1"));

      if (facility.getLocation() != null) {
        toReturn.put("location.lat", "" + facility.getLocation().lat());
        toReturn.put("location.lon", "" + facility.getLocation().lon());
      }

      toReturn.put("_source", objectWriter.writeValueAsString(facility));
      toReturn.put("phoneNumbers", objectMapper.writeValueAsString(facility.getPhoneNumbers()));
      return toReturn;
    }
    catch (JsonProcessingException e) {
      LOGGER.error("Failed to convert facility to map", e);
      throw new RuntimeException("Failed to convert facility to map", e);
    }
  }
}
