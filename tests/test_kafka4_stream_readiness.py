from __future__ import annotations

import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
KAFKA4_STREAMS_TEST = (
    ROOT
    / "infra"
    / "kafka4"
    / "src"
    / "test"
    / "kotlin"
    / "org"
    / "springframework"
    / "kafka"
    / "streams"
    / "KafkaStreamsTests.kt"
)
CI_WORKFLOW = ROOT / ".github" / "workflows" / "ci.yml"


class Kafka4StreamsReadinessContractTest(unittest.TestCase):
    def test_streams_wait_for_running_before_producing_records(self) -> None:
        source = KAFKA4_STREAMS_TEST.read_text()
        start_to_produce = source.split(
            "this.streamsBuilderFactoryBean.start()", maxsplit=1
        )[1].split("val payload1", maxsplit=1)[0]

        self.assertIn("if (newState == KafkaStreams.State.RUNNING)", source)
        self.assertIn("runningLatch.await(60.seconds).shouldBeTrue()", start_to_produce)
        self.assertIn("resultFuture.get(60.seconds)", source)
        self.assertNotIn("resultFuture.get(600.seconds)", source)
        self.assertIn("@DirtiesContext", source)
        self.assertIn("stopKafkaClientsBeforeBrokerShutdown", source)
        teardown = source[source.index("fun stopKafkaClientsBeforeBrokerShutdown"):source.index("    @Test", source.index("fun stopKafkaClientsBeforeBrokerShutdown"))]
        listener_index = teardown.index("kafkaListenerEndpointRegistry.stop()")
        streams_index = teardown.index("streamsBuilderFactoryBean.stop()")
        producer_index = teardown.index("producerFactory.destroy()")
        self.assertLess(listener_index, streams_index)
        self.assertLess(streams_index, producer_index)

    def test_kafka_ci_runs_contract_without_retry_masking(self) -> None:
        workflow = CI_WORKFLOW.read_text()
        kafka_job = workflow.split("test-kafka-infra:", maxsplit=1)[1].split(
            "test-telemetry-infra:", maxsplit=1
        )[0]

        self.assertIn("python3 -m unittest tests.test_kafka4_stream_readiness -v", kafka_job)
        self.assertNotIn("nick-fields/retry@v4", kafka_job)


if __name__ == "__main__":
    unittest.main()
