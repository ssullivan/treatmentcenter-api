package com.github.ssullivan.db.env;

import com.github.ssullivan.db.IApiKeyDao;

public class EnvApiKeyDao implements IApiKeyDao {

  // the api key that is allowed to write data into the system
  private static final String ENV_API_KEY = "API_KEY";
  private final String authorizedApiKey;

  public EnvApiKeyDao() {
    authorizedApiKey = System.getenv(ENV_API_KEY) == null ? System.getProperty(ENV_API_KEY) : null;
  }

  @Override
  public boolean isValidApiKey(String apiKey) {
    if (null == apiKey || authorizedApiKey == null) {
      return false;
    }

    return authorizedApiKey.equals(apiKey);
  }
}
