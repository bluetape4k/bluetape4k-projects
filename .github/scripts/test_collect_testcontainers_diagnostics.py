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
CONTAINER_ID = "a" * 12


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
                def fake_logs(task_name, container_id, max_bytes):
                    del task_name, max_bytes
                    result = docker_run(["docker", "logs", "--tail", "200", container_id])
                    return diagnostics.DockerLogsResult(
                        returncode=result.returncode,
                        stdout=result.stdout,
                        truncated=getattr(result, "truncated", False),
                    )

                with (
                    patch.object(diagnostics.subprocess, "run", side_effect=docker_run),
                    patch.object(diagnostics, "run_docker_logs", side_effect=fake_logs),
                ):
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
clientSecret=client-secret access_token=access-secret
secretKey=secret-key-secret secret_key=snake-secret-key
mongodb://user:password@mongo.example/db
aws_access_key_id=AKIA-SECRET
private_key=private-key-secret
client.privateKey=client-key-secret
<failure message="failure-attribute-secret">failure-body-secret</failure>
<error>error-body-secret</error>
PLAINTEXT://broker:9092
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
            "client-secret",
            "access-secret",
            "secret-key-secret",
            "snake-secret-key",
            "password@mongo.example",
            "AKIA-SECRET",
            "private-key-secret",
            "client-key-secret",
            "failure-attribute-secret",
            "failure-body-secret",
            "error-body-secret",
            "exception-secret",
        ):
            self.assertNotIn(secret, sanitized)
        self.assertIn("PLAINTEXT://broker:9092", sanitized)
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
            if command[1:3] == ["inspect", CONTAINER_ID]:
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
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 0)
        self.assertEqual(stderr, "")
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["containers"][0]["image_digest"], image_digest)
        log = (output_dir / f"{CONTAINER_ID}.log").read_text(encoding="utf-8")
        self.assertNotIn("secret", log)
        self.assertNotIn("sensitive", log)

    def test_allowlisted_digest_is_resolved_from_image_inspect(self):
        output_dir = self.root / "diagnostics" / "image-inspect"
        image_digest = next(
            digest for digest in diagnostics.ALLOWLIST if digest.startswith("confluentinc/cp-kafka@")
        )

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
                payload = [{
                    "Name": "/kafka",
                    "Config": {"Image": "confluentinc/cp-kafka@sha256:" + "a" * 64},
                    "Image": "sha256:image-id",
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["image", "inspect", "sha256:image-id"]:
                payload = [{"Id": "sha256:image-id", "RepoDigests": [image_digest]}]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["logs", "--tail", "200"]:
                return subprocess.CompletedProcess(command, 0, "", "")
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "image-inspect-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 0)
        self.assertEqual(stderr, "")
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(manifest["containers"][0]["image_digest"], image_digest)

    def test_allowlisted_digest_rejects_image_id_mismatch(self):
        output_dir = self.root / "diagnostics" / "image-id-mismatch"
        image_digest = next(
            digest for digest in diagnostics.ALLOWLIST if digest.startswith("confluentinc/cp-kafka@")
        )

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
                payload = [{
                    "Name": "/kafka",
                    "Config": {"Image": "confluentinc/cp-kafka:mutable"},
                    "Image": "sha256:running-image",
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["image", "inspect", "sha256:running-image"]:
                payload = [{"Id": "sha256:current-image", "RepoDigests": [image_digest]}]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["logs", "--tail", "200"]:
                return subprocess.CompletedProcess(command, 0, "", "")
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "image-id-mismatch-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)
        self.assertIn("image-id-mismatch-task", stderr)

    def test_missing_running_image_id_rejects_mutable_config_image(self):
        output_dir = self.root / "diagnostics" / "missing-image-id"

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
                payload = [{
                    "Name": "/kafka",
                    "Config": {"Image": "confluentinc/cp-kafka:mutable"},
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:3] == ["image", "inspect"]:
                self.fail("mutable Config.Image must not be used without a running image ID")
            if command[1:4] == ["logs", "--tail", "200"]:
                return subprocess.CompletedProcess(command, 0, "", "")
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "missing-image-id-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)
        self.assertIn("missing-image-id-task", stderr)

    def test_image_outside_allowlist_fails_without_leaking_docker_output(self):
        output_dir = self.root / "diagnostics" / "rejected"

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
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
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)
        self.assertIn("rejected-task", stderr)
        self.assertNotIn("private-secret", stderr)

    def test_docker_log_failure_is_fail_closed_without_leaking_output(self):
        output_dir = self.root / "diagnostics" / "log-failure"
        image_digest = next(
            digest for digest in diagnostics.ALLOWLIST if digest.startswith("confluentinc/cp-kafka@")
        )

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
                payload = [{
                    "Name": "/kafka",
                    "Config": {"Image": "confluentinc/cp-kafka:local"},
                    "Image": "sha256:image-id",
                    "RepoDigests": [image_digest],
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["logs", "--tail", "200"]:
                return subprocess.CompletedProcess(command, 1, "token=secret", "private-docker-error")
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "log-failure-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)
        self.assertIn("log-failure-task", stderr)
        self.assertNotIn("private-docker-error", stderr)
        self.assertNotIn("secret", stderr)

    def test_docker_log_truncation_is_preserved_in_manifest(self):
        output_dir = self.root / "diagnostics" / "log-truncated"
        image_digest = next(
            digest for digest in diagnostics.ALLOWLIST if digest.startswith("confluentinc/cp-kafka@")
        )

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
                payload = [{
                    "Name": "/kafka",
                    "Config": {"Image": "confluentinc/cp-kafka:local"},
                    "Image": "sha256:image-id",
                    "RepoDigests": [image_digest],
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["logs", "--tail", "200"]:
                result = subprocess.CompletedProcess(command, 0, "token=secret", "")
                result.truncated = True
                return result
            self.fail(f"unexpected docker command: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "log-truncated-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 0)
        self.assertEqual(stderr, "")
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertTrue(manifest["truncated"])
        self.assertNotIn("secret", (output_dir / f"{CONTAINER_ID}.log").read_text(encoding="utf-8"))

    def test_docker_logs_are_bounded_before_decoding(self):
        class FakeProcess:
            def __init__(self):
                self.stdout = io.BytesIO(b"0123456789")
                self.returncode = 0
                self.killed = False

            def kill(self):
                self.killed = True

            def wait(self):
                return self.returncode

        process = FakeProcess()
        with patch.object(diagnostics.subprocess, "Popen", return_value=process):
            result = diagnostics.run_docker_logs("bounded-task", CONTAINER_ID, 4)

        self.assertEqual(result.returncode, 0)
        self.assertEqual(result.stdout, "01234")
        self.assertTrue(result.truncated)
        self.assertTrue(process.killed)

    def test_local_image_id_without_allowlisted_repo_digest_fails(self):
        output_dir = self.root / "diagnostics" / "local-id"

        def docker_run(command, **_kwargs):
            if command[1:3] == ["inspect", CONTAINER_ID]:
                payload = [{
                    "Name": "/unknown",
                    "Config": {"Image": "private/image:local"},
                    "Image": "sha256:image-id",
                }]
                return subprocess.CompletedProcess(command, 0, json.dumps(payload), "")
            if command[1:4] == ["image", "inspect", "sha256:image-id"]:
                return subprocess.CompletedProcess(command, 0, json.dumps([{"Id": "sha256:image-id"}]), "")
            self.fail(f"unexpected docker command: {command}")

        result, _ = self.run_main(
            "--task-name",
            "local-id-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            CONTAINER_ID,
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)

    def test_bounded_bytes_keeps_utf8_valid(self):
        data, truncated = diagnostics.bounded_bytes("가나다", 4)

        self.assertTrue(truncated)
        self.assertEqual(data.decode("utf-8"), "가")
        self.assertLessEqual(len(data), 4)

    def test_report_sanitization_preserves_relative_paths(self):
        test_results = self.root / "examples" / "coroutines-demo" / "build" / "test-results" / "test"
        html_reports = self.root / "examples" / "coroutines-demo" / "build" / "reports" / "tests" / "test"
        test_results.mkdir(parents=True)
        html_reports.mkdir(parents=True)
        (test_results / "TEST-example.xml").write_text(
            '<testsuite><system-out>token="xml-secret"</system-out></testsuite>', encoding="utf-8"
        )
        (html_reports / "index.html").write_text(
            '<html><body>https://user:pass@example.test'
            '<pre id="root-0-test-stdout-example">verbose-output</pre></body></html>',
            encoding="utf-8",
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
        self.assertNotIn("verbose-output", content)
        self.assertIn("[REDACTED]", content)
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertEqual(len(manifest["sanitized_reports"]), 2)

    def test_report_standard_output_is_compacted_before_file_cap(self):
        test_results = self.root / "examples" / "virtualthreads-demo" / "build" / "test-results" / "test"
        html_reports = self.root / "examples" / "virtualthreads-demo" / "build" / "reports" / "tests" / "test"
        test_results.mkdir(parents=True)
        html_reports.mkdir(parents=True)
        verbose_output = "debug output\n" * 300_000
        (test_results / "TEST-verbose.xml").write_text(
            f"<testsuite><system-out>{verbose_output}</system-out></testsuite>", encoding="utf-8"
        )
        (html_reports / "verbose.html").write_text(
            f'<html><pre id="root-0-test-stdout-verbose">{verbose_output}</pre></html>',
            encoding="utf-8",
        )
        output_dir = self.root / "examples" / "build" / "testcontainers-diagnostics" / "verbose"
        destination = self.root / "examples" / "build" / "sanitized-test-reports"

        result, stderr = self.run_main(
            "--task-name",
            "verbose-report-task",
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
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertFalse(manifest["report_truncated"])
        self.assertEqual(len(manifest["sanitized_reports"]), 2)
        self.assertTrue(all(item["bytes"] < 1_000 for item in manifest["sanitized_reports"]))
        content = "\n".join(path.read_text(encoding="utf-8") for path in destination.rglob("*") if path.is_file())
        self.assertNotIn("debug output", content)
        self.assertEqual(content.count("[REDACTED]"), 2)

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

    def test_report_byte_cap_is_fail_closed(self):
        reports = self.root / "examples" / "coroutines-demo" / "build" / "test-results" / "test"
        reports.mkdir(parents=True)
        (reports / "TEST-large.xml").write_text("<testsuite>" + ("x" * 128) + "</testsuite>", encoding="utf-8")
        output_dir = self.root / "examples" / "build" / "testcontainers-diagnostics" / "byte-cap"
        destination = self.root / "examples" / "build" / "sanitized-test-reports"

        result, _ = self.run_main(
            "--task-name",
            "byte-cap-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--sanitized-report-dir",
            destination,
            "--report-path",
            reports,
            "--max-report-total-bytes",
            "16",
        )

        self.assertEqual(result, 1)
        manifest = json.loads((output_dir / "manifest.json").read_text(encoding="utf-8"))
        self.assertTrue(manifest["report_truncated"])
        copied = destination / "examples" / "coroutines-demo" / "build" / "test-results" / "test" / "TEST-large.xml"
        self.assertLessEqual(copied.stat().st_size, 16)

    def test_nested_and_quoted_workflow_action_references_are_checked(self):
        upload_sha = "abcdef0123456789abcdef0123456789abcdef01"
        setup_sha = "1234567890abcdef1234567890abcdef12345678"
        self.workflow.write_text(
            f'''\
steps:
  - "uses": "actions/upload-artifact@{upload_sha}"
  - {{uses: actions/setup-java@{setup_sha}}}
''',
            encoding="utf-8",
        )

        refs = diagnostics.workflow_action_refs(self.workflow)

        self.assertEqual(
            refs,
            [
                f"actions/upload-artifact@{upload_sha}",
                f"actions/setup-java@{setup_sha}",
            ],
        )

    def test_invalid_container_id_is_rejected_before_docker(self):
        output_dir = self.root / "diagnostics" / "invalid-id"

        def docker_run(command, **_kwargs):
            self.fail(f"docker must not run for invalid id: {command}")

        result, stderr = self.run_main(
            "--task-name",
            "invalid-id-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--container-id",
            "../outside",
            docker_run=docker_run,
        )

        self.assertEqual(result, 1)
        self.assertIn("invalid-id-task", stderr)

    def test_symlink_report_root_is_rejected(self):
        reports = self.root / "real-reports"
        reports.mkdir()
        (reports / "TEST-example.xml").write_text("<testsuite/>", encoding="utf-8")
        symlink = self.root / "examples" / "coroutines-demo" / "build" / "test-results" / "test"
        symlink.parent.mkdir(parents=True)
        symlink.symlink_to(reports, target_is_directory=True)
        output_dir = self.root / "examples" / "build" / "testcontainers-diagnostics" / "symlink"
        destination = self.root / "examples" / "build" / "sanitized-test-reports"

        result, stderr = self.run_main(
            "--task-name",
            "symlink-task",
            "--output-dir",
            output_dir,
            "--workflow-file",
            self.workflow,
            "--sanitized-report-dir",
            destination,
            "--report-path",
            symlink,
        )

        self.assertEqual(result, 1)
        self.assertIn("symlink-task", stderr)

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
