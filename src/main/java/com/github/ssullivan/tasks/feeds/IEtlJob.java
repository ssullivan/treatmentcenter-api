package com.github.ssullivan.tasks.feeds;

import java.io.IOException;

public interface IEtlJob {
  void extract() throws IOException;

  void transform() throws IOException;

  void load() throws IOException;
}
