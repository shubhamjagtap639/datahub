/**
 * Copyright 2015 LinkedIn Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package wherehows.ingestion.processors;

import com.linkedin.events.metadata.ChangeAuditStamp;
import com.linkedin.events.metadata.ChangeType;
import com.linkedin.events.metadata.DatasetIdentifier;
import com.linkedin.events.metadata.DatasetSchema;
import com.linkedin.events.metadata.FailedMetadataChangeEvent;
import com.linkedin.events.metadata.MetadataChangeEvent;
import com.linkedin.events.metadata.Schemaless;
import java.util.Properties;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.IndexedRecord;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import wherehows.common.exceptions.UnauthorizedException;
import wherehows.dao.DaoFactory;
import wherehows.dao.table.DatasetComplianceDao;
import wherehows.dao.table.DatasetOwnerDao;
import wherehows.dao.table.DictDatasetDao;
import wherehows.dao.table.FieldDetailDao;
import wherehows.models.table.DictDataset;
import wherehows.ingestion.converters.KafkaLogCompactionConverter;
import wherehows.ingestion.utils.ProcessorUtil;

import static wherehows.common.utils.StringUtil.*;


@Slf4j
public class MetadataChangeProcessor extends KafkaMessageProcessor {

  private final DictDatasetDao _dictDatasetDao;
  private final FieldDetailDao _fieldDetailDao;
  private final DatasetOwnerDao _ownerDao;
  private final DatasetComplianceDao _complianceDao;

  private final Set<String> _whitelistActors;

  private final static int MAX_DATASET_NAME_LENGTH = 400;

  public MetadataChangeProcessor(@Nonnull Properties config, @Nonnull DaoFactory daoFactory,
      @Nonnull String producerTopic, @Nonnull KafkaProducer<String, IndexedRecord> producer) {
    super(config, daoFactory, producerTopic, producer);

    _dictDatasetDao = _daoFactory.getDictDatasetDao();
    _fieldDetailDao = _daoFactory.getDictFieldDetailDao();
    _ownerDao = _daoFactory.getDatasteOwnerDao();
    _complianceDao = _daoFactory.getDatasetComplianceDao();

    _whitelistActors = ProcessorUtil.getWhitelistedActors(_config, "whitelist.mce");
    log.info("MCE whitelist: " + _whitelistActors);
  }

  /**
   * Process a MetadataChangeEvent record
   * @param indexedRecord IndexedRecord
   */
  public void process(IndexedRecord indexedRecord) {
    if (indexedRecord == null || indexedRecord.getClass() != MetadataChangeEvent.class) {
      throw new IllegalArgumentException("Invalid record");
    }

    log.debug("Processing Metadata Change Event record.");

    final MetadataChangeEvent event = (MetadataChangeEvent) indexedRecord;
    try {
      processEvent(event);
    } catch (Exception exception) {
      log.error("MCE Processor Error:", exception);
      log.error("Message content: {}", event.toString());
      sendMessage(newFailedEvent(event, exception));
    }
  }

  public void processEvent(MetadataChangeEvent event) throws Exception {
    final ChangeAuditStamp changeAuditStamp = event.changeAuditStamp;
    String actorUrn = changeAuditStamp.actorUrn == null ? null : changeAuditStamp.actorUrn.toString();
    if (_whitelistActors != null && !_whitelistActors.contains(actorUrn)) {
      throw new UnauthorizedException("Actor " + actorUrn + " not in whitelist, skip processing");
    }

    event = new KafkaLogCompactionConverter().convert(event);

    final ChangeType changeType = changeAuditStamp.type;

    final DatasetIdentifier identifier = event.datasetIdentifier;
    log.debug("MCE: " + identifier);

    // check dataset name length to be within limit. Otherwise, save to DB will fail.
    if (identifier.nativeName.length() > MAX_DATASET_NAME_LENGTH) {
      throw new IllegalArgumentException("Dataset name too long: " + identifier);
    }

    // if DELETE, mark dataset as removed and return
    if (changeType == ChangeType.DELETE) {
      _dictDatasetDao.setDatasetRemoved(identifier, true, event.deploymentInfo, changeAuditStamp);
      return;
    }

    final DatasetSchema dsSchema = event.schema instanceof DatasetSchema ? (DatasetSchema) event.schema : null;

    // create or update dataset
    final DictDataset dataset =
        _dictDatasetDao.insertUpdateDataset(identifier, changeAuditStamp, event.datasetProperty, dsSchema,
            event.deploymentInfo, toStringList(event.tags), event.capacity, event.partitionSpec);

    // if schema is not null, insert or update schema
    if (dsSchema != null) { // if instanceof DatasetSchema
      _fieldDetailDao.insertUpdateDatasetFields(identifier, dataset, event.deploymentInfo, event.datasetProperty,
          changeAuditStamp, dsSchema);
    } else if (event.schema instanceof Schemaless) { // if instanceof Schemaless
      _fieldDetailDao.insertUpdateSchemaless(identifier, event.deploymentInfo, changeAuditStamp);
    }

    // if owners are not null, insert or update owner
    if (event.owners != null) {
      _ownerDao.insertUpdateOwnership(identifier, dataset, changeAuditStamp, event.owners);
    }

    // if retention or compliance is not null, insert or update retention / compliance
    // if both null, bypass this
    if (event.compliancePolicy != null || event.retentionPolicy != null) {
      _complianceDao.insertUpdateCompliance(identifier, dataset, changeAuditStamp, event.compliancePolicy,
          event.retentionPolicy);
    }

    // if suggested compliance is not null, insert or update suggested compliance
    if (event.suggestedCompliancePolicy != null) {
      _complianceDao.insertUpdateSuggestedCompliance(identifier, dataset, changeAuditStamp,
          event.suggestedCompliancePolicy);
    }
  }

  public FailedMetadataChangeEvent newFailedEvent(MetadataChangeEvent event, Throwable throwable) {
    FailedMetadataChangeEvent failedEvent = new FailedMetadataChangeEvent();
    failedEvent.time = System.currentTimeMillis();
    failedEvent.error = ExceptionUtils.getStackTrace(throwable);
    failedEvent.metadataChangeEvent = event;
    return failedEvent;
  }
}
