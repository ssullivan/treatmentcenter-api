package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.RedisFeedDao;
import com.google.inject.ImplementedBy;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

@ImplementedBy(RedisFeedDao.class)
public interface IFeedDao {

  /**
   * This is used to generate the id for the next feed we load.
   * This will genereate a new id every time this method is invoked
   *
   * @return a new id
   * @throws IOException
   */
  Optional<String> nextFeedId() throws IOException;

  Optional<String> setCurrentFeedId(final String id) throws IOException;

  Optional<String> setSearchFeedId(final String id) throws IOException;

  Collection<String> getFeedIds() throws IOException;




  /**
   * THis is the identifier of the most recent feed that was loaded.
   *
   * @return the id of the most recently loaded feed
   * @throws IOException
   */
  Optional<String> currentFeedId() throws IOException;

  /**
   * This is the identifier of the feed that we are currently using to serve search requests.
   *
   * @return the id of the feed to search
   * @throws IOException
   */
  Optional<String> searchFeedId() throws IOException;
}
