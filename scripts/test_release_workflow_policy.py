#!/usr/bin/env python3

import json
import re
import shlex
import unittest
from pathlib import Path
from typing import Optional

from scripts.validate_nightly_matrix import (
    REQUIRED_JOB_NAMES,
    expected_matrix_names,
    matrix_contract_errors,
    validation_errors,
)

REPOSITORY = Path(__file__).resolve().parents[1]
WORKFLOWS = REPOSITORY / ".github" / "workflows"

GITHUB_RELEASE = re.compile(
    r"(?:gh\s+release(?:\s|$)|softprops/action-gh-release|actions/create-release|"
    r"gh\s+api[^\n]*/releases(?:\s|$))",
    re.IGNORECASE | re.MULTILINE,
)
ISSUE_RELEASE_MACHINERY = re.compile(
    r"(?:issue[-_ ]?754[^\n]*(?:release|tag|settings|candidate)|"
    r"(?:release|tag|settings|candidate)[^\n]*issue[-_ ]?754|"
    r"release[-_ ]hold|create-github-app-token|RELEASE_(?:TAG|SETTINGS)_|"
    r"tag[-_ ]immutability)",
    re.IGNORECASE,
)
YAML_BLOCK_SCALAR = re.compile(
    r"^(?P<indent> *)(?:- )?[^#\n][^:\n]*:\s*(?P<style>[|>][+-]?)"
    r"(?:\s+#.*)?$"
)
YAML_EXECUTION_GUARD = re.compile(
    r"^(?:\"(?P<double>if|continue-on-error)\"|"
    r"'(?P<single>if|continue-on-error)'|"
    r"(?P<plain>if|continue-on-error)):\s*(?P<value>.*?)\s*$"
)
SNAPSHOT_PUBLISH_JOB_IF = (
    "${{ needs.validate-full-nightly.outputs.publish_eligible == 'true' }}"
)
PUBLICATION_VALIDATION_COMMANDS = (
    "      - name: Validate publication metadata\n"
    "        run: |\n"
    "          ruby scripts/publication/publication_pom_audit_test.rb\n"
    "          ruby scripts/publication/publication_inventory_audit_test.rb\n"
    "          ruby scripts/publication/publication_pom_integration_test.rb\n"
    "          ruby scripts/publication/publication_module_metadata_audit_test.rb\n"
    "          ./gradlew generatePomFileForBluetape4kPublication "
    "checkPomFileForBluetape4kPublication "
    "generateMetadataFileForBluetape4kPublication \\\n"
    "            -PsnapshotVersion=-SNAPSHOT \\\n"
    "            --no-daemon --no-configuration-cache --no-build-cache\n"
    "          ruby scripts/publication/validate_poms.rb\n"
    "          ruby scripts/publication/validate_module_metadata.rb\n",
    "      - name: Validate publication metadata\n"
    "        run: |\n"
    "          ./gradlew generatePomFileForBluetape4kPublication "
    "checkPomFileForBluetape4kPublication "
    "generateMetadataFileForBluetape4kPublication \\\n"
    "            --no-daemon --no-configuration-cache --no-build-cache\n"
    "          ruby scripts/publication/validate_poms.rb\n"
    "          ruby scripts/publication/validate_module_metadata.rb\n",
    "      - name: Validate publication metadata\n"
    "        run: |\n"
    "          ./gradlew generatePomFileForBluetape4kPublication "
    "checkPomFileForBluetape4kPublication "
    "generateMetadataFileForBluetape4kPublication \\\n"
    "            -PsnapshotVersion=-SNAPSHOT \\\n"
    "            --no-daemon --no-configuration-cache --no-build-cache\n"
    "          ruby scripts/publication/validate_poms.rb\n"
    "          ruby scripts/publication/validate_module_metadata.rb\n",
)
RELEASE_PUBLICATION_COMMAND = (
    "      - name: Publish to Central Portal\n"
    "        run: >-\n"
    "          ./gradlew nmcpPublishAggregationToCentralPortal\n"
    "          --no-daemon\n"
    "          --no-configuration-cache\n"
    "          --no-build-cache\n"
    "          --stacktrace\n"
)
SNAPSHOT_PUBLICATION_COMMAND = (
    "      - name: Publish SNAPSHOT\n"
    "        run: >-\n"
    "          ./gradlew nmcpPublishAggregationToCentralPortalSnapshots\n"
    "          -PsnapshotVersion=-SNAPSHOT\n"
    "          --no-daemon\n"
    "          --no-configuration-cache\n"
    "          --no-build-cache\n"
)


def expected_run_contract(step: str) -> tuple[str, tuple[str, ...]]:
    lines = step.splitlines()
    style = lines[1].split("run:", 1)[1].strip()
    return style, tuple(line[10:] for line in lines[2:])


