package com.github.ssullivan.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.ssullivan.model.conditions.RangeCondition;
import java.io.IOException;
import org.eclipse.jetty.util.IO;

public class RangeConditionDeserializer extends JsonDeserializer<RangeCondition> {

  @Override
  public RangeCondition deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JsonProcessingException {

    final JsonNode json = p.readValueAsTree();
    if (json.isInt()) {
      return new RangeCondition(json.intValue(), json.intValue());
    }

    JsonNode startNode = json.get("start");
    JsonNode stopNode = json.get("stop");

    if ((startNode != null && stopNode != null) && startNode.getNodeType() != stopNode.getNodeType()) {
      throw new IOException("start and stop must be the same type");
    }

    if (startNode != null && stopNode != null) {
      final JsonNodeType jsonNodeType = startNode.getNodeType();
      if (jsonNodeType == JsonNodeType.NUMBER) {
        return new RangeCondition(startNode.intValue(), stopNode.intValue());
      }
    }
    else if (startNode != null) {
      return RangeCondition.greaterThanOrEqual(startNode.intValue());
    }
    else if (stopNode != null) {
      return RangeCondition.lessThanOrEqual(startNode.intValue());
    }

    return null;
  }

}
