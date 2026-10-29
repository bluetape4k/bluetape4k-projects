"""Validate the catalog-governance and dependency-submission workflow contracts."""

from __future__ import annotations

import ast
import json
import re
import sys
import textwrap
from collections import Counter
from pathlib import Path

FULL_SHA_RE = re.compile(r"^[0-9a-f]{40}$")
CATALOG_TAG_RE = re.compile(r"^catalog/\d{4}-\d{2}-\d{2}-\d{2}$")
PROJECTS_REPOSITORY = "bluetape4k/bluetape4k-projects"
PROJECTS_CHECKOUT_COUNT = 24
MERGE_RESULT_JOB = "test-merge-result"
MERGE_RESULT_UNSAFE_ENV = (
    "BASH_ENV",
    "ENV",
    "PATH",
    "GIT_DIR",
    "GIT_WORK_TREE",
    "GIT_INDEX_FILE",
    "GIT_OBJECT_DIRECTORY",
    "GIT_ALTERNATE_OBJECT_DIRECTORIES",
    "GIT_EXEC_PATH",
)
EXPECTED_MERGE_RESULT_VERIFY_SCRIPT = (
    "set -euo pipefail",
    'merge_result_sha="$(git rev-parse HEAD)"',
    'test "$merge_result_sha" = "$GITHUB_SHA"',
    'git merge-base --is-ancestor "$PR_HEAD_SHA" "$merge_result_sha"',
    'git merge-base --is-ancestor "$PR_BASE_SHA" "$merge_result_sha"',
    'test -n "$PR_BASE_REF"',
    'git fetch --no-tags origin "refs/heads/$PR_BASE_REF:refs/remotes/origin/$PR_BASE_REF"',
    'current_base_sha="$(git rev-parse "origin/$PR_BASE_REF")"',
    'git merge-base --is-ancestor "$current_base_sha" "$PR_HEAD_SHA"',
    'echo "pr head=$PR_HEAD_SHA"',
    'echo "pr base=$PR_BASE_SHA"',
    'echo "current base ref=$PR_BASE_REF"',
    'echo "current base sha=$current_base_sha"',
    'echo "merge result=$merge_result_sha"',
)
EXPECTED_DEPENDABOT_PARSER_CONTRACT_SCRIPT = (
    "set -euo pipefail",
    'mkdir -p "$(dirname "$OUTPUT_PATH")"',
    'test "$(git -C "$DEPENDABOT_CORE_ROOT" rev-parse HEAD)" = "${{ steps.dependabot-pin.outputs.commit }}"',
    "bundle exec ruby .gradle/catalog-source/bluetape4k-dependencies/tests/fixtures/project-dependabot-catalog/dependabot-parser-contract.rb",
)
EXPECTED_DEPENDABOT_PARSER_CONTRACT_ENV = (
    "        env:",
    "          BUNDLE_GEMFILE: ${{ github.workspace }}/.gradle/dependabot-core/Gemfile",
    "          DEPENDABOT_CORE_ROOT: ${{ github.workspace }}/.gradle/dependabot-core",
    "          MAPPING_PATH: ${{ github.workspace }}/.gradle/catalog-source/bluetape4k-dependencies/config/projects-dependabot-catalog.toml",
    "          GENERATED_CATALOG_PATH: ${{ github.workspace }}/gradle/libs.versions.toml",
    "          OUTPUT_PATH: ${{ github.workspace }}/.gradle/issue-1832-evidence/parser-receipt.json",
)
EXPECTED_DEPENDABOT_CORE_PIN_SCRIPT = (
    "python3.13 - <<'PY' >> \"$GITHUB_OUTPUT\"",
    "import tomllib",
    "from pathlib import Path",
    "mapping_path = Path(",
    '".gradle/catalog-source/bluetape4k-dependencies/config/projects-dependabot-catalog.toml"',
    ")",
    'mapping = tomllib.loads(mapping_path.read_text(encoding="utf-8"))',
    'commit = mapping.get("dependabot_core_commit")',
    'if not isinstance(commit, str) or len(commit) != 40 or any(c not in "0123456789abcdef" for c in commit):',
    'raise SystemExit("Dependabot Core pin must be a full lowercase commit SHA")',
    'print(f"commit={commit}")',
    "PY",
)
EXPECTED_CATALOG_VERIFICATION_SCRIPT = (
    "set -euo pipefail",
    'git switch -c ci-project-catalog "$PROJECTS_EXPECTED_SHA"',
    'git -C .gradle/catalog-source/bluetape4k-dependencies switch -c ci-central-catalog "$BLUETAPE4K_DEPENDENCIES_CATALOG_SHA"',
    "git remote set-url origin git@github.com:bluetape4k/bluetape4k-projects.git",
    "git -C .gradle/catalog-source/bluetape4k-dependencies remote set-url origin git@github.com:bluetape4k/bluetape4k-dependencies.git",
    'evidence_dir="$GITHUB_WORKSPACE/.gradle/issue-1832-evidence"',
    'mkdir -p "$evidence_dir"',
    "python3.13 .gradle/catalog-source/bluetape4k-dependencies/scripts/catalog_candidate.py build-project-map \\",
    '--projects-root "$GITHUB_WORKSPACE" \\',
    '--central-root "$GITHUB_WORKSPACE/.gradle/catalog-source/bluetape4k-dependencies" \\',
    '--output "$evidence_dir/repository-map.json"',
    "python3.13 .gradle/catalog-source/bluetape4k-dependencies/scripts/project-dependabot-catalog.py \\",
    '--projects-root "$GITHUB_WORKSPACE" \\',
    '--central-root "$GITHUB_WORKSPACE/.gradle/catalog-source/bluetape4k-dependencies" \\',
    '--repository-map "$evidence_dir/repository-map.json" \\',
    '--evidence-dir "$evidence_dir" \\',
    "--check",
    "python3.13 -m unittest discover \\",
    "-s .gradle/catalog-source/bluetape4k-dependencies/tests \\",
    "-p 'test_project_dependabot_catalog.py'",
    "python3.13 -m unittest tests.test_catalog_workflows",
    "python3.13 scripts/verify-catalog-workflows.py",
)
EXPECTED_CATALOG_EVIDENCE_VERIFICATION_SCRIPT = (
    "set -euo pipefail",
    'evidence_dir="$GITHUB_WORKSPACE/.gradle/issue-1832-evidence"',
    "required_files=(",
    "parser-receipt.json",
    "repository-map.json",
    "generator-receipt.json",
    "candidate-lock.json",
    ")",
    'for file in "${required_files[@]}"; do',
    'path="$evidence_dir/$file"',
    'if [[ ! -f "$path" || -L "$path" ]]; then',
    'echo "Catalog provenance evidence must be a regular file: $path" >&2',
    "exit 1",
    "fi",
    "done",
)
EXPECTED_CATALOG_EVIDENCE_ARTIFACT_INPUTS = (
    "name",
    "path",
    "if-no-files-found",
    "retention-days",
)
EXPECTED_CI_STATUS_SCRIPT = (
    'echo "Job results: $RESULTS"',
    'if [[ "$CATALOG_GOVERNANCE_RESULT" != "success" ]]; then',
    'echo "Catalog governance must succeed; got $CATALOG_GOVERNANCE_RESULT"',
    "exit 1",
    "fi",
    'if [[ "$EVENT_NAME" == "pull_request" && "$MERGE_RESULT_RESULT" != "success" ]]; then',
    'echo "PR merge-result contract must succeed; got $MERGE_RESULT_RESULT"',
    "exit 1",
    "fi",
    'if echo "$RESULTS" | grep -qE "failure|cancelled"; then',
    'echo "CI failed"',
    "exit 1",
    "fi",
    'if [[ "$SPRING_BOOT_CHANGED" == "true" && "$SPRING_BOOT_RESULT" == "skipped" ]]; then',
    'echo "Spring Boot changes were detected but the Spring Boot test job was skipped"',
    "exit 1",
    "fi",
    'if [[ "$SCIENCE_CHANGED" == "true" && "$SCIENCE_RESULT" == "skipped" ]]; then',
    'echo "Science changes were detected but the Science test job was skipped"',
    "exit 1",
    "fi",
    'if [[ "$MEASURED_CHANGED" == "true" && "$MEASURED_RESULT" == "skipped" ]]; then',
    'echo "Measured changes were detected but the Measured test job was skipped"',
    "exit 1",
    "fi",
    'echo "CI passed; conditional jobs may be skipped when their change scope is empty"',
)
EXPECTED_CI_STATUS_ENV = (
    "        env:",
    "          RESULTS: ${{ join(needs.*.result, ',') }}",
    "          CATALOG_GOVERNANCE_RESULT: ${{ needs.catalog-governance.result }}",
    "          MERGE_RESULT_RESULT: ${{ needs.test-merge-result.result }}",
    "          EVENT_NAME: ${{ github.event_name }}",
    "          SPRING_BOOT_CHANGED: ${{ needs.changes.outputs['spring-boot'] }}",
    "          SPRING_BOOT_RESULT: ${{ needs.test-spring-boot.result }}",
    "          SCIENCE_CHANGED: ${{ needs.changes.outputs.science }}",
    "          SCIENCE_RESULT: ${{ needs.test-science.result }}",
    "          MEASURED_CHANGED: ${{ needs.changes.outputs.measured }}",
    "          MEASURED_RESULT: ${{ needs.test-measured.result }}",
)
EXPECTED_NIGHTLY_TESTCONTAINERS_PLAN_SHELL_PREFIX = (
    "set -euo pipefail",
    "",
    'if [[ "${{ github.event_name }}" == "workflow_dispatch" ]]; then',
    '  scope="${{ inputs.scope }}"',
    'elif [[ "${{ github.event.schedule }}" == "0 18 * * 0" ]]; then',
    '  scope="full"',
    "else",
    '  scope="smoke"',
    "fi",
    "",
    'case "$scope" in',
    '  smoke) run_smoke="true"; run_standard="false" ;;',
    '  full) run_smoke="true"; run_standard="true" ;;',
    '  *) run_smoke="false"; run_standard="false" ;;',
    "esac",
    "",
    'case "$scope" in',
    '  full|testcontainers) run_testcontainers_coverage="true" ;;',
    '  *) run_testcontainers_coverage="false" ;;',
    "esac",
    "",
)
EXPECTED_NIGHTLY_STATUS_SCRIPT = (
    'echo "Nightly results: $RESULTS"',
    'if echo "$RESULTS" | grep -qE "failure|cancelled"; then',
    'echo "Nightly tests failed"',
    "exit 1",
    "fi",
    'if [ "$SCOPE" = full ]; then',
    'IFS=, read -ra full_results <<< "$RESULTS"',
    'for result in "${full_results[@]}"; do',
    'if [ "$result" != success ]; then',
    'echo "Full Nightly required jobs must all succeed"',
    "exit 1",
    "fi",
    "done",
    "fi",
    'if [ "$PLAN_RESULT" != success ]; then',
    'echo "Nightly plan job is not successful"',
    "exit 1",
    "fi",
    'if [ "$SCOPE" != "$EXPECTED_SCOPE" ]; then',
    'echo "Nightly plan scope does not match the requested scope"',
    "exit 1",
    "fi",
    'if [ "$COVERAGE_REPORT_RESULT" != success ]; then',
    'echo "Nightly coverage report job is not successful"',
    "exit 1",
    "fi",
    'if [ "$TESTCONTAINERS_RESULT" != success ]; then',
    'echo "Nightly Testcontainers job is not successful for the selected scope"',
    "exit 1",
    "fi",
    'if [ "$SCOPE" = smoke ] && { [ "$DETEKT_RESULT" != success ] || [ "$TEST_CORE_RESULT" != success ]; }; then',
    'echo "Smoke Nightly Detekt and core tests must succeed"',
    "exit 1",
    "fi",
    'if { [ "$SCOPE" = full ] || [ "$SCOPE" = testcontainers ]; } && [ "$TESTCONTAINERS_SPRING_RESULT" != success ]; then',
    'echo "Nightly Testcontainers Spring bridge is not successful for the selected scope"',
    "exit 1",
    "fi",
    'if [ "$SCOPE" = full ] && [ "$TEST_DATA_RESULT" != success ]; then',
    'echo "Full Nightly data tests are not successful"',
    "exit 1",
    "fi",
    'if [ "$SCOPE" = full ] && [ "$TEST_SPRING_DOCKER_RESULT" != success ]; then',
    'echo "Full Nightly Spring Docker tests are not successful"',
    "exit 1",
    "fi",
    'if [ "$SCOPE" = full ] && [ "$X64_GATE_RESULT" != success ]; then',
    'echo "Full Nightly native image gate is not successful"',
    "exit 1",
    "fi",
    'echo "Nightly tests passed"',
)
EXPECTED_DEPENDENCY_SUBMISSION_VERIFY_SCRIPT = (
    "set -euo pipefail",
    'CHECKOUT_SHA="$(git rev-parse HEAD)"',
    'test "$CHECKOUT_SHA" = "$GITHUB_SHA"',
    'test "$CHECKOUT_SHA" = "$EXPECTED_SHA"',
    'echo "run_head_sha=$GITHUB_SHA"',
    'echo "submission_graph_commit_sha=$CHECKOUT_SHA"',
    "{",
    'echo "run_head_sha: \\`$GITHUB_SHA\\`"',
    'echo "submission_graph_commit_sha: \\`$CHECKOUT_SHA\\`"',
    '} >> "$GITHUB_STEP_SUMMARY"',
)
YAML_ACTION_ALIAS_RE = re.compile(
    r"(?m)^ {6}- (?:uses|\"uses\"|'uses')\s*:\s*[*&][A-Za-z_][A-Za-z0-9_-]*"
    r"|^ {8}(?:uses|\"uses\"|'uses')\s*:\s*[*&][A-Za-z_][A-Za-z0-9_-]*"
    r"|^ {6}- [*&][A-Za-z_][A-Za-z0-9_-]*"
)
YAML_ENV_ALIAS_VALUE_RE = re.compile(r"(?:^|[:,\[\-]\s*)[*&][A-Za-z_][A-Za-z0-9_-]*\b")
YAML_MERGE_KEY_RE = re.compile(r"(?<![A-Za-z0-9_-])<<\s*:")
YAML_ESCAPED_KEY_RE = re.compile(
    r"""(?m)^\s*(?:-\s+)?(?:"[^"\n]*\\[^"\n]*"|'[^'\n]*\\[^'\n]*')\s*:"""
)
YAML_ESCAPED_ACTION_VALUE_RE = re.compile(
    r"""(?m)^\s*(?:-\s*)?(?:uses|"uses"|'uses')\s*:\s*(?:"[^"\n]*\\[^"\n]*"|'[^'\n]*\\[^'\n]*')\s*(?:#.*)?$"""
)
YAML_SIMPLE_KEY_RE = (
    r"(?:[A-Za-z_][A-Za-z0-9_-]*|\"[A-Za-z_][A-Za-z0-9_-]*\"|'[A-Za-z_][A-Za-z0-9_-]*')"
)


