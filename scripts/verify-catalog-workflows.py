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


def _top_level_block(text: str, name: str) -> str:
    match = re.search(rf"(?m)^{re.escape(name)}:\s*$", text)
    if match is None:
        return ""
    following = re.search(r"(?m)^[A-Za-z_][A-Za-z0-9_-]*:\s*$", text[match.end() :])
    end = match.end() + following.start() if following else len(text)
    return text[match.start() : end]


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
    env = _top_level_block(text, "env")
    if not env:
        return None
    match = re.search(rf"(?m)^  {re.escape(name)}:\s*(.*?)\s*$", env)
    if match is None:
        return None
    return match.group(1).strip("'\"")


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


def _step_env_value(step: str, name: str) -> str | None:
    lines = step.splitlines()
    values: list[str] = []
    for index, line in enumerate(lines):
        if line != "        env:":
            continue
        for entry in lines[index + 1 :]:
            if entry.strip() and len(entry) - len(entry.lstrip()) <= 8:
                break
            match = re.fullmatch(rf"          {re.escape(name)}:\s*(.*?)\s*", entry)
            if match is not None:
                values.append(match.group(1).strip("'\""))
    return values[0] if len(values) == 1 else None


def _checkout_steps(text: str) -> list[tuple[str, str]]:
    checkout_steps: list[tuple[str, str]] = []
    for job_name, job in _job_blocks(text):
        lines = job.splitlines()
        for index, line in enumerate(lines):
            if re.match(
                r"^\s+-\s+uses:\s+actions/checkout@[^\s#]+(?:\s+#.*)?$",
                line,
            ) is None:
                continue

            step_indent = len(line) - len(line.lstrip())
            end = index + 1
            while end < len(lines):
                following = lines[end]
                following_indent = len(following) - len(following.lstrip())
                if following.strip() and following_indent <= step_indent:
                    break
                end += 1
            checkout_steps.append((job_name, "\n".join(lines[index:end])))
    return checkout_steps


def _step_value(step: str, name: str) -> str | None:
    match = re.search(rf"(?m)^\s+{re.escape(name)}:\s*([^\n#]+?)\s*$", step)
    if match is None:
        return None
    return match.group(1).strip().strip("'\"")


def _has_exact_governance_gate(status_job: str) -> bool:
    return (
        "CATALOG_GOVERNANCE_RESULT: ${{ needs.catalog-governance.result }}"
        in status_job
        and re.search(
            r'if\s+\[\[\s+"\$CATALOG_GOVERNANCE_RESULT"\s+!=\s+"success"\s+\]\]'
            r"\s*;?\s*then",
            status_job,
        )
        is not None
    )


def validate_workflows(ci_text: str, submission_text: str) -> list[str]:
    """Return contract violations in the Projects GitHub workflows."""
    errors: list[str] = []
    if _workflow_env(ci_text, "PROJECTS_EXPECTED_SHA") != (
        "${{ github.event.pull_request.head.sha || github.sha }}"
    ):
        errors.append("CI must resolve the Projects PR head instead of its merge SHA")
    first_party_repositories = {
        PROJECTS_REPOSITORY,
        "${{ github.repository }}",
    }
    expected_external = Counter(EXPECTED_EXTERNAL_CHECKOUTS)
    actual_external: list[tuple[str, str, str | None, str | None]] = []
    projects_checkout_count = 0
    merge_result_checkout_count = 0
    for job_name, checkout in _checkout_steps(ci_text):
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
        parser_contract = governance.find("dependabot-parser-contract.rb")
        catalog_check = governance.find(
            "scripts/project-dependabot-catalog.py"
        )
        if parser_contract < 0 or catalog_check < 0 or parser_contract > catalog_check:
            errors.append(
                "catalog-governance must create the pinned parser receipt before catalog check"
            )

    merge_result = _job_block(ci_text, MERGE_RESULT_JOB)
    if not merge_result:
        errors.append("CI must define the PR merge-result contract job")
    else:
        required_merge_result = (
            "if: ${{ github.event_name == 'pull_request' }}",
            "ref: ${{ github.sha }}",
            'test "$merge_result_sha" = "$GITHUB_SHA"',
            'git merge-base --is-ancestor "$PR_HEAD_SHA" "$merge_result_sha"',
            'git merge-base --is-ancestor "$PR_BASE_SHA" "$merge_result_sha"',
            "python3 -m unittest tests.test_catalog_workflows -v",
            "python3 scripts/verify-catalog-workflows.py",
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
        checkout = submission.find("actions/checkout@")
        head_verification = submission.find('test "$CHECKOUT_SHA" = "$GITHUB_SHA"')
        graph_logging = submission.find("submission_graph_commit_sha=$CHECKOUT_SHA")
        graph_action = submission.find("gradle/actions/dependency-submission@")
        if "ref: ${{ github.sha }}" not in submission:
            errors.append("dependency-submission checkout must pin ref to github.sha")
        if checkout < 0 or head_verification < 0 or checkout > head_verification:
            errors.append(
                "dependency-submission must verify checkout HEAD against GITHUB_SHA"
            )
        if graph_logging < 0 or graph_action < 0 or graph_logging > graph_action:
            errors.append(
                "dependency-submission must log run and graph commit SHAs before submission"
            )
        if "run_head_sha=$GITHUB_SHA" not in submission:
            errors.append("dependency-submission must log run_head_sha")

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
