package com.linkedin.metadata.search.elasticsearch.query.request;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.IndicesOptions;


public class PITAwareSearchRequest extends SearchRequest {
  private IndicesOptions indicesOptions;

  @Override
  public SearchRequest indicesOptions(IndicesOptions indicesOptions) {
    this.indicesOptions = indicesOptions;
    return this;
  }

  @Override
  public IndicesOptions indicesOptions() {
    return indicesOptions;
  }

  @Override
  public boolean isCcsMinimizeRoundtrips() {
    return false;
  }
}
