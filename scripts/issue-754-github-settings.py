#!/usr/bin/env python3

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from typing import Any


SCHEMA_VERSION = 1
MAVEN_SECRET_NAMES = [
    "CENTRAL_PASSWORD",
    "CENTRAL_USERNAME",
    "SIGNING_KEY",
    "SIGNING_KEY_ID",
    "SIGNING_PASSWORD",
]
RELEASE_SECRET_NAMES = sorted(MAVEN_SECRET_NAMES + [
    "RELEASE_SETTINGS_APP_PRIVATE_KEY",
    "RELEASE_TAG_APP_PRIVATE_KEY",
])
RELEASE_VARIABLE_NAMES = ["RELEASE_SETTINGS_APP_ID", "RELEASE_TAG_APP_ID"]
RULESET_PATTERNS = ["1.12.0", "release-gate-probe/issue-754/*"]
RULESET_RULE_TYPES = ["creation", "update", "deletion"]
CANDIDATE_SLICE_PATHS = {
    "contract": "docs/evidence/issue-754/contract/SHA256SUMS",
    "core-serializers": "docs/evidence/issue-754/core-serializers/SHA256SUMS",
    "json-serializers": "docs/evidence/issue-754/json-serializers/SHA256SUMS",
    "avro-serializers": "docs/evidence/issue-754/avro-serializers/SHA256SUMS",
    "allocation-proof": "docs/evidence/issue-754/allocation-proof/SHA256SUMS",
}


class SettingsError(ValueError):
    pass


class UnknownDriftError(SettingsError):
    pass


class PartialUpdateError(SettingsError):
    def __init__(self, component: str, state: dict[str, Any], journal: list[dict[str, Any]]):
        super().__init__(component)
        self.component = component
        self.state = state
        self.journal = journal

    def __str__(self) -> str:
        return f"partial update failed after {self.component}"


def is_lower_hex(value: Any, length: int) -> bool:
    return (
        isinstance(value, str)
        and len(value) == length
        and all(character in "0123456789abcdef" for character in value)
    )


