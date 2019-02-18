package com.github.ssullivan.db;

import com.github.ssullivan.db.redis.search.FindBySearchRequest;
import com.github.ssullivan.model.Facility;
import com.github.ssullivan.model.Page;
import com.github.ssullivan.model.SearchRequest;
import com.github.ssullivan.model.SearchResults;
import com.google.inject.ImplementedBy;
import java.util.concurrent.CompletionStage;

@ImplementedBy(FindBySearchRequest.class)
public interface IFindBySearchRequest {

  CompletionStage<SearchResults<Facility>> find(SearchRequest searchRequest,
      Page page) throws Exception;
}