def _yaml_key_pattern(name: str) -> str:
    escaped = re.escape(name)
    return rf"(?:{escaped}|\"{escaped}\"|'{escaped}')"


EXPECTED_EXTERNAL_CHECKOUTS = (
    (
        "manual-contract",
        "bluetape4k/bluetape4k.github.io",
        "${{ env.BLUETAPE4K_MANUAL_REF }}",
        ".manual-site",
    ),
    (
        "test-key-utils",
        "bluetape4k/bluetape4k.github.io",
        "${{ env.BLUETAPE4K_MANUAL_REF }}",
        ".manual-site",
    ),
    (
        "catalog-governance",
        "bluetape4k/bluetape4k-dependencies",
        "${{ env.BLUETAPE4K_DEPENDENCIES_CATALOG_TAG }}",
        ".gradle/catalog-source/bluetape4k-dependencies",
    ),
    (
        "catalog-governance",
        "dependabot/dependabot-core",
        "${{ steps.dependabot-pin.outputs.commit }}",
        ".gradle/dependabot-core",
    ),
)


def _top_level_blocks(text: str, name: str) -> list[str]:
    matches = list(re.finditer(rf"(?m)^{_yaml_key_pattern(name)}:\s*$", text))
    blocks: list[str] = []
    for match in matches:
        following = re.search(rf"(?m)^{YAML_SIMPLE_KEY_RE}:\s*$", text[match.end() :])
        end = match.end() + following.start() if following else len(text)
        blocks.append(text[match.start() : end])
    return blocks


def _top_level_block(text: str, name: str) -> str:
    blocks = _top_level_blocks(text, name)
    return blocks[0] if blocks else ""


def _job_blocks(text: str) -> list[tuple[str, str]]:
    jobs_match = re.search(r"(?m)^jobs:\s*$", text)
    if jobs_match is None:
        return []
    jobs = text[jobs_match.end() :]
    matches = list(re.finditer(r"(?m)^  ([A-Za-z0-9_-]+):\s*$", jobs))
    return [
        (
            match.group(1),
            jobs[match.start() : matches[index + 1].start()]
            if index + 1 < len(matches)
            else jobs[match.start() :],
        )
        for index, match in enumerate(matches)
    ]


def _job_block(text: str, name: str) -> str:
    return dict(_job_blocks(text)).get(name, "")


def _job_values(job: str, name: str) -> list[str]:
    values = []
    key = _yaml_key_pattern(name)
    for line in job.splitlines():
        match = re.fullmatch(rf"    {key}:[ \t]*(.*)", line)
        if match is not None:
            value = match.group(1).split(" #", maxsplit=1)[0]
            values.append(_yaml_scalar_value(value))
    return values


def _matrix_group_blocks(job: str) -> list[tuple[str, str]]:
    matrix_match = re.search(r"(?m)^      matrix:\s*$", job)
    if matrix_match is None:
        return []
    matrix_tail = job[matrix_match.end() :]
    steps_match = re.search(r"(?m)^    steps:\s*$", matrix_tail)
    matrix = matrix_tail[: steps_match.start()] if steps_match else matrix_tail
    matches = list(re.finditer(r"(?m)^          - group: ([A-Za-z0-9_-]+)\s*$", matrix))
    return [
        (
            match.group(1),
            matrix[match.start() : matches[index + 1].start()]
            if index + 1 < len(matches)
            else matrix[match.start() :],
        )
        for index, match in enumerate(matches)
    ]


def _matrix_group_test_tasks(group: str) -> list[str]:
    match = re.search(r"(?m)^            test_tasks:[ \t]*>-[ \t]*$", group)
    if match is None:
        return []
    tasks = []
    for line in group[match.end() :].splitlines():
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip())
        if indent < 14:
            break
        task = line.strip()
        if not task.startswith("#"):
            tasks.append(task)
    return tasks


def _matrix_group_test_task_errors(group: str) -> list[str]:
    tasks = _matrix_group_test_tasks(group)
    if any(re.fullmatch(r":[A-Za-z0-9_.:-]+", task) is None for task in tasks):
        return ["test_tasks must contain only Gradle task paths"]
    return []


def _workflow_env(text: str, name: str) -> str | None:
    values = _workflow_env_values(text, name)
    return values[0] if len(values) == 1 else None


def _yaml_scalar_value(value: str) -> str:
    value = value.strip()
    if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
        return value[1:-1]
    return value


def _workflow_env_values(text: str, name: str) -> list[str]:
    values: list[str] = []
    key = _yaml_key_pattern(name)
    for env in _top_level_blocks(text, "env"):
        values.extend(
            _yaml_scalar_value(value)
            for value in re.findall(rf"(?m)^  {key}:\s*(.*?)\s*$", env)
        )
    return values


def _yaml_mapping_keys(value: str) -> set[str]:
    return {
        _yaml_scalar_value(match.group(1))
        for match in re.finditer(
            rf"(?<![A-Za-z0-9_-])({YAML_SIMPLE_KEY_RE})\s*:", value
        )
    }


def _env_mapping_keys(lines: list[str], start: int, scope_indent: int) -> set[str]:
    line = lines[start]
    prefix = " " * scope_indent
    match = re.fullmatch(
        rf"{re.escape(prefix)}{_yaml_key_pattern('env')}:\s*(.*)", line
    )
    if match is None:
        return set()

    keys = _yaml_mapping_keys(match.group(1))
    for entry in lines[start + 1 :]:
        if entry.strip() and len(entry) - len(entry.lstrip()) <= scope_indent:
            break
        if entry.strip() and len(entry) - len(entry.lstrip()) == scope_indent + 2:
            key_match = re.match(rf"({YAML_SIMPLE_KEY_RE})\s*:", entry.lstrip())
            if key_match is not None:
                keys.add(_yaml_scalar_value(key_match.group(1)))
    return keys


def _has_environment_mapping_alias_or_merge(text: str) -> bool:
    lines = text.splitlines()
    for index, line in enumerate(lines):
        match = re.fullmatch(
            rf"(?P<indent> {{0,8}}){_yaml_key_pattern('env')}:\s*(?P<inline>.*)",
            line,
        )
        if match is None:
            continue

        scope_indent = len(match.group("indent"))
        inline_value = match.group("inline")
        if YAML_ENV_ALIAS_VALUE_RE.search(inline_value) or YAML_MERGE_KEY_RE.search(
            inline_value
        ):
            return True

        for entry in lines[index + 1 :]:
            if entry.strip() and len(entry) - len(entry.lstrip()) <= scope_indent:
                break
            if not entry.strip():
                continue
            value = entry.strip()
            if YAML_ENV_ALIAS_VALUE_RE.search(value) or YAML_MERGE_KEY_RE.search(value):
                return True

    return False


def _environment_mapping_key_count(text: str, name: str) -> int:
    count = 0
    lines = text.splitlines()
    for index, line in enumerate(lines):
        match = re.fullmatch(
            rf"(?P<indent> {{0,8}}){_yaml_key_pattern('env')}:\s*.*", line
        )
        if match is None:
            continue
        keys = _env_mapping_keys(lines, index, len(match.group("indent")))
        count += sum(key.upper() == name.upper() for key in keys)
    return count


def _unsafe_execution_environment_names(
    text: str, protected_jobs: tuple[str, ...]
) -> list[str]:
    keys: set[str] = set()
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if re.fullmatch(rf"{_yaml_key_pattern('env')}:\s*.*", line):
            keys.update(_env_mapping_keys(lines, index, 0))

    for job_name in protected_jobs:
        job_lines = _job_block(text, job_name).splitlines()
        for index, line in enumerate(job_lines):
            for scope_indent in (4, 8):
                if re.fullmatch(
                    rf"{' ' * scope_indent}{_yaml_key_pattern('env')}:\s*.*",
                    line,
                ):
                    keys.update(_env_mapping_keys(job_lines, index, scope_indent))

    return sorted(
        key
        for key in keys
        if key.upper()
        in {
            "BASH_ENV",
            "BASHOPTS",
            "CDPATH",
            "CLASSPATH",
            "DYLD_INSERT_LIBRARIES",
            "DYLD_LIBRARY_PATH",
            "ENV",
            "GEM_HOME",
            "GEM_PATH",
            "GEMRC",
            "JAVA_OPTS",
            "JAVA_TOOL_OPTIONS",
            "JDK_JAVA_OPTIONS",
            "LD_LIBRARY_PATH",
            "LD_PRELOAD",
            "NODE_OPTIONS",
            "NODE_PATH",
            "PATH",
            "PERL5LIB",
            "PERL5OPT",
            "PYTHONHOME",
            "PYTHONPATH",
            "RUBYLIB",
            "RUBYOPT",
            "SHELLOPTS",
            "_JAVA_OPTIONS",
        }
        or key.upper().startswith(("GIT_", "BASH_FUNC_", "GRADLE_", "KOTLIN_"))
        or key.upper().startswith("BUNDLE_")
        and key.upper() != "BUNDLE_GEMFILE"
    )


def _has_job_or_step_env_override(text: str, name: str) -> bool:
    key = _yaml_key_pattern(name)
    for _, job in _job_blocks(text):
        lines = job.splitlines()
        for index, line in enumerate(lines):
            match = re.fullmatch(
                rf"(?P<indent> {{4}}| {{8}}){_yaml_key_pattern('env')}:\s*(?P<inline>.*)",
                line,
            )
            if match is None:
                continue
            scope_indent = len(match.group("indent"))
            inline = match.group("inline")
            if inline and (
                re.search(rf"(?<![A-Za-z0-9_-]){key}\s*:", inline)
                or re.search(r"(?:<<\s*:|\*[A-Za-z_][A-Za-z0-9_-]*)", inline)
            ):
                return True
            for entry in lines[index + 1 :]:
                entry_indent = len(entry) - len(entry.lstrip())
                if entry.strip() and entry_indent <= scope_indent:
                    break
                if (
                    entry.strip()
                    and entry_indent == scope_indent + 2
                    and (
                        re.match(rf"{key}\s*:", entry.lstrip())
                        or re.match(r"<<\s*:", entry.lstrip())
                    )
                ):
                    return True
    return False


def _step_blocks(job: str) -> list[str]:
    lines = job.splitlines()
    starts = [index for index, line in enumerate(lines) if re.match(r"^      - ", line)]
    return [
        "\n".join(
            lines[start : starts[index + 1] if index + 1 < len(starts) else len(lines)]
        )
        for index, start in enumerate(starts)
    ]


def _named_step_block(job: str, name: str) -> str:
    lines = job.splitlines()
    starts = [index for index, line in enumerate(lines) if line == f"      - name: {name}"]
    if len(starts) != 1:
        return ""
    start = starts[0]
    end = next(
        (
            index
            for index in range(start + 1, len(lines))
            if lines[index].startswith("      - ")
        ),
        len(lines),
    )
    return "\n".join(lines[start:end])


