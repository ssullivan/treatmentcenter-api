package com.github.ssullivan.db.postgres;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Objects;

public class PgJSONBJacksonBinding implements Binding<Object, JsonNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PgJSONBJacksonBinding.class);
    private static final Converter<Object, JsonNode> CONVERTER = new JacksonConverter();

    @Override
    public Converter<Object, JsonNode> converter() {
        return CONVERTER;
    }

    @Override
    public void sql(BindingSQLContext<JsonNode> ctx) throws SQLException {
        // Depending on how you generate your SQL, you may need to explicitly distinguish
        // between jOOQ generating bind variables or inlined literals.
        if (ctx.render().paramType() == ParamType.INLINED)
            ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("::jsonb");
        else
            ctx.render().sql("?::jsonb");
    }

    // Registering VARCHAR types for JDBC CallableStatement OUT parameters
    @Override
    public void register(BindingRegisterContext<JsonNode> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
    }

    // Converting the JsonNode to a String value and setting that on a JDBC PreparedStatement
    @Override
    public void set(BindingSetStatementContext<JsonNode> ctx) throws SQLException {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
    }

    // Getting a String value from a JDBC ResultSet and converting that to a JsonNode
    @Override
    public void get(BindingGetResultSetContext<JsonNode> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
    }

    // Getting a String value from a JDBC CallableStatement and converting that to a JsonNode
    @Override
    public void get(BindingGetStatementContext<JsonNode> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
    }

    // Setting a value on a JDBC SQLOutput (useful for Oracle OBJECT types)
    @Override
    public void set(BindingSetSQLOutputContext<JsonNode> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    // Getting a value from a JDBC SQLInput (useful for Oracle OBJECT types)
    @Override
    public void get(BindingGetSQLInputContext<JsonNode> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }


    private static class JacksonConverter implements Converter<Object, JsonNode> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        @Override
        public JsonNode from(Object databaseObject) {
            try {
                return databaseObject == null ? null : OBJECT_MAPPER.readValue("" + databaseObject, JsonNode.class);
            } catch (IOException e) {
                LOGGER.error("Failed to deserialize json '{}' to JsonNode", databaseObject, e);
            }
            return null;
        }

        @Override
        public Object to(JsonNode userObject) {
            if (userObject == null) {
                return null;
            }

            try {
                return OBJECT_MAPPER.writeValueAsString(userObject);
            } catch (JsonProcessingException e) {
                LOGGER.error("Failed to serialize to json: {}", userObject, e);
            }

            return null;
        }

        @Override
        public Class<Object> fromType() {
            return Object.class;
        }

        @Override
        public Class<JsonNode> toType() {
            return JsonNode.class;
        }
    }
}
