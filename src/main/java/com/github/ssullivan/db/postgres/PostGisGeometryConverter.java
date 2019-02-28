package com.github.ssullivan.db.postgres;

import com.github.ssullivan.core.UncheckedSqlException;
import org.jooq.Converter;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class PostGisGeometryConverter implements Converter<Object, Geometry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostGisGeometryConverter.class);

    @Override
    public Geometry from(Object databaseObject) {
        try {
            if (databaseObject == null) return null;
            else return PGgeometry.geomFromString(databaseObject.toString());
        }
        catch (SQLException e) {
            LOGGER.error("Failed to convert value to Geometry", e);
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public Object to(Geometry userObject) {
        try {
            if (null == userObject) return null;
            else {
                PGobject pGobject = new PGobject();
                pGobject.setType(userObject.getTypeString());
                pGobject.setValue(userObject.getValue());

                return pGobject;
            }
        }
        catch (SQLException e) {
            LOGGER.error("Failed to convert value to PGobject", e);
            throw new UncheckedSqlException(e);
        }
    }

    @Override
    public Class<Object> fromType() {
        return Object.class;
    }

    @Override
    public Class<Geometry> toType() {
        return Geometry.class;
    }


}
