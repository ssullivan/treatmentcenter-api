package com.github.ssullivan.db;

public interface IApiKeyDao {
  boolean isValidApiKey(String apiKey);
}
