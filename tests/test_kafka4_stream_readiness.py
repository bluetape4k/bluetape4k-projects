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


class Kafka4StreamsReadinessContractTest(unittest.TestCase):
    def test_streams_wait_for_running_before_producing_records(self) -> None:
        source = KAFKA4_STREAMS_TEST.read_text()
        start_to_produce = source.split(
            "this.streamsBuilderFactoryBean.start()", maxsplit=1
        )[1].split("val payload1", maxsplit=1)[0]

        self.assertIn("if (newState == KafkaStreams.State.RUNNING)", source)
        self.assertIn("runningLatch.await(60.seconds).shouldBeTrue()", start_to_produce)


if __name__ == "__main__":
    unittest.main()
