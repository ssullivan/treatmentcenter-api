package com.github.ssullivan.db.postgres;

import com.github.ssullivan.model.GeoPoint;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.impl.DSL;

public class PostGisGeometryBinding implements Binding<Object, GeoPoint> {

  private final Converter<Object, GeoPoint> converter = new PostGisGeometryConverter();

  @Override
  public Converter<Object, GeoPoint> converter() {
    return converter;
  }

  @Override
  public void sql(BindingSQLContext<GeoPoint> ctx) throws SQLException {
    ctx.render().visit(DSL.sql("?::geometry(POINT)"));
  }

  @Override
  public void register(BindingRegisterContext<GeoPoint> ctx) throws SQLException {
    ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
  }

  @Override
  public void set(BindingSetStatementContext<GeoPoint> ctx) throws SQLException {
    ctx.statement().setObject(ctx.index(), ctx.convert(converter).value());
  }

  @Override
  public void set(BindingSetSQLOutputContext<GeoPoint> ctx) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void get(BindingGetResultSetContext<GeoPoint> ctx) throws SQLException {
    ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
  }

  @Override
  public void get(BindingGetStatementContext<GeoPoint> ctx) throws SQLException {
    ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
  }

  @Override
  public void get(BindingGetSQLInputContext<GeoPoint> ctx) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }
}
