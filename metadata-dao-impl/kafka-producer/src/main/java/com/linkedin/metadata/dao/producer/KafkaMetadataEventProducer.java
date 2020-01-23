package com.linkedin.metadata.dao.producer;

import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.metadata.EventUtils;
import com.linkedin.metadata.dao.exception.ModelConversionException;
import com.linkedin.metadata.dao.utils.ModelUtils;
import com.linkedin.metadata.dao.utils.RecordUtils;
import com.linkedin.metadata.snapshot.Snapshot;
import com.linkedin.mxe.MetadataAuditEvent;
import com.linkedin.mxe.MetadataChangeEvent;
import com.linkedin.mxe.Topics;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.IndexedRecord;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;


/**
 * A Kafka implementation of {@link BaseMetadataEventProducer}.
 */
public class KafkaMetadataEventProducer<SNAPSHOT extends RecordTemplate, ASPECT_UNION extends UnionTemplate, URN extends Urn>
    extends BaseMetadataEventProducer<SNAPSHOT, ASPECT_UNION, URN> {

  private final Producer<String, ? extends IndexedRecord> _producer;
  private final Optional<Callback> _callback;

  /**
   * Constructor
   *
   * @param snapshotClass The snapshot class for the produced events
   * @param aspectUnionClass The aspect union in the snapshot
   * @param producer The Kafka {@link Producer} to use
   */
  public KafkaMetadataEventProducer(@Nonnull Class<SNAPSHOT> snapshotClass,
      @Nonnull Class<ASPECT_UNION> aspectUnionClass, @Nonnull Producer<String, ? extends IndexedRecord> producer) {
    super(snapshotClass, aspectUnionClass);
    _producer = producer;
    _callback = Optional.empty();
  }

  /**
   * Constructor
   *
   * @param snapshotClass The snapshot class for the produced events
   * @param aspectUnionClass The aspect union in the snapshot
   * @param producer The Kafka {@link Producer} to use
   * @param callback The {@link Callback} to invoke when the request is completed
   */
  public KafkaMetadataEventProducer(@Nonnull Class<SNAPSHOT> snapshotClass,
      @Nonnull Class<ASPECT_UNION> aspectUnionClass, @Nonnull Producer<String, ? extends IndexedRecord> producer,
      @Nonnull Callback callback) {
    super(snapshotClass, aspectUnionClass);
    _producer = producer;
    _callback = Optional.of(callback);
  }

  @Override
  public <ASPECT extends RecordTemplate> void produceSnapshotBasedMetadataChangeEvent(@Nonnull URN urn,
      @Nonnull ASPECT newValue) {
    MetadataChangeEvent metadataChangeEvent = new MetadataChangeEvent();
    metadataChangeEvent.setProposedSnapshot(makeSnapshot(urn, newValue));

    GenericRecord record;
    try {
      record = EventUtils.pegasusToAvroMCE(metadataChangeEvent);
    } catch (IOException e) {
      throw new ModelConversionException("Failed to convert Pegasus MCE to Avro", e);
    }

    if (_callback.isPresent()) {
      _producer.send(new ProducerRecord(Topics.METADATA_CHANGE_EVENT, urn.toString(), record), _callback.get());
    } else {
      _producer.send(new ProducerRecord(Topics.METADATA_CHANGE_EVENT, urn.toString(), record));
    }
  }

  @Override
  public <ASPECT extends RecordTemplate> void produceMetadataAuditEvent(@Nonnull URN urn, @Nullable ASPECT oldValue,
      @Nonnull ASPECT newValue) {

    MetadataAuditEvent metadataAuditEvent = new MetadataAuditEvent();
    metadataAuditEvent.setNewSnapshot(makeSnapshot(urn, newValue));
    if (oldValue != null) {
      metadataAuditEvent.setOldSnapshot(makeSnapshot(urn, oldValue));
    }

    GenericRecord record;
    try {
      record = EventUtils.pegasusToAvroMAE(metadataAuditEvent);
    } catch (IOException e) {
      throw new ModelConversionException("Failed to convert Pegasus MAE to Avro", e);
    }

    if (_callback.isPresent()) {
      _producer.send(new ProducerRecord(Topics.METADATA_AUDIT_EVENT, urn.toString(), record), _callback.get());
    } else {
      _producer.send(new ProducerRecord(Topics.METADATA_AUDIT_EVENT, urn.toString(), record));
    }
  }

  @Nonnull
  private Snapshot makeSnapshot(@Nonnull URN urn, @Nonnull RecordTemplate value) {
    Snapshot snapshot = new Snapshot();
    List<ASPECT_UNION> aspects = Collections.singletonList(ModelUtils.newAspectUnion(_aspectUnionClass, value));
    RecordUtils.setSelectedRecordTemplateInUnion(snapshot, ModelUtils.newSnapshot(_snapshotClass, urn, aspects));
    return snapshot;
  }
}