PUBLICATION_VALIDATION_RUNS = tuple(
    expected_run_contract(command) for command in PUBLICATION_VALIDATION_COMMANDS
)
RELEASE_PUBLICATION_RUN = expected_run_contract(RELEASE_PUBLICATION_COMMAND)
SNAPSHOT_PUBLICATION_RUN = expected_run_contract(SNAPSHOT_PUBLICATION_COMMAND)


def job_ids(workflow: str) -> set[str]:
    jobs = workflow.split("\njobs:\n", 1)
    if len(jobs) != 2:
        return set()
    return set(re.findall(r"^  ([a-z0-9_-]+):\s*$", jobs[1], re.MULTILINE))


def line_indent(line: str) -> int:
    return len(line) - len(line.lstrip())


def execution_guard_field(
    line: str, expected_indent: int
) -> Optional[tuple[str, str]]:
    if line_indent(line) != expected_indent:
        return None
    match = YAML_EXECUTION_GUARD.fullmatch(line.strip())
    if match is None:
        return None
    key = next(
        value
        for value in (
            match.group("double"),
            match.group("single"),
            match.group("plain"),
        )
        if value is not None
    )
    return key, match.group("value")


def yaml_block_end(lines: list[str], start: int, header_indent: int) -> int:
    index = start + 1
    while index < len(lines):
        line = lines[index]
        if line.strip() and line_indent(line) <= header_indent:
            break
        index += 1
    return index


def yaml_run_contract(
    lines: list[str], run_index: int, run_indent: int, style: str
) -> tuple[tuple[str, tuple[str, ...]], int]:
    end = yaml_block_end(lines, run_index, run_indent)
    content = []
    for line in lines[run_index + 1 : end]:
        if not line.strip():
            content.append("")
        elif line_indent(line) < run_indent + 2:
            content.append("<invalid-indentation>")
        else:
            content.append(line[run_indent + 2 :])
    while content and not content[-1]:
        content.pop()
    return (style, tuple(content)), end


def workflow_step_runs(
    workflow: str,
    expected_job: Optional[str] = None,
    expected_name: Optional[str] = None,
) -> list[tuple[Optional[tuple[str, tuple[str, ...]]], bool]]:
    lines = workflow.splitlines()
    runs = []
    in_jobs = False
    current_job = None
    index = 0

    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        indent = line_indent(line)
        scalar = YAML_BLOCK_SCALAR.match(line)

        if scalar is not None:
            index = yaml_block_end(lines, index, len(scalar.group("indent")))
            continue
        if not in_jobs:
            if indent == 0 and stripped == "jobs:":
                in_jobs = True
            index += 1
            continue
        if stripped and indent == 0:
            break
        job_match = re.match(r"^ {2}(?P<job>[a-z0-9_-]+):\s*$", line)
        if job_match is not None:
            current_job = job_match.group("job")
            index += 1
            continue
        if indent != 4 or stripped != "steps:":
            index += 1
            continue

        index += 1
        while index < len(lines):
            step_line = lines[index]
            step_stripped = step_line.strip()
            step_indent = line_indent(step_line)
            if step_stripped and step_indent <= 4:
                break
            if step_indent != 6 or not step_stripped.startswith("- "):
                scalar = YAML_BLOCK_SCALAR.match(step_line)
                if scalar is not None:
                    index = yaml_block_end(
                        lines, index, len(scalar.group("indent"))
                    )
                else:
                    index += 1
                continue

            name_match = re.match(r"^ {6}- name:\s*(?P<name>.+?)\s*$", step_line)
            step_name = name_match.group("name") if name_match else None
            run = None
            guarded = False
            index += 1
            while index < len(lines):
                field_line = lines[index]
                field_stripped = field_line.strip()
                field_indent = line_indent(field_line)
                if field_stripped and field_indent <= 4:
                    break
                if field_indent == 6 and field_stripped.startswith("- "):
                    break

                run_match = re.match(
                    r"^ {8}run:\s*(?P<header>.*)$", field_line
                )
                if run_match is not None:
                    header = run_match.group("header").strip()
                    if re.fullmatch(r"[|>][+-]?", header):
                        run, index = yaml_run_contract(
                            lines, index, field_indent, header
                        )
                    else:
                        run = ("inline", (header,))
                        index += 1
                    continue

                if execution_guard_field(field_line, 8) is not None:
                    guarded = True
                    index += 1
                    continue

                scalar = YAML_BLOCK_SCALAR.match(field_line)
                if scalar is not None:
                    index = yaml_block_end(
                        lines, index, len(scalar.group("indent"))
                    )
                else:
                    index += 1

            if (expected_job is None or current_job == expected_job) and (
                expected_name is None or step_name == expected_name
            ):
                runs.append((run, guarded))

    return runs


