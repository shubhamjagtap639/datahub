package com.linkedin.metadata;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.linkedin.data.avro.DataTranslator;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.mxe.FailedMetadataChangeEvent;
import com.linkedin.mxe.MetadataAuditEvent;
import com.linkedin.mxe.MetadataChangeEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.annotation.Nonnull;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;


public class EventUtils {

  private static final RecordDataSchema MCE_PEGASUS_SCHEMA = new MetadataChangeEvent().schema();

  private static final RecordDataSchema MAE_PEGASUS_SCHEMA = new MetadataAuditEvent().schema();

  private static final Schema ORIGINAL_MCE_AVRO_SCHEMA =
      getAvroSchemaFromResource("avro/com/linkedin/mxe/MetadataChangeEvent.avsc");

  private static final Schema ORIGINAL_MAE_AVRO_SCHEMA =
      getAvroSchemaFromResource("avro/com/linkedin/mxe/MetadataAuditEvent.avsc");

  private static final Schema ORIGINAL_FAILED_MCE_AVRO_SCHEMA =
      getAvroSchemaFromResource("avro/com/linkedin/mxe/FailedMetadataChangeEvent.avsc");

  private static final Schema RENAMED_MCE_AVRO_SCHEMA = com.linkedin.pegasus2avro.mxe.MetadataChangeEvent.SCHEMA$;

  private static final Schema RENAMED_MAE_AVRO_SCHEMA = com.linkedin.pegasus2avro.mxe.MetadataAuditEvent.SCHEMA$;

  private static final Schema RENAMED_FAILED_MCE_AVRO_SCHEMA = com.linkedin.pegasus2avro.mxe.FailedMetadataChangeEvent.SCHEMA$;

  private EventUtils() {
    // Util class
  }

  @Nonnull
  private static Schema getAvroSchemaFromResource(@Nonnull String resourcePath) {
    URL url = Resources.getResource(resourcePath);
    try {
      return Schema.parse(Resources.toString(url, Charsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Converts a {@link GenericRecord} MAE into the equivalent Pegasus model.
   *
   * @param record the {@link GenericRecord} that contains the MAE in com.linkedin.pegasus2avro namespace
   * @return the Pegasus {@link MetadataAuditEvent} model
   */
  @Nonnull
  public static MetadataAuditEvent avroToPegasusMAE(@Nonnull GenericRecord record) throws IOException {
    return new MetadataAuditEvent(DataTranslator.genericRecordToDataMap(
        renameSchemaNamespace(record, RENAMED_MAE_AVRO_SCHEMA, ORIGINAL_MAE_AVRO_SCHEMA), MAE_PEGASUS_SCHEMA,
        ORIGINAL_MAE_AVRO_SCHEMA));
  }

  /**
   * Converts a {@link GenericRecord} MCE into the equivalent Pegasus model.
   *
   * @param record the {@link GenericRecord} that contains the MCE in com.linkedin.pegasus2avro namespace
   * @return the Pegasus {@link MetadataChangeEvent} model
   */
  @Nonnull
  public static MetadataChangeEvent avroToPegasusMCE(@Nonnull GenericRecord record) throws IOException {
    return new MetadataChangeEvent(DataTranslator.genericRecordToDataMap(
        renameSchemaNamespace(record, RENAMED_MCE_AVRO_SCHEMA, ORIGINAL_MCE_AVRO_SCHEMA), MCE_PEGASUS_SCHEMA,
        ORIGINAL_MCE_AVRO_SCHEMA));
  }

  /**
   * Converts a Pegasus MAE into the equivalent Avro model as a {@link GenericRecord}.
   *
   * @param event the Pegasus {@link MetadataAuditEvent} model
   * @return the Avro model with com.linkedin.pegasus2avro.mxe namesapce
   * @throws IOException if the conversion fails
   */
  @Nonnull
  public static GenericRecord pegasusToAvroMAE(@Nonnull MetadataAuditEvent event) throws IOException {
    GenericRecord original =
        DataTranslator.dataMapToGenericRecord(event.data(), event.schema(), ORIGINAL_MAE_AVRO_SCHEMA);
    return renameSchemaNamespace(original, ORIGINAL_MAE_AVRO_SCHEMA, RENAMED_MAE_AVRO_SCHEMA);
  }

  /**
   * Converts a Pegasus MCE into the equivalent Avro model as a {@link GenericRecord}.
   *
   * @param event the Pegasus {@link MetadataChangeEvent} model
   * @return the Avro model with com.linkedin.pegasus2avro.mxe namesapce
   * @throws IOException if the conversion fails
   */
  @Nonnull
  public static GenericRecord pegasusToAvroMCE(@Nonnull MetadataChangeEvent event) throws IOException {
    GenericRecord original =
        DataTranslator.dataMapToGenericRecord(event.data(), event.schema(), ORIGINAL_MCE_AVRO_SCHEMA);
    return renameSchemaNamespace(original, ORIGINAL_MCE_AVRO_SCHEMA, RENAMED_MCE_AVRO_SCHEMA);
  }

  /**
   * Converts a Pegasus Failed MCE into the equivalent Avro model as a {@link GenericRecord}.
   *
   * @param failedMetadataChangeEvent the Pegasus {@link FailedMetadataChangeEvent} model
   * @return the Avro model with com.linkedin.pegasus2avro.mxe namesapce
   * @throws IOException if the conversion fails
   */
  @Nonnull
  public static GenericRecord pegasusToAvroFailedMCE(@Nonnull FailedMetadataChangeEvent failedMetadataChangeEvent) throws IOException {
    GenericRecord original =
        DataTranslator.dataMapToGenericRecord(failedMetadataChangeEvent.data(), failedMetadataChangeEvent.schema(), ORIGINAL_FAILED_MCE_AVRO_SCHEMA);
    return renameSchemaNamespace(original, ORIGINAL_FAILED_MCE_AVRO_SCHEMA, RENAMED_FAILED_MCE_AVRO_SCHEMA);
  }

  /**
   * Converts original MXE into a renamed namespace
   */
  @Nonnull
  private static GenericRecord renameSchemaNamespace(@Nonnull GenericRecord original, @Nonnull Schema originalSchema,
      @Nonnull Schema newSchema) throws IOException {

    // Step 1: Updates to the latest original schema
    final GenericRecord record = changeSchema(original, original.getSchema(), originalSchema);

    // Step 2: Updates to the new renamed schema
    return changeSchema(record, newSchema, newSchema);
  }

  /**
   * Changes the schema of a {@link GenericRecord} to a compatible schema
   *
   * Achieved by serializing the record using its embedded schema and deserializing it using the new compatible schema.
   *
   * @param record the record to update schema for
   * @param writerSchema the writer schema to use when deserializing
   * @param readerSchema the reader schema to use when deserializing
   * @return a {@link GenericRecord} using the new {@code readerSchema}
   * @throws IOException
   */
  @Nonnull
  private static GenericRecord changeSchema(@Nonnull GenericRecord record, @Nonnull Schema writerSchema,
      @Nonnull Schema readerSchema) throws IOException {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(os, null);
      DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(record.getSchema());
      writer.write(record, encoder);
      encoder.flush();
      os.close();

      try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
        Decoder decoder = DecoderFactory.get().binaryDecoder(is, null);
        // Must specify both writer & reader schemas for a backward compatible read
        DatumReader<GenericRecord> reader = new GenericDatumReader<>(writerSchema, readerSchema);
        return reader.read(null, decoder);
      }
    }
  }
}
