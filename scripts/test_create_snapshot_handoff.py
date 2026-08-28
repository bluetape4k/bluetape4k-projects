#!/usr/bin/env python3

import hashlib
import importlib.util
import json
import ssl
import tempfile
import threading
import unittest
import urllib.error
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest.mock import patch


REPOSITORY = Path(__file__).resolve().parents[1]
MODULE_PATH = REPOSITORY / "scripts" / "create_snapshot_handoff.py"


def load_module():
    spec = importlib.util.spec_from_file_location("create_snapshot_handoff", MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    spec.loader.exec_module(module)
    return module


def metadata(artifact: str, timestamp: str = "20260828.123456", build: str = "7") -> bytes:
    version = f"2.0.0-{timestamp}-{build}"
    extensions = ("pom",) if artifact == "bluetape4k-bom" else ("pom", "jar")
    versions = "".join(
        f"<snapshotVersion><extension>{extension}</extension><value>{version}</value>"
        f"<updated>20260828123456</updated></snapshotVersion>"
        for extension in extensions
    )
    return (
        "<metadata><groupId>io.github.bluetape4k</groupId>"
        f"<artifactId>{artifact}</artifactId><version>2.0.0-SNAPSHOT</version>"
        "<versioning><snapshot>"
        f"<timestamp>{timestamp}</timestamp><buildNumber>{build}</buildNumber>"
        "</snapshot><lastUpdated>20260828123456</lastUpdated>"
        f"<snapshotVersions>{versions}</snapshotVersions></versioning></metadata>"
    ).encode()


class FixtureServer:
    def __init__(self, routes: dict[str, list[tuple[int, bytes]]]):
        self.routes = routes
        self.counts: dict[str, int] = {}

        fixture = self

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self):
                responses = fixture.routes.get(self.path, [(404, b"missing")])
                index = fixture.counts.get(self.path, 0)
                fixture.counts[self.path] = index + 1
                status, body = responses[min(index, len(responses) - 1)]
                self.send_response(status)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, _format, *_args):
                return

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    @property
    def base_url(self) -> str:
        host, port = self.server.server_address
        return f"http://{host}:{port}"

    def __enter__(self):
        self.thread.start()
        return self

    def __exit__(self, *_args):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)