def publication_job_guard_errors(
    workflow: str, expected_job: str, expected_if: Optional[str] = None
) -> list[str]:
    lines = workflow.splitlines()
    guards = []
    in_jobs = False
    current_job = None
    index = 0

    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        indent = line_indent(line)
        scalar = YAML_BLOCK_SCALAR.match(line)
        if scalar is not None:
            index = yaml_block_end(lines, index, len(scalar.group("indent")))
            continue
        if not in_jobs:
            if indent == 0 and stripped == "jobs:":
                in_jobs = True
            index += 1
            continue
        if stripped and indent == 0:
            break
        job_match = re.match(r"^ {2}(?P<job>[a-z0-9_-]+):\s*$", line)
        if job_match is not None:
            current_job = job_match.group("job")
            index += 1
            continue
        if current_job == expected_job:
            guard = execution_guard_field(line, 4)
            if guard is not None:
                guards.append(guard)
        index += 1

    expected_guards = [] if expected_if is None else [("if", expected_if)]
    if guards != expected_guards:
        return ["publication job must use the exact execution guard contract"]
    return []


def _shell_tokens(line: str) -> list[str]:
    lexer = shlex.shlex(line, posix=True, punctuation_chars=";&|<>")
    lexer.whitespace_split = True
    lexer.commenters = "#"
    return list(lexer)


def _heredoc_delimiter(tokens: list[str]) -> Optional[str]:
    for index, token in enumerate(tokens):
        if token == "<<" and index + 1 < len(tokens):
            return tokens[index + 1].lstrip("-")
        if token.startswith("<<") and len(token) > 2:
            return token[2:].lstrip("-").strip("'\"")
    return None


def publication_task_invocation_count(workflow: str, task: str) -> int:
    count = 0
    separators = {";", "&&", "||", "&", "|"}

    for run, _ in workflow_step_runs(workflow):
        if run is None:
            continue
        _, lines = run
        heredoc = None
        for raw_line in lines:
            stripped = raw_line.rstrip()
            if heredoc is not None:
                if stripped.strip() == heredoc:
                    heredoc = None
                continue
            if stripped.endswith("\\"):
                stripped = stripped[:-1]
            try:
                tokens = _shell_tokens(stripped)
            except ValueError:
                continue
            heredoc = _heredoc_delimiter(tokens)
            command = []
            for token in [*tokens, None]:
                if token is None or token in separators:
                    if command:
                        executable_index = 0
                        while executable_index < len(command) and re.fullmatch(
                            r"[A-Za-z_][A-Za-z0-9_]*=.*", command[executable_index]
                        ):
                            executable_index += 1
                        if (
                            executable_index < len(command)
                            and command[executable_index] == "./gradlew"
                            and task in command[executable_index + 1 :]
                        ):
                            count += 1
                    command = []
                else:
                    command.append(token)
    return count


def publication_validation_errors(
    workflow: str, expected_job: str, expected_job_if: Optional[str] = None
) -> list[str]:
    errors = publication_job_guard_errors(
        workflow, expected_job, expected_job_if
    )
    runs = workflow_step_runs(
        workflow, expected_job, "Validate publication metadata"
    )
    if not runs:
        errors.append("workflow must contain the publication metadata validation step")
        return errors
    if (
        len(runs) != 1
        or runs[0][0] not in PUBLICATION_VALIDATION_RUNS
        or runs[0][1]
    ):
        errors.append("publication metadata validation must use the exact Gradle command")
    return errors


def release_policy_errors(workflow: str) -> list[str]:
    errors = []
    release_runs = workflow_step_runs(
        workflow, "publish", "Publish to Central Portal"
    )
    release_command_valid = release_runs == [(RELEASE_PUBLICATION_RUN, False)]
    release_task_count = publication_task_invocation_count(
        workflow, "nmcpPublishAggregationToCentralPortal"
    )
    if not release_command_valid:
        errors.append("release workflow must invoke the exact Maven release task")
    if release_task_count != 1:
        errors.append(
            "release publication task must have exactly one executable invocation"
        )
    if GITHUB_RELEASE.search(workflow):
        errors.append("release workflow must not create a GitHub Release")
    if "contents: write" in workflow:
        errors.append("release workflow must not request write access to repository contents")
    if ISSUE_RELEASE_MACHINERY.search(workflow):
        errors.append("release workflow must not contain issue-specific release machinery")
    expected_jobs = {
        "resolve-version",
        "testcontainers-manifest-contract",
        "testcontainers-image-gate",
        "testcontainers-ignite2-arm64-image-gate",
        "publish",
    }
    if job_ids(workflow) != expected_jobs:
        errors.append(
            "release workflow must contain resolve-version, testcontainers-manifest-contract, "
            "testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate, and publish jobs"
        )
    if "needs: [resolve-version, testcontainers-manifest-contract]" not in workflow:
        errors.append("full Testcontainers image gate must wait for the manifest contract")
    if (
        "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate]"
        not in workflow
    ):
        errors.append("publish must depend on the manifest contract and full image gate")
    if "--scope full" not in workflow or not (
        "coverage=48/48" in workflow or "expected_coverage=\"48/48\"" in workflow
    ):
        errors.append("release workflow must verify the full 48/48 release evidence gate")
    arm_contract = (
        "testcontainers-ignite2-arm64-image-gate" in workflow
        and "--scope family" in workflow
        and "--family-id ignite2" in workflow
        and "--platform-id arm64" in workflow
        and "expected_coverage=\"1/1\"" in workflow
    )
    if not arm_contract:
        errors.append("release workflow must verify the exact Ignite2 arm64 1/1 image gate")
    errors.extend(publication_validation_errors(workflow, "publish"))
    if not release_command_valid:
        errors.append("release publication must disable the configuration cache")
    return errors


