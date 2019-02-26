package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.psql.tables.FeedDetail;
import com.github.ssullivan.db.psql.tables.daos.CategoryDao;
import com.github.ssullivan.db.psql.tables.daos.FeedDetailDao;
import com.github.ssullivan.db.psql.tables.daos.LocationDao;
import com.github.ssullivan.db.psql.tables.daos.ServiceDao;
import com.google.inject.ImplementedBy;
import com.zaxxer.hikari.HikariDataSource;

@ImplementedBy(JooqDaoFactory.class)
public interface IJooqDaoFactory {
    LocationDao locationDao();

    CategoryDao categoryDao();

    ServiceDao serviceDao();

    FeedDetailDao feedDetailDao();
}
