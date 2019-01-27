package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFeedDao;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.Optional;

@ImplementedBy(RedisFeedDao.class)
public interface IFeedDao {
  Optional<String> nextFeedId() throws IOException;

  Optional<String> currentFeedId() throws IOException;

  Optional<String> searchFeedId() throws IOException;
}