def _step_script_lines(step: str, name: str) -> list[str] | None:
    lines = step.splitlines()
    script_lines = [
        (index, len(line) - len(line.lstrip()), line.split(":", 1)[1].strip())
        for index, line in enumerate(lines)
        if re.match(rf"^\s+{_yaml_key_pattern(name)}:\s*", line)
    ]
    if len(script_lines) != 1:
        return None

    start, script_indent, inline = script_lines[0]
    block_scalar = re.fullmatch(
        r"([|>])(?:[+-]?[1-9]?|[1-9][+-]?)?(?:[ \t]+#.*)?", inline
    )
    if block_scalar is None:
        return [_yaml_scalar_value(inline)] if inline else None

    paragraphs: list[list[str]] = [[]]
    for line in lines[start + 1 :]:
        indent = len(line) - len(line.lstrip())
        if line.strip() and indent <= script_indent:
            break
        if not line.strip():
            if block_scalar.group(1) == ">" and paragraphs[-1]:
                paragraphs.append([])
            continue
        paragraphs[-1].append(line.strip())
    if block_scalar.group(1) == ">":
        return [" ".join(paragraph) for paragraph in paragraphs if paragraph]
    return [line for paragraph in paragraphs for line in paragraph]


def _step_script_source(step: str, name: str) -> str | None:
    lines = step.splitlines()
    script_lines = [
        (index, len(line) - len(line.lstrip()), line.split(":", 1)[1].strip())
        for index, line in enumerate(lines)
        if re.match(rf"^\s+{_yaml_key_pattern(name)}:\s*", line)
    ]
    if len(script_lines) != 1:
        return None

    start, script_indent, inline = script_lines[0]
    block_scalar = re.fullmatch(
        r"([|>])(?:[+-]?[1-9]?|[1-9][+-]?)?(?:[ \t]+#.*)?", inline
    )
    if block_scalar is None:
        return _yaml_scalar_value(inline) if inline else None

    body = []
    for line in lines[start + 1 :]:
        if line.strip() and len(line) - len(line.lstrip()) <= script_indent:
            break
        body.append(line)
    return textwrap.dedent("\n".join(body))


def _step_run_script(step: str) -> list[str] | None:
    return _step_script_lines(step, "run")


def _step_command_script(step: str) -> list[str] | None:
    return _step_script_lines(step, "command")


def _indented_values(block: str, name: str, indent: int) -> list[str]:
    prefix = " " * indent
    return [
        _yaml_scalar_value(value)
        for value in re.findall(
            rf"(?m)^{re.escape(prefix)}{_yaml_key_pattern(name)}:\s*(.*?)\s*$",
            block,
        )
    ]


def _step_env_value(step: str, name: str) -> str | None:
    lines = step.splitlines()
    values: list[str] = []
    for index, line in enumerate(lines):
        if re.fullmatch(rf"        {_yaml_key_pattern('env')}:\s*", line) is None:
            continue
        for entry in lines[index + 1 :]:
            if entry.strip() and len(entry) - len(entry.lstrip()) <= 8:
                break
            match = re.fullmatch(
                rf"          {_yaml_key_pattern(name)}:\s*(.*?)\s*", entry
            )
            if match is not None:
                values.append(_yaml_scalar_value(match.group(1)))
    return values[0] if len(values) == 1 else None


def _step_env_block(step: str) -> list[str] | None:
    lines = step.splitlines()
    starts = [
        index
        for index, line in enumerate(lines)
        if re.fullmatch(rf"        {_yaml_key_pattern('env')}:\s*", line)
    ]
    if len(starts) != 1:
        return None
    start = starts[0]
    end = next(
        (
            index
            for index in range(start + 1, len(lines))
            if lines[index].strip()
            and len(lines[index]) - len(lines[index].lstrip()) <= 8
        ),
        len(lines),
    )
    return lines[start:end]


def _step_environment_keys(step: str) -> set[str] | None:
    lines = step.splitlines()
    starts = [
        index
        for index, line in enumerate(lines)
        if re.fullmatch(rf"        {_yaml_key_pattern('env')}:\s*", line)
    ]
    if len(starts) != 1:
        return None
    return _env_mapping_keys(lines, starts[0], 8)


def _mongodb_test_environment_errors(
    workflow_text: str,
    test_steps: tuple[tuple[str, str, str | None], ...],
) -> list[str]:
    protected_jobs = tuple(job_name for job_name, _, _ in test_steps)
    unsafe_names = _unsafe_execution_environment_names(workflow_text, protected_jobs)
    disallowed_names = sorted(
        name for name in unsafe_names if name.upper() != "GRADLE_OPTS"
    )
    errors = []
    if disallowed_names:
        errors.append(
            "MongoDB protected execution scopes must not define unsafe execution "
            "environment variable(s): " + ", ".join(disallowed_names)
        )

    for job_name, step_name, expected_test_tasks in test_steps:
        step = _named_step_block(_job_block(workflow_text, job_name), step_name)
        expected_keys = {"GRADLE_OPTS"}
        if expected_test_tasks is not None:
            expected_keys.add("TEST_TASKS")
        if _step_environment_keys(step) != expected_keys:
            errors.append(
                f"{job_name} MongoDB test step environment allowlist must be "
                f"{sorted(expected_keys)!r}"
            )
            continue
        if _step_env_value(step, "GRADLE_OPTS") != "-Dorg.gradle.daemon=false":
            errors.append(
                f"{job_name} MongoDB test step environment allowlist must pin GRADLE_OPTS"
            )
        if (
            expected_test_tasks is not None
            and _step_env_value(step, "TEST_TASKS") != expected_test_tasks
        ):
            errors.append(
                f"{job_name} MongoDB test step environment allowlist must pin TEST_TASKS"
            )
    return errors


def _step_with_block(step: str) -> list[str] | None:
    lines = step.splitlines()
    starts = [
        index
        for index, line in enumerate(lines)
        if re.fullmatch(rf"        {_yaml_key_pattern('with')}:\s*", line)
    ]
    if len(starts) != 1:
        return None
    start = starts[0]
    end = next(
        (
            index
            for index in range(start + 1, len(lines))
            if lines[index].strip()
            and len(lines[index]) - len(lines[index].lstrip()) <= 8
        ),
        len(lines),
    )
    return lines[start:end]


def _step_with_input_keys(step: str) -> list[str] | None:
    with_block = _step_with_block(step)
    if with_block is None:
        return None

    keys = []
    for line in with_block[1:]:
        if not line.strip():
            continue
        indent = len(line) - len(line.lstrip())
        if indent < 10:
            return None
        if indent != 10:
            continue
        match = re.match(rf"^          ({YAML_SIMPLE_KEY_RE}|<<)\s*:", line)
        if match is None:
            return None
        keys.append(_yaml_scalar_value(match.group(1)))
    return keys


def _mongodb_retry_action_input_errors(step: str) -> list[str]:
    input_keys = _step_with_input_keys(step)
    approved_inputs = {"timeout_minutes", "max_attempts", "shell", "command"}
    if (
        input_keys is None
        or len(input_keys) != len(approved_inputs)
        or set(input_keys) != approved_inputs
    ):
        return ["must use only the approved retry action inputs"]

    with_block = "\n".join(_step_with_block(step) or [])
    expected_values = {
        "timeout_minutes": "60",
        "max_attempts": "3",
        "shell": "bash",
    }
    for name, expected in expected_values.items():
        if _indented_values(with_block, name, 10) != [expected]:
            return ["must keep the approved retry action inputs fixed"]
    return []


def _mongodb_test_step_guard_errors(step: str) -> list[str]:
    errors = []
    if _indented_values(step, "if", 8):
        errors.append("must not have a step-level if that can skip MongoDB tests")
    if _indented_values(step, "continue-on-error", 8):
        errors.append("must not use step-level continue-on-error to mask failures")
    return errors


def _mongodb_test_job_guard_errors(job: str, expected_if: str) -> list[str]:
    errors = []
    if _job_values(job, "if") != [expected_if]:
        errors.append(
            "must retain its expected execution guard to avoid skipping MongoDB tests"
        )
    if _job_values(job, "continue-on-error"):
        errors.append("must not use job-level continue-on-error to mask failures")
    return errors


def _matrix_test_step_errors(job: str, step_name: str) -> list[str]:
    step = _named_step_block(job, step_name)
    if not step:
        return ["is missing or duplicated"]
    guard_errors = _mongodb_test_step_guard_errors(step)
    if guard_errors:
        return guard_errors
    retry_input_errors = _mongodb_retry_action_input_errors(step)
    if retry_input_errors:
        return retry_input_errors

    expected_command = [
        'read -ra tasks <<< "$TEST_TASKS"',
        './gradlew "${tasks[@]}" --max-workers=2 --no-configuration-cache',
    ]
    if (
        _step_values(step, "uses") != ["nick-fields/retry@v4"]
        or _step_command_script(step) != expected_command
        or _step_env_value(step, "TEST_TASKS") != "${{ matrix.test_tasks }}"
    ):
        return ["must execute matrix.test_tasks through the expected retry command"]
    return []


def _ci_data_test_step_errors(job: str) -> list[str]:
    step = _named_step_block(job, "Test data modules")
    if not step:
        return ["is missing or duplicated"]
    guard_errors = _mongodb_test_step_guard_errors(step)
    if guard_errors:
        return guard_errors
    retry_input_errors = _mongodb_retry_action_input_errors(step)
    if retry_input_errors:
        return retry_input_errors

    expected_command = [
        "./gradlew \\",
        ":bluetape4k-jdbc:test \\",
        ":bluetape4k-hibernate:test \\",
        ":bluetape4k-hibernate-cache-lettuce:test \\",
        ":bluetape4k-hibernate-reactive:test \\",
        ":bluetape4k-r2dbc:test \\",
        ":bluetape4k-mongodb:test \\",
        ":bluetape4k-cassandra:test \\",
        "--max-workers=2 \\",
        "--no-configuration-cache",
    ]
    if (
        _step_values(step, "uses") != ["nick-fields/retry@v4"]
        or _step_command_script(step) != expected_command
    ):
        return ["must execute the exact Data test task list through the retry command"]
    return []


def _spring_boot_test_step_errors(job: str) -> list[str]:
    return _matrix_test_step_errors(
        job, "Test Spring Boot modules (${{ matrix.group }})"
    )


def _checkout_steps(text: str) -> list[tuple[str, str]]:
    checkout_steps: list[tuple[str, str]] = []
    checkout_action = (
        r"(?:actions/checkout@[^\s#\"']+|"
        r"\"actions/checkout@[^\"\s#]+\"|"
        r"'actions/checkout@[^'\s#]+')"
    )
    for job_name, job in _job_blocks(text):
        for step in _step_blocks(job):
            if (
                re.search(
                    rf"(?m)^      - {_yaml_key_pattern('uses')}\s*:\s*{checkout_action}(?:\s+#.*)?$"
                    rf"|^        {_yaml_key_pattern('uses')}\s*:\s*{checkout_action}(?:\s+#.*)?$",
                    step,
                )
                is not None
            ):
                checkout_steps.append((job_name, step))
    return checkout_steps


def _step_values(step: str, name: str) -> list[str]:
    values = []
    for line in step.splitlines():
        match = re.fullmatch(
            rf"[ \t]+(?:-\s+)?{_yaml_key_pattern(name)}:[ \t]*(.*)", line
        )
        if match is None:
            continue
        value = _yaml_scalar_value(match.group(1).split(" #", maxsplit=1)[0])
        values.append(value)
    return values


def _step_value(step: str, name: str) -> str | None:
    values = _step_values(step, name)
    return values[0] if len(values) == 1 else None


def _named_step_index(job: str, name: str) -> int | None:
    matches = [
        index
        for index, step in enumerate(_step_blocks(job))
        if step.splitlines()[:1] == [f"      - name: {name}"]
    ]
    return matches[0] if len(matches) == 1 else None


def _runner_diagnostics_step_errors(job: str, expected_if: str | None) -> list[str]:
    step = _named_step_block(job, "Report runner image and kernel")
    if not step:
        return ["runner diagnostics step is missing or duplicated"]

    conditions = _step_values(step, "if")
    if expected_if is None:
        if conditions:
            return ["runner diagnostics step must run for every Data job"]
    elif conditions != [expected_if]:
        return ["runner diagnostics step must match its expected matrix group"]

    if _step_values(step, "shell") != ["bash"]:
        return ["runner diagnostics step must use an explicit bash shell"]
    if _step_values(step, "continue-on-error"):
        return ["runner diagnostics step must propagate failures"]

    required = [
        'echo "ImageOS=${ImageOS:-<unset>}"',
        'echo "ImageVersion=${ImageVersion:-<unset>}"',
        "cat /etc/os-release",
        "uname -r",
    ]
    script = _step_run_script(step)
    if script != required:
        return ["runner diagnostics step must report image and kernel details in order"]
    return []


def _testcontainers_plan_python_source(nightly_text: str) -> str | None:
    plan_job = _job_block(nightly_text, "plan")
    plan_steps = [
        step for step in _step_blocks(plan_job) if _step_values(step, "id") == ["plan"]
    ]
    if len(plan_steps) != 1:
        return None
    script = _step_script_source(plan_steps[0], "run")
    if script is None:
        return None
    lines = script.splitlines()
    starts = [
        index
        for index, line in enumerate(lines)
        if line.lstrip().startswith("matrix_json=") and "python3 <<'PY'" in line
    ]
    if len(starts) != 1:
        return None
    start = starts[0]
    ends = [index for index in range(start + 1, len(lines)) if lines[index] == "PY"]
    if not ends:
        return None
    return "\n".join(lines[start + 1 : ends[0]])


