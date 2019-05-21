package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.jooq.Condition;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PgJsonBUtils {
    private static final PgJSONBJacksonBinding PG_JSONB_JACKSON_BINDING = new PgJSONBJacksonBinding();
    private static final DataType<JsonNode> JSON_NODE_DATA_TYPE = SQLDataType.OTHER.asConvertedDataType(PG_JSONB_JACKSON_BINDING);

    public static Condition jsonContains(final Field<JsonNode> field, final JsonNode value) {
        return DSL.condition("{0} @> {1}::jsonb", field, DSL.val(value, field));
    }

    public static <T> Condition jsonContains(final Field<JsonNode> field, final Map<String, Object> keyValues) {
        return jsonContains(field, PG_JSONB_JACKSON_BINDING.converter().from(keyValues));
    }

    public static <T> Condition jsonContains(final Field<JsonNode> field, Map.Entry<String, T> entry) {
        final Map<String, T> keyValue = new LinkedHashMap<>(1);
        keyValue.put(entry.getKey(), entry.getValue());

        return jsonContains(field, PG_JSONB_JACKSON_BINDING.converter().from(keyValue));
    }

    public static <T> Condition jsonContains(final Field<JsonNode> field, final String key, T value) {
        final Map<String, T> keyValue = new LinkedHashMap<>(1);
        keyValue.put(key, value);

        return jsonContains(field, PG_JSONB_JACKSON_BINDING.converter().from(keyValue));
    }

    public static Condition jsonExists(final Field<JsonNode> field, final String fieldName) {
        return DSL.condition("{0} ? {1}", field, fieldName);
    }

    public static Field<JsonNode> jsonGetObjectByKey(final Field<JsonNode> field, final String fieldName) {
        return DSL.field("{0} -> {1}", JSON_NODE_DATA_TYPE, field, fieldName);
    }


    public static Field jsonGetArrayElement(final Field<JsonNode> field, final int index) {
        return DSL.field("{0} -> {1}", field, index);
    }
}
