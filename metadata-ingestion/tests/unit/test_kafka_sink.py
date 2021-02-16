import unittest
from unittest.mock import MagicMock, patch

from datahub.ingestion.api.common import RecordEnvelope
from datahub.ingestion.api.sink import SinkReport, WriteCallback
from datahub.ingestion.sink.datahub_kafka import DatahubKafkaSink, KafkaCallback


class KafkaSinkTest(unittest.TestCase):
    @patch("datahub.ingestion.sink.datahub_kafka.PipelineContext")
    @patch("datahub.ingestion.sink.datahub_kafka.SerializingProducer")
    def test_kafka_sink_config(self, mock_producer, mock_context):
        kafka_sink = DatahubKafkaSink.create(
            {"connection": {"bootstrap": "foobar:9092"}}, mock_context
        )
        kafka_sink.close()
        assert mock_producer.call_count == 1  # constructor should be called

    def validate_kafka_callback(self, mock_k_callback, record_envelope, write_callback):
        assert mock_k_callback.call_count == 1  # KafkaCallback constructed
        constructor_args, constructor_kwargs = mock_k_callback.call_args
        assert constructor_args[1] == record_envelope
        assert constructor_args[2] == write_callback

    @patch("datahub.ingestion.sink.datahub_kafka.PipelineContext")
    @patch("datahub.ingestion.sink.datahub_kafka.SerializingProducer")
    @patch("datahub.ingestion.sink.datahub_kafka.KafkaCallback")
    def test_kafka_sink_write(self, mock_k_callback, mock_producer, mock_context):
        mock_producer_instance = mock_producer.return_value
        mock_k_callback_instance = mock_k_callback.return_value
        callback = MagicMock(spec=WriteCallback)
        kafka_sink = DatahubKafkaSink.create(
            {"connection": {"bootstrap": "foobar:9092"}}, mock_context
        )
        re = RecordEnvelope(record="test", metadata={})
        kafka_sink.write_record_async(re, callback)
        assert mock_producer_instance.poll.call_count == 1  # poll() called once
        self.validate_kafka_callback(
            mock_k_callback, re, callback
        )  # validate kafka callback was constructed appropriately

        # validate that confluent_kafka.Producer.produce was called with the right arguments
        args, kwargs = mock_producer_instance.produce.call_args
        created_callback = kwargs["on_delivery"]
        assert created_callback == mock_k_callback_instance.kafka_callback

    # TODO: Test that kafka producer is configured correctly

    @patch("datahub.ingestion.sink.datahub_kafka.PipelineContext")
    @patch("datahub.ingestion.sink.datahub_kafka.SerializingProducer")
    def test_kafka_sink_close(self, mock_producer, mock_context):
        mock_producer_instance = mock_producer.return_value
        kafka_sink = DatahubKafkaSink.create({}, mock_context)
        kafka_sink.close()
        mock_producer_instance.flush.assert_called_once()

    @patch("datahub.ingestion.sink.datahub_kafka.RecordEnvelope")
    @patch("datahub.ingestion.sink.datahub_kafka.WriteCallback")
    def test_kafka_callback_class(self, mock_w_callback, mock_re):
        callback = KafkaCallback(
            SinkReport(), record_envelope=mock_re, write_callback=mock_w_callback
        )
        mock_error = MagicMock()
        mock_message = MagicMock()
        callback.kafka_callback(mock_error, mock_message)
        assert mock_w_callback.on_failure.call_count == 1
        mock_w_callback.on_failure.called_with(mock_re, None, {"error", mock_error})
        callback.kafka_callback(None, mock_message)
        mock_w_callback.on_success.called_once_with(mock_re, {"msg", mock_message})