def _assignment_values(tree: ast.Module, name: str) -> list[ast.expr]:
    return [
        statement.value
        for statement in tree.body
        if isinstance(statement, ast.Assign)
        and any(
            isinstance(target, ast.Name) and target.id == name
            for target in statement.targets
        )
    ]


def _matches_python_expression(node: ast.expr, source: str) -> bool:
    expected = ast.parse(source, mode="eval").body
    return ast.dump(node, include_attributes=False) == ast.dump(
        expected, include_attributes=False
    )


def _testcontainers_plan_contract_errors(nightly_text: str) -> list[str]:
    source = _testcontainers_plan_python_source(nightly_text)
    if source is None:
        return ["Nightly Testcontainers plan Python block is missing or duplicated"]
    try:
        tree = ast.parse(source)
    except SyntaxError:
        return ["Nightly Testcontainers plan Python block must remain valid Python"]

    expected_statement_shapes = (
        ("import", ("json",)),
        ("import", ("os",)),
        ("assignment", "all_groups"),
        ("assignment", "scope"),
        ("assignment", "groups_by_scope"),
        ("assignment", "selected"),
        ("matrix-output",),
    )
    statement_shapes = []
    for statement in tree.body:
        if isinstance(statement, ast.Import):
            statement_shapes.append(
                ("import", tuple(alias.name for alias in statement.names))
            )
        elif (
            isinstance(statement, ast.Assign)
            and len(statement.targets) == 1
            and isinstance(statement.targets[0], ast.Name)
        ):
            statement_shapes.append(("assignment", statement.targets[0].id))
        elif (
            isinstance(statement, ast.Expr)
            and isinstance(statement.value, ast.Call)
            and isinstance(statement.value.func, ast.Name)
            and statement.value.func.id == "print"
        ):
            statement_shapes.append(("matrix-output",))
        else:
            statement_shapes.append(("unexpected",))
    if tuple(statement_shapes) != expected_statement_shapes:
        return [
            (
                "Nightly storage-cache plan Python block must contain only the ordered "
                "plan assignments and final matrix output"
            )
        ]

    errors = []
    group_values = _assignment_values(tree, "all_groups")
    if len(group_values) != 1 or not isinstance(group_values[0], ast.List):
        return ["Nightly Testcontainers plan must define literal all_groups"]

    storage_cache_groups = []
    for group in group_values[0].elts:
        if not isinstance(group, ast.Dict):
            continue
        fields = {
            key.value: value
            for key, value in zip(group.keys, group.values)
            if isinstance(key, ast.Constant) and isinstance(key.value, str)
        }
        group_name = fields.get("group")
        if isinstance(group_name, ast.Constant) and group_name.value == "storage-cache":
            storage_cache_groups.append(fields)
    if len(storage_cache_groups) != 1:
        return [
            "Nightly Testcontainers plan must define exactly one storage-cache group"
        ]

    test_patterns = storage_cache_groups[0].get("test_patterns")
    pattern_values = None
    if (
        isinstance(test_patterns, ast.Call)
        and isinstance(test_patterns.func, ast.Attribute)
        and test_patterns.func.attr == "join"
        and isinstance(test_patterns.func.value, ast.Constant)
        and test_patterns.func.value.value == " "
        and len(test_patterns.args) == 1
        and isinstance(test_patterns.args[0], ast.List)
        and all(
            isinstance(pattern, ast.Constant) and isinstance(pattern.value, str)
            for pattern in test_patterns.args[0].elts
        )
    ):
        pattern_values = [pattern.value for pattern in test_patterns.args[0].elts]
    elif isinstance(test_patterns, ast.Constant) and isinstance(
        test_patterns.value, str
    ):
        pattern_values = test_patterns.value.split()
    if (
        pattern_values is None
        or pattern_values.count(
            "io.bluetape4k.testcontainers.storage.MongoDBServerTest"
        )
        != 1
    ):
        errors.append(
            "Nightly storage-cache plan must include MongoDBServerTest as an emitted test pattern"
        )

    scope_values = _assignment_values(tree, "groups_by_scope")
    if len(scope_values) != 1 or not isinstance(scope_values[0], ast.Dict):
        errors.append("Nightly Testcontainers plan must define literal groups_by_scope")
    else:
        scope_map = {
            key.value: value
            for key, value in zip(scope_values[0].keys, scope_values[0].values)
            if isinstance(key, ast.Constant) and isinstance(key.value, str)
        }
        for scope in ("full", "testcontainers"):
            value = scope_map.get(scope)
            if value is None or not _matches_python_expression(
                value, '[group["group"] for group in all_groups]'
            ):
                errors.append(
                    f"Nightly storage-cache MongoDB tests must be selected by the {scope} scope"
                )

    selected_scope_values = _assignment_values(tree, "scope")
    if len(selected_scope_values) != 1 or not _matches_python_expression(
        selected_scope_values[0], 'os.environ["SCOPE"]'
    ):
        errors.append("Nightly Testcontainers plan must select the requested scope")

    selected_values = _assignment_values(tree, "selected")
    if len(selected_values) != 1 or not _matches_python_expression(
        selected_values[0], "set(groups_by_scope[scope])"
    ):
        errors.append("Nightly Testcontainers plan must select its configured scope")
    output_calls = [
        statement.value
        for statement in tree.body
        if isinstance(statement, ast.Expr)
        and isinstance(statement.value, ast.Call)
        and isinstance(statement.value.func, ast.Name)
        and statement.value.func.id == "print"
    ]
    expected_output = (
        'print(json.dumps({"include": [group for group in all_groups '
        'if group["group"] in selected]}, separators=(",", ":")))'
    )
    if len(output_calls) != 1 or not _matches_python_expression(
        output_calls[0], expected_output
    ):
        errors.append(
            "Nightly Testcontainers plan must emit selected groups into the matrix"
        )
    return errors


def _testcontainers_plan_output_errors(nightly_text: str) -> list[str]:
    plan_job = _job_block(nightly_text, "plan")
    plan_steps = [
        step for step in _step_blocks(plan_job) if _step_values(step, "id") == ["plan"]
    ]
    if len(plan_steps) != 1:
        return ["Nightly Testcontainers plan output step is missing or duplicated"]

    errors = []
    if _job_values(plan_job, "if"):
        errors.append("Nightly Testcontainers plan job must not be skippable")
    if _job_values(plan_job, "continue-on-error"):
        errors.append("Nightly Testcontainers plan job must not allow errors")
    if _job_values(plan_job, "env") or any(
        re.search(r"(?m)^        env:", step) for step in _step_blocks(plan_job)
    ):
        errors.append(
            "Nightly Testcontainers plan job and steps must not define environment overrides"
        )
    for output_name, step_output in (
        ("scope", "scope"),
        ("run_smoke", "run_smoke"),
        ("run_standard", "run_standard"),
        ("run_testcontainers_coverage", "run_testcontainers_coverage"),
        ("testcontainers_matrix", "testcontainers_matrix"),
    ):
        if _indented_values(plan_job, output_name, 6) != [
            f"${{{{ steps.plan.outputs.{step_output} }}}}"
        ]:
            errors.append(
                f"Nightly Testcontainers plan output must expose {output_name}"
            )

    plan_step = plan_steps[0]
    if _step_values(plan_step, "shell") != ["bash"]:
        errors.append("Nightly Testcontainers plan output step must use bash")
    if _step_values(plan_step, "if"):
        errors.append("Nightly Testcontainers plan output step must not be skippable")
    if _step_values(plan_step, "continue-on-error"):
        errors.append("Nightly Testcontainers plan output step must not allow errors")
    script = _step_script_source(plan_step, "run")
    if script is None:
        errors.append("Nightly Testcontainers plan output step must have one script")
        return errors

    lines = script.splitlines()
    matrix_assignment = 'matrix_json="$(SCOPE="$scope" python3 <<\'PY\''
    assignment_indexes = [
        index for index, line in enumerate(lines) if line == matrix_assignment
    ]
    if len(assignment_indexes) != 1:
        errors.append(
            "Nightly Testcontainers plan must assign its matrix from the validated Python output"
        )
        return errors
    assignment_index = assignment_indexes[0]
    terminators = [
        index
        for index in range(assignment_index + 1, len(lines))
        if lines[index] == "PY"
    ]
    if len(terminators) != 1:
        errors.append("Nightly Testcontainers plan must have one matrix heredoc")
        return errors

    expected_tail = [
        ')"',
        "",
        "{",
        '  echo "scope=$scope"',
        '  echo "run_smoke=$run_smoke"',
        '  echo "run_standard=$run_standard"',
        '  echo "run_testcontainers_coverage=$run_testcontainers_coverage"',
        '  echo "testcontainers_matrix=$matrix_json"',
        '} >> "$GITHUB_OUTPUT"',
    ]
    if not lines or lines[0] != "set -euo pipefail":
        errors.append("Nightly Testcontainers plan must fail on shell errors")
    if lines[:assignment_index] != list(
        EXPECTED_NIGHTLY_TESTCONTAINERS_PLAN_SHELL_PREFIX
    ):
        errors.append(
            "Nightly Testcontainers plan shell prelude must not override the validated matrix command"
        )
    if lines[terminators[0] + 1 :] != expected_tail:
        errors.append(
            "Nightly Testcontainers plan must publish the validated matrix unchanged to GITHUB_OUTPUT"
        )
    return errors


def _nightly_status_contract_errors(nightly_text: str) -> list[str]:
    status_job = _job_block(nightly_text, "nightly-status")
    errors = []
    smoke_guard = "${{ needs.plan.outputs.run_smoke == 'true' }}"
    standard_guard = "${{ needs.plan.outputs.run_standard == 'true' }}"
    expected_job_guards = (
        ("detekt", smoke_guard),
        ("test-core", smoke_guard),
        ("test-io", standard_guard),
        ("test-ktor", standard_guard),
        ("test-utils", standard_guard),
        ("test-misc", standard_guard),
        ("test-infra", standard_guard),
        ("test-data", standard_guard),
        ("test-spring-docker", standard_guard),
        (
            "test-testcontainers-spring",
            "${{ always() && (needs.plan.outputs.scope == 'full' || needs.plan.outputs.scope == 'testcontainers') && needs.test-testcontainers.result == 'success' && ((needs.plan.outputs.scope == 'full' && needs.test-testcontainers-image-gate.result == 'success') || (needs.plan.outputs.scope == 'testcontainers' && needs.test-testcontainers-image-gate.result == 'skipped')) }}",
        ),
        ("coverage-report", "always()"),
    )
    for job_name, expected_if in expected_job_guards:
        if _job_values(_job_block(nightly_text, job_name), "if") != [expected_if]:
            errors.append(
                f"Nightly required job {job_name} must use its exact execution guard"
            )

    expected_needs = (
        "plan",
        "build",
        "detekt",
        "test-core",
        "test-io",
        "test-io-http",
        "test-ktor",
        "test-utils",
        "test-misc",
        "test-infra",
        "test-data",
        "test-spring-docker",
        "test-testcontainers",
        "test-testcontainers-image-gate",
        "test-testcontainers-spring",
        "coverage-report",
    )
    actual_needs = re.findall(r"(?m)^      - ([A-Za-z0-9_-]+)\s*$", status_job)
    if actual_needs != list(expected_needs):
        errors.append("Nightly status must depend on every scope result exactly once")
    if _job_values(status_job, "if") != ["always()"]:
        errors.append("Nightly status must run after failed or skipped jobs")
    if _job_values(status_job, "continue-on-error"):
        errors.append("Nightly status job must not allow errors")
    if _job_values(status_job, "env"):
        errors.append(
            "Nightly status job must not define job-level environment overrides"
        )

    status_step = _named_step_block(status_job, "Check all jobs")
    if _step_values(status_step, "shell") != ["bash"]:
        errors.append("Nightly status check step must use bash directly")
    if _step_values(status_step, "if"):
        errors.append("Nightly status check step must not be skippable")
    if _step_values(status_step, "continue-on-error"):
        errors.append("Nightly status check step must not allow errors")
    if _step_environment_keys(status_step) != {
        "EXPECTED_SCOPE",
        "PLAN_RESULT",
        "RESULTS",
        "SCOPE",
        "DETEKT_RESULT",
        "TEST_CORE_RESULT",
        "TEST_DATA_RESULT",
        "TEST_SPRING_DOCKER_RESULT",
        "TESTCONTAINERS_RESULT",
        "TESTCONTAINERS_SPRING_RESULT",
        "X64_GATE_RESULT",
        "COVERAGE_REPORT_RESULT",
    }:
        errors.append(
            "Nightly status check step environment must be the exact allowlist of required job results"
        )
    expected_env = {
        "EXPECTED_SCOPE": "${{ github.event_name == 'workflow_dispatch' && inputs.scope || github.event.schedule == '0 18 * * 0' && 'full' || 'smoke' }}",
        "PLAN_RESULT": "${{ needs.plan.result }}",
        "RESULTS": "${{ join(needs.*.result, ',') }}",
        "SCOPE": "${{ needs.plan.outputs.scope }}",
        "DETEKT_RESULT": "${{ needs.detekt.result }}",
        "TEST_CORE_RESULT": "${{ needs.test-core.result }}",
        "TEST_DATA_RESULT": "${{ needs.test-data.result }}",
        "TEST_SPRING_DOCKER_RESULT": "${{ needs.test-spring-docker.result }}",
        "TESTCONTAINERS_RESULT": "${{ needs.test-testcontainers.result }}",
        "TESTCONTAINERS_SPRING_RESULT": "${{ needs.test-testcontainers-spring.result }}",
        "X64_GATE_RESULT": "${{ needs.test-testcontainers-image-gate.result }}",
        "COVERAGE_REPORT_RESULT": "${{ needs.coverage-report.result }}",
    }
    if any(
        _step_env_value(status_step, name) != value
        for name, value in expected_env.items()
    ):
        errors.append(
            "Nightly status must bind its result checks to direct job outputs"
        )
    if _step_run_script(status_step) != list(EXPECTED_NIGHTLY_STATUS_SCRIPT):
        errors.append(
            "Nightly status must fail when any scope-required job is unsuccessful"
        )

    bridge_job = _job_block(nightly_text, "test-testcontainers-spring")
    bridge_test_step = _named_step_block(
        bridge_job, "Test Testcontainers Spring bridge"
    )
    if _job_values(bridge_job, "continue-on-error"):
        errors.append(
            "Nightly Testcontainers Spring bridge job must propagate failures"
        )
    if (
        not bridge_test_step
        or _step_values(bridge_test_step, "shell") != ["bash"]
        or _step_values(bridge_test_step, "if")
        or _step_values(bridge_test_step, "continue-on-error")
        or _step_environment_keys(bridge_test_step) != {"GRADLE_OPTS"}
        or _step_env_value(bridge_test_step, "GRADLE_OPTS")
        != "-Dorg.gradle.daemon=false"
        or _step_run_script(bridge_test_step)
        != ["./gradlew :bluetape4k-testcontainers-spring:test --no-configuration-cache"]
    ):
        errors.append(
            "Nightly Testcontainers Spring bridge test step must run the exact command directly and propagate failures"
        )
    return errors


