package com.github.ssullivan.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.github.ssullivan.db.psql.Tables;
import com.github.ssullivan.db.psql.tables.records.RecoveryHousingRecord;
import java.io.IOException;

public class RecoveryHousingRecordSerializer extends JsonSerializer<RecoveryHousingRecord> {

  @Override
  public void serialize(RecoveryHousingRecord value, JsonGenerator gen,
      SerializerProvider serializers) throws IOException {
      gen.writeStartObject();

      if (value.getName() != null)
        gen.writeStringField(Tables.RECOVERY_HOUSING.NAME.getName(), value.getName());

      if (value.getState() != null)
        gen.writeStringField(Tables.RECOVERY_HOUSING.STATE.getName(), value.getState());

      if (value.getCity() != null)
        gen.writeStringField(Tables.RECOVERY_HOUSING.CITY.getName(), value.getCity());

      if (value.getStreet() != null)
        gen.writeStringField(Tables.RECOVERY_HOUSING.STREET.getName(), value.getStreet());

      if (value.getPostalcode() != null)
        gen.writeStringField(Tables.RECOVERY_HOUSING.POSTALCODE.getName(), value.getPostalcode());

      if (value.getCapacity() != null)
        gen.writeNumberField(Tables.RECOVERY_HOUSING.CAPACITY.getName(), value.getCapacity());

      if (value.getFee() != null)
        gen.writeNumberField(Tables.RECOVERY_HOUSING.FEE.getName(), value.getFee());

      if (value.getContactName() != null || value.getContactEmail() != null || value.getContactPhonenumber() != null) {
        gen.writeArrayFieldStart("contacts");
        gen.writeStartObject();
        if (value.getContactName() != null)
          gen.writeStringField("name", value.getContactName());
        if (value.getContactPhonenumber() != null)
          gen.writeStringField("phone", value.getContactPhonenumber());
        if (value.getContactEmail() != null)
          gen.writeStringField("email", value.getContactEmail());
        gen.writeEndObject();
        gen.writeEndArray();
      }


      gen.writeEndObject();
  }
}
