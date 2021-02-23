from dataclasses import dataclass

from confluent_kafka import SerializingProducer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from confluent_kafka.serialization import StringSerializer

from datahub.configuration.common import ConfigModel
from datahub.configuration.kafka import KafkaProducerConnectionConfig
from datahub.ingestion.api.common import PipelineContext, RecordEnvelope, WorkUnit
from datahub.ingestion.api.sink import Sink, SinkReport, WriteCallback
from datahub.metadata.com.linkedin.pegasus2avro.mxe import MetadataChangeEvent
from datahub.metadata.schema_classes import SCHEMA_JSON_STR

DEFAULT_KAFKA_TOPIC = "MetadataChangeEvent_v4"


class KafkaSinkConfig(ConfigModel):
    connection: KafkaProducerConnectionConfig = KafkaProducerConnectionConfig()
    topic: str = DEFAULT_KAFKA_TOPIC


@dataclass
class KafkaCallback:
    reporter: SinkReport
    record_envelope: RecordEnvelope
    write_callback: WriteCallback

    def kafka_callback(self, err, msg):
        if err is not None:
            self.reporter.report_failure(err)
            self.write_callback.on_failure(self.record_envelope, None, {"error": err})
        else:
            self.reporter.report_record_written(self.record_envelope)
            self.write_callback.on_success(self.record_envelope, {"msg": msg})


@dataclass
class DatahubKafkaSink(Sink):
    config: KafkaSinkConfig
    report: SinkReport

    def __init__(self, config: KafkaSinkConfig, ctx):
        super().__init__(ctx)
        self.config = config
        self.report = SinkReport()

        schema_registry_conf = {
            "url": self.config.connection.schema_registry_url,
            **self.config.connection.schema_registry_config,
        }
        schema_registry_client = SchemaRegistryClient(schema_registry_conf)

        def convert_mce_to_dict(mce: MetadataChangeEvent, ctx):
            tuple_encoding = mce.to_obj(tuples=True)
            return tuple_encoding

        avro_serializer = AvroSerializer(
            SCHEMA_JSON_STR, schema_registry_client, to_dict=convert_mce_to_dict
        )

        producer_config = {
            "bootstrap.servers": self.config.connection.bootstrap,
            "key.serializer": StringSerializer("utf_8"),
            "value.serializer": avro_serializer,
            **self.config.connection.producer_config,
        }

        self.producer = SerializingProducer(producer_config)

    @classmethod
    def create(cls, config_dict, ctx: PipelineContext):
        config = KafkaSinkConfig.parse_obj(config_dict)
        return cls(config, ctx)

    def handle_work_unit_start(self, workunit: WorkUnit) -> None:
        pass

    def handle_work_unit_end(self, workunit: WorkUnit) -> None:
        self.producer.flush()

    def write_record_async(
        self,
        record_envelope: RecordEnvelope[MetadataChangeEvent],
        write_callback: WriteCallback,
    ):
        # call poll to trigger any callbacks on success / failure of previous writes
        self.producer.poll(0)
        mce = record_envelope.record
        self.producer.produce(
            topic=self.config.topic,
            value=mce,
            on_delivery=KafkaCallback(
                self.report, record_envelope, write_callback
            ).kafka_callback,
        )

    def get_report(self):
        return self.report

    def close(self):
        self.producer.flush()
        # self.producer.close()
