package com.github.ssullivan.db.postgres;

import com.github.ssullivan.db.IndexFacility;
import com.github.ssullivan.model.Facility;
import java.io.IOException;
import java.util.List;

public class NoOpIndexFacility implements IndexFacility {

  @Override
  public void index(String feed, Facility facility) throws IOException {

  }

  @Override
  public void index(String feed, List<Facility> batch) throws IOException {

  }

  @Override
  public void index(Facility facility) throws IOException {

  }

  @Override
  public void expire(String feed, long seconds, boolean overwrite) throws Exception {

  }
}