def validate_mongodb_image_gate_manifest(manifest_text: str) -> list[str]:
    """Return manifest contract violations for the MongoDB image-gate family."""
    try:
        payload = json.loads(manifest_text)
    except json.JSONDecodeError:
        return ["MongoDB image-gate manifest must be valid JSON"]
    if not isinstance(payload, dict) or not isinstance(payload.get("families"), list):
        return ["MongoDB image-gate manifest must define a families list"]

    families = [
        family
        for family in payload["families"]
        if isinstance(family, dict) and family.get("id") == "mongo-d-b"
    ]
    if len(families) != 1:
        return ["MongoDB image-gate manifest must contain exactly one mongo-d-b family"]
    family = families[0]
    expected = {
        "server": "MongoDBServer",
        "image": "mongo",
        "tag": "8.0.28",
        "testPattern": "io.bluetape4k.testcontainers.storage.MongoDBServerTest",
        "workload": "io.bluetape4k.testcontainers.storage.MongoDBServerTest:representative_test",
    }
    return [
        f"MongoDB image-gate manifest {key} must remain {value!r}"
        for key, value in expected.items()
        if family.get(key) != value
    ]


def _has_mongodb_test_invocation(script: list[str]) -> bool:
    active_lines = [line for line in script if not line.lstrip().startswith("#")]
    return (
        re.search(
            r"(?<![A-Za-z0-9_-]):bluetape4k-(?:spring-boot-)?mongodb:test"
            r"(?![A-Za-z0-9_-])",
            " ".join(active_lines),
        )
        is not None
    )


def _mongodb_test_invocation_errors(
    workflow_text: str,
    data_job_name: str,
    spring_boot_job_name: str,
    data_expected_runner: str = "ubuntu-24.04",
) -> list[str]:
    errors = []
    expected_spring_runner = (
        "${{ matrix.group == 'batch-nosql' && 'ubuntu-24.04' || 'ubuntu-latest' }}"
    )
    batch_nosql_condition = "${{ matrix.group == 'batch-nosql' }}"
    for job_name, job in _job_blocks(workflow_text):
        runner_values = _job_values(job, "runs-on")
        for step in _step_blocks(job):
            scripts = (
                _step_run_script(step),
                _step_command_script(step),
            )
            if not any(
                script is not None and _has_mongodb_test_invocation(script)
                for script in scripts
            ):
                continue

            data_job_is_compatible = job_name == data_job_name and runner_values == [
                data_expected_runner
            ]
            spring_step_is_compatible = (
                job_name == spring_boot_job_name
                and runner_values == [expected_spring_runner]
                and _step_values(step, "if") == [batch_nosql_condition]
            )
            if not data_job_is_compatible and not spring_step_is_compatible:
                errors.append(
                    f"MongoDB test invocation in {job_name} must run only on a compatible runner"
                )
    return errors


def validate_nightly_mongodb_workflow(
    nightly_text: str, image_gate_manifest_text: str | None = None
) -> list[str]:
    """Return MongoDB runner contract violations in the Nightly workflow."""
    errors: list[str] = []
    if YAML_ESCAPED_KEY_RE.search(nightly_text):
        errors.append("Nightly workflow must not use escaped YAML mapping keys")
    if re.search(
        r"(?<![A-Za-z0-9_-])(?:defaults|\"defaults\"|'defaults')\s*:",
        nightly_text,
    ):
        errors.append("Nightly workflow must not define custom run shell defaults")
    data_job = _job_block(nightly_text, "test-data")
    nightly_execution_if = "${{ needs.plan.outputs.run_standard == 'true' }}"
    errors.extend(
        f"Nightly Test/Data MongoDB job {error}"
        for error in _mongodb_test_job_guard_errors(data_job, nightly_execution_if)
    )
    expected_data_runner = (
        "${{ matrix.group == 'nosql' && 'ubuntu-24.04' || 'ubuntu-latest' }}"
    )
    if _job_values(data_job, "runs-on") != [expected_data_runner]:
        errors.append(
            "Nightly MongoDB tests in the Test/Data nosql group must use a compatible runner"
        )

    spring_boot_job = _job_block(nightly_text, "test-spring-docker")
    errors.extend(
        f"Nightly Spring Boot MongoDB job {error}"
        for error in _mongodb_test_job_guard_errors(
            spring_boot_job, nightly_execution_if
        )
    )
    expected_spring_boot_runner = (
        "${{ matrix.group == 'batch-nosql' && 'ubuntu-24.04' || 'ubuntu-latest' }}"
    )
    if _job_values(spring_boot_job, "runs-on") != [expected_spring_boot_runner]:
        errors.append(
            "Nightly MongoDB container tests in Spring Boot batch-nosql must use a compatible runner"
        )
    errors.extend(
        _mongodb_test_environment_errors(
            nightly_text,
            (
                (
                    "test-data",
                    "Test data modules (${{ matrix.group }})",
                    "${{ matrix.test_tasks }}",
                ),
                (
                    "test-spring-docker",
                    "Test spring-boot modules (${{ matrix.group }})",
                    "${{ matrix.test_tasks }}",
                ),
            ),
        )
    )

    data_mongodb_groups = [
        group_name
        for group_name, group_block in _matrix_group_blocks(data_job)
        if ":bluetape4k-mongodb:test" in _matrix_group_test_tasks(group_block)
    ]
    if data_mongodb_groups != ["nosql"]:
        errors.append("Nightly MongoDB test task must remain in the nosql group")
    for group_name, group_block in _matrix_group_blocks(data_job):
        if group_name == "nosql":
            errors.extend(
                f"Nightly Test/Data MongoDB matrix {error}"
                for error in _matrix_group_test_task_errors(group_block)
            )

    spring_mongodb_groups = [
        group_name
        for group_name, group_block in _matrix_group_blocks(spring_boot_job)
        if ":bluetape4k-spring-boot-mongodb:test"
        in _matrix_group_test_tasks(group_block)
    ]
    if spring_mongodb_groups != ["batch-nosql"]:
        errors.append(
            "Nightly Spring Boot MongoDB test task must remain in batch-nosql"
        )
    for group_name, group_block in _matrix_group_blocks(spring_boot_job):
        if group_name == "batch-nosql":
            errors.extend(
                f"Nightly Spring Boot MongoDB matrix {error}"
                for error in _matrix_group_test_task_errors(group_block)
            )

    errors.extend(
        _mongodb_test_invocation_errors(
            nightly_text,
            "test-data",
            "test-spring-docker",
            data_expected_runner=expected_data_runner,
        )
    )
    errors.extend(
        f"Nightly Test/Data runner diagnostics {error}"
        for error in _runner_diagnostics_step_errors(
            data_job, "${{ matrix.group == 'nosql' }}"
        )
    )
    errors.extend(
        f"Nightly Spring Boot runner diagnostics {error}"
        for error in _runner_diagnostics_step_errors(
            spring_boot_job, "${{ matrix.group == 'batch-nosql' }}"
        )
    )

    for job, test_step_name, label in (
        (data_job, "Test data modules (${{ matrix.group }})", "Nightly Test/Data"),
        (
            spring_boot_job,
            "Test spring-boot modules (${{ matrix.group }})",
            "Nightly Spring Boot",
        ),
    ):
        errors.extend(
            f"{label} MongoDB test step {error}"
            for error in _matrix_test_step_errors(job, test_step_name)
        )
        diagnostic_index = _named_step_index(job, "Report runner image and kernel")
        test_index = _named_step_index(job, test_step_name)
        if (
            diagnostic_index is not None
            and test_index is not None
            and diagnostic_index >= test_index
        ):
            errors.append(f"{label} runner diagnostics must precede the test step")

    testcontainers_job = _job_block(nightly_text, "test-testcontainers")
    expected_testcontainers_runner = (
        "${{ matrix.group == 'storage-cache' && 'ubuntu-24.04' || 'ubuntu-latest' }}"
    )
    if _job_values(testcontainers_job, "runs-on") != [expected_testcontainers_runner]:
        errors.append(
            "Nightly test-testcontainers storage-cache MongoDB tests must use a compatible runner"
        )
    if _job_values(testcontainers_job, "continue-on-error"):
        errors.append("Nightly test-testcontainers MongoDB job must propagate failures")
    if _job_values(testcontainers_job, "if"):
        errors.append(
            "Nightly test-testcontainers job must not be conditionally skipped"
        )
    if (
        re.search(
            r"(?m)^      matrix: \$\{\{ fromJson\(needs\.plan\.outputs\.testcontainers_matrix\) }}$",
            testcontainers_job,
        )
        is None
    ):
        errors.append(
            "Nightly test-testcontainers must use the planned Testcontainers matrix"
        )

    errors.extend(_testcontainers_plan_contract_errors(nightly_text))
    errors.extend(_testcontainers_plan_output_errors(nightly_text))

    testcontainers_diagnostics_errors = _runner_diagnostics_step_errors(
        testcontainers_job, "${{ matrix.group == 'storage-cache' }}"
    )
    errors.extend(
        f"Nightly test-testcontainers runner diagnostics {error}"
        for error in testcontainers_diagnostics_errors
    )
    testcontainers_step_name = "Test testcontainers (${{ matrix.group }})"
    testcontainers_step = _named_step_block(
        testcontainers_job, testcontainers_step_name
    )
    if not testcontainers_step:
        errors.append(
            "Nightly test-testcontainers MongoDB test step is missing or duplicated"
        )
    else:
        errors.extend(
            f"Nightly test-testcontainers MongoDB test step {error}"
            for error in _mongodb_test_step_guard_errors(testcontainers_step)
        )
        expected_command = [
            'read -ra patterns <<< "$TEST_PATTERNS"',
            "tests_args=()",
            'for pattern in "${patterns[@]}"; do',
            'tests_args+=(--tests "${pattern}")',
            "done",
            'if [[ "$NEEDS_MOCK_SERVERS" != "true" ]]; then',
            "tests_args+=(",
            "-x :bluetape4k-mock-web-server:jibDockerBuild",
            "-x :bluetape4k-mock-webflux-server:jibDockerBuild",
            ")",
            "fi",
            'timeout --kill-after=60s "${TEST_TIMEOUT_MINUTES}m" \\',
            './gradlew :bluetape4k-testcontainers:test "${tests_args[@]}" --no-configuration-cache',
        ]
        if (
            _step_values(testcontainers_step, "uses") != ["nick-fields/retry@v4"]
            or _step_with_input_keys(testcontainers_step)
            != ["timeout_minutes", "max_attempts", "shell", "command"]
            or _indented_values(
                "\n".join(_step_with_block(testcontainers_step) or []),
                "timeout_minutes",
                10,
            )
            != ["${{ matrix.timeout_minutes }}"]
            or _indented_values(
                "\n".join(_step_with_block(testcontainers_step) or []),
                "max_attempts",
                10,
            )
            != ["${{ matrix.max_attempts }}"]
            or _indented_values(
                "\n".join(_step_with_block(testcontainers_step) or []),
                "shell",
                10,
            )
            != ["bash"]
            or _step_command_script(testcontainers_step) != expected_command
        ):
            errors.append(
                "Nightly test-testcontainers MongoDB test step must execute the planned test patterns through the expected retry command"
            )
        if _step_environment_keys(testcontainers_step) != {
            "GRADLE_OPTS",
            "TEST_PATTERNS",
            "TEST_TIMEOUT_MINUTES",
            "NEEDS_MOCK_SERVERS",
        } or any(
            _step_env_value(testcontainers_step, name) != value
            for name, value in (
                ("GRADLE_OPTS", "-Dorg.gradle.daemon=false"),
                ("TEST_PATTERNS", "${{ matrix.test_patterns }}"),
                ("TEST_TIMEOUT_MINUTES", "${{ matrix.timeout_minutes }}"),
                ("NEEDS_MOCK_SERVERS", "${{ matrix.needs_mock_servers }}"),
            )
        ):
            errors.append(
                "Nightly test-testcontainers MongoDB test step environment must stay pinned to the planned matrix"
            )
    diagnostic_index = _named_step_index(
        testcontainers_job, "Report runner image and kernel"
    )
    testcontainers_index = _named_step_index(
        testcontainers_job, testcontainers_step_name
    )
    if (
        diagnostic_index is not None
        and testcontainers_index is not None
        and diagnostic_index >= testcontainers_index
    ):
        errors.append(
            "Nightly test-testcontainers runner diagnostics must precede the test step"
        )

    image_gate_job_name = "test-testcontainers-image-gate-shard"
    image_gate_aggregate_job_name = "test-testcontainers-image-gate"
    image_gate_job = _job_block(nightly_text, image_gate_job_name)
    if _job_values(image_gate_job, "runs-on") != ["ubuntu-24.04"]:
        errors.append(
            "Nightly MongoDB image-gate shard must use the compatible ubuntu-24.04 runner"
        )
    if _job_values(image_gate_job, "if") != [
        "${{ needs.plan.outputs.scope == 'full' }}"
    ]:
        errors.append(
            "Nightly MongoDB image-gate shard must remain scoped to full runs"
        )
    if _job_values(image_gate_job, "continue-on-error"):
        errors.append("Nightly MongoDB image-gate job must propagate failures")
    if _indented_values(image_gate_job, "shard", 8) != ["[0, 1, 2, 3]"]:
        errors.append(
            "Nightly MongoDB image-gate shard matrix must include exactly shards 0 through 3"
        )
    image_gate_aggregate_job = _job_block(nightly_text, image_gate_aggregate_job_name)
    if _job_values(image_gate_aggregate_job, "runs-on") != ["ubuntu-24.04"]:
        errors.append("Nightly image-gate aggregate must use ubuntu-24.04")
    if _job_values(image_gate_aggregate_job, "if") != [
        "${{ always() && needs.plan.outputs.scope == 'full' }}"
    ]:
        errors.append("Nightly image-gate aggregate must run for every full scope")
    if _job_values(image_gate_aggregate_job, "needs") != [
        "[build, plan, test-testcontainers-image-gate-shard]"
    ]:
        errors.append("Nightly image-gate aggregate must depend on all shard results")
    if _job_values(image_gate_aggregate_job, "continue-on-error"):
        errors.append("Nightly image-gate aggregate must propagate failures")
    aggregate_step_name = "Aggregate and verify image gate evidence"
    aggregate_step = _named_step_block(image_gate_aggregate_job, aggregate_step_name)
    expected_aggregate_script = [
        "set +e",
        "python3 scripts/aggregate_testcontainers_image_gate.py \\",
        "--input-dir shard-artifacts \\",
        "--output-dir build/reports/testcontainers-image-gate \\",
        "--manifest scripts/testcontainers_image_gate_manifest.json \\",
        "--expected-shards 4",
        "aggregate_exit=$?",
        "set -e",
        'test "$SHARD_RESULT" = success',
        'test "$aggregate_exit" -eq 0',
    ]
    if (
        not aggregate_step
        or _step_values(aggregate_step, "if") != ["always()"]
        or _step_values(aggregate_step, "shell") != ["bash"]
        or _step_values(aggregate_step, "continue-on-error")
        or _step_environment_keys(aggregate_step) != {"SHARD_RESULT"}
        or _step_env_value(aggregate_step, "SHARD_RESULT")
        != "${{ needs.test-testcontainers-image-gate-shard.result }}"
        or _step_run_script(aggregate_step) != expected_aggregate_script
    ):
        errors.append(
            "Nightly image-gate aggregate must always verify shard status and aggregate exit"
        )
    aggregate_step_names = (
        "Download image gate shard evidence",
        aggregate_step_name,
        "Publish aggregate image gate summary",
        "Stage exact aggregate image gate evidence",
        "Upload aggregate image gate evidence",
    )
    aggregate_step_positions = [
        image_gate_aggregate_job.find(f"      - name: {name}\n")
        for name in aggregate_step_names
    ]
    if any(
        position < 0 for position in aggregate_step_positions
    ) or aggregate_step_positions != sorted(aggregate_step_positions):
        errors.append(
            "Nightly image-gate aggregate must run before publishing and artifact staging"
        )
    unsafe_names = _unsafe_execution_environment_names(
        nightly_text,
        (
            "plan",
            "test-testcontainers",
            image_gate_job_name,
            image_gate_aggregate_job_name,
            "nightly-status",
        ),
    )
    unsafe_names = [name for name in unsafe_names if name.upper() != "GRADLE_OPTS"]
    if unsafe_names:
        errors.append(
            "Nightly MongoDB protected workflow scopes must not define unsafe execution environment variable(s): "
            + ", ".join(unsafe_names)
        )
    if _has_environment_mapping_alias_or_merge(nightly_text):
        errors.append(
            "Nightly workflow must not use YAML anchors and aliases in environment mappings"
        )
    errors.extend(
        f"Nightly image-gate runner diagnostics {error}"
        for error in _runner_diagnostics_step_errors(image_gate_job, None)
    )
    image_gate_step_name = "Run image family gate shard sequentially"
    image_gate_step = _named_step_block(image_gate_job, image_gate_step_name)
    expected_image_gate_command = (
        "python3 scripts/run_testcontainers_image_gate.py --scope full "
        "--shard-index ${{ matrix.shard }} --shard-count 4 "
        "--report-dir build/reports/testcontainers-image-gate "
        "--default-platform-id amd64 --max-attempts 2 --pull-timeout-seconds 60 "
        "--timeout-minutes 4 --diagnostic-timeout-seconds 30 --job-budget-minutes 120"
    )
    if not image_gate_step:
        errors.append(
            "Nightly MongoDB image-gate execution step is missing or duplicated"
        )
    elif (
        _step_run_script(image_gate_step) != [expected_image_gate_command]
        or _step_values(image_gate_step, "shell") != ["bash"]
        or _indented_values(image_gate_step, "if", 8)
        or _indented_values(image_gate_step, "continue-on-error", 8)
        or _step_environment_keys(image_gate_step) != {"DOCKER_AUTH_CONFIG"}
        or _step_env_value(image_gate_step, "DOCKER_AUTH_CONFIG")
        != "${{ secrets.TESTCONTAINERS_DOCKER_AUTH_CONFIG }}"
    ):
        errors.append(
            "Nightly MongoDB image-gate execution must run full scope and propagate failures"
        )
    image_gate_diagnostics_index = _named_step_index(
        image_gate_job, "Report runner image and kernel"
    )
    image_gate_execution_index = _named_step_index(image_gate_job, image_gate_step_name)
    if (
        image_gate_diagnostics_index is not None
        and image_gate_execution_index is not None
        and image_gate_diagnostics_index >= image_gate_execution_index
    ):
        errors.append(
            "Nightly image-gate runner diagnostics must precede the image gate"
        )

    if image_gate_manifest_text is not None:
        errors.extend(validate_mongodb_image_gate_manifest(image_gate_manifest_text))
    errors.extend(_nightly_status_contract_errors(nightly_text))
    return errors


