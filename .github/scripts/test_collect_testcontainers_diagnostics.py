import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stderr
from pathlib import Path
from unittest.mock import patch


SCRIPT = Path(__file__).with_name("collect-testcontainers-diagnostics.py")
SPEC = importlib.util.spec_from_file_location("collect_testcontainers_diagnostics", SCRIPT)
assert SPEC and SPEC.loader
diagnostics = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(diagnostics)


PINNED_WORKFLOW = """\
name: fixture
steps:
  - uses: actions/checkout@0123456789abcdef0123456789abcdef01234567
"""


class DiagnosticsCollectorTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.workflow = self.root / ".github" / "workflows" / "examples.yml"
        self.workflow.parent.mkdir(parents=True)
        self.workflow.write_text(PINNED_WORKFLOW, encoding="utf-8")

    def tearDown(self):
        self.temp_dir.cleanup()

    def run_main(self, *args, docker_run=None):
        argv = ["collect-testcontainers-diagnostics.py", *map(str, args)]
        stderr = io.StringIO()
        with patch.object(sys, "argv", argv), redirect_stderr(stderr):
            if docker_run is None:
                result = diagnostics.main()
            else:
                with patch.object(diagnostics.subprocess, "run", side_effect=docker_run):
                    result = diagnostics.main()
        return result, stderr.getvalue()

    def test_sanitize_redacts_credentials_uris_payloads_xml_and_exception_messages(self):
        raw = """\
https://user:password@example.test/path?token=secret
Authorization: Bearer authorization-secret
{"payload":{"nested":"payload-secret","token":"deep-secret"},"message":"message-secret"}
<body>xml-secret</body>
AWS_SECRET_ACCESS_KEY=upper-secret
aws_secret_access_key=lower-secret
IllegalStateException: exception-secret
"""

        sanitized = diagnostics.sanitize(raw)

        for secret in (
            "password@example.test",
            "authorization-secret",
            "payload-secret",
            "deep-secret",
            "message-secret",
            "xml-secret",
            "upper-secret",
            "lower-secret",
            "exception-secret",
        ):
            self.assertNotIn(secret, sanitized)
        self.assertGreaterEqual(sanitized.count("[REDACTED]"), 7)

    def test_empty_container_set_writes_manifest(self):
        output_dir = self.root / "diagnostics" / "empty"
        result, stderr = self.run_main(
            "--task-name",
            "empty-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
        )

        self.assertEqual(result, 0)
        self.assertEqual(stderr, "")
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["containers"], [])
        self.assertEqual(manifest["workflow_action_refs"], [
            "actions/checkout@0123456789abcdef0123456789abcdef01234567"
        ])

    def test_allowlisted_container_writes_sanitized_log_and_digest(self):
        output_dir = self.root / "diagnostics" / "kafka"
        image_digest = next(
            digest for digest in diagnostics.ALLOWLIST if digest.startswith("confluentinc/cp-kafka@")
        )

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", "container-1"]:
                payload = [{
                    "Name": "/kafka",
                    "Config": {"Image": "confluentinc/cp-kafka:local"},
                    "Image": "sha256:image-id",
                    "RepoDigests": [image_digest],
                    "Created": "2026-08-26T00:00:00Z",
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["logs", "--tail", "200"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    "token=secret\nIllegalStateException: sensitive\n",
                    "",
                )
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "kafka-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            "container-1",
            docker_run=docker_run,
        )

        self.assertEqual(result, 0)
        self.assertEqual(stderr, "")
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["containers"][0]["image_digest"], image_digest)
        log = (output_dir / "container-1.log").read_text(encoding="utf-8")
        self.assertNotIn("secret", log)
        self.assertNotIn("sensitive", log)

    def test_image_outside_allowlist_fails_without_leaking_docker_output(self):
        output_dir = self.root / "diagnostics" / "rejected"

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", "container-1"]:
                payload = [{
                    "Name": "/unknown",
                    "Config": {"Image": "private/image:latest"},
                    "Image": "sha256:image-id",
                    "RepoDigests": ["private/image@sha256:" + "f" * 64],
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "private-secret")
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "rejected-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            "container-1",
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)
        self.assertIn("rejected-task", stderr)
        self.assertNotIn("private-secret", stderr)

    def test_local_image_id_without_allowlisted_repo_digest_fails(self):
        output_dir = self.root / "diagnostics" / "local-id"

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", "container-1"]:
                payload = [{
                    "Name": "/unknown",
                    "Config": {"Image": "private/image:local"},
                    "Image": "sha256:image-id",
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["image", "inspect", "private/image:local"]:
                return subprocess.CompletedProcess(
                    command,
                    0,
                    json.dumps([{"RepoDigests": []}]),
                    "",
                )
            self.fail(f"unexpected docker command: {command}")

        result, _ = self.run_main(
            "--task-name",
            "local-id-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            "container-1",
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)

    def test_report_sanitization_preserves_relative_paths(self):
        test_results = self.root / "examples" / "coroutines-demo" / "build" / "test-results" / "test"
        html_reports = self.root / "examples" / "coroutines-demo" / "build" / "reports" / "tests" / "test"
        test_results.mkdir(parents=True)
        html_reports.mkdir(parents=True)
        (test_results / "TEST-example.xml").write_text(
            '<testsuite><system-out>token="xml-secret"</system-out></testsuite>', encoding="utf-8"
        )
        (html_reports / "index.html").write_text(
            '<html><body>https://user:pass@example.test</body></html>', encoding="utf-8"
        )
        output_dir = self.root / "examples" / "build" / "testcontainers-diagnostics" / "reports"
        destination = self.root / "examples" / "build" / "sanitized-test-reports"

        result, stderr = self.run_main(
            "--task-name",
            "report-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--sanitized-report-dir",
            destination,
            "--report-path",
            test_results,
            "--report-path",
            html_reports,
        )

        self.assertEqual(result, 0)
        self.assertEqual(stderr, "")
        copied = list(destination.rglob("*"))
        files = [path for path in copied if path.is_file()]
        self.assertEqual(len(files), 2)
        content = "\n".join(path.read_text(encoding="utf-8") for path in files)
        self.assertNotIn("xml-secret", content)
        self.assertNotIn("user:pass@example.test", content)
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(len(manifest["sanitized_reports"]), 2)

    def test_report_file_cap_is_fail_closed(self):
        reports = self.root / "examples" / "coroutines-demo" / "build" / "test-results" / "test"
        reports.mkdir(parents=True)
        for index in range(2):
            (reports / f"TEST-{index}.xml").write_text("<testsuite/>", encoding="utf-8")
        output_dir = self.root / "examples" / "build" / "testcontainers-diagnostics" / "cap"
        destination = self.root / "examples" / "build" / "sanitized-test-reports"

        result, _ = self.run_main(
            "--task-name",
            "cap-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--sanitized-report-dir",
            destination,
            "--report-path",
            reports,
            "--max-report-files",
            "1",
        )

        self.assertEqual(result, 1)
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertTrue(manifest["report_truncated"])
        self.assertEqual(len(manifest["sanitized_reports"]), 1)

    def test_mutable_workflow_reference_is_rejected(self):
        self.workflow.write_text("- uses: actions/checkout@v7\n", encoding="utf-8")
        output_dir = self.root / "diagnostics" / "mutable"

        result, stderr = self.run_main(
            "--task-name",
            "mutable-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
        )

        self.assertEqual(result, 1)
        self.assertIn("mutable-task", stderr)


if __name__ == "__main__":
    unittest.main()
