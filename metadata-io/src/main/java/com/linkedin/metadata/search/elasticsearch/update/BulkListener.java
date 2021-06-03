package com.linkedin.metadata.search.elasticsearch.update;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;


@Slf4j
public class BulkListener implements BulkProcessor.Listener {
  private static final BulkListener INSTANCE = new BulkListener();

  public static BulkListener getInstance() {
    return INSTANCE;
  }

  @Override
  public void beforeBulk(long executionId, BulkRequest request) {

  }

  @Override
  public void afterBulk(long executionId, BulkRequest request, BulkResponse response) {
    log.info("Successfully feeded bulk request. Number of events: " + response.getItems().length + " Took time ms: "
        + response.getIngestTookInMillis());
  }

  @Override
  public void afterBulk(long executionId, BulkRequest request, Throwable failure) {
    log.info("Error feeding bulk request. No retries left", failure);
  }
}
