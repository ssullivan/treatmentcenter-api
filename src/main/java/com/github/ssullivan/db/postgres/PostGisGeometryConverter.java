package com.github.ssullivan.db.postgres;

import com.github.ssullivan.model.GeoPoint;
import java.sql.SQLException;
import org.jooq.Converter;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgis.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostGisGeometryConverter implements Converter<Object, GeoPoint> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostGisGeometryConverter.class);

    @Override
    public GeoPoint from(Object databaseObject) {
        if (databaseObject == null) {
            return null;
        }

        Geometry geometry = null;

        try {
            geometry = PGgeometry.geomFromString(databaseObject.toString());
        }
        catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }

        if (!(geometry instanceof Point)) {
            throw new IllegalArgumentException("Geometry is not a org.postgis.Point.");
        }

        //https://postgis.net/2013/08/18/tip_lon_lat/
        //x = longitude, y = latitude
        Point point = (Point) geometry;
        return GeoPoint.geoPoint(point.getY(), point.getX());
    }

    @Override
    public Object to(GeoPoint geoPoint) {
        if (geoPoint == null) {
            return null;
        }
        //https://postgis.net/2013/08/18/tip_lon_lat/
        //x = longitude, y = latitude
        Point p = new Point(geoPoint.lon(), geoPoint.lat());
        p.setSrid(4326);

        return new PGgeometry(p);
    }

    @Override
    public Class<Object> fromType() {
        return Object.class;
    }

    @Override
    public Class<GeoPoint> toType() {
        return GeoPoint.class;
    }


}