def stable_hash(payload: Any) -> str:
    normalized = json.dumps(payload, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(normalized.encode("utf-8")).hexdigest()


def empty_state(repository: str) -> dict[str, Any]:
    return {
        "schemaVersion": SCHEMA_VERSION,
        "repository": repository,
        "ruleset": None,
        "environments": {},
        "apps": {},
        "repositorySecretNames": [],
        "repositoryVariableNames": [],
        "legacyEnvironmentSecretNames": [],
    }


def expected_state(repository: str) -> dict[str, Any]:
    tag_installation_id = 75401
    settings_installation_id = 75402
    return {
        "schemaVersion": SCHEMA_VERSION,
        "repository": repository,
        "ruleset": {
            "id": 754,
            "name": "release-tags-1.12.0",
            "target": "tag",
            "enforcement": "active",
            "patterns": list(RULESET_PATTERNS),
            "rules": list(RULESET_RULE_TYPES),
            "bypassActors": [
                {"actorType": "Integration", "actorId": 7541}
            ],
        },
        "environments": {
            "snapshot-publish-1.12.0": {
                "deploymentBranches": ["develop"],
                "requiredReviewers": ["release-maintainer"],
                "secretNames": list(MAVEN_SECRET_NAMES),
                "variableNames": [],
            },
            "release-tag-1.12.0": {
                "deploymentBranches": ["develop"],
                "requiredReviewers": ["release-maintainer"],
                "secretNames": list(RELEASE_SECRET_NAMES),
                "variableNames": list(RELEASE_VARIABLE_NAMES),
            },
        },
        "apps": {
            "tag": {
                "slug": "bluetape4k-release-tag-bot",
                "appId": 7541,
                "installationId": tag_installation_id,
                "permissions": {
                    "administration": "none",
                    "contents": "write",
                    "metadata": "read",
                },
            },
            "settings": {
                "slug": "bluetape4k-release-settings-bot",
                "appId": 7542,
                "installationId": settings_installation_id,
                "permissions": {
                    "administration": "write",
                    "contents": "read",
                    "metadata": "read",
                },
            },
        },
        "repositorySecretNames": [],
        "repositoryVariableNames": [],
        "legacyEnvironmentSecretNames": list(MAVEN_SECRET_NAMES),
    }


def validate_settings(state: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    expected_top = {
        "schemaVersion",
        "repository",
        "ruleset",
        "environments",
        "apps",
        "repositorySecretNames",
        "repositoryVariableNames",
        "legacyEnvironmentSecretNames",
    }
    missing = sorted(expected_top - state.keys())
    unknown = sorted(state.keys() - expected_top)
    if missing:
        errors.append(f"missing state fields: {', '.join(missing)}")
    if unknown:
        errors.append(f"unknown state fields: {', '.join(unknown)}")
    if errors:
        return errors

    if state["schemaVersion"] != SCHEMA_VERSION:
        errors.append(f"schemaVersion must be {SCHEMA_VERSION}")

    ruleset = state.get("ruleset") or {}
    if ruleset.get("name") != "release-tags-1.12.0":
        errors.append("ruleset name must be release-tags-1.12.0")
    if ruleset.get("target") != "tag" or ruleset.get("enforcement") != "active":
        errors.append("ruleset must actively target tags")
    if ruleset.get("patterns") != RULESET_PATTERNS:
        errors.append("ruleset patterns are stale or incomplete")
    if ruleset.get("rules") != RULESET_RULE_TYPES:
        errors.append("ruleset must protect creation, update, and deletion")

    apps = state.get("apps") or {}
    tag_app = apps.get("tag") or {}
    settings_app = apps.get("settings") or {}
    if not tag_app or not settings_app:
        errors.append("tag and settings Apps are required")
    else:
        if tag_app.get("appId") == settings_app.get("appId") or tag_app.get("installationId") == settings_app.get("installationId"):
            errors.append("tag and settings App/installation IDs must be distinct")
        tag_permissions = tag_app.get("permissions") or {}
        settings_permissions = settings_app.get("permissions") or {}
        if tag_permissions != {"administration": "none", "contents": "write", "metadata": "read"}:
            errors.append("tag App permissions must be exactly Administration none, Contents write, Metadata read")
        if settings_permissions != {"administration": "write", "contents": "read", "metadata": "read"}:
            errors.append("settings App permissions must be exactly Administration write, Contents read, Metadata read")

        expected_bypass = [
            {"actorType": "Integration", "actorId": tag_app.get("appId")}
        ]
        if ruleset.get("bypassActors") != expected_bypass:
            errors.append("only tag App may be the ruleset bypass actor")

    environments = state.get("environments") or {}
    snapshot = environments.get("snapshot-publish-1.12.0") or {}
    release = environments.get("release-tag-1.12.0") or {}
    if snapshot.get("deploymentBranches") != ["develop"]:
        errors.append("snapshot environment must allow only develop")
    if sorted(snapshot.get("secretNames") or []) != MAVEN_SECRET_NAMES:
        errors.append("snapshot environment must own exactly five Maven/signing secret names")
    if not snapshot.get("requiredReviewers"):
        errors.append("snapshot environment requires reviewers")
    if release.get("deploymentBranches") != ["develop"]:
        errors.append("release environment must allow only the exact guarded develop candidate")
    if sorted(release.get("secretNames") or []) != RELEASE_SECRET_NAMES:
        errors.append("release environment secret-name ownership is incomplete")
    if sorted(release.get("variableNames") or []) != RELEASE_VARIABLE_NAMES:
        errors.append("release environment variable-name ownership is incomplete")
    if not release.get("requiredReviewers"):
        errors.append("release environment requires reviewers")

    old_repository_secrets = sorted(set(state.get("repositorySecretNames") or []) & set(MAVEN_SECRET_NAMES + [
        "RELEASE_TAG_APP_PRIVATE_KEY",
        "RELEASE_SETTINGS_APP_PRIVATE_KEY",
    ]))
    if old_repository_secrets:
        errors.append(f"forbidden repository secret scope: {', '.join(old_repository_secrets)}")
    old_variables = sorted(set(state.get("repositoryVariableNames") or []) & set(RELEASE_VARIABLE_NAMES))
    if old_variables:
        errors.append(f"release-only variable exists at repository scope: {', '.join(old_variables)}")
    legacy_environment_secrets = sorted(state.get("legacyEnvironmentSecretNames") or [])
    if legacy_environment_secrets != MAVEN_SECRET_NAMES:
        errors.append(
            "legacy maven-central-release environment must retain exactly five Maven/signing secret names"
        )
    return errors


def component_values(state: dict[str, Any]) -> list[tuple[str, Any]]:
    return [
        ("ruleset", state["ruleset"]),
        ("snapshot-environment", state["environments"].get("snapshot-publish-1.12.0")),
        ("release-environment", state["environments"].get("release-tag-1.12.0")),
        ("apps", state["apps"]),
        ("old-scopes", {
            "repositorySecretNames": state["repositorySecretNames"],
            "repositoryVariableNames": state["repositoryVariableNames"],
            "legacyEnvironmentSecretNames": state["legacyEnvironmentSecretNames"],
        }),
    ]


def set_component(state: dict[str, Any], name: str, value: Any) -> None:
    if name == "ruleset":
        state["ruleset"] = copy.deepcopy(value)
    elif name == "snapshot-environment":
        state["environments"]["snapshot-publish-1.12.0"] = copy.deepcopy(value)
    elif name == "release-environment":
        state["environments"]["release-tag-1.12.0"] = copy.deepcopy(value)
    elif name == "apps":
        state["apps"] = copy.deepcopy(value)
    elif name == "old-scopes":
        for key, item in value.items():
            state[key] = copy.deepcopy(item)
    else:
        raise SettingsError(f"unknown apply component: {name}")


def apply_fixture(
    current: dict[str, Any],
    desired: dict[str, Any],
    *,
    response_loss_at: str | None = None,
    fail_after: str | None = None,
) -> dict[str, Any]:
    if current.get("repository") != desired.get("repository"):
        raise SettingsError("repository identity cannot change during apply")
    state = copy.deepcopy(current)
    journal: list[dict[str, Any]] = []
    for component, value in component_values(desired):
        before_hash = stable_hash(state)
        set_component(state, component, value)
        result = "accepted-after-readback" if component == response_loss_at else "applied"
        journal.append(
            {
                "component": component,
                "beforeHash": before_hash,
                "afterHash": stable_hash(state),
                "result": result,
            }
        )
        if component == fail_after:
            raise PartialUpdateError(component, state, journal)
    errors = validate_settings(state)
    if errors:
        raise SettingsError("applied state failed verification: " + "; ".join(errors))
    return {"state": state, "journal": journal}


def rollback_fixture(
    current: dict[str, Any],
    pre_state: dict[str, Any],
    journal: list[dict[str, Any]],
) -> dict[str, Any]:
    current_hash = stable_hash(current)
    recognized_hashes = {stable_hash(pre_state)}
    recognized_hashes.update(row.get("afterHash") for row in journal)
    if current_hash not in recognized_hashes:
        raise UnknownDriftError("rollback blocked: current state is not a recognized journaled intermediate")
    return copy.deepcopy(pre_state)


def validate_candidate_artifact(
    payload: dict[str, Any], expected_sha: str, repository: str
) -> list[str]:
    errors: list[str] = []
    required = {
        "runId",
        "requestId",
        "artifactName",
        "artifactCount",
        "conclusion",
        "candidateSha",
        "expired",
        "repository",
        "workflow",
        "workflowRef",
        "displayTitle",
        "checkName",
        "testedCodeTreeSha256",
        "checksums",
        "evidenceSlices",
    }
    missing = sorted(required - payload.keys())
    unknown = sorted(payload.keys() - required)
    if missing:
        errors.append(f"candidateValidation.{missing[0]} is missing")
        return errors
    if unknown:
        errors.append(f"candidateValidation has unknown fields: {', '.join(unknown)}")
    if not isinstance(payload["runId"], int) or payload["runId"] <= 0:
        errors.append("candidateValidation.runId must be a positive integer")
    if not isinstance(payload["requestId"], str) or not payload["requestId"]:
        errors.append("candidateValidation.requestId must be non-empty")
    if payload["artifactName"] != f"issue-754-release-candidate-{payload['requestId']}":
        errors.append("candidateValidation artifact name does not match request ID")
    if payload["artifactCount"] != 1:
        errors.append("candidateValidation must contain exactly one artifact")
    if payload["expired"] is not False:
        errors.append("candidateValidation artifact is expired")
    if payload["conclusion"] != "PASS":
        errors.append("candidateValidation artifact conclusion is not PASS")
    if payload["candidateSha"] != expected_sha:
        errors.append("candidateValidation artifact candidate SHA is wrong")
    if payload["repository"] != repository:
        errors.append("candidateValidation repository mismatch")
    if payload["workflow"] != "validate-issue-754-release-candidate.yml":
        errors.append("candidateValidation workflow mismatch")
    expected_ref = f"{repository}/.github/workflows/validate-issue-754-release-candidate.yml@refs/heads/develop"
    if payload["workflowRef"] != expected_ref:
        errors.append("candidateValidation workflow ref mismatch")
    if payload["displayTitle"] != f"issue-754-candidate-{payload['requestId']}":
        errors.append("candidateValidation display title mismatch")
    if payload["checkName"] != "issue-754-release-candidate":
        errors.append("candidateValidation check name mismatch")
    digest = payload["testedCodeTreeSha256"]
    if not is_lower_hex(digest, 64):
        errors.append("candidateValidation tested-code digest is invalid")
    if not isinstance(payload["checksums"], dict) or not payload["checksums"]:
        errors.append("candidateValidation checksums are missing")
    elif any(not is_lower_hex(value, 64) for value in payload["checksums"].values()):
        errors.append("candidateValidation checksums are invalid")
    slices = payload["evidenceSlices"]
    expected_slices = {
        "contract",
        "core-serializers",
        "json-serializers",
        "avro-serializers",
        "allocation-proof",
    }
    if (
        not isinstance(slices, list)
        or len(slices) != 5
        or any(not isinstance(item, dict) for item in slices)
        or {item.get("name") for item in slices} != expected_slices
    ):
        errors.append("candidateValidation must contain exactly five evidence slices")
    elif any(item.get("conclusion") != "PASS" or not is_lower_hex(item.get("sha256"), 64) for item in slices):
        errors.append("candidateValidation evidence slices are invalid")
    return errors


def validate_immutable_closeout(closeout: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    required = {
        "schemaVersion", "artifactName", "artifactCount", "repository", "workflow",
        "workflowRef", "displayTitle", "checkName", "prepareRunId", "requestId",
        "candidateSha", "tagName", "tagTargetSha", "candidateValidation",
        "normalizedRulesetSha256", "rulesetIdentity", "checksums", "actors", "probes",
        "rollbackClassification", "transition", "conclusion", "expired",
    }
    missing = sorted(required - closeout.keys())
    unknown = sorted(closeout.keys() - required)
    if missing:
        return [f"closeout missing fields: {', '.join(missing)}"]
    if unknown:
        errors.append(f"closeout unknown fields: {', '.join(unknown)}")
    candidate_sha = closeout["candidateSha"]
    if not is_lower_hex(candidate_sha, 40):
        errors.append("candidate SHA must be a 40-character Git SHA")
    if closeout["tagName"] != "1.12.0":
        errors.append("tag name must be exactly 1.12.0")
    if closeout["tagTargetSha"] != candidate_sha:
        errors.append("tag target does not match candidate SHA")
    repository = closeout["repository"]
    if closeout["schemaVersion"] != SCHEMA_VERSION:
        errors.append("schemaVersion mismatch")
    if closeout["artifactName"] != f"issue-754-tag-immutability-{closeout['requestId']}":
        errors.append("prepare artifact name does not match request ID")
    if closeout["artifactCount"] != 1:
        errors.append("prepare must contain exactly one artifact")
    if closeout["expired"] is not False:
        errors.append("prepare artifact is expired")
    if closeout["workflow"] != "release.yml":
        errors.append("prepare workflow mismatch")
    expected_ref = f"{repository}/.github/workflows/release.yml@refs/heads/develop"
    if closeout["workflowRef"] != expected_ref:
        errors.append("prepare workflow ref mismatch")
    if closeout["displayTitle"] != f"issue-754-release-prepare-{closeout['requestId']}":
        errors.append("prepare display title mismatch")
    if closeout["checkName"] != "issue-754-tag-immutability":
        errors.append("prepare check name mismatch")
    if closeout["conclusion"] != "PASS":
        errors.append("prepare conclusion is not PASS")
    if not isinstance(closeout["checksums"], dict) or not closeout["checksums"]:
        errors.append("prepare checksums are missing")
    elif any(not is_lower_hex(value, 64) for value in closeout["checksums"].values()):
        errors.append("prepare checksums are invalid")
    if not is_lower_hex(closeout["normalizedRulesetSha256"], 64):
        errors.append("normalized ruleset digest is invalid")
    ruleset_identity = closeout["rulesetIdentity"]
    if (
        not isinstance(ruleset_identity, dict)
        or set(ruleset_identity) != {"id", "updatedAt"}
        or not isinstance(ruleset_identity.get("id"), int)
        or ruleset_identity.get("id", 0) <= 0
        or not isinstance(ruleset_identity.get("updatedAt"), str)
        or not ruleset_identity.get("updatedAt")
    ):
        errors.append("prepare ruleset identity is invalid")
    if not isinstance(closeout["candidateValidation"], dict):
        errors.append("candidateValidation artifact metadata is missing")
    else:
        errors.extend(validate_candidate_artifact(closeout["candidateValidation"], candidate_sha, repository))
        release_hold_checksum = closeout["candidateValidation"].get("checksums", {}).get(
            ".github/release-holds/1.12.0-issue-754.json"
        )
        if closeout.get("checksums", {}).get("releaseHoldManifest") != release_hold_checksum:
            errors.append("prepare release-hold checksum does not match candidate authority")
    candidate_run = (closeout.get("candidateValidation") or {}).get("runId")
    prepare_run = closeout.get("prepareRunId")
    if not isinstance(prepare_run, int) or prepare_run <= 0:
        errors.append("prepareRunId must be a positive integer")
    if candidate_run is not None and candidate_run == prepare_run:
        errors.append("candidate-validation and prepare run IDs must be distinct")
    actors = closeout["actors"]
    if not isinstance(actors, dict) or set(actors) != {"tag", "settings"}:
        errors.append("prepare actors must bind tag and settings Apps")
    else:
        tag_actor = actors["tag"]
        settings_actor = actors["settings"]
        actor_ids = (
            tag_actor.get("appId"), tag_actor.get("installationId"),
            settings_actor.get("appId"), settings_actor.get("installationId"),
        )
        if any(not isinstance(value, int) or value <= 0 for value in actor_ids):
            errors.append("prepare actor IDs must be positive integers")
        if tag_actor.get("appId") == settings_actor.get("appId") or tag_actor.get("installationId") == settings_actor.get("installationId"):
            errors.append("prepare actor App and installation IDs must be distinct")
        if tag_actor.get("permissions") != {"administration": "none", "contents": "write", "metadata": "read"}:
            errors.append("prepare tag actor permissions are invalid")
        if settings_actor.get("permissions") != {"administration": "write", "contents": "read", "metadata": "read"}:
            errors.append("prepare settings actor permissions are invalid")
    expected_probes = {
        "ordinaryCreate": "denied",
        "ordinaryUpdate": "denied",
        "ordinaryDelete": "denied",
        "tagAppUpdate": "denied",
        "tagAppDelete": "denied",
        "productionReadback": "exact-candidate",
        "rulesetReadback": "no-bypass",
        "twinPolicyMatch": True,
        "cleanup": "PASS",
    }
    if closeout["probes"] != expected_probes:
        errors.append("prepare probe evidence is incomplete or failed")
    if closeout["transition"] != "no-bypass-applied":
        errors.append("prepare transition is not immutable")
    if closeout["rollbackClassification"] != "not-required":
        errors.append("prepare rollback classification is inconsistent")
    return errors


EXPECTED_EVENT_ACTORS = {
    "production-tag-create": "tag",
    "production-tag-readback": "tag",
    "ruleset-remove-bypass": "settings",
    "ruleset-readback": "settings",
    "twin-ordinary-create": "ordinary",
    "twin-ordinary-update": "ordinary",
    "twin-ordinary-delete": "ordinary",
    "twin-tag-update": "tag",
    "twin-tag-delete": "tag",
    "twin-policy-readback": "settings",
    "twin-cleanup": "settings",
}


def event_map(events: Any) -> dict[str, dict[str, Any]]:
    if not isinstance(events, list):
        raise SettingsError("fixture events must be a list")
    mapped = {event.get("operation"): event for event in events if isinstance(event, dict)}
    if len(events) != len(EXPECTED_EVENT_ACTORS) or len(mapped) != len(events) or set(mapped) != set(EXPECTED_EVENT_ACTORS):
        raise SettingsError("fixture events are incomplete or duplicated")
    for operation, actor in EXPECTED_EVENT_ACTORS.items():
        if mapped[operation].get("actor") != actor:
            raise SettingsError(f"actor isolation failed for {operation}")
    return mapped


def probe_fixture(settings_state: dict[str, Any], events: Any) -> dict[str, Any]:
    errors = validate_settings(settings_state)
    if errors:
        raise SettingsError("settings state is invalid: " + "; ".join(errors))
    mapped = event_map(events)
    expected = {
        "production-tag-create": {"accepted"},
        "production-tag-readback": {"exact-candidate"},
        "ruleset-remove-bypass": {"accepted", "response-lost"},
        "ruleset-readback": {"no-bypass"},
        "twin-ordinary-create": {"denied"},
        "twin-ordinary-update": {"denied"},
        "twin-ordinary-delete": {"denied"},
        "twin-tag-update": {"denied"},
        "twin-tag-delete": {"denied"},
        "twin-policy-readback": {"matches-production"},
        "twin-cleanup": {"PASS"},
    }
    failures = [operation for operation, outcomes in expected.items() if mapped[operation].get("outcome") not in outcomes]
    return {"schemaVersion": SCHEMA_VERSION, "decision": "PASS" if not failures else "HOLD", "failures": failures}


def run_immutable_closeout(payload: dict[str, Any]) -> dict[str, Any]:
    settings_state = payload.get("settingsState")
    events = event_map(payload.get("fixtureEvents"))
    if not isinstance(settings_state, dict):
        raise SettingsError("settingsState is required")
    settings_errors = validate_settings(settings_state)
    if settings_errors:
        raise SettingsError("settings state is invalid: " + "; ".join(settings_errors))

    result = copy.deepcopy(payload)
    result.pop("settingsState", None)
    result.pop("fixtureEvents", None)
    ruleset = copy.deepcopy(settings_state["ruleset"])
    ruleset["bypassActors"] = []
    result["normalizedRulesetSha256"] = stable_hash(ruleset)
    result["probes"] = {
        "ordinaryCreate": events["twin-ordinary-create"]["outcome"],
        "ordinaryUpdate": events["twin-ordinary-update"]["outcome"],
        "ordinaryDelete": events["twin-ordinary-delete"]["outcome"],
        "tagAppUpdate": events["twin-tag-update"]["outcome"],
        "tagAppDelete": events["twin-tag-delete"]["outcome"],
        "productionReadback": events["production-tag-readback"]["outcome"],
        "rulesetReadback": events["ruleset-readback"]["outcome"],
        "twinPolicyMatch": events["twin-policy-readback"]["outcome"] == "matches-production",
        "cleanup": events["twin-cleanup"]["outcome"],
    }
    remove_outcome = events["ruleset-remove-bypass"]["outcome"]
    readback_outcome = events["ruleset-readback"]["outcome"]
    prerequisite_outcomes = {
        "production-tag-create": "accepted",
        "production-tag-readback": "exact-candidate",
        "twin-ordinary-create": "denied",
        "twin-ordinary-update": "denied",
        "twin-ordinary-delete": "denied",
        "twin-tag-update": "denied",
        "twin-tag-delete": "denied",
        "twin-policy-readback": "matches-production",
        "twin-cleanup": "PASS",
    }
    prerequisites_pass = all(events[name].get("outcome") == outcome for name, outcome in prerequisite_outcomes.items())
    if prerequisites_pass and remove_outcome in {"accepted", "response-lost"} and readback_outcome == "no-bypass":
        result.update(conclusion="PASS", transition="no-bypass-applied", rollbackClassification="not-required")
    elif readback_outcome == "bypass-retained":
        result.update(conclusion="HOLD", transition="bypass-retained", rollbackClassification="recognized-and-restored")
    else:
        result.update(conclusion="HOLD", transition="ambiguous", rollbackClassification="unknown-drift")
    result["expired"] = False
    return result


def gh_api(
    endpoint: str,
    token: str,
    *,
    method: str = "GET",
    payload: dict[str, Any] | None = None,
    check: bool = True,
    paginate: bool = False,
) -> tuple[int, Any]:
    command = ["gh", "api", "--method", method, endpoint]
    if paginate:
        command.extend(["--paginate", "--slurp"])
    input_text = None
    if payload is not None:
        command.extend(["--input", "-"])
        input_text = json.dumps(payload)
    result = subprocess.run(
        command,
        text=True,
        input=input_text,
        capture_output=True,
        env={**os.environ, "GH_TOKEN": token},
        check=False,
    )
    if check and result.returncode != 0:
        raise SettingsError(f"GitHub API {method} {endpoint} failed: {result.stderr.strip()}")
    body: Any = None
    if result.stdout.strip():
        try:
            body = json.loads(result.stdout)
        except json.JSONDecodeError:
            body = result.stdout.strip()
    if paginate and isinstance(body, list) and all(isinstance(page, list) for page in body):
        body = [item for page in body for item in page]
    return result.returncode, body


def require_token(name: str) -> str:
    value = os.environ.get(name, "")
    if not value:
        raise SettingsError(f"{name} is required")
    return value


def find_single_json(path: Path) -> Path:
    if path.is_file():
        return path
    matches = sorted(path.rglob("*.json")) if path.is_dir() else []
    if len(matches) != 1:
        raise SettingsError(f"expected exactly one JSON artifact under {path}")
    return matches[0]


def validate_candidate_report(
    report: dict[str, Any],
    *,
    candidate: str,
    run_id: int,
    request_id: str,
    repository_root: Path,
) -> list[str]:
    errors: list[str] = []
    expected_fields = {
        "candidateSha", "runId", "requestId", "conclusion",
        "testedCodeTreeSha256", "slices", "checksums",
    }
    if set(report) != expected_fields:
        errors.append("candidate artifact fields are missing or unknown")
    expected = {
        "candidateSha": candidate,
        "runId": run_id,
        "requestId": request_id,
        "conclusion": "PASS",
    }
    for field, value in expected.items():
        if report.get(field) != value:
            errors.append(f"candidate artifact {field} mismatch")
    digest = report.get("testedCodeTreeSha256")
    if not is_lower_hex(digest, 64):
        errors.append("candidate artifact tested-code digest is invalid")
    expected_names = ["contract", "core-serializers", "json-serializers", "avro-serializers", "allocation-proof"]
    slices = report.get("slices")
    if not isinstance(slices, list) or [item.get("name") for item in slices if isinstance(item, dict)] != expected_names:
        errors.append("candidate artifact must bind the exact five slices in order")
    else:
        for item in slices:
            if set(item) != {"name", "path", "conclusion", "sha256"}:
                errors.append(f"candidate slice {item.get('name')} fields are missing or unknown")
            if item.get("conclusion") != "PASS":
                errors.append(f"candidate slice {item.get('name')} is not PASS")
            path = item.get("path")
            checksum = item.get("sha256")
            if not isinstance(path, str) or not is_lower_hex(checksum, 64):
                errors.append(f"candidate slice {item.get('name')} evidence identity is invalid")
                continue
            if path != CANDIDATE_SLICE_PATHS[item["name"]]:
                errors.append(f"candidate slice {item.get('name')} evidence path is not canonical")
                continue
            evidence = (repository_root / path).resolve()
            try:
                evidence.relative_to(repository_root.resolve())
            except ValueError:
                errors.append(f"candidate slice {item.get('name')} path escapes repository")
                continue
            if not evidence.is_file() or hashlib.sha256(evidence.read_bytes()).hexdigest() != checksum:
                errors.append(f"candidate slice {item.get('name')} evidence checksum mismatch")
    checksums = report.get("checksums")
    if not isinstance(checksums, dict) or not checksums:
        errors.append("candidate artifact checksums are missing")
    else:
        expected_checksum_paths = set(CANDIDATE_SLICE_PATHS.values())
        expected_checksum_paths.add(".github/release-holds/1.12.0-issue-754.json")
        if set(checksums) != expected_checksum_paths:
            errors.append("candidate checksum paths do not match the exact evidence set")
        for path, checksum in checksums.items():
            target = (repository_root / path).resolve() if isinstance(path, str) else repository_root.parent
            try:
                target.relative_to(repository_root.resolve())
            except ValueError:
                errors.append("candidate checksum path escapes repository")
                continue
            if not is_lower_hex(checksum, 64) or not target.is_file():
                errors.append(f"candidate checksum entry is invalid: {path}")
            elif hashlib.sha256(target.read_bytes()).hexdigest() != checksum:
                errors.append(f"candidate checksum mismatch: {path}")
    return errors


def normalized_ruleset(ruleset: dict[str, Any], *, bypass_actors: list[Any]) -> dict[str, Any]:
    return {
        "name": ruleset.get("name"),
        "target": ruleset.get("target"),
        "enforcement": ruleset.get("enforcement"),
        "bypass_actors": bypass_actors,
        "conditions": ruleset.get("conditions"),
        "rules": ruleset.get("rules"),
    }


def has_exact_production_rules(ruleset: dict[str, Any]) -> bool:
    rules = ruleset.get("rules")
    if not isinstance(rules, list):
        return False
    rule_types = [rule.get("type") if isinstance(rule, dict) else rule for rule in rules]
    return len(rule_types) == len(RULESET_RULE_TYPES) and set(rule_types) == set(RULESET_RULE_TYPES)


def list_rulesets(repository: str, token: str) -> list[dict[str, Any]]:
    _, rulesets = gh_api(f"repos/{repository}/rulesets?per_page=100", token, paginate=True)
    if not isinstance(rulesets, list):
        raise SettingsError("repository ruleset listing returned an invalid payload")
    return rulesets


def configured_actor(role: str, permissions: dict[str, str]) -> dict[str, Any]:
    prefix = f"RELEASE_{role.upper()}"
    try:
        app_id = int(require_token(f"{prefix}_APP_ID"))
        installation_id = int(require_token(f"{prefix}_INSTALLATION_ID"))
    except ValueError as error:
        raise SettingsError(f"{role} App identity must contain integer IDs") from error
    if app_id <= 0 or installation_id <= 0:
        raise SettingsError(f"{role} App identity must contain positive IDs")
    return {"appId": app_id, "installationId": installation_id, "permissions": permissions}


def download_candidate_artifact(args: argparse.Namespace, token: str, root: Path) -> tuple[Path, dict[str, Any]]:
    artifact_name = f"issue-754-release-candidate-{args.candidate_validation_request_id}"
    result = subprocess.run(
        [
            "gh", "run", "download", str(args.candidate_validation_run_id),
            "--name", artifact_name, "--dir", str(root),
        ],
        text=True,
        capture_output=True,
        env={**os.environ, "GH_TOKEN": token},
        check=False,
    )
    if result.returncode != 0:
        raise SettingsError(f"candidate artifact download failed: {result.stderr.strip()}")
    report_path = find_single_json(root)
    report = read_json(report_path)
    errors = validate_candidate_report(
        report,
        candidate=args.candidate,
        run_id=args.candidate_validation_run_id,
        request_id=args.candidate_validation_request_id,
        repository_root=Path.cwd(),
    )
    if errors:
        raise SettingsError("; ".join(errors))
    return report_path, report


def verify_live_immutable_state(args: argparse.Namespace, artifact: dict[str, Any], token: str) -> list[str]:
    errors: list[str] = []
    tag_rc, tag = gh_api(f"repos/{args.repository}/git/ref/tags/{args.tag}", token, check=False)
    if tag_rc != 0 or tag.get("object", {}).get("sha") != args.candidate:
        errors.append("live production tag target mismatch")
    rulesets = list_rulesets(args.repository, token)
    matches = [item for item in rulesets if item.get("name") == args.ruleset]
    if len(matches) != 1:
        errors.append("live immutable ruleset identity mismatch")
        return errors
    _, ruleset = gh_api(f"repos/{args.repository}/rulesets/{matches[0]['id']}", token)
    if "bypass_actors" in ruleset and ruleset.get("bypass_actors"):
        errors.append("live immutable ruleset regained a bypass actor")
    if not has_exact_production_rules(ruleset):
        errors.append("live immutable ruleset does not protect creation, update, and deletion")
    identity = artifact.get("rulesetIdentity") or {}
    if ruleset.get("id") != identity.get("id") or ruleset.get("updated_at") != identity.get("updatedAt"):
        errors.append("live immutable ruleset identity or update timestamp drifted")
    normalized = normalized_ruleset(ruleset, bypass_actors=[])
    if stable_hash(normalized) != artifact.get("normalizedRulesetSha256"):
        errors.append("live immutable ruleset digest drifted")
    return errors


def verify_prepare_run_authority(args: argparse.Namespace, token: str) -> list[str]:
    errors: list[str] = []
    base = f"repos/{args.repository}/actions/runs/{args.prepare_run_id}"
    _, run = gh_api(base, token)
    _, artifacts_payload = gh_api(f"{base}/artifacts?per_page=100", token)
    _, jobs_payload = gh_api(f"{base}/jobs?filter=latest&per_page=100", token)
    if not isinstance(run, dict):
        return ["prepare run metadata is invalid"]
    expected_run = {
        "repository": args.repository,
        "headSha": args.candidate,
        "headBranch": args.ref,
        "status": "completed",
        "conclusion": "success",
        "displayTitle": f"issue-754-release-prepare-{args.request_id}",
    }
    actual_run = {
        "repository": (run.get("repository") or {}).get("full_name"),
        "headSha": run.get("head_sha"),
        "headBranch": run.get("head_branch"),
        "status": run.get("status"),
        "conclusion": run.get("conclusion"),
        "displayTitle": run.get("display_title"),
    }
    for field, expected in expected_run.items():
        if actual_run[field] != expected:
            errors.append(f"prepare run {field} mismatch")
    if not str(run.get("path", "")).endswith(f".github/workflows/{args.workflow}"):
        errors.append("prepare run workflow identity mismatch")
    artifacts = artifacts_payload.get("artifacts", []) if isinstance(artifacts_payload, dict) else []
    artifact_name = f"issue-754-tag-immutability-{args.request_id}"
    matches = [item for item in artifacts if isinstance(item, dict) and item.get("name") == artifact_name]
    if len(matches) != 1 or matches[0].get("expired") is not False:
        errors.append("prepare run must contain one unexpired immutable authority artifact")
    jobs = jobs_payload.get("jobs", []) if isinstance(jobs_payload, dict) else []
    successful_jobs = [
        item for item in jobs
        if isinstance(item, dict)
        and item.get("name") == "issue-754-tag-immutability"
        and item.get("conclusion") == "success"
    ]
    if len(successful_jobs) != 1:
        errors.append("prepare run must contain one successful immutability job")
    return errors


def run_live_twin_probe(
    args: argparse.Namespace,
    ruleset: dict[str, Any],
    rulesets_endpoint: str,
    tag_token: str,
    settings_token: str,
    ordinary_token: str,
    tag_bypass_actors: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    ref_collection = f"repos/{args.repository}/git/refs"
    probe_name = f"release-gate-probe/issue-754/{args.request_id}"
    probe_read_ref = f"repos/{args.repository}/git/ref/tags/{probe_name}"
    probe_mutation_ref = f"repos/{args.repository}/git/refs/tags/{probe_name}"
    twin_id: int | None = None
    probe_created = False
    result: dict[str, Any] = {}
    failure: Exception | None = None
    twin_name = f"issue-754-twin-{args.request_id}"
    try:
        existing_twins = [item for item in list_rulesets(args.repository, settings_token) if item.get("name") == twin_name]
        if len(existing_twins) > 1:
            raise SettingsError("multiple stale twin rulesets require manual recovery")
        if existing_twins:
            gh_api(f"{rulesets_endpoint}/{existing_twins[0]['id']}", settings_token, method="DELETE")
            if any(item.get("name") == twin_name for item in list_rulesets(args.repository, settings_token)):
                raise SettingsError("stale twin ruleset cleanup did not reconcile")
        twin_conditions = copy.deepcopy(ruleset.get("conditions") or {})
        twin_conditions["ref_name"] = {"include": [f"refs/tags/{probe_name}"], "exclude": []}
        twin_body = {
            "name": twin_name,
            "target": ruleset.get("target"),
            "enforcement": "active",
            "bypass_actors": (
                tag_bypass_actors
                if tag_bypass_actors is not None
                else ruleset.get("bypass_actors") or []
            ),
            "conditions": twin_conditions,
            "rules": ruleset.get("rules"),
        }
        _, twin = gh_api(rulesets_endpoint, settings_token, method="POST", payload=twin_body, check=False)
        twin_id = twin.get("id") if isinstance(twin, dict) else None
        if not isinstance(twin_id, int):
            for attempt in range(3):
                reconciled = [
                    item for item in list_rulesets(args.repository, settings_token)
                    if item.get("name") == twin_name
                ]
                if len(reconciled) == 1:
                    twin_id = reconciled[0].get("id")
                    break
                if len(reconciled) > 1:
                    raise SettingsError("duplicate twin rulesets require manual recovery")
                if attempt < 2:
                    time.sleep(1)
        if not isinstance(twin_id, int):
            raise SettingsError("twin ruleset creation response is ambiguous; no probe ref was created")
        ordinary_create = gh_api(
            ref_collection,
            ordinary_token,
            method="POST",
            payload={"ref": f"refs/tags/{probe_name}", "sha": args.candidate},
            check=False,
        )
        result["ordinaryCreate"] = denied_by_ruleset(*ordinary_create)
        if not result["ordinaryCreate"]:
            allowed_rc, allowed_ref = gh_api(probe_read_ref, tag_token, check=False)
            probe_created = (
                allowed_rc == 0
                and isinstance(allowed_ref, dict)
                and allowed_ref.get("object", {}).get("sha") == args.candidate
            )
            raise SettingsError("ordinary probe tag creation was not denied by the ruleset")
        gh_api(
            ref_collection, tag_token, method="POST",
            payload={"ref": f"refs/tags/{probe_name}", "sha": args.candidate}, check=False,
        )
        read_rc, readback_ref = gh_api(probe_read_ref, tag_token, check=False)
        if read_rc != 0 or readback_ref.get("object", {}).get("sha") != args.candidate:
            raise SettingsError("probe tag creation did not reconcile to the candidate")
        probe_created = True
        _, twin_with_bypass = gh_api(f"{rulesets_endpoint}/{twin_id}", settings_token)
        no_bypass_body = normalized_ruleset(twin_with_bypass, bypass_actors=[])
        gh_api(
            f"{rulesets_endpoint}/{twin_id}",
            settings_token,
            method="PUT",
            payload=no_bypass_body,
            check=False,
        )
        _, twin_readback = gh_api(f"{rulesets_endpoint}/{twin_id}", settings_token)
        result["policyMatch"] = (
            twin_readback.get("target") == ruleset.get("target")
            and twin_readback.get("enforcement") == ruleset.get("enforcement")
            and twin_readback.get("rules") == ruleset.get("rules")
            and not (twin_readback.get("bypass_actors") or [])
        )
        parent = subprocess.run(
            ["git", "rev-parse", f"{args.candidate}^"], text=True, capture_output=True, check=False
        ).stdout.strip() or args.candidate
        mutation_payload = {"sha": parent, "force": True}
        ordinary_update = gh_api(
            probe_mutation_ref, ordinary_token, method="PATCH", payload=mutation_payload, check=False
        )
        ordinary_delete = gh_api(
            probe_mutation_ref, ordinary_token, method="DELETE", check=False
        )
        tag_update = gh_api(
            probe_mutation_ref, tag_token, method="PATCH", payload=mutation_payload, check=False
        )
        tag_delete = gh_api(
            probe_mutation_ref, tag_token, method="DELETE", check=False
        )
        result["ordinaryUpdate"] = denied_by_ruleset(*ordinary_update)
        result["ordinaryDelete"] = denied_by_ruleset(*ordinary_delete)
        result["tagUpdate"] = denied_by_ruleset(*tag_update)
        result["tagDelete"] = denied_by_ruleset(*tag_delete)
    except Exception as error:
        failure = error
    finally:
        cleanup_ids = {twin_id} if twin_id is not None else set()
        discovered_twins = [
            item for item in list_rulesets(args.repository, settings_token)
            if item.get("name") == twin_name and isinstance(item.get("id"), int)
        ]
        cleanup_ids.update(item["id"] for item in discovered_twins)
        for cleanup_id in cleanup_ids:
            gh_api(f"{rulesets_endpoint}/{cleanup_id}", settings_token, method="DELETE", check=False)
        if probe_created:
            gh_api(probe_mutation_ref, tag_token, method="DELETE", check=False)
        remaining_rulesets = list_rulesets(args.repository, settings_token)
        twin_absent = not any(item.get("name") == twin_name for item in remaining_rulesets)
        absent_rc, absent_payload = gh_api(probe_read_ref, tag_token, check=False)
        result["cleanup"] = twin_absent and confirmed_absent(absent_rc, absent_payload)
    if failure is not None:
        raise SettingsError(f"isolated twin probe failed after cleanup: {failure}") from failure
    denied = all(
        result[name] is True
        for name in ("ordinaryCreate", "ordinaryUpdate", "ordinaryDelete", "tagUpdate", "tagDelete")
    )
    if not result.get("policyMatch") or not denied or not result["cleanup"]:
        raise SettingsError("isolated twin probe or cleanup failed; production state was not mutated")
    return result


def rollback_created_tag(
    read_endpoint: str,
    mutation_endpoint: str,
    token: str,
    *,
    created_this_run: bool,
    candidate: str,
) -> str:
    if not created_this_run:
        return "pre-existing-tag-retained"
    current_rc, current = gh_api(read_endpoint, token, check=False)
    if (
        current_rc != 0
        or not isinstance(current, dict)
        or current.get("object", {}).get("sha") != candidate
    ):
        return "unknown-drift"
    gh_api(mutation_endpoint, token, method="DELETE", check=False)
    read_rc, read_payload = gh_api(read_endpoint, token, check=False)
    return "recognized-and-restored" if confirmed_absent(read_rc, read_payload) else "unknown-drift"


def confirmed_absent(returncode: int, payload: Any) -> bool:
    return returncode != 0 and isinstance(payload, dict) and str(payload.get("status")) == "404"


def denied_by_ruleset(returncode: int, payload: Any) -> bool:
    if returncode == 0 or not isinstance(payload, dict):
        return False
    if str(payload.get("status")) not in {"403", "422"}:
        return False
    detail = json.dumps(payload, sort_keys=True).lower()
    return "rule" in detail and ("violation" in detail or "ruleset" in detail)


def existing_tag_state(returncode: int, payload: Any, candidate: str) -> str:
    if returncode == 0:
        if not isinstance(payload, dict) or payload.get("object", {}).get("sha") != candidate:
            raise SettingsError("existing production tag targets the wrong candidate")
        return "pre-existing"
    if confirmed_absent(returncode, payload):
        return "absent"
    raise SettingsError("production tag existence read is ambiguous")


def require_absent_production_tag_for_prepare(
    returncode: int,
    payload: Any,
    candidate: str,
) -> None:
    if existing_tag_state(returncode, payload, candidate) != "absent":
        raise SettingsError("pre-existing production tag has no current prepare ownership")


def creation_owned_by_current_run(
    returncode: int,
    payload: Any,
    *,
    tag: str,
    candidate: str,
) -> bool:
    return (
        returncode == 0
        and isinstance(payload, dict)
        and payload.get("ref") == f"refs/tags/{tag}"
        and payload.get("object", {}).get("sha") == candidate
    )


def annotated_tag_message(request_id: str) -> str:
    return f"issue-754 prepare request {request_id}"


def create_annotated_tag_object(
    repository: str,
    token: str,
    *,
    tag: str,
    candidate: str,
    request_id: str,
) -> str:
    returncode, payload = gh_api(
        f"repos/{repository}/git/tags",
        token,
        method="POST",
        payload={
            "tag": tag,
            "message": annotated_tag_message(request_id),
            "object": candidate,
            "type": "commit",
        },
        check=False,
    )
    if returncode != 0 or not isinstance(payload, dict) or not is_lower_hex(payload.get("sha"), 40):
        raise SettingsError("annotated production tag object creation is ambiguous")
    return payload["sha"]


def validate_annotated_tag_object(
    repository: str,
    token: str,
    object_sha: str,
    *,
    tag: str,
    candidate: str,
    request_id: str,
) -> dict[str, Any]:
    _, payload = gh_api(f"repos/{repository}/git/tags/{object_sha}", token)
    if (
        not isinstance(payload, dict)
        or payload.get("sha") != object_sha
        or payload.get("tag") != tag
        or payload.get("message") != annotated_tag_message(request_id)
        or payload.get("object", {}).get("type") != "commit"
        or payload.get("object", {}).get("sha") != candidate
    ):
        raise SettingsError("annotated production tag ownership or target mismatch")
    return payload


def reconcile_created_tag_readback(
    read_endpoint: str,
    mutation_endpoint: str,
    token: str,
    *,
    candidate: str,
    created_this_run: bool,
) -> tuple[dict[str, Any], bool]:
    read_rc, readback = gh_api(read_endpoint, token, check=False)
    if read_rc == 0 and isinstance(readback, dict) and readback.get("object", {}).get("sha") == candidate:
        # The expected SHA is a request-owned annotated tag object, not the candidate commit.
        # Exact readback therefore proves ownership even when the ref POST response was lost.
        return readback, True
    if created_this_run:
        rollback = rollback_created_tag(
            read_endpoint,
            mutation_endpoint,
            token,
            created_this_run=True,
            candidate=candidate,
        )
        if rollback == "recognized-and-restored":
            raise SettingsError("production tag readback failed; owned tag was rolled back")
        raise SettingsError("production tag readback failed and rollback is unknown")
    raise SettingsError("production tag creation did not reconcile to the candidate")


def apply_live_immutable_transition(
    args: argparse.Namespace,
    ruleset: dict[str, Any],
    ruleset_endpoint: str,
    production_read_ref: str,
    production_mutation_ref: str,
    settings_token: str,
    tag_token: str,
    *,
    created_this_run: bool,
    expected_ref_sha: str | None = None,
) -> dict[str, Any]:
    expected_ref_sha = expected_ref_sha or args.candidate
    try:
        no_bypass_body = normalized_ruleset(ruleset, bypass_actors=[])
        update_rc, _ = gh_api(
            ruleset_endpoint,
            settings_token,
            method="PUT",
            payload=no_bypass_body,
            check=False,
        )
        _, readback = gh_api(ruleset_endpoint, settings_token)
        readback_bypass = readback.get("bypass_actors") or []
        _, tag_readback = gh_api(production_read_ref, tag_token)
        exact_target = tag_readback.get("object", {}).get("sha") == expected_ref_sha
    except Exception as error:
        if created_this_run:
            rollback = rollback_created_tag(
                production_read_ref,
                production_mutation_ref,
                tag_token,
                created_this_run=True,
                candidate=expected_ref_sha,
            )
            if rollback == "recognized-and-restored":
                raise SettingsError("immutable transition failed; owned tag was rolled back") from error
            raise SettingsError("immutable transition failed and owned-tag rollback is unknown") from error
        raise SettingsError("immutable transition failed for a pre-existing tag") from error
    return {
        "updateReturnCode": update_rc,
        "readback": readback,
        "readbackBypass": readback_bypass,
        "tagReadback": tag_readback,
        "exactTarget": exact_target,
    }


def live_immutable_closeout(args: argparse.Namespace) -> dict[str, Any]:
    tag_token = require_token("RELEASE_TAG_TOKEN")
    settings_token = require_token("RELEASE_SETTINGS_TOKEN")
    download_token = require_token("GH_TOKEN")
    tag_actor = configured_actor(
        "tag", {"administration": "none", "contents": "write", "metadata": "read"}
    )
    settings_actor = configured_actor(
        "settings", {"administration": "write", "contents": "read", "metadata": "read"}
    )
    if tag_actor["appId"] == settings_actor["appId"] or tag_actor["installationId"] == settings_actor["installationId"]:
        raise SettingsError("tag and settings App identities must be distinct")
    if tag_actor["permissions"] != {"administration": "none", "contents": "write", "metadata": "read"}:
        raise SettingsError("tag App permission contract is invalid")
    if settings_actor["permissions"] != {"administration": "write", "contents": "read", "metadata": "read"}:
        raise SettingsError("settings App permission contract is invalid")

    rulesets_endpoint = f"repos/{args.repository}/rulesets"
    rulesets = list_rulesets(args.repository, settings_token)
    matches = [item for item in rulesets if item.get("name") == args.ruleset]
    if len(matches) != 1:
        raise SettingsError(f"expected exactly one ruleset named {args.ruleset}")
    ruleset_id = matches[0].get("id")
    _, ruleset = gh_api(f"{rulesets_endpoint}/{ruleset_id}", settings_token)
    bypass = ruleset.get("bypass_actors") or []
    bypass_ids = {item.get("actor_id") for item in bypass}
    if bypass_ids not in ({tag_actor["appId"]}, set()) or len(bypass) > 1:
        raise SettingsError("only the tag App may bypass the production ruleset")
    recovery_mode = not bypass
    include_patterns = ((ruleset.get("conditions") or {}).get("ref_name") or {}).get("include") or []
    normalized_patterns = [
        str(pattern)[len("refs/tags/"):] if str(pattern).startswith("refs/tags/") else str(pattern)
        for pattern in include_patterns
    ]
    if normalized_patterns != RULESET_PATTERNS:
        raise SettingsError("production ruleset patterns are stale or incomplete")
    if not has_exact_production_rules(ruleset):
        raise SettingsError("production ruleset must protect creation, update, and deletion")

    with tempfile.TemporaryDirectory(prefix="issue-754-candidate-") as directory:
        candidate_path, candidate_report = download_candidate_artifact(args, download_token, Path(directory))
        evidence_slices = [
            {
                "name": item.get("name"),
                "conclusion": item.get("conclusion"),
                "sha256": item.get("sha256"),
            }
            for item in candidate_report["slices"]
        ]
        candidate_metadata = {
            "runId": args.candidate_validation_run_id,
            "requestId": args.candidate_validation_request_id,
            "artifactName": f"issue-754-release-candidate-{args.candidate_validation_request_id}",
            "artifactCount": 1,
            "conclusion": "PASS",
            "candidateSha": args.candidate,
            "expired": False,
            "repository": args.repository,
            "workflow": "validate-issue-754-release-candidate.yml",
            "workflowRef": f"{args.repository}/.github/workflows/validate-issue-754-release-candidate.yml@refs/heads/{args.ref}",
            "displayTitle": f"issue-754-candidate-{args.candidate_validation_request_id}",
            "checkName": "issue-754-release-candidate",
            "testedCodeTreeSha256": candidate_report.get("testedCodeTreeSha256"),
            "checksums": candidate_report["checksums"],
            "evidenceSlices": evidence_slices,
        }
        candidate_checksum = hashlib.sha256(candidate_path.read_bytes()).hexdigest()

    twin = run_live_twin_probe(
        args,
        ruleset,
        rulesets_endpoint,
        tag_token,
        settings_token,
        download_token,
        tag_bypass_actors=[
            {
                "actor_id": tag_actor["appId"],
                "actor_type": "Integration",
                "bypass_mode": "always",
            }
        ],
    )

    ref_collection = f"repos/{args.repository}/git/refs"
    production_read_ref = f"repos/{args.repository}/git/ref/tags/{args.tag}"
    production_mutation_ref = f"repos/{args.repository}/git/refs/tags/{args.tag}"
    existing_rc, existing = gh_api(production_read_ref, tag_token, check=False)
    created_this_run = False
    if confirmed_absent(existing_rc, existing):
        if recovery_mode:
            raise SettingsError("immutable recovery has no request-owned production tag")
        tag_object_sha = create_annotated_tag_object(
            args.repository,
            tag_token,
            tag=args.tag,
            candidate=args.candidate,
            request_id=args.request_id,
        )
        create_rc, create_response = gh_api(
            ref_collection, tag_token, method="POST",
            payload={"ref": f"refs/tags/{args.tag}", "sha": tag_object_sha}, check=False,
        )
        created_this_run = creation_owned_by_current_run(
            create_rc,
            create_response,
            tag=args.tag,
            candidate=tag_object_sha,
        )
        _, created_this_run = reconcile_created_tag_readback(
            production_read_ref,
            production_mutation_ref,
            tag_token,
            candidate=tag_object_sha,
            created_this_run=created_this_run,
        )
        validate_annotated_tag_object(
            args.repository,
            tag_token,
            tag_object_sha,
            tag=args.tag,
            candidate=args.candidate,
            request_id=args.request_id,
        )
    elif existing_rc == 0 and isinstance(existing, dict):
        if not recovery_mode:
            raise SettingsError("pre-existing production tag has no current prepare ownership")
        tag_object_sha = existing.get("object", {}).get("sha")
        if existing.get("object", {}).get("type") != "tag" or not is_lower_hex(tag_object_sha, 40):
            raise SettingsError("immutable recovery tag is not an annotated request-owned tag")
        validate_annotated_tag_object(
            args.repository,
            tag_token,
            tag_object_sha,
            tag=args.tag,
            candidate=args.candidate,
            request_id=args.request_id,
        )
    else:
        raise SettingsError("production tag existence read is ambiguous")

    if recovery_mode:
        _, tag_readback = gh_api(production_read_ref, tag_token)
        transition_result = {
            "updateReturnCode": 0,
            "readback": ruleset,
            "readbackBypass": [],
            "tagReadback": tag_readback,
            "exactTarget": tag_readback.get("object", {}).get("sha") == tag_object_sha,
        }
    else:
        transition_result = apply_live_immutable_transition(
            args,
            ruleset,
            f"{rulesets_endpoint}/{ruleset_id}",
            production_read_ref,
            production_mutation_ref,
            settings_token,
            tag_token,
            created_this_run=created_this_run,
            expected_ref_sha=tag_object_sha,
        )
    readback = transition_result["readback"]
    readback_bypass = transition_result["readbackBypass"]
    tag_readback = transition_result["tagReadback"]
    exact_target = transition_result["exactTarget"]

    transition = "no-bypass-applied" if not readback_bypass else "bypass-retained"
    rollback = "not-required"
    conclusion = "PASS" if not readback_bypass and exact_target else "HOLD"
    if transition == "bypass-retained" and exact_target:
        rollback = rollback_created_tag(
            production_read_ref,
            production_mutation_ref,
            tag_token,
            created_this_run=created_this_run,
            candidate=tag_object_sha,
        )
    elif readback_bypass or not exact_target:
        transition = "ambiguous"
        rollback = "unknown-drift"

    normalized = normalized_ruleset(readback, bypass_actors=readback_bypass)
    artifact = {
        "schemaVersion": SCHEMA_VERSION,
        "artifactName": f"issue-754-tag-immutability-{args.request_id}",
        "artifactCount": 1,
        "repository": args.repository,
        "workflow": args.workflow,
        "workflowRef": f"{args.repository}/.github/workflows/{args.workflow}@refs/heads/{args.ref}",
        "displayTitle": f"issue-754-release-prepare-{args.request_id}",
        "checkName": "issue-754-tag-immutability",
        "prepareRunId": args.run_id,
        "requestId": args.request_id,
        "candidateSha": args.candidate,
        "tagName": args.tag,
        "tagTargetSha": args.candidate if exact_target else None,
        "candidateValidation": candidate_metadata,
        "normalizedRulesetSha256": stable_hash(normalized),
        "rulesetIdentity": {"id": readback.get("id"), "updatedAt": readback.get("updated_at")},
        "checksums": {
            "candidateArtifact": candidate_checksum,
            "releaseHoldManifest": candidate_report["checksums"].get(
                ".github/release-holds/1.12.0-issue-754.json"
            ),
        },
        "actors": {"tag": tag_actor, "settings": settings_actor},
        "probes": {
            "ordinaryCreate": "denied" if twin["ordinaryCreate"] is True else "unproven",
            "ordinaryUpdate": "denied" if twin["ordinaryUpdate"] is True else "unproven",
            "ordinaryDelete": "denied" if twin["ordinaryDelete"] is True else "unproven",
            "tagAppUpdate": "denied" if twin["tagUpdate"] is True else "unproven",
            "tagAppDelete": "denied" if twin["tagDelete"] is True else "unproven",
            "productionReadback": "exact-candidate" if exact_target else "wrong-target",
            "rulesetReadback": "no-bypass" if not readback_bypass else "bypass-retained",
            "twinPolicyMatch": twin["policyMatch"],
            "cleanup": "PASS" if twin["cleanup"] else "FAIL",
        },
        "rollbackClassification": rollback,
        "transition": transition,
        "conclusion": conclusion,
        "expired": False,
    }
    return artifact


def sanitize(payload: Any) -> Any:
    forbidden = {"secretValue", "privateKeyValue", "token", "privateKey"}
    if isinstance(payload, dict):
        return {
            key: sanitize(value)
            for key, value in payload.items()
            if key not in forbidden
        }
    if isinstance(payload, list):
        return [sanitize(value) for value in payload]
    return payload


def read_json(path: Path) -> dict[str, Any]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise SettingsError(f"JSON fixture must be an object: {path}")
    return payload


def write_json(path: Path, payload: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(sanitize(payload), indent=2, sort_keys=True) + "\n", encoding="utf-8")


def add_common(parser: argparse.ArgumentParser, *, output: bool = True) -> None:
    parser.add_argument("--repository", required=True)
    parser.add_argument("--fixture-state", type=Path)
    if output:
        parser.add_argument("--output", type=Path, required=True)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Issue #754 GitHub release-setting state machine (fixture-safe in Task 4)"
    )
    subparsers = parser.add_subparsers(dest="command", required=True)
    for command in ("snapshot", "verify"):
        add_common(subparsers.add_parser(command))

    probe_parser = subparsers.add_parser("probe")
    add_common(probe_parser)
    probe_parser.add_argument("--fixture-events", type=Path, required=True)

    isolation_parser = subparsers.add_parser("prove-workflow-isolation")
    add_common(isolation_parser)
    isolation_parser.add_argument("--workflow-fixture", type=Path, required=True)

    verify_rollback_parser = subparsers.add_parser("verify-rollback")
    add_common(verify_rollback_parser)
    verify_rollback_parser.add_argument("--pre-state", type=Path, required=True)
    verify_rollback_parser.add_argument("--journal", type=Path, required=True)

    apply_parser = subparsers.add_parser("apply")
    add_common(apply_parser)
    apply_parser.add_argument("--desired-state", type=Path)
    apply_parser.add_argument("--journal", type=Path, required=True)

    rollback_parser = subparsers.add_parser("rollback")
    add_common(rollback_parser)
    rollback_parser.add_argument("--pre-state", type=Path, required=True)
    rollback_parser.add_argument("--journal", type=Path, required=True)

    closeout_parser = subparsers.add_parser("immutable-closeout")
    closeout_parser.add_argument("--input", type=Path)
    closeout_parser.add_argument("--output", type=Path, required=True)
    closeout_parser.add_argument("--repository")
    closeout_parser.add_argument("--workflow", default="release.yml")
    closeout_parser.add_argument("--ref", default="develop")
    closeout_parser.add_argument("--tag")
    closeout_parser.add_argument("--candidate")
    closeout_parser.add_argument("--candidate-validation-run-id", type=int)
    closeout_parser.add_argument("--candidate-validation-request-id")
    closeout_parser.add_argument("--ruleset")
    closeout_parser.add_argument("--request-id")
    closeout_parser.add_argument("--run-id", type=int)

    candidate_parser = subparsers.add_parser("verify-candidate-artifact")
    candidate_parser.add_argument("--candidate", required=True)
    candidate_parser.add_argument("--run-id", type=int, required=True)
    candidate_parser.add_argument("--request-id", required=True)
    candidate_parser.add_argument("--artifact", type=Path, required=True)
    candidate_parser.add_argument("--repository-root", type=Path, default=Path("."))
    candidate_parser.add_argument("--output", type=Path)

    for command in ("verify-immutable-artifact", "verify-immutable-closeout"):
        verify_parser = subparsers.add_parser(command)
        verify_parser.add_argument("--repository", required=True)
        verify_parser.add_argument("--workflow", required=True)
        verify_parser.add_argument("--ref", required=True)
        verify_parser.add_argument("--prepare-run-id", type=int, required=True)
        verify_parser.add_argument("--candidate-validation-run-id", type=int, required=True)
        verify_parser.add_argument("--candidate-validation-request-id", required=True)
        verify_parser.add_argument("--tag", required=True)
        verify_parser.add_argument("--candidate", required=True)
        verify_parser.add_argument("--request-id", required=True)
        verify_parser.add_argument("--artifact", type=Path, required=True)
        verify_parser.add_argument("--candidate-artifact", type=Path)
        verify_parser.add_argument("--ruleset", default="release-tags-1.12.0")
        verify_parser.add_argument("--output", type=Path, required=True)
    return parser


def require_fixture(args: argparse.Namespace) -> dict[str, Any]:
    if args.fixture_state is None:
        raise SettingsError(
            "Task 4 forbids live GitHub mutation; provide --fixture-state for offline state-machine execution"
        )
    return read_json(args.fixture_state)


def main(argv: list[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    try:
        if args.command == "verify-candidate-artifact":
            report = read_json(find_single_json(args.artifact))
            errors = validate_candidate_report(
                report,
                candidate=args.candidate,
                run_id=args.run_id,
                request_id=args.request_id,
                repository_root=args.repository_root.resolve(),
            )
            result = {"schemaVersion": SCHEMA_VERSION, "decision": "PASS" if not errors else "HOLD", "errors": errors}
            if args.output:
                write_json(args.output, result)
            return 0 if not errors else 3

        if args.command == "immutable-closeout":
            if args.input:
                report = run_immutable_closeout(read_json(args.input))
            else:
                required = (
                    "repository", "tag", "candidate", "candidate_validation_run_id",
                    "candidate_validation_request_id", "ruleset", "request_id", "run_id",
                )
                missing = [name for name in required if getattr(args, name) in {None, ""}]
                if missing:
                    raise SettingsError("immutable-closeout missing live arguments: " + ", ".join(missing))
                report = live_immutable_closeout(args)
            errors = validate_immutable_closeout(report)
            if errors and report.get("conclusion") == "PASS":
                raise SettingsError("immutable closeout artifact is invalid: " + "; ".join(errors))
            write_json(args.output, report)
            return 0 if report["conclusion"] == "PASS" else 3

        if args.command in {"verify-immutable-artifact", "verify-immutable-closeout"}:
            live_token = None
            if args.command == "verify-immutable-closeout":
                if args.candidate_artifact is None:
                    raise SettingsError("verify-immutable-closeout requires --candidate-artifact")
                live_token = require_token("GH_TOKEN")
            artifact_path = find_single_json(args.artifact)
            artifact = read_json(artifact_path)
            errors = validate_immutable_closeout(artifact)
            expected = {
                "repository": args.repository,
                "workflow": args.workflow,
                "prepareRunId": args.prepare_run_id,
                "requestId": args.request_id,
                "tagName": args.tag,
                "candidateSha": args.candidate,
            }
            for field, value in expected.items():
                if artifact.get(field) != value:
                    errors.append(f"immutable artifact {field} mismatch")
            candidate = artifact.get("candidateValidation") or {}
            if candidate.get("runId") != args.candidate_validation_run_id:
                errors.append("candidate-validation run ID mismatch")
            if candidate.get("requestId") != args.candidate_validation_request_id:
                errors.append("candidate-validation request ID mismatch")
            expected_ref = f"{args.repository}/.github/workflows/{args.workflow}@refs/heads/{args.ref}"
            if artifact.get("workflowRef") != expected_ref:
                errors.append("immutable artifact workflow ref mismatch")
            if args.candidate_artifact:
                candidate_path = find_single_json(args.candidate_artifact)
                candidate_report = read_json(candidate_path)
                errors.extend(validate_candidate_report(
                    candidate_report,
                    candidate=args.candidate,
                    run_id=args.candidate_validation_run_id,
                    request_id=args.candidate_validation_request_id,
                    repository_root=Path.cwd(),
                ))
                candidate_checksum = hashlib.sha256(candidate_path.read_bytes()).hexdigest()
                if artifact.get("checksums", {}).get("candidateArtifact") != candidate_checksum:
                    errors.append("candidate artifact checksum does not match immutable authority")
                nested_candidate = artifact.get("candidateValidation") or {}
                expected_slices = [
                    {"name": item.get("name"), "conclusion": item.get("conclusion"), "sha256": item.get("sha256")}
                    for item in candidate_report.get("slices", [])
                ]
                if nested_candidate.get("testedCodeTreeSha256") != candidate_report.get("testedCodeTreeSha256"):
                    errors.append("candidate tested-code digest does not match immutable authority")
                if nested_candidate.get("checksums") != candidate_report.get("checksums"):
                    errors.append("candidate checksums do not match immutable authority")
                if nested_candidate.get("evidenceSlices") != expected_slices:
                    errors.append("candidate slices do not match immutable authority")
            if live_token is not None:
                errors.extend(verify_prepare_run_authority(args, live_token))
                errors.extend(verify_live_immutable_state(args, artifact, live_token))
            report = {"schemaVersion": SCHEMA_VERSION, "decision": "PASS" if not errors else "HOLD", "errors": errors}
            write_json(args.output, report)
            return 0 if not errors else 3

        current = require_fixture(args)
        if current.get("repository") != args.repository:
            raise SettingsError("fixture repository does not match --repository")

        if args.command == "snapshot":
            write_json(args.output, current)
            return 0
        if args.command == "verify":
            errors = validate_settings(current)
            write_json(args.output, {"schemaVersion": SCHEMA_VERSION, "decision": "PASS" if not errors else "HOLD", "errors": errors})
            return 0 if not errors else 3
        if args.command == "apply":
            desired = read_json(args.desired_state) if args.desired_state else expected_state(args.repository)
            result = apply_fixture(current, desired)
            write_json(args.output, result["state"])
            write_json(args.journal, result["journal"])
            return 0
        if args.command == "rollback":
            pre_state = read_json(args.pre_state)
            journal = json.loads(args.journal.read_text(encoding="utf-8"))
            restored = rollback_fixture(current, pre_state, journal)
            write_json(args.output, restored)
            return 0
        if args.command == "verify-rollback":
            pre_state = read_json(args.pre_state)
            journal = json.loads(args.journal.read_text(encoding="utf-8"))
            recognized = stable_hash(current) == stable_hash(pre_state) and isinstance(journal, list)
            report = {"schemaVersion": SCHEMA_VERSION, "decision": "PASS" if recognized else "HOLD", "stateHash": stable_hash(current)}
            write_json(args.output, report)
            return 0 if recognized else 3
        if args.command == "probe":
            events = json.loads(args.fixture_events.read_text(encoding="utf-8"))
            report = probe_fixture(current, events)
            write_json(args.output, report)
            return 0 if report["decision"] == "PASS" else 3
        if args.command == "prove-workflow-isolation":
            workflows = json.loads(args.workflow_fixture.read_text(encoding="utf-8"))
            expected = {
                "release.yml": ["release-tag-1.12.0"],
                "publish-snapshot.yml": ["snapshot-publish-1.12.0"],
            }
            passed = workflows == expected
            report = {"schemaVersion": SCHEMA_VERSION, "decision": "PASS" if passed else "HOLD"}
            write_json(args.output, report)
            return 0 if passed else 3
        raise SettingsError(f"unsupported command: {args.command}")
    except (SettingsError, OSError, json.JSONDecodeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