def _has_exact_governance_gate(status_job: str) -> bool:
    check_step = _named_step_block(status_job, "Check all jobs")
    return _step_run_script(check_step) == list(EXPECTED_CI_STATUS_SCRIPT)


def validate_workflows(ci_text: str, submission_text: str) -> list[str]:
    """Return contract violations in the Projects GitHub workflows."""
    errors: list[str] = []
    if YAML_ESCAPED_KEY_RE.search(ci_text):
        errors.append("CI workflow must not use escaped YAML mapping keys")
    if YAML_ESCAPED_ACTION_VALUE_RE.search(
        ci_text
    ) or YAML_ESCAPED_ACTION_VALUE_RE.search(submission_text):
        errors.append("workflow action values must not use escaped YAML scalars")
    if re.search(
        r"(?<![A-Za-z0-9_-])(?:defaults|\"defaults\"|'defaults')\s*:", ci_text
    ):
        errors.append(
            "CI workflow must not define run defaults that can replace the shell"
        )
    if re.search(
        r"(?<![A-Za-z0-9_-])(?:defaults|\"defaults\"|'defaults')\s*:",
        submission_text,
    ):
        errors.append(
            "dependency-submission workflow must not define run defaults that can replace the shell"
        )
    if (
        YAML_ACTION_ALIAS_RE.search(ci_text)
        or re.search(r"(?m)^\s*<<\s*:", ci_text)
        or _has_environment_mapping_alias_or_merge(ci_text)
    ):
        errors.append(
            "CI workflow must not use YAML anchors and aliases for Actions steps or environment mappings"
        )
    if _has_environment_mapping_alias_or_merge(submission_text):
        errors.append(
            "dependency-submission workflow must not use YAML anchors and aliases in environment mappings"
        )
    on_block = _top_level_block(ci_text, "on")
    pull_request_match = re.search(r"(?m)^  pull_request:\s*$", on_block)
    if pull_request_match is None:
        errors.append("CI must define a pull_request trigger")
    else:
        following = re.search(
            r"(?m)^  [A-Za-z_][A-Za-z0-9_-]*:\s*$",
            on_block[pull_request_match.end() :],
        )
        pull_request_trigger = on_block[
            pull_request_match.start() : pull_request_match.end()
            + (following.start() if following else len(on_block))
        ]
        if re.search(r"(?m)^\s+paths(?:-ignore)?:\s*$", pull_request_trigger):
            errors.append(
                "CI pull_request trigger must not use path filters that can skip the required status check"
            )
    expected_sha_values = _workflow_env_values(ci_text, "PROJECTS_EXPECTED_SHA")
    if len(expected_sha_values) != 1:
        errors.append("CI must define exactly one top-level PROJECTS_EXPECTED_SHA")
    elif _workflow_env(ci_text, "PROJECTS_EXPECTED_SHA") != (
        "${{ github.event.pull_request.head.sha || github.sha }}"
    ):
        errors.append("CI must resolve the Projects PR head instead of its merge SHA")
    if _has_job_or_step_env_override(ci_text, "PROJECTS_EXPECTED_SHA"):
        errors.append("PROJECTS_EXPECTED_SHA must not be overridden in job or step env")
    first_party_repositories = {
        PROJECTS_REPOSITORY,
        "${{ github.repository }}",
    }
    expected_external = Counter(EXPECTED_EXTERNAL_CHECKOUTS)
    actual_external: list[tuple[str, str, str | None, str | None]] = []
    projects_checkout_count = 0
    merge_result_checkout_count = 0
    for job_name, checkout in _checkout_steps(ci_text):
        if len(_step_values(checkout, "uses")) > 1:
            errors.append("checkout step must not define duplicate uses keys")
            continue
        if any(
            len(_step_values(checkout, name)) > 1
            for name in ("repository", "ref", "path")
        ):
            errors.append(
                "checkout step must not define duplicate repository/ref/path keys"
            )
            continue
        repository = _step_value(checkout, "repository")
        ref = _step_value(checkout, "ref")
        path = _step_value(checkout, "path")

        if job_name == MERGE_RESULT_JOB:
            merge_result_checkout_count += 1
            if repository is not None or ref != "${{ github.sha }}" or path is not None:
                errors.append(
                    "PR merge-result checkout must use the current Projects repository and github.sha"
                )
            continue

        if repository is None or repository in first_party_repositories:
            projects_checkout_count += 1
            if ref != "${{ env.PROJECTS_EXPECTED_SHA }}" or path is not None:
                errors.append(
                    "every first-party Projects checkout in CI must pin PROJECTS_EXPECTED_SHA"
                )
            continue

        checkout_identity = (job_name, repository, ref, path)
        if expected_external[checkout_identity] == 0:
            errors.append(
                f"checkout is not an allowlisted external checkout with an immutable ref and path: {checkout_identity!r}"
            )
        actual_external.append(checkout_identity)

    if projects_checkout_count != PROJECTS_CHECKOUT_COUNT:
        errors.append(
            "CI must contain exactly 24 first-party Projects head checkouts; "
            f"found {projects_checkout_count}"
        )
    if Counter(actual_external) != expected_external:
        errors.append(
            "CI cross-repository checkout inventory must match the explicit allowlist"
        )
    if merge_result_checkout_count != 1:
        errors.append("CI must contain one exact github.sha merge-result checkout")

    changes_job = _job_block(ci_text, "changes")
    if not re.search(
        r"(?m)^    permissions:\n      contents: read\n      pull-requests: read$",
        changes_job,
    ):
        errors.append(
            "changes job must grant contents: read and pull-requests: read to paths-filter"
        )

    data_job = _job_block(ci_text, "test-data")
    ci_data_execution_if = (
        "${{ needs.changes.outputs.data == 'true' || "
        "needs.changes.outputs.shared == 'true' || "
        "github.event_name == 'workflow_dispatch' }}"
    )
    errors.extend(
        f"Test/Data MongoDB job {error}"
        for error in _mongodb_test_job_guard_errors(data_job, ci_data_execution_if)
    )
    if _job_values(data_job, "runs-on") != ["ubuntu-24.04"]:
        errors.append(
            "MongoDB container tests in Test/Data must use a compatible runner"
        )
    spring_boot_job = _job_block(ci_text, "test-spring-boot")
    errors.extend(
        _mongodb_test_environment_errors(
            ci_text,
            (
                ("test-data", "Test data modules", None),
                (
                    "test-spring-boot",
                    "Test Spring Boot modules (${{ matrix.group }})",
                    "${{ matrix.test_tasks }}",
                ),
            ),
        )
    )
    ci_spring_execution_if = (
        "${{ needs.changes.outputs['spring-boot'] == 'true' || "
        "needs.changes.outputs.shared == 'true' || "
        "github.event_name == 'workflow_dispatch' }}"
    )
    errors.extend(
        f"Spring Boot MongoDB job {error}"
        for error in _mongodb_test_job_guard_errors(
            spring_boot_job, ci_spring_execution_if
        )
    )
    expected_spring_boot_runner = (
        "${{ matrix.group == 'batch-nosql' && 'ubuntu-24.04' || 'ubuntu-latest' }}"
    )
    if _job_values(spring_boot_job, "runs-on") != [expected_spring_boot_runner]:
        errors.append(
            "MongoDB container tests in Spring Boot batch-nosql must use a compatible runner"
        )
    mongodb_test_groups = [
        group_name
        for group_name, group_block in _matrix_group_blocks(spring_boot_job)
        if ":bluetape4k-spring-boot-mongodb:test"
        in _matrix_group_test_tasks(group_block)
    ]
    if mongodb_test_groups != ["batch-nosql"]:
        errors.append("Spring Boot MongoDB test task must remain in batch-nosql")
    for group_name, group_block in _matrix_group_blocks(spring_boot_job):
        if group_name == "batch-nosql":
            errors.extend(
                f"Spring Boot MongoDB matrix {error}"
                for error in _matrix_group_test_task_errors(group_block)
            )
    errors.extend(
        f"Spring Boot MongoDB test step {error}"
        for error in _spring_boot_test_step_errors(spring_boot_job)
    )
    errors.extend(
        f"Test/Data MongoDB test step {error}"
        for error in _ci_data_test_step_errors(data_job)
    )
    errors.extend(
        _mongodb_test_invocation_errors(ci_text, "test-data", "test-spring-boot")
    )
    errors.extend(
        f"Test/Data runner diagnostics {error}"
        for error in _runner_diagnostics_step_errors(data_job, None)
    )
    errors.extend(
        f"Spring Boot runner diagnostics {error}"
        for error in _runner_diagnostics_step_errors(
            spring_boot_job, "${{ matrix.group == 'batch-nosql' }}"
        )
    )
    data_diagnostics_index = _named_step_index(
        data_job, "Report runner image and kernel"
    )
    data_test_index = _named_step_index(data_job, "Test data modules")
    if (
        data_diagnostics_index is not None
        and data_test_index is not None
        and data_diagnostics_index >= data_test_index
    ):
        errors.append("Test/Data runner diagnostics must precede the test step")
    spring_diagnostics_index = _named_step_index(
        spring_boot_job, "Report runner image and kernel"
    )
    spring_test_index = _named_step_index(
        spring_boot_job, "Test Spring Boot modules (${{ matrix.group }})"
    )
    if (
        spring_diagnostics_index is not None
        and spring_test_index is not None
        and spring_diagnostics_index >= spring_test_index
    ):
        errors.append("Spring Boot runner diagnostics must precede the test step")

    ref = _workflow_env(ci_text, "BLUETAPE4K_DEPENDENCIES_CATALOG_REF")
    tag = _workflow_env(ci_text, "BLUETAPE4K_DEPENDENCIES_CATALOG_TAG")
    tag_object_sha = _workflow_env(
        ci_text, "BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA"
    )
    expected_sha = _workflow_env(ci_text, "BLUETAPE4K_DEPENDENCIES_CATALOG_SHA")
    manual_ref = _workflow_env(ci_text, "BLUETAPE4K_MANUAL_REF")
    if manual_ref is None or FULL_SHA_RE.fullmatch(manual_ref) is None:
        errors.append("manual REF must be a full commit SHA")
    if ref is None or FULL_SHA_RE.fullmatch(ref) is None:
        errors.append("catalog REF must be a full commit SHA")
    if tag is None or CATALOG_TAG_RE.fullmatch(tag) is None:
        errors.append("catalog TAG must be an immutable catalog tag")
    if tag_object_sha is None or FULL_SHA_RE.fullmatch(tag_object_sha) is None:
        errors.append("catalog tag object SHA must be a full 40-character hash")
    if expected_sha is None or FULL_SHA_RE.fullmatch(expected_sha) is None:
        errors.append("catalog SHA must be a full 40-character commit hash")
    elif ref is not None and ref != expected_sha:
        errors.append("catalog REF must equal the expected catalog SHA")

    governance = _job_block(ci_text, "catalog-governance")
    if not governance:
        errors.append("CI must define catalog-governance")
    else:
        if _indented_values(governance, "continue-on-error", 4):
            errors.append("catalog-governance must not use job-level continue-on-error")
        if not re.search(
            r"(?m)^      - uses: actions/checkout@v7\n"
            r"        with:\n"
            r"          ref: \$\{\{ env\.PROJECTS_EXPECTED_SHA }}\n"
            r"          fetch-depth: 0$",
            governance,
        ):
            errors.append("catalog-governance must check out the exact Projects PR head")
        required_governance = (
            'projects_head_sha="$(git rev-parse HEAD)"',
            'test "$projects_head_sha" = "$PROJECTS_EXPECTED_SHA"',
            "repository: bluetape4k/bluetape4k-dependencies",
            "ref: ${{ env.BLUETAPE4K_DEPENDENCIES_CATALOG_TAG }}",
            "path: .gradle/catalog-source/bluetape4k-dependencies",
            (
                'tag_object_sha="$(git -C .gradle/catalog-source/bluetape4k-dependencies '
                'rev-parse "${BLUETAPE4K_DEPENDENCIES_CATALOG_TAG}^{tag}")"'
            ),
            (
                'test "$tag_object_sha" = '
                '"$BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA"'
            ),
            "python-version: '3.13'",
            "python3 --version",
            "git remote set-url origin git@github.com:bluetape4k/bluetape4k-projects.git",
            (
                "git -C .gradle/catalog-source/bluetape4k-dependencies remote set-url origin "
                "git@github.com:bluetape4k/bluetape4k-dependencies.git"
            ),
            "git -C .gradle/catalog-source/bluetape4k-dependencies rev-parse HEAD",
            (
                "git -C .gradle/catalog-source/bluetape4k-dependencies rev-parse "
                '"${BLUETAPE4K_DEPENDENCIES_CATALOG_TAG}^{commit}"'
            ),
            '"$BLUETAPE4K_DEPENDENCIES_CATALOG_SHA"',
            'test "$head_sha" = "$BLUETAPE4K_DEPENDENCIES_CATALOG_SHA"',
            'test "$peeled_sha" = "$BLUETAPE4K_DEPENDENCIES_CATALOG_SHA"',
            "dependabot_core_commit",
            "repository: dependabot/dependabot-core",
            "ref: ${{ steps.dependabot-pin.outputs.commit }}",
            "path: .gradle/dependabot-core",
            "ruby/setup-ruby@26e5b7d197ebf60545ee9667bcbace0be11ccaec # v1",
            "DEPENDABOT_CORE_ROOT: ${{ github.workspace }}/.gradle/dependabot-core",
            "OUTPUT_PATH: ${{ github.workspace }}/.gradle/issue-1832-evidence/parser-receipt.json",
            "dependabot-parser-contract.rb",
            "build-project-map",
            "project-dependabot-catalog.py",
            'git switch -c ci-project-catalog "$PROJECTS_EXPECTED_SHA"',
            "--check",
            "--catalog-filename libs.project.versions.toml",
            "tests.test_catalog_workflows",
        )
        for required in required_governance:
            if required not in governance:
                errors.append(f"catalog-governance is missing {required!r}")
        if (
            'test "$tag_object_sha" = '
            '"$BLUETAPE4K_DEPENDENCIES_CATALOG_TAG_OBJECT_SHA"'
            not in governance
        ):
            errors.append("catalog-governance must verify the exact catalog tag object SHA")
        pin_step = _named_step_block(governance, "Read pinned Dependabot Core commit")
        pin_id_steps = [
            step
            for step in _step_blocks(governance)
            if "dependabot-pin" in _step_values(step, "id")
        ]
        if (
            len(pin_id_steps) != 1
            or _step_value(pin_step, "id") != "dependabot-pin"
            or len(_step_values(pin_step, "id")) != 1
        ):
            errors.append(
                "catalog-governance must define one pinned Dependabot Core commit step with id dependabot-pin"
            )
        if _step_run_script(pin_step) != list(EXPECTED_DEPENDABOT_CORE_PIN_SCRIPT):
            errors.append(
                "catalog-governance pinned Dependabot Core commit step must execute its exact script"
            )
        if _step_values(pin_step, "shell") != ["bash"]:
            errors.append(
                "catalog-governance pinned Dependabot Core commit step must use the exact fail-closed bash shell"
            )
        if _step_values(pin_step, "working-directory"):
            errors.append(
                "catalog-governance pinned Dependabot Core commit step must read the mapping from the workspace root"
            )
        if _indented_values(pin_step, "if", 8) or _indented_values(
            pin_step, "continue-on-error", 8
        ):
            errors.append(
                "catalog-governance pinned Dependabot Core commit step must run and fail closed"
            )
        if _indented_values(pin_step, "env", 8):
            errors.append(
                "catalog-governance pinned Dependabot Core commit step must not override its execution environment"
            )
        parser_step = _named_step_block(
            governance, "Run pinned Dependabot Gradle parser contract"
        )
        catalog_step = _named_step_block(
            governance, "Verify generated catalog and parser contract"
        )
        parser_step_index = _named_step_index(
            governance, "Run pinned Dependabot Gradle parser contract"
        )
        catalog_step_index = _named_step_index(
            governance, "Verify generated catalog and parser contract"
        )
        if (
            parser_step_index is None
            or catalog_step_index is None
            or parser_step_index >= catalog_step_index
        ):
            errors.append(
                "catalog-governance must create the pinned parser receipt before catalog check"
            )
        if _step_run_script(parser_step) != list(
            EXPECTED_DEPENDABOT_PARSER_CONTRACT_SCRIPT
        ):
            errors.append(
                "catalog-governance parser contract step must execute the pinned parser script"
            )
        if _step_env_block(parser_step) != list(
            EXPECTED_DEPENDABOT_PARSER_CONTRACT_ENV
        ):
            errors.append(
                "catalog-governance parser contract step environment must pin the expected paths and values"
            )
        if _environment_mapping_key_count(ci_text, "BUNDLE_GEMFILE") != 1:
            errors.append(
                "CI must define BUNDLE_GEMFILE only in the pinned parser contract step environment"
            )
        if _step_values(parser_step, "shell"):
            errors.append(
                "catalog-governance parser contract step must not override the fail-closed shell"
            )
        if _indented_values(parser_step, "if", 8) or _indented_values(
            parser_step, "continue-on-error", 8
        ):
            errors.append(
                "catalog-governance parser contract step must run and fail closed"
            )
        catalog_script = _step_run_script(catalog_step)
        if (
            catalog_script != list(EXPECTED_CATALOG_VERIFICATION_SCRIPT)
            or _indented_values(catalog_step, "if", 8)
            or _indented_values(catalog_step, "continue-on-error", 8)
        ):
            errors.append(
                "catalog verification step must execute its exact script and fail closed"
            )
        if _step_values(catalog_step, "shell"):
            errors.append(
                "catalog verification step must not override the fail-closed shell"
            )

        evidence_verify_step = _named_step_block(
            governance, "Verify catalog provenance evidence"
        )
        if not evidence_verify_step:
            errors.append(
                "catalog-governance must verify all catalog provenance evidence files"
            )
        else:
            if _step_values(evidence_verify_step, "if") != ["always()"]:
                errors.append(
                    "catalog provenance evidence verification must run on success and failure"
                )
            if _step_values(evidence_verify_step, "shell") != ["bash"]:
                errors.append(
                    "catalog provenance evidence verification must use the bash shell"
                )
            if _step_run_script(evidence_verify_step) != list(
                EXPECTED_CATALOG_EVIDENCE_VERIFICATION_SCRIPT
            ):
                errors.append(
                    "catalog provenance evidence verification must require all four regular files"
                )
            if _indented_values(evidence_verify_step, "continue-on-error", 8):
                errors.append(
                    "catalog provenance evidence verification must fail closed"
                )

        evidence_upload_step = _named_step_block(
            governance, "Upload catalog provenance evidence"
        )
        if not evidence_upload_step:
            errors.append(
                "catalog-governance must upload catalog provenance evidence"
            )
        else:
            if _step_values(evidence_upload_step, "if") != ["always()"]:
                errors.append(
                    "catalog provenance evidence upload must run on success and failure"
                )
            if _step_values(evidence_upload_step, "uses") != [
                "actions/upload-artifact@v7"
            ]:
                errors.append(
                    "catalog provenance evidence must use actions/upload-artifact@v7"
                )
            if _step_with_input_keys(evidence_upload_step) != list(
                EXPECTED_CATALOG_EVIDENCE_ARTIFACT_INPUTS
            ):
                errors.append(
                    "catalog provenance evidence upload must use the complete artifact input set"
                )
            with_block = _step_with_block(evidence_upload_step) or []
            with_text = "\n".join(with_block)
            if "path: .gradle/issue-1832-evidence/**" not in with_text:
                errors.append(
                    "catalog provenance evidence artifact must upload the complete evidence directory"
                )
            if "if-no-files-found: warn" not in with_text:
                errors.append(
                    "catalog provenance evidence artifact must preserve partial failure evidence"
                )
        evidence_verify_index = _named_step_index(
            governance, "Verify catalog provenance evidence"
        )
        evidence_upload_index = _named_step_index(
            governance, "Upload catalog provenance evidence"
        )
        if (
            evidence_verify_index is None
            or evidence_upload_index is None
            or evidence_verify_index >= evidence_upload_index
        ):
            errors.append(
                "catalog provenance evidence must be verified before artifact upload"
            )

    unsafe_ci_environment = _unsafe_execution_environment_names(
        ci_text, ("catalog-governance", "ci-status")
    )
    if unsafe_ci_environment:
        errors.append(
            "CI protected execution scopes must not define unsafe execution environment variable(s): "
            + ", ".join(unsafe_ci_environment)
        )

    merge_result = _job_block(ci_text, MERGE_RESULT_JOB)
    if not merge_result:
        errors.append("CI must define the PR merge-result contract job")
    else:
        for name in MERGE_RESULT_UNSAFE_ENV:
            if _workflow_env_values(ci_text, name):
                errors.append(
                    f"CI workflow env must not override merge-result execution environment variable {name}"
                )
        if any(
            re.search(rf"(?m)^ {{4}}{_yaml_key_pattern('shell')}:\s*", defaults)
            for defaults in _top_level_blocks(ci_text, "defaults")
        ):
            errors.append(
                "CI workflow defaults must not override the merge-result execution environment shell"
            )
        required_merge_result = (
            "ref: ${{ github.sha }}",
            "python3 -m unittest tests.test_catalog_workflows -v",
            "python3 scripts/verify-catalog-workflows.py",
        )
        if _indented_values(merge_result, "if", 4) != [
            "${{ github.event_name == 'pull_request' }}"
        ]:
            errors.append(
                "PR merge-result job must have one exact pull_request condition"
            )
        if _indented_values(merge_result, "continue-on-error", 4):
            errors.append("PR merge-result job must not use continue-on-error")
        if _indented_values(merge_result, "env", 4) or _indented_values(
            merge_result, "defaults", 4
        ):
            errors.append(
                "PR merge-result job must not override the merge-result execution environment"
            )
        for required in required_merge_result:
            if required not in merge_result:
                errors.append(
                    f"PR merge-result contract job is missing {required!r}"
                )
        verify_merge_step = _named_step_block(
            merge_result,
            "Verify synthetic merge result contains the current PR commits",
        )
        merge_steps = _step_blocks(merge_result)
        first_step_is_checkout = (
            bool(merge_steps)
            and re.search(r"(?m)^      - uses: actions/checkout@v7$", merge_steps[0])
            is not None
        )
        if (
            not first_step_is_checkout
            or len(merge_steps) < 2
            or merge_steps[1] != verify_merge_step
        ):
            errors.append(
                "merge-result execution environment requires checkout followed immediately by its verification step"
            )
        if _step_values(verify_merge_step, "shell"):
            errors.append(
                "merge-result execution environment must use the default fail-closed shell in the named verification step"
            )
        if _indented_values(verify_merge_step, "if", 8) or _indented_values(
            verify_merge_step, "continue-on-error", 8
        ):
            errors.append(
                "named merge-result verification step must run unconditionally and fail the job"
            )
        verify_script = _step_run_script(verify_merge_step)
        required_verification_commands = (
            "set -euo pipefail",
            'merge_result_sha="$(git rev-parse HEAD)"',
            'test "$merge_result_sha" = "$GITHUB_SHA"',
            'git merge-base --is-ancestor "$PR_HEAD_SHA" "$merge_result_sha"',
            'git merge-base --is-ancestor "$PR_BASE_SHA" "$merge_result_sha"',
        )
        command_positions = (
            [verify_script.index(command) for command in required_verification_commands]
            if verify_script is not None
            and all(
                command in verify_script for command in required_verification_commands
            )
            else []
        )
        if (
            verify_script is None
            or len(command_positions) != len(required_verification_commands)
            or command_positions != sorted(command_positions)
            or len(set(command_positions)) != len(command_positions)
            or any(
                re.match(r"^(?:if|elif|then|else|fi|case|for|while|until)\b", line)
                for line in verify_script
            )
        ):
            errors.append(
                "PR merge-result ancestry checks must run in order as commands in the named merge-result verification step"
            )
        if verify_script != list(EXPECTED_MERGE_RESULT_VERIFY_SCRIPT):
            errors.append(
                "PR merge-result verification script must match the exact fail-closed allowlist"
            )
        expected_step_env = [
            "        env:",
            "          PR_HEAD_SHA: ${{ github.event.pull_request.head.sha }}",
            "          PR_BASE_SHA: ${{ github.event.pull_request.base.sha }}",
            "          PR_BASE_REF: ${{ github.base_ref }}",
        ]
        if _step_env_block(verify_merge_step) != expected_step_env:
            errors.append(
                "named merge-result verification step must use the exact merge-result execution environment"
            )
        verify_script_text = "\n".join(verify_script or [])
        if re.search(
            r"(?m)(?:^|[;&|]\s*)(?:export\s+|declare\s+(?:-x\s+)?|readonly\s+)?"
            r"PR_(?:HEAD|BASE)_(?:SHA|REF)(?:\s*\+?=|\s+)",
            verify_script_text,
        ) or re.search(
            r"(?m)^\s*unset\s+.*\bPR_(?:HEAD|BASE)_(?:SHA|REF)\b", verify_script_text
        ):
            errors.append(
                "PR_HEAD_SHA and PR_BASE_SHA must not be reassigned; PR_BASE_REF must not be reassigned"
            )
        if "GITHUB_ENV" in merge_result and re.search(
            r"\bPR_(?:HEAD|BASE)_(?:SHA|REF)\b", merge_result
        ):
            errors.append(
                "PR merge-result job must not write PR SHA values to GITHUB_ENV; PR_BASE_REF must not be written there"
            )
        for name, expected in (
            ("PR_HEAD_SHA", "${{ github.event.pull_request.head.sha }}"),
            ("PR_BASE_SHA", "${{ github.event.pull_request.base.sha }}"),
            ("PR_BASE_REF", "${{ github.base_ref }}"),
        ):
            if _step_env_value(verify_merge_step, name) != expected:
                errors.append(
                    f"PR merge-result verification step must set {name} to {expected!r}"
                )

    status_job = _job_block(ci_text, "ci-status")
    if not status_job or not _has_exact_governance_gate(status_job):
        errors.append("ci-status must fail unless catalog-governance result is success")
    if _indented_values(status_job, "continue-on-error", 4):
        errors.append("ci-status must not use job-level continue-on-error")
    if _indented_values(status_job, "if", 4) != ["always()"]:
        errors.append("ci-status must run for every event with if: always()")
    status_step = _named_step_block(status_job, "Check all jobs")
    if _indented_values(status_step, "if", 8) or _indented_values(
        status_step, "continue-on-error", 8
    ):
        errors.append("ci-status Check all jobs step must run and fail closed")
    if _step_values(status_step, "shell"):
        errors.append(
            "ci-status Check all jobs step must not override the fail-closed shell"
        )
    if _step_env_block(status_step) != list(EXPECTED_CI_STATUS_ENV):
        errors.append(
            "ci-status Check all jobs step must use the exact gate environment"
        )
    if (
        "test-merge-result" not in status_job
        or "MERGE_RESULT_RESULT: ${{ needs.test-merge-result.result }}" not in status_job
        or 'EVENT_NAME: ${{ github.event_name }}' not in status_job
        or re.search(
            r'if\s+\[\[\s+"\$EVENT_NAME"\s+==\s+"pull_request"\s+&&\s+'
            r'"\$MERGE_RESULT_RESULT"\s+!=\s+"success"\s+\]\]\s*;?\s*then',
            status_job,
        )
        is None
    ):
        errors.append(
            "ci-status must require a successful merge-result job for pull requests"
        )

    submission_concurrency = _top_level_block(submission_text, "concurrency")
    if (
        "group: dependency-submission-${{ github.sha }}" not in submission_concurrency
        or "cancel-in-progress: false" not in submission_concurrency
    ):
        errors.append(
            "dependency-submission concurrency must be SHA-scoped and non-cancelling"
        )

    submission = _job_block(submission_text, "dependency-submission")
    if not submission:
        errors.append("workflow must define dependency-submission")
    else:
        unsafe_submission_environment = _unsafe_execution_environment_names(
            submission_text, ("dependency-submission",)
        )
        if unsafe_submission_environment:
            errors.append(
                "dependency-submission protected execution scopes must not define unsafe execution environment variable(s): "
                + ", ".join(unsafe_submission_environment)
            )
        if _indented_values(submission, "continue-on-error", 4):
            errors.append(
                "dependency-submission must not use job-level continue-on-error"
            )
        expected_step_names = (
            "Checkout sources",
            "Verify dependency submission commit",
            "Setup Java",
            "Generate and submit dependency graph",
        )
        submission_steps = _step_blocks(submission)
        actual_step_names = tuple(
            _step_value(step, "name") for step in submission_steps
        )
        if actual_step_names != expected_step_names:
            errors.append(
                "dependency-submission steps must preserve checkout, verification, setup, and submission order"
            )

        checkout_step = _named_step_block(submission, "Checkout sources")
        verification_step = _named_step_block(
            submission, "Verify dependency submission commit"
        )
        graph_step = _named_step_block(
            submission, "Generate and submit dependency graph"
        )
        if _step_value(checkout_step, "uses") != (
            "actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1"
        ):
            errors.append("dependency-submission must use the pinned checkout action")
        if _step_value(checkout_step, "ref") != "${{ github.sha }}":
            errors.append("dependency-submission checkout must pin ref to github.sha")
        if _step_run_script(verification_step) != list(
            EXPECTED_DEPENDENCY_SUBMISSION_VERIFY_SCRIPT
        ):
            errors.append(
                "dependency-submission must verify checkout HEAD against GITHUB_SHA and EXPECTED_SHA and log both commit SHAs before submission"
            )
        if _step_values(verification_step, "shell"):
            errors.append(
                "dependency-submission verification step must not override the fail-closed shell"
            )
        if _step_env_block(verification_step) != [
            "        env:",
            "          EXPECTED_SHA: ${{ github.sha }}",
        ]:
            errors.append(
                "dependency-submission verification step must bind EXPECTED_SHA to github.sha"
            )
        if _indented_values(verification_step, "if", 8) or _indented_values(
            verification_step, "continue-on-error", 8
        ):
            errors.append(
                "dependency-submission verification step must run and fail closed"
            )
        if _step_value(graph_step, "uses") != (
            "gradle/actions/dependency-submission@3f5f9adaf7d9fecd50b5935e54106014257a94e6"
        ):
            errors.append("dependency-submission must use the pinned graph action")
        if _indented_values(graph_step, "if", 8) or _indented_values(
            graph_step, "continue-on-error", 8
        ):
            errors.append("dependency graph submission step must run and fail closed")
        if _indented_values(submission, "if", 4) != [
            "github.event_name == 'push' || github.ref == 'refs/heads/develop'"
        ]:
            errors.append(
                "dependency-submission job must stay scoped to pushes and dispatches on develop"
            )

    return errors


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    ci_path = root / ".github" / "workflows" / "ci.yml"
    nightly_path = root / ".github" / "workflows" / "nightly-tests.yml"
    submission_path = root / ".github" / "workflows" / "dependency-submission.yml"
    image_gate_manifest_path = (
        root / "scripts" / "testcontainers_image_gate_manifest.json"
    )
    errors = validate_workflows(
        ci_path.read_text(encoding="utf-8"),
        submission_path.read_text(encoding="utf-8"),
    )
    errors.extend(
        validate_nightly_mongodb_workflow(
            nightly_path.read_text(encoding="utf-8"),
            image_gate_manifest_path.read_text(encoding="utf-8"),
        )
    )
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("catalog workflow contract passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
