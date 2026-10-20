"""Validate the catalog-governance and dependency-submission workflow contracts."""

from __future__ import annotations

import re
import sys
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
    'echo "pr head=$PR_HEAD_SHA"',
    'echo "pr base=$PR_BASE_SHA"',
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


def _step_run_script(step: str) -> list[str] | None:
    lines = step.splitlines()
    run_lines = [
        (index, len(line) - len(line.lstrip()), line.split(":", 1)[1].strip())
        for index, line in enumerate(lines)
        if re.match(r"^\s+run:\s*", line)
    ]
    if len(run_lines) != 1:
        return None

    start, run_indent, inline = run_lines[0]
    if re.fullmatch(r"\|[+-]?", inline) is None:
        return None

    script: list[str] = []
    for line in lines[start + 1 :]:
        indent = len(line) - len(line.lstrip())
        if line.strip() and indent <= run_indent:
            break
        if line.strip():
            script.append(line.strip())
    return script


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
        if _step_value(step, "name") == name
    ]
    return matches[0] if len(matches) == 1 else None


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
    if "runs-on: ubuntu-22.04" not in data_job:
        errors.append("MongoDB container tests in Test/Data must use a compatible runner")
    spring_boot_job = _job_block(ci_text, "test-spring-boot")
    if (
        "runs-on: ${{ matrix.group == 'batch-nosql' && 'ubuntu-22.04' || 'ubuntu-latest' }}"
        not in spring_boot_job
    ):
        errors.append(
            "MongoDB container tests in Spring Boot batch-nosql must use a compatible runner"
        )

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
            "ruby/setup-ruby@651ff0d62c5c79bed6d53ec518387aae20e72c1a # v1",
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
        ]
        if _step_env_block(verify_merge_step) != expected_step_env:
            errors.append(
                "named merge-result verification step must use the exact merge-result execution environment"
            )
        verify_script_text = "\n".join(verify_script or [])
        if re.search(
            r"(?m)(?:^|[;&|]\s*)(?:export\s+|declare\s+(?:-x\s+)?|readonly\s+)?"
            r"PR_(?:HEAD|BASE)_SHA(?:\s*\+?=|\s+)",
            verify_script_text,
        ) or re.search(
            r"(?m)^\s*unset\s+.*\bPR_(?:HEAD|BASE)_SHA\b", verify_script_text
        ):
            errors.append("PR_HEAD_SHA and PR_BASE_SHA must not be reassigned")
        if "GITHUB_ENV" in merge_result and re.search(
            r"\bPR_(?:HEAD|BASE)_SHA\b", merge_result
        ):
            errors.append(
                "PR merge-result job must not write PR SHA values to GITHUB_ENV"
            )
        for name, expected in (
            ("PR_HEAD_SHA", "${{ github.event.pull_request.head.sha }}"),
            ("PR_BASE_SHA", "${{ github.event.pull_request.base.sha }}"),
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
    submission_path = root / ".github" / "workflows" / "dependency-submission.yml"
    errors = validate_workflows(
        ci_path.read_text(encoding="utf-8"),
        submission_path.read_text(encoding="utf-8"),
    )
    if errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
        return 1
    print("catalog workflow contract passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