class SnapshotHandoffTest(unittest.TestCase):

    def setUp(self) -> None:
        self.module = load_module()
        self.group_path = "/io/github/bluetape4k"
        self.timestamped = "2.0.0-20260828.123456-7"
        self.resources = [
            ("bluetape4k-bom", "pom"),
            ("bluetape4k-tenant", "pom"),
            ("bluetape4k-tenant", "jar"),
        ]

    def routes(self) -> dict[str, list[tuple[int, bytes]]]:
        routes = {}
        for artifact in {artifact for artifact, _ in self.resources}:
            routes[f"{self.group_path}/{artifact}/2.0.0-SNAPSHOT/maven-metadata.xml"] = [
                (200, metadata(artifact)),
            ]
        for artifact, extension in self.resources:
            path = (
                f"{self.group_path}/{artifact}/2.0.0-SNAPSHOT/"
                f"{artifact}-{self.timestamped}.{extension}"
            )
            routes[path] = [(200, f"{artifact}:{extension}".encode())]
        return routes

    def create(self, base_url: str, output: Path):
        return self.module.create_verified_receipt(
            repository="bluetape4k/bluetape4k-projects",
            merge_sha="a" * 40,
            verified_ci_run_id="1234",
            publication_run_id="5678",
            handoff_issue_number="1562",
            base_url=base_url,
            group="io.github.bluetape4k",
            artifact="bluetape4k-bom",
            base_version="2.0.0",
            resource_specs=self.resources,
            output=output,
        )

    def test_verified_receipt_reads_metadata_and_resources_twice(self) -> None:
        with tempfile.TemporaryDirectory() as directory, FixtureServer(self.routes()) as fixture:
            output = Path(directory) / "tenant-context-handoff.json"
            receipt = self.create(fixture.base_url, output)

            self.assertEqual("bluetape.snapshot-handoff/v1", receipt["schema"])
            self.assertEqual("verified", receipt["status"])
            self.assertIsNone(receipt["catalog_commit_sha"])
            self.assertIsNone(receipt["supersedes"])
            self.assertEqual("20260828.123456", receipt["timestamp"])
            self.assertEqual("7", receipt["build_number"])
            self.assertEqual("20260828123456", receipt["last_updated"])
            self.assertEqual("1562", receipt["handoff_issue_number"])
            self.assertEqual(3, len(receipt["resources"]))
            self.assertEqual(receipt, json.loads(output.read_text(encoding="utf-8")))
            self.assertTrue(all(len(item["sha256"]) == 64 for item in receipt["resources"]))
            self.assertTrue(all(count == 2 for count in fixture.counts.values()))

    def test_missing_metadata_fails_closed(self) -> None:
        routes = self.routes()
        metadata_path = f"{self.group_path}/bluetape4k-bom/2.0.0-SNAPSHOT/maven-metadata.xml"
        routes[metadata_path] = [(404, b"missing")]
        with tempfile.TemporaryDirectory() as directory, FixtureServer(routes) as fixture:
            with self.assertRaises(self.module.TransientSnapshotHandoffError):
                self.create(fixture.base_url, Path(directory) / "receipt.json")

    def test_missing_resource_fails_closed(self) -> None:
        routes = self.routes()
        resource_path = (
            f"{self.group_path}/bluetape4k-tenant/2.0.0-SNAPSHOT/"
            f"bluetape4k-tenant-{self.timestamped}.jar"
        )
        routes[resource_path] = [(404, b"missing")]
        with tempfile.TemporaryDirectory() as directory, FixtureServer(routes) as fixture:
            with self.assertRaises(self.module.TransientSnapshotHandoffError):
                self.create(fixture.base_url, Path(directory) / "receipt.json")

    def test_resource_checksum_mutation_fails_closed(self) -> None:
        routes = self.routes()
        resource_path = (
            f"{self.group_path}/bluetape4k-tenant/2.0.0-SNAPSHOT/"
            f"bluetape4k-tenant-{self.timestamped}.pom"
        )
        routes[resource_path] = [(200, b"first"), (200, b"second")]
        with tempfile.TemporaryDirectory() as directory, FixtureServer(routes) as fixture:
            with self.assertRaisesRegex(self.module.SnapshotHandoffError, "checksum changed"):
                self.create(fixture.base_url, Path(directory) / "receipt.json")

    def test_metadata_mutation_during_readback_fails_closed(self) -> None:
        routes = self.routes()
        metadata_path = f"{self.group_path}/bluetape4k-bom/2.0.0-SNAPSHOT/maven-metadata.xml"
        routes[metadata_path] = [
            (200, metadata("bluetape4k-bom")),
            (200, metadata("bluetape4k-bom", timestamp="20260828.123500", build="8")),
        ]
        with tempfile.TemporaryDirectory() as directory, FixtureServer(routes) as fixture:
            with self.assertRaisesRegex(self.module.SnapshotHandoffError, "metadata changed"):
                self.create(fixture.base_url, Path(directory) / "receipt.json")

    def test_wrong_group_or_artifact_metadata_fails_closed(self) -> None:
        routes = self.routes()
        metadata_path = f"{self.group_path}/bluetape4k-tenant/2.0.0-SNAPSHOT/maven-metadata.xml"
        routes[metadata_path] = [
            (200, metadata("wrong-artifact")),
        ]
        with tempfile.TemporaryDirectory() as directory, FixtureServer(routes) as fixture:
            with self.assertRaisesRegex(self.module.SnapshotHandoffError, "coordinate mismatch"):
                self.create(fixture.base_url, Path(directory) / "receipt.json")

    def test_metadata_identity_rejects_control_or_path_characters(self) -> None:
        fixtures = (
            (b"20260828.123456", b"20260828/123456"),
            (b"<buildNumber>7</buildNumber>", b"<buildNumber>7\n8</buildNumber>"),
            (b"20260828123456", b"2026082812345/"),
        )
        for original, replacement in fixtures:
            with self.subTest(replacement=replacement):
                document = metadata("bluetape4k-bom").replace(original, replacement)
                with self.assertRaisesRegex(
                    self.module.SnapshotHandoffError, "invalid snapshot identity"
                ):
                    self.module.parse_metadata(
                        document,
                        "io.github.bluetape4k",
                        "bluetape4k-bom",
                        "2.0.0",
                    )

    def test_snapshot_version_value_must_match_metadata_identity(self) -> None:
        document = metadata("bluetape4k-bom").replace(
            b"2.0.0-20260828.123456-7",
            b"2.0.0-20260828.123456-8",
        )
        with self.assertRaisesRegex(self.module.SnapshotHandoffError, "version mismatch"):
            self.module.parse_metadata(
                document,
                "io.github.bluetape4k",
                "bluetape4k-bom",
                "2.0.0",
            )

    def test_tls_verification_failure_is_permanent(self) -> None:
        error = urllib.error.URLError(ssl.SSLCertVerificationError("invalid certificate"))
        with patch.object(self.module.urllib.request, "urlopen", side_effect=error):
            with self.assertRaises(self.module.SnapshotHandoffError) as raised:
                self.module.fetch("https://central.example.invalid/resource")
        self.assertNotIsInstance(
            raised.exception, self.module.TransientSnapshotHandoffError
        )

    def test_connection_failure_is_transient(self) -> None:
        error = urllib.error.URLError(ConnectionResetError("connection reset"))
        with patch.object(self.module.urllib.request, "urlopen", side_effect=error):
            with self.assertRaises(self.module.TransientSnapshotHandoffError):
                self.module.fetch("https://central.example.invalid/resource")

    def test_rejected_receipt_is_append_only_and_links_source_digest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "verified.json"
            source.write_text('{"schema":"bluetape.snapshot-handoff/v1","status":"verified"}\n')
            original = source.read_bytes()
            rejected = Path(directory) / "rejected.json"

            receipt = self.module.create_rejected_receipt(source, rejected)

            self.assertEqual(original, source.read_bytes())
            self.assertEqual("rejected", receipt["status"])
            self.assertEqual(hashlib.sha256(original).hexdigest(), receipt["supersedes"])
            self.assertNotEqual(source, rejected)

    def test_last_good_manifest_pins_receipt_identity_and_validation(self) -> None:
        with tempfile.TemporaryDirectory() as directory, FixtureServer(self.routes()) as fixture:
            root = Path(directory)
            handoff = root / "tenant-context-handoff.json"
            receipt = self.create(fixture.base_url, handoff)
            manifest_path = root / "last-good-manifest.json"

            manifest = self.module.create_last_good_manifest(
                receipt_path=handoff,
                catalog_commit_sha=None,
                validation_command="./gradlew test --refresh-dependencies",
                validation_run_id="local-tenant-consumer",
                output=manifest_path,
            )

            self.assertEqual("bluetape.last-good-manifest/v1", manifest["schema"])
            self.assertEqual(receipt["merge_sha"], manifest["base_sha"])
            self.assertEqual(
                "io.github.bluetape4k:bluetape4k-bom:2.0.0-SNAPSHOT",
                manifest["dependency"]["coordinate"],
            )
            self.assertEqual(receipt["timestamp"], manifest["dependency"]["timestamp"])
            self.assertEqual(receipt["build_number"], manifest["dependency"]["build_number"])
            self.assertEqual(receipt["resources"], manifest["resources"])
            self.assertIsNone(manifest["catalog_commit_sha"])
            self.assertEqual(manifest, json.loads(manifest_path.read_text(encoding="utf-8")))


if __name__ == "__main__":
    unittest.main()
