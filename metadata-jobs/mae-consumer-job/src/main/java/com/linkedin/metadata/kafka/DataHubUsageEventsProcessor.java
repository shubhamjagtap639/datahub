package com.linkedin.metadata.kafka;

import com.linkedin.events.metadata.ChangeType;
import com.linkedin.metadata.kafka.elasticsearch.ElasticsearchConnector;
import com.linkedin.metadata.kafka.elasticsearch.JsonElasticEvent;
import com.linkedin.metadata.kafka.transformer.DataHubUsageEventTransformer;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import com.linkedin.mxe.Topics;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@ConditionalOnProperty(value = "DATAHUB_ANALYTICS_ENABLED", havingValue = "true", matchIfMissing = true)
@EnableKafka
public class DataHubUsageEventsProcessor {

  private final ElasticsearchConnector elasticSearchConnector;
  private final DataHubUsageEventTransformer dataHubUsageEventTransformer;
  private final String indexName;

  public DataHubUsageEventsProcessor(ElasticsearchConnector elasticSearchConnector,
      DataHubUsageEventTransformer dataHubUsageEventTransformer, IndexConvention indexConvention) {
    this.elasticSearchConnector = elasticSearchConnector;
    this.dataHubUsageEventTransformer = dataHubUsageEventTransformer;
    this.indexName = indexConvention.getIndexName("datahub_usage_event");
  }

  @KafkaListener(id = "${KAFKA_CONSUMER_GROUP_ID:datahub-usage-event-consumer-job-client}", topics =
      "${DATAHUB_USAGE_EVENT_NAME:" + Topics.DATAHUB_USAGE_EVENT + "}", containerFactory = "stringSerializedKafkaListener")
  public void consume(final ConsumerRecord<String, String> consumerRecord) {
    final String record = consumerRecord.value();
    log.debug("Got DHUE");

    Optional<DataHubUsageEventTransformer.TransformedDocument> eventDocument =
        dataHubUsageEventTransformer.transformDataHubUsageEvent(record);
    if (!eventDocument.isPresent()) {
      log.info("failed transform: {}", record);
      return;
    }
    JsonElasticEvent elasticEvent = new JsonElasticEvent(eventDocument.get().getDocument());
    try {
      elasticEvent.setId(URLEncoder.encode(eventDocument.get().getId(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      log.error("Failed to encode the urn with error: {}", e.toString());
      return;
    }
    elasticEvent.setIndex(indexName);
    elasticEvent.setActionType(ChangeType.CREATE);
    elasticSearchConnector.feedElasticEvent(elasticEvent);
  }
}
