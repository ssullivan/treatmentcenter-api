package com.github.ssullivan.db;

import java.io.IOException;
import java.util.Optional;

public interface IFeedDao {
  Optional<String> nextFeedId() throws IOException;

  Optional<String> currentFeedId() throws IOException;
}