def snapshot_policy_errors(workflow: str) -> list[str]:
    errors = []
    snapshot_runs = workflow_step_runs(workflow, "publish", "Publish SNAPSHOT")
    snapshot_command_valid = snapshot_runs == [(SNAPSHOT_PUBLICATION_RUN, False)]
    snapshot_task_count = publication_task_invocation_count(
        workflow, "nmcpPublishAggregationToCentralPortalSnapshots"
    )
    if not snapshot_command_valid:
        errors.append("snapshot workflow must invoke the exact Maven snapshot task")
    if snapshot_task_count != 1:
        errors.append(
            "snapshot publication task must have exactly one executable invocation"
        )
    if GITHUB_RELEASE.search(workflow) or ISSUE_RELEASE_MACHINERY.search(workflow):
        errors.append("snapshot workflow must not contain release or issue-specific machinery")
    if "contents: write" in workflow:
        errors.append("snapshot workflow must not request write access to repository contents")
    if job_ids(workflow) != {"validate-full-nightly", "publish"}:
        errors.append("snapshot workflow must contain validation and publish jobs")
    errors.extend(
        publication_validation_errors(
            workflow, "publish", SNAPSHOT_PUBLISH_JOB_IF
        )
    )
    if not snapshot_command_valid:
        errors.append("snapshot publication must disable the configuration cache")
    return errors


def runtime_policy_errors() -> list[str]:
    errors = []
    candidates = list(WORKFLOWS.glob("*.yml")) + list(WORKFLOWS.glob("*.yaml"))
    candidates.extend(
        path
        for path in (REPOSITORY / "scripts").rglob("*")
        if path.is_file() and path.suffix in {".bash", ".py", ".rb", ".sh"}
    )
    for path in candidates:
        if path.resolve() == Path(__file__).resolve():
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if GITHUB_RELEASE.search(text) or ISSUE_RELEASE_MACHINERY.search(text):
            errors.append(path.relative_to(REPOSITORY).as_posix())
    return errors


class ReleaseWorkflowPolicyTest(unittest.TestCase):

    @staticmethod
    def _nightly_matrix_contract() -> dict:
        return json.loads(
            (REPOSITORY / "scripts" / "nightly_matrix_contract.json").read_text(
                encoding="utf-8"
            )
        )

    def _successful_matrix_jobs(self) -> list[dict[str, str]]:
        expected_names, _ = expected_matrix_names(self._nightly_matrix_contract())
        return [
            {"name": name, "conclusion": "success"}
            for name in sorted(expected_names)
        ]

    def test_semantic_checker_rejects_snapshot_task_and_release_action(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "nmcpPublishAggregationToCentralPortal\n",
            "nmcpPublishAggregationToCentralPortalSnapshots\n",
        ) + "\n      - uses: softprops/action-gh-release@v2\n"

        errors = release_policy_errors(mutated)

        self.assertIn("release workflow must invoke the exact Maven release task", errors)
        self.assertIn("release workflow must not create a GitHub Release", errors)

    def test_publication_validation_contract_is_shared_by_ci_and_publish_workflows(self) -> None:
        workflow_jobs = {
            "ci.yml": ("build", None),
            "release.yml": ("publish", None),
            "publish-snapshot.yml": ("publish", SNAPSHOT_PUBLISH_JOB_IF),
        }
        for workflow_name, (expected_job, expected_if) in workflow_jobs.items():
            with self.subTest(workflow=workflow_name):
                workflow = (WORKFLOWS / workflow_name).read_text(encoding="utf-8")
                self.assertEqual(
                    [],
                    publication_validation_errors(
                        workflow, expected_job, expected_if
                    ),
                )

    def test_publication_validation_rejects_required_task_outside_gradle_invocation(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "          ./gradlew generatePomFileForBluetape4kPublication "
            "checkPomFileForBluetape4kPublication "
            "generateMetadataFileForBluetape4kPublication \\\n",
            "          echo checkPomFileForBluetape4kPublication\n"
            "          ./gradlew generatePomFileForBluetape4kPublication "
            "generateMetadataFileForBluetape4kPublication \\\n",
        )

        self.assertIn(
            "publication metadata validation must use the exact Gradle command",
            publication_validation_errors(mutated, "publish"),
        )

    def test_publication_commands_reject_configuration_cache(self) -> None:
        release = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        snapshot = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        release_without_policy = release.replace(
            "          ./gradlew nmcpPublishAggregationToCentralPortal\n"
            "          --no-daemon\n"
            "          --no-configuration-cache\n",
            "          ./gradlew nmcpPublishAggregationToCentralPortal\n"
            "          --no-daemon\n",
        )
        snapshot_without_policy = snapshot.replace(
            "          ./gradlew nmcpPublishAggregationToCentralPortalSnapshots\n"
            "          -PsnapshotVersion=-SNAPSHOT\n"
            "          --no-daemon\n"
            "          --no-configuration-cache\n",
            "          ./gradlew nmcpPublishAggregationToCentralPortalSnapshots\n"
            "          -PsnapshotVersion=-SNAPSHOT\n"
            "          --no-daemon\n",
        )

        self.assertIn(
            "release publication must disable the configuration cache",
            release_policy_errors(release_without_policy),
        )
        self.assertIn(
            "snapshot publication must disable the configuration cache",
            snapshot_policy_errors(snapshot_without_policy),
        )

    def test_release_publication_rejects_task_name_embedded_in_an_argument(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "          ./gradlew nmcpPublishAggregationToCentralPortal\n",
            "          ./gradlew help "
            "-Pnote=nmcpPublishAggregationToCentralPortal\n",
        )

        self.assertIn(
            "release workflow must invoke the exact Maven release task",
            release_policy_errors(mutated),
        )

    def test_release_publication_rejects_task_name_used_as_option_value(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "          ./gradlew nmcpPublishAggregationToCentralPortal\n",
            "          ./gradlew --init-script "
            "nmcpPublishAggregationToCentralPortal help\n",
        )

        self.assertIn(
            "release workflow must invoke the exact Maven release task",
            release_policy_errors(mutated),
        )

    def test_release_publication_rejects_unsupported_option_value_pair(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "          ./gradlew nmcpPublishAggregationToCentralPortal\n"
            "          --no-daemon\n"
            "          --no-configuration-cache\n",
            "          ./gradlew nmcpPublishAggregationToCentralPortal\n"
            "          --no-daemon\n"
            "          --init-script --no-configuration-cache\n",
        )

        self.assertIn(
            "release workflow must invoke the exact Maven release task",
            release_policy_errors(mutated),
        )

    def test_release_publication_rejects_task_name_after_shell_operator(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")

        for operator in ("&&", ";"):
            with self.subTest(operator=operator):
                mutated = workflow.replace(
                    "          ./gradlew nmcpPublishAggregationToCentralPortal\n",
                    "          ./gradlew help "
                    f"{operator} echo nmcpPublishAggregationToCentralPortal\n",
                )

                self.assertIn(
                    "release workflow must invoke the exact Maven release task",
                    release_policy_errors(mutated),
                )

    def test_publication_rejects_gradle_command_inside_heredoc(self) -> None:
        cases = (
            (
                "release.yml",
                "          ./gradlew nmcpPublishAggregationToCentralPortal\n"
                "          --no-daemon\n"
                "          --no-configuration-cache\n"
                "          --no-build-cache\n"
                "          --stacktrace\n",
                "./gradlew nmcpPublishAggregationToCentralPortal "
                "--no-daemon --no-configuration-cache --no-build-cache --stacktrace",
                release_policy_errors,
                "release workflow must invoke the exact Maven release task",
            ),
            (
                "publish-snapshot.yml",
                "          ./gradlew nmcpPublishAggregationToCentralPortalSnapshots\n"
                "          -PsnapshotVersion=-SNAPSHOT\n"
                "          --no-daemon\n"
                "          --no-configuration-cache\n"
                "          --no-build-cache\n",
                "./gradlew nmcpPublishAggregationToCentralPortalSnapshots "
                "-PsnapshotVersion=-SNAPSHOT --no-daemon "
                "--no-configuration-cache --no-build-cache",
                snapshot_policy_errors,
                "snapshot workflow must invoke the exact Maven snapshot task",
            ),
        )

        delimiters = (
            ("'PUBLICATION_COMMAND'", "PUBLICATION_COMMAND"),
            ("123", "123"),
            ("$EOF", "$EOF"),
            ("\\EOF", "EOF"),
            ("PUBLICATION-COMMAND", "PUBLICATION-COMMAND"),
        )
        for workflow_name, command, fake_command, checker, expected_error in cases:
            for opener, closer in delimiters:
                with self.subTest(workflow=workflow_name, delimiter=opener):
                    workflow = (WORKFLOWS / workflow_name).read_text(encoding="utf-8")
                    mutated = workflow.replace(
                        "        run: >-\n" + command,
                        "        run: |\n"
                        f"          cat <<{opener}\n"
                        f"          {fake_command}\n"
                        f"          {closer}\n",
                    )

                    self.assertIn(expected_error, checker(mutated))

    def test_release_publication_rejects_exact_contract_in_yaml_scalar(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            RELEASE_PUBLICATION_COMMAND,
            "      - name: Publish to Central Portal\n"
            "        run: echo no-publication\n",
        )
        mutated += "\npublication_fixture: |\n" + RELEASE_PUBLICATION_COMMAND

        self.assertIn(
            "release workflow must invoke the exact Maven release task",
            release_policy_errors(mutated),
        )

    def test_validation_rejects_exact_contract_in_yaml_scalar(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        expected_command = PUBLICATION_VALIDATION_COMMANDS[1]
        mutated = workflow.replace(
            expected_command,
            "      - name: Validate publication metadata\n"
            "        run: echo no-validation\n",
        )
        mutated += "\npublication_fixture: |\n" + expected_command

        self.assertIn(
            "publication metadata validation must use the exact Gradle command",
            publication_validation_errors(mutated, "publish"),
        )

    def test_release_publication_rejects_exact_step_in_another_job(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(RELEASE_PUBLICATION_COMMAND, "")
        mutated = mutated.replace(
            "    steps:\n",
            "    steps:\n" + RELEASE_PUBLICATION_COMMAND,
            1,
        )

        self.assertIn(
            "release workflow must invoke the exact Maven release task",
            release_policy_errors(mutated),
        )

    def test_publication_steps_reject_execution_guards(self) -> None:
        cases = (
            (
                "release.yml",
                RELEASE_PUBLICATION_COMMAND,
                release_policy_errors,
                "release workflow must invoke the exact Maven release task",
            ),
            (
                "publish-snapshot.yml",
                SNAPSHOT_PUBLICATION_COMMAND,
                snapshot_policy_errors,
                "snapshot workflow must invoke the exact Maven snapshot task",
            ),
        )

        for workflow_name, command, checker, expected_error in cases:
            for guard in (
                "if: false",
                "continue-on-error: true",
                '"if": false',
                '"continue-on-error": true',
            ):
                with self.subTest(workflow=workflow_name, guard=guard):
                    workflow = (WORKFLOWS / workflow_name).read_text(encoding="utf-8")
                    mutated = workflow.replace(
                        command,
                        command.replace(
                            "        run:", f"        {guard}\n        run:", 1
                        ),
                    )

                    self.assertIn(expected_error, checker(mutated))

    def test_publication_jobs_reject_execution_guard_bypasses(self) -> None:
        release = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        snapshot = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")
        cases = (
            (
                "release-if",
                release.replace("  publish:\n", "  publish:\n    if: false\n", 1),
                release_policy_errors,
            ),
            (
                "release-continue-on-error",
                release.replace(
                    "  publish:\n",
                    "  publish:\n    continue-on-error: true\n",
                    1,
                ),
                release_policy_errors,
            ),
            (
                "snapshot-if",
                snapshot.replace(
                    "    if: ${{ needs.validate-full-nightly.outputs.publish_eligible == 'true' }}\n",
                    "    if: false\n",
                    1,
                ),
                snapshot_policy_errors,
            ),
            (
                "snapshot-continue-on-error",
                snapshot.replace(
                    "    if: ${{ needs.validate-full-nightly.outputs.publish_eligible == 'true' }}\n",
                    "    if: ${{ needs.validate-full-nightly.outputs.publish_eligible == 'true' }}\n"
                    "    continue-on-error: true\n",
                    1,
                ),
                snapshot_policy_errors,
            ),
        )

        for name, mutated, checker in cases:
            with self.subTest(case=name):
                self.assertIn(
                    "publication job must use the exact execution guard contract",
                    checker(mutated),
                )

    def test_validation_step_rejects_continue_on_error(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        command = PUBLICATION_VALIDATION_COMMANDS[1]
        mutated = workflow.replace(
            command,
            command.replace(
                "        run:", "        continue-on-error: true\n        run:", 1
            ),
        )

        self.assertIn(
            "publication metadata validation must use the exact Gradle command",
            publication_validation_errors(mutated, "publish"),
        )

    def test_publication_validation_rejects_requirements_after_shell_operator(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "          ./gradlew generatePomFileForBluetape4kPublication "
            "checkPomFileForBluetape4kPublication "
            "generateMetadataFileForBluetape4kPublication \\\n"
            "            --no-daemon --no-configuration-cache --no-build-cache\n",
            "          ./gradlew generatePomFileForBluetape4kPublication "
            "generateMetadataFileForBluetape4kPublication \\\n"
            "            --no-daemon --no-build-cache "
            "&& echo checkPomFileForBluetape4kPublication "
            "--no-configuration-cache\n",
        )

        self.assertIn(
            "publication metadata validation must use the exact Gradle command",
            publication_validation_errors(mutated, "publish"),
        )

    def test_publication_tasks_reject_duplicate_executable_invocation(self) -> None:
        cases = (
            (
                "release.yml",
                "./gradlew nmcpPublishAggregationToCentralPortal "
                "--no-daemon --no-configuration-cache --no-build-cache\n",
                release_policy_errors,
                "release publication task must have exactly one executable invocation",
            ),
            (
                "publish-snapshot.yml",
                "./gradlew nmcpPublishAggregationToCentralPortalSnapshots "
                "-PsnapshotVersion=-SNAPSHOT --no-daemon "
                "--no-configuration-cache --no-build-cache\n",
                snapshot_policy_errors,
                "snapshot publication task must have exactly one executable invocation",
            ),
        )

        for workflow_name, command, checker, expected_error in cases:
            with self.subTest(workflow=workflow_name):
                workflow = (WORKFLOWS / workflow_name).read_text(encoding="utf-8")
                duplicate_step = (
                    "      - name: Duplicate publication invocation\n"
                    "        run: |\n"
                    f"          {command}"
                )
                mutated = workflow.replace(
                    "      - name: Validate publication metadata\n",
                    duplicate_step
                    + "      - name: Validate publication metadata\n",
                    1,
                )

                self.assertIn(expected_error, checker(mutated))

    def test_publication_task_text_in_comments_and_heredocs_is_not_executable(self) -> None:
        cases = (
            ("release.yml", release_policy_errors),
            ("publish-snapshot.yml", snapshot_policy_errors),
        )

        for workflow_name, checker in cases:
            with self.subTest(workflow=workflow_name):
                workflow = (WORKFLOWS / workflow_name).read_text(encoding="utf-8")
                task = (
                    "nmcpPublishAggregationToCentralPortal"
                    if workflow_name == "release.yml"
                    else "nmcpPublishAggregationToCentralPortalSnapshots"
                )
                non_executable_step = (
                    "      - name: Publication text fixture\n"
                    "        run: |\n"
                    f"          # ./gradlew {task}\n"
                    "          cat <<'PUBLICATION_FIXTURE'\n"
                    f"          ./gradlew {task}\n"
                    "          PUBLICATION_FIXTURE\n"
                )
                mutated = workflow.replace(
                    "      - name: Validate publication metadata\n",
                    non_executable_step
                    + "      - name: Validate publication metadata\n",
                    1,
                )

                self.assertEqual([], checker(mutated))

    def test_release_workflow_publishes_only_to_maven_central(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")

        self.assertIn("Publish RELEASE to Maven Central Portal", workflow)
        self.assertEqual([], release_policy_errors(workflow))

    def test_release_workflow_blocks_publish_without_full_image_gate(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "needs: [resolve-version, testcontainers-manifest-contract, testcontainers-image-gate, testcontainers-ignite2-arm64-image-gate]",
            "needs: resolve-version",
        )
        errors = release_policy_errors(mutated)
        self.assertIn("publish must depend on the manifest contract and full image gate", errors)

    def test_release_workflow_blocks_image_gate_without_manifest_contract(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace(
            "needs: [resolve-version, testcontainers-manifest-contract]",
            "needs: resolve-version",
        )
        errors = release_policy_errors(mutated)
        self.assertIn("full Testcontainers image gate must wait for the manifest contract", errors)

    def test_release_workflow_blocks_arm_gate_without_exact_selector(self) -> None:
        workflow = (WORKFLOWS / "release.yml").read_text(encoding="utf-8")
        mutated = workflow.replace("--platform-id arm64", "--platform-id amd64")
        errors = release_policy_errors(mutated)
        self.assertIn("release workflow must verify the exact Ignite2 arm64 1/1 image gate", errors)

    def test_snapshot_workflow_is_not_coupled_to_issue_754_release_state(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("Publish SNAPSHOT to Maven Central", workflow)
        self.assertEqual([], snapshot_policy_errors(workflow))

    def test_snapshot_workflow_requires_full_nightly_validation_before_publish(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("actions: read", workflow)
        self.assertIn("validate-full-nightly:", workflow)
        self.assertIn("needs: validate-full-nightly", workflow)
        self.assertIn(
            "needs.validate-full-nightly.outputs.publish_eligible == 'true'",
            workflow,
        )
        self.assertIn('validation_run_id:', workflow)
        self.assertIn("required: true", workflow)
        self.assertIn("gh api", workflow)
        self.assertIn("actions/runs/${validation_run_id}", workflow)
        self.assertIn("actions/runs/${validation_run_id}/jobs", workflow)
        self.assertIn(
            "contents/scripts/nightly_matrix_contract.json?ref=${validation_head_sha}",
            workflow,
        )
        self.assertIn(
            "contents/scripts/validate_nightly_matrix.py?ref=${validation_head_sha}",
            workflow,
        )
        self.assertIn("Accept: application/vnd.github.raw", workflow)
        self.assertIn("valid head SHA", workflow)
        self.assertIn('python3 "$validator_script"', workflow)
        self.assertIn("publish_eligible=", workflow)
        self.assertNotIn("override_full_validation", workflow)

    def test_nightly_matrix_contract_matches_current_workflow_groups(self) -> None:
        workflow = (WORKFLOWS / "nightly-tests.yml").read_text(encoding="utf-8")
        contract = self._nightly_matrix_contract()
        expected_groups = {
            group
            for groups in contract["matrix_jobs"].values()
            for group in groups
        }
        workflow_groups = set(
            re.findall(r"^\s*-\s+group:\s*([a-z0-9-]+)", workflow, re.MULTILINE)
        )
        workflow_groups.update(
            re.findall(r'"group":\s*"([a-z0-9-]+)"', workflow)
        )
        self.assertEqual(expected_groups, workflow_groups)
        self.assertEqual(29, len(expected_matrix_names(contract)[0]))

    def test_nightly_matrix_contract_accepts_current_expected_set(self) -> None:
        contract = self._nightly_matrix_contract()
        errors = matrix_contract_errors(self._successful_matrix_jobs(), contract)
        self.assertEqual([], errors)

    def test_nightly_matrix_contract_rejects_missing_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = [
            job
            for job in self._successful_matrix_jobs()
            if job["name"] != "Test / Infra (redis)"
        ]
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn("missing matrix job: Test / Infra (redis)", errors)

    def test_nightly_matrix_contract_rejects_additional_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = self._successful_matrix_jobs() + [
            {"name": "Test / Infra (unexpected)", "conclusion": "success"}
        ]
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn("unexpected matrix job: Test / Infra (unexpected)", errors)

    def test_nightly_matrix_contract_rejects_renamed_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = [
            {
                "name": (
                    "Test / Infra (renamed)"
                    if job["name"] == "Test / Infra (redis)"
                    else job["name"]
                ),
                "conclusion": job["conclusion"],
            }
            for job in self._successful_matrix_jobs()
        ]
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn("missing matrix job: Test / Infra (redis)", errors)
        self.assertIn("unexpected matrix job: Test / Infra (renamed)", errors)

    def test_nightly_matrix_contract_rejects_non_success_shard(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = self._successful_matrix_jobs()
        jobs[0]["conclusion"] = "failure"
        errors = matrix_contract_errors(jobs, contract)
        self.assertIn(f"non-success matrix job: {jobs[0]['name']}", errors)

    def test_nightly_matrix_validation_accepts_complete_successful_run(self) -> None:
        contract = self._nightly_matrix_contract()
        jobs = [
            *({"name": name, "conclusion": "success"} for name in REQUIRED_JOB_NAMES),
            *self._successful_matrix_jobs(),
        ]
        head_sha, errors = validation_errors(
            {
                "status": "completed",
                "conclusion": "success",
                "path": ".github/workflows/nightly-tests.yml",
                "head_branch": "develop",
                "head_sha": "a" * 40,
            },
            jobs,
            "a" * 40,
            contract,
        )
        self.assertEqual("a" * 40, head_sha)
        self.assertEqual([], errors)

    def test_snapshot_checkout_uses_validated_nightly_head(self) -> None:
        workflow = (WORKFLOWS / "publish-snapshot.yml").read_text(encoding="utf-8")

        self.assertIn("head_sha", workflow)
        self.assertIn(
            "ref: ${{ needs.validate-full-nightly.outputs.head_sha }}",
            workflow,
        )

    def test_runtime_surfaces_have_no_renamed_release_machinery(self) -> None:
        self.assertEqual([], runtime_policy_errors())

    def test_issue_specific_release_machinery_is_absent(self) -> None:
        forbidden = (
            ".github/release-holds/1.12.0-github-settings.json",
            ".github/release-holds/1.12.0-issue-754.json",
            ".github/workflows/release-generic.yml",
            "scripts/check-release-holds.py",
            "scripts/issue-754-github-settings.py",
            "scripts/test_check_release_holds.py",
            "scripts/test_issue_754_github_settings.py",
        )

        existing = [path for path in forbidden if (REPOSITORY / path).exists()]
        self.assertEqual([], existing)

    def test_serializer_abi_check_is_release_policy_independent(self) -> None:
        script = (REPOSITORY / "scripts" / "check-serializer-buffer-abi.sh").read_text(
            encoding="utf-8"
        )

        self.assertNotIn("check-release-holds.py", script)
        self.assertNotIn("release-hold", script)

    def test_pull_request_ci_runs_this_policy(self) -> None:
        workflow = (WORKFLOWS / "ci.yml").read_text(encoding="utf-8")

        self.assertIn("name: Release Workflow Policy", workflow)
        self.assertIn(
            "python3 -m unittest scripts/test_release_workflow_policy.py -v",
            workflow,
        )


if __name__ == "__main__":
    unittest.main()
