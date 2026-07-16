#!/usr/bin/env python3

import copy
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock


REPOSITORY = Path(__file__).resolve().parents[1]
SCRIPT = REPOSITORY / "scripts" / "issue-754-github-settings.py"


def load_module():
    spec = importlib.util.spec_from_file_location("issue_754_github_settings", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class GitHubSettingsStateMachineTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.settings = load_module()

    def setUp(self):
        self.desired = self.settings.expected_state("bluetape4k/bluetape4k-projects")

    def test_complete_settings_pass(self):
        self.assertEqual([], self.settings.validate_settings(self.desired))
        self.assertEqual(["creation", "update", "deletion"], self.desired["ruleset"]["rules"])
        self.assertEqual(
            ["develop"],
            self.desired["environments"]["release-tag-1.12.0"]["deploymentBranches"],
        )

    def test_ruleset_requires_exact_creation_update_and_deletion_protection(self):
        state = copy.deepcopy(self.desired)
        state["ruleset"]["rules"] = ["update", "deletion"]
        errors = self.settings.validate_settings(state)
        self.assertTrue(any("creation, update, and deletion" in error for error in errors), errors)

    def test_actor_confusion_and_app_permissions_fail(self):
        state = copy.deepcopy(self.desired)
        state["apps"]["settings"]["appId"] = state["apps"]["tag"]["appId"]
        state["apps"]["tag"]["permissions"]["administration"] = "write"
        state["apps"]["settings"]["permissions"]["contents"] = "write"
        state["ruleset"]["bypassActors"] = [
            {"actorType": "Integration", "actorId": state["apps"]["settings"]["installationId"]}
        ]
        errors = self.settings.validate_settings(state)
        self.assertTrue(any("distinct" in error for error in errors), errors)
        self.assertTrue(any("tag App permissions must be exactly" in error for error in errors), errors)
        self.assertTrue(any("settings App permissions must be exactly" in error for error in errors), errors)
        self.assertTrue(any("only tag App" in error for error in errors), errors)

    def test_repository_scopes_fail_and_generic_release_secrets_stay_environment_scoped(self):
        state = copy.deepcopy(self.desired)
        state["repositorySecretNames"] = ["CENTRAL_USERNAME"]
        state["repositoryVariableNames"] = ["RELEASE_TAG_APP_ID"]
        errors = self.settings.validate_settings(state)
        self.assertTrue(any("repository secret scope" in error for error in errors), errors)
        self.assertTrue(any("release-only variable" in error for error in errors), errors)

        state = copy.deepcopy(self.desired)
        state["legacyEnvironmentSecretNames"] = ["SIGNING_KEY"]
        errors = self.settings.validate_settings(state)
        self.assertTrue(any("maven-central-release environment" in error for error in errors), errors)

    def test_accepted_response_loss_reconciles_from_readback(self):
        current = self.settings.empty_state("bluetape4k/bluetape4k-projects")
        result = self.settings.apply_fixture(
            current,
            self.desired,
            response_loss_at="ruleset",
        )
        self.assertEqual(self.desired, result["state"])
        self.assertEqual("accepted-after-readback", result["journal"][0]["result"])
        self.assertEqual([], self.settings.validate_settings(result["state"]))

    def test_partial_update_is_journaled_and_not_reported_complete(self):
        current = self.settings.empty_state("bluetape4k/bluetape4k-projects")
        with self.assertRaises(self.settings.PartialUpdateError) as raised:
            self.settings.apply_fixture(current, self.desired, fail_after="snapshot-environment")
        failure = raised.exception
        self.assertEqual("snapshot-environment", failure.journal[-1]["component"])
        self.assertNotEqual(self.desired, failure.state)
        self.assertTrue(failure.journal)

    def test_stale_ruleset_state_fails_verification(self):
        state = copy.deepcopy(self.desired)
        state["ruleset"]["patterns"] = ["1.12.0"]
        errors = self.settings.validate_settings(state)
        self.assertTrue(any("ruleset patterns" in error for error in errors), errors)

    def valid_closeout(self):
        candidate_request = "candidate-1001"
        release_request = "release-2002"
        return {
            "schemaVersion": 1,
            "artifactName": f"issue-754-tag-immutability-{release_request}",
            "artifactCount": 1,
            "repository": "bluetape4k/bluetape4k-projects",
            "workflow": "release.yml",
            "workflowRef": "bluetape4k/bluetape4k-projects/.github/workflows/release.yml@refs/heads/develop",
            "displayTitle": f"issue-754-release-prepare-{release_request}",
            "checkName": "issue-754-tag-immutability",
            "prepareRunId": 2002,
            "requestId": release_request,
            "candidateSha": "a" * 40,
            "tagName": "1.12.0",
            "tagTargetSha": "a" * 40,
            "candidateValidation": {
                "runId": 1001,
                "requestId": candidate_request,
                "artifactName": f"issue-754-release-candidate-{candidate_request}",
                "artifactCount": 1,
                "conclusion": "PASS",
                "candidateSha": "a" * 40,
                "expired": False,
                "repository": "bluetape4k/bluetape4k-projects",
                "workflow": "validate-issue-754-release-candidate.yml",
                "workflowRef": "bluetape4k/bluetape4k-projects/.github/workflows/validate-issue-754-release-candidate.yml@refs/heads/develop",
                "displayTitle": f"issue-754-candidate-{candidate_request}",
                "checkName": "issue-754-release-candidate",
                "testedCodeTreeSha256": "b" * 64,
                "checksums": {".github/release-holds/1.12.0-issue-754.json": "c" * 64},
                "evidenceSlices": [
                    {"name": name, "conclusion": "PASS", "sha256": f"{index:x}" * 64}
                    for index, name in enumerate(
                        [
                            "contract",
                            "core-serializers",
                            "json-serializers",
                            "avro-serializers",
                            "allocation-proof",
                        ],
                        start=1,
                    )
                ],
            },
            "normalizedRulesetSha256": "d" * 64,
            "rulesetIdentity": {"id": 754, "updatedAt": "2026-07-16T00:00:00Z"},
            "checksums": {
                "candidateArtifact": "e" * 64,
                "releaseHoldManifest": "c" * 64,
            },
            "actors": {
                "tag": {
                    "appId": 7541,
                    "installationId": 75401,
                    "permissions": {"administration": "none", "contents": "write", "metadata": "read"},
                },
                "settings": {
                    "appId": 7542,
                    "installationId": 75402,
                    "permissions": {"administration": "write", "contents": "read", "metadata": "read"},
                },
            },
            "probes": {
                "ordinaryCreate": "denied",
                "ordinaryUpdate": "denied",
                "ordinaryDelete": "denied",
                "tagAppUpdate": "denied",
                "tagAppDelete": "denied",
                "productionReadback": "exact-candidate",
                "rulesetReadback": "no-bypass",
                "twinPolicyMatch": True,
                "cleanup": "PASS",
            },
            "rollbackClassification": "not-required",
            "transition": "no-bypass-applied",
            "conclusion": "PASS",
            "expired": False,
        }

    def test_wrong_tag_target_fails_immutable_closeout(self):
        closeout = self.valid_closeout()
        closeout["tagTargetSha"] = "b" * 40
        errors = self.settings.validate_immutable_closeout(closeout)
        self.assertTrue(any("tag target" in error for error in errors), errors)

    def test_missing_wrong_expired_and_duplicate_artifacts_or_run_ids_fail(self):
        mutations = []
        missing = self.valid_closeout()
        del missing["prepareRunId"]
        mutations.append((missing, "prepareRunId"))

        wrong = self.valid_closeout()
        wrong["candidateValidation"]["requestId"] = "wrong"
        mutations.append((wrong, "artifact name"))

        expired = self.valid_closeout()
        expired["candidateValidation"]["expired"] = True
        mutations.append((expired, "expired"))

        duplicate = self.valid_closeout()
        duplicate["artifactCount"] = 2
        mutations.append((duplicate, "exactly one"))

        duplicate_run = self.valid_closeout()
        duplicate_run["prepareRunId"] = duplicate_run["candidateValidation"]["runId"]
        mutations.append((duplicate_run, "distinct"))

        for closeout, message in mutations:
            with self.subTest(message=message):
                errors = self.settings.validate_immutable_closeout(closeout)
                self.assertTrue(any(message in error for error in errors), errors)

    def test_complete_immutable_closeout_passes(self):
        self.assertEqual([], self.settings.validate_immutable_closeout(self.valid_closeout()))

    def test_candidate_and_immutability_artifacts_bind_all_authority_identity(self):
        mutations = [
            (lambda value: value["candidateValidation"].__setitem__("repository", "other/repo"), "repository"),
            (lambda value: value["candidateValidation"].__setitem__("workflow", "release.yml"), "workflow"),
            (lambda value: value["candidateValidation"].__setitem__("workflowRef", "wrong@refs/heads/main"), "workflow ref"),
            (lambda value: value["candidateValidation"].__setitem__("displayTitle", "wrong"), "display title"),
            (lambda value: value["candidateValidation"].__setitem__("checkName", "wrong"), "check name"),
            (lambda value: value["candidateValidation"].__setitem__("checksums", {}), "checksums"),
            (lambda value: value["candidateValidation"].__setitem__("evidenceSlices", value["candidateValidation"]["evidenceSlices"][:-1]), "five evidence slices"),
            (lambda value: value.__setitem__("workflowRef", "wrong@refs/heads/main"), "prepare workflow ref"),
            (lambda value: value.__setitem__("displayTitle", "wrong"), "prepare display title"),
            (lambda value: value.__setitem__("checkName", "wrong"), "prepare check name"),
            (lambda value: value.__setitem__("checksums", {}), "prepare checksums"),
        ]
        for mutation, message in mutations:
            with self.subTest(message=message):
                artifact = self.valid_closeout()
                mutation(artifact)
                errors = self.settings.validate_immutable_closeout(artifact)
                self.assertTrue(any(message in error for error in errors), errors)

    def valid_transition_input(self):
        payload = self.valid_closeout()
        for field in (
            "normalizedRulesetSha256",
            "probes",
            "rollbackClassification",
            "transition",
            "conclusion",
            "expired",
        ):
            payload.pop(field)
        payload["settingsState"] = copy.deepcopy(self.desired)
        payload["fixtureEvents"] = [
            {"operation": "production-tag-create", "actor": "tag", "outcome": "accepted"},
            {"operation": "production-tag-readback", "actor": "tag", "outcome": "exact-candidate"},
            {"operation": "ruleset-remove-bypass", "actor": "settings", "outcome": "accepted"},
            {"operation": "ruleset-readback", "actor": "settings", "outcome": "no-bypass"},
            {"operation": "twin-ordinary-create", "actor": "ordinary", "outcome": "denied"},
            {"operation": "twin-ordinary-update", "actor": "ordinary", "outcome": "denied"},
            {"operation": "twin-ordinary-delete", "actor": "ordinary", "outcome": "denied"},
            {"operation": "twin-tag-update", "actor": "tag", "outcome": "denied"},
            {"operation": "twin-tag-delete", "actor": "tag", "outcome": "denied"},
            {"operation": "twin-policy-readback", "actor": "settings", "outcome": "matches-production"},
            {"operation": "twin-cleanup", "actor": "settings", "outcome": "PASS"},
        ]
        return payload

    def test_immutable_transition_actor_isolation_probe_outcomes_and_classification(self):
        result = self.settings.run_immutable_closeout(self.valid_transition_input())
        self.assertEqual("PASS", result["conclusion"])
        self.assertEqual("no-bypass-applied", result["transition"])
        self.assertEqual("issue-754-tag-immutability-release-2002", result["artifactName"])

        confused = self.valid_transition_input()
        confused["fixtureEvents"][0]["actor"] = "settings"
        with self.assertRaisesRegex(self.settings.SettingsError, "actor isolation"):
            self.settings.run_immutable_closeout(confused)

        rollback = self.valid_transition_input()
        next(item for item in rollback["fixtureEvents"] if item["operation"] == "ruleset-remove-bypass")["outcome"] = "rejected"
        next(item for item in rollback["fixtureEvents"] if item["operation"] == "ruleset-readback")["outcome"] = "bypass-retained"
        rolled_back = self.settings.run_immutable_closeout(rollback)
        self.assertEqual("HOLD", rolled_back["conclusion"])
        self.assertEqual("bypass-retained", rolled_back["transition"])
        self.assertEqual("recognized-and-restored", rolled_back["rollbackClassification"])

        ambiguous = self.valid_transition_input()
        next(item for item in ambiguous["fixtureEvents"] if item["operation"] == "ruleset-remove-bypass")["outcome"] = "response-lost"
        next(item for item in ambiguous["fixtureEvents"] if item["operation"] == "ruleset-readback")["outcome"] = "unknown"
        blocked = self.settings.run_immutable_closeout(ambiguous)
        self.assertEqual("HOLD", blocked["conclusion"])
        self.assertEqual("ambiguous", blocked["transition"])
        self.assertEqual("unknown-drift", blocked["rollbackClassification"])

        accepted_loss = self.valid_transition_input()
        next(item for item in accepted_loss["fixtureEvents"] if item["operation"] == "ruleset-remove-bypass")["outcome"] = "response-lost"
        reconciled = self.settings.run_immutable_closeout(accepted_loss)
        self.assertEqual("PASS", reconciled["conclusion"])

    def test_closeout_fails_when_tag_probe_or_cleanup_evidence_fails(self):
        cases = [
            ("production-tag-create", "rejected"),
            ("production-tag-readback", "wrong-target"),
            ("twin-ordinary-create", "allowed"),
            ("twin-ordinary-update", "allowed"),
            ("twin-tag-delete", "allowed"),
            ("twin-policy-readback", "mismatch"),
            ("twin-cleanup", "FAIL"),
        ]
        for operation, outcome in cases:
            with self.subTest(operation=operation):
                payload = self.valid_transition_input()
                next(item for item in payload["fixtureEvents"] if item["operation"] == operation)["outcome"] = outcome
                result = self.settings.run_immutable_closeout(payload)
                self.assertEqual("HOLD", result["conclusion"])
                self.assertEqual("ambiguous", result["transition"])

    def test_closeout_rejects_duplicate_events_and_tampered_authority(self):
        duplicate = self.valid_transition_input()
        duplicate["fixtureEvents"][-1] = copy.deepcopy(duplicate["fixtureEvents"][0])
        with self.assertRaisesRegex(self.settings.SettingsError, "incomplete or duplicated"):
            self.settings.run_immutable_closeout(duplicate)

        mutations = [
            (lambda value: value["actors"]["tag"]["permissions"].__setitem__("administration", "write"), "tag actor permissions"),
            (lambda value: value["probes"].__setitem__("cleanup", "FAIL"), "probe evidence"),
            (lambda value: value.__setitem__("transition", "ambiguous"), "transition"),
            (lambda value: value["checksums"].__setitem__("candidateArtifact", "short"), "checksums"),
        ]
        for mutation, message in mutations:
            with self.subTest(message=message):
                artifact = self.valid_closeout()
                mutation(artifact)
                errors = self.settings.validate_immutable_closeout(artifact)
                self.assertTrue(any(message in error for error in errors), errors)

    def test_prepare_workflow_live_cli_arguments_parse(self):
        args = self.settings.build_parser().parse_args([
            "immutable-closeout",
            "--repository", "bluetape4k/bluetape4k-projects",
            "--workflow", "release.yml",
            "--ref", "develop",
            "--tag", "1.12.0",
            "--candidate", "a" * 40,
            "--candidate-validation-run-id", "1001",
            "--candidate-validation-request-id", "candidate-1001",
            "--ruleset", "release-tags-1.12.0",
            "--request-id", "release-2002",
            "--run-id", "2002",
            "--output", "closeout.json",
        ])
        self.assertEqual("immutable-closeout", args.command)
        self.assertIsNone(args.input)

    def test_candidate_artifact_recomputes_exact_slice_and_manifest_checksums(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            names = ["contract", "core-serializers", "json-serializers", "avro-serializers", "allocation-proof"]
            slices = []
            checksums = {}
            for name in names:
                path = Path(self.settings.CANDIDATE_SLICE_PATHS[name])
                target = root / path
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(json.dumps({"slice": name}) + "\n", encoding="utf-8")
                digest = self.settings.hashlib.sha256(target.read_bytes()).hexdigest()
                slices.append({"name": name, "path": path.as_posix(), "conclusion": "PASS", "sha256": digest})
                checksums[path.as_posix()] = digest
            manifest = root / ".github/release-holds/1.12.0-issue-754.json"
            manifest.parent.mkdir(parents=True, exist_ok=True)
            manifest.write_text("{}\n", encoding="utf-8")
            checksums[manifest.relative_to(root).as_posix()] = self.settings.hashlib.sha256(manifest.read_bytes()).hexdigest()
            report = {
                "candidateSha": "a" * 40,
                "runId": 1001,
                "requestId": "candidate-1001",
                "conclusion": "PASS",
                "testedCodeTreeSha256": "b" * 64,
                "slices": slices,
                "checksums": checksums,
            }
            self.assertEqual([], self.settings.validate_candidate_report(
                report,
                candidate="a" * 40,
                run_id=1001,
                request_id="candidate-1001",
                repository_root=root,
            ))
            (root / slices[2]["path"]).write_text("tampered\n", encoding="utf-8")
            errors = self.settings.validate_candidate_report(
                report,
                candidate="a" * 40,
                run_id=1001,
                request_id="candidate-1001",
                repository_root=root,
            )
            self.assertTrue(any("checksum mismatch" in error for error in errors), errors)

    def test_live_twin_probe_cleans_up_after_intermediate_failure(self):
        args = self.settings.argparse.Namespace(
            repository="bluetape4k/bluetape4k-projects",
            request_id="release-2002",
            candidate="a" * 40,
        )
        ruleset = {
            "target": "tag",
            "enforcement": "active",
            "conditions": {"ref_name": {"include": ["refs/tags/1.12.0"], "exclude": []}},
            "rules": [{"type": rule_type} for rule_type in self.settings.RULESET_RULE_TYPES],
        }
        calls = []
        probe_reads = 0

        def fake_api(endpoint, token, *, method="GET", payload=None, check=True, paginate=False):
            nonlocal probe_reads
            calls.append((method, endpoint))
            if method == "POST" and endpoint.endswith("/rulesets"):
                return 0, {"id": 75499}
            if method == "GET" and endpoint.endswith("/rulesets/75499"):
                raise self.settings.SettingsError("readback failed")
            if method == "GET" and "/git/ref/tags/" in endpoint:
                probe_reads += 1
                return (0, {"object": {"sha": "a" * 40}}) if probe_reads == 1 else (1, None)
            if method == "GET" and (endpoint.endswith("/rulesets") or "/rulesets?" in endpoint):
                return 0, []
            return 0, {}

        with mock.patch.object(self.settings, "gh_api", side_effect=fake_api):
            with self.assertRaisesRegex(self.settings.SettingsError, "after cleanup"):
                self.settings.run_live_twin_probe(
                    args, ruleset, "repos/bluetape4k/bluetape4k-projects/rulesets",
                    "tag-token", "settings-token", "ordinary-token",
                )
        self.assertIn(("DELETE", "repos/bluetape4k/bluetape4k-projects/rulesets/75499"), calls)
        self.assertTrue(any(method == "DELETE" and "/git/refs/tags/" in endpoint for method, endpoint in calls))
        self.assertTrue(any(method == "GET" and "/git/ref/tags/" in endpoint for method, endpoint in calls))

    def test_live_twin_probe_discovers_and_deletes_response_lost_creation(self):
        args = self.settings.argparse.Namespace(
            repository="bluetape4k/bluetape4k-projects",
            request_id="release-2002",
            candidate="a" * 40,
        )
        ruleset = {
            "target": "tag",
            "enforcement": "active",
            "conditions": {"ref_name": {"include": ["refs/tags/1.12.0"], "exclude": []}},
            "rules": [{"type": rule_type} for rule_type in self.settings.RULESET_RULE_TYPES],
        }
        calls = []
        list_reads = 0

        def fake_api(endpoint, token, *, method="GET", payload=None, check=True, paginate=False):
            nonlocal list_reads
            calls.append((method, endpoint))
            if method == "POST" and endpoint.endswith("/rulesets"):
                return 1, {"message": "gateway timeout", "status": "502"}
            if method == "GET" and (endpoint.endswith("/rulesets") or "/rulesets?" in endpoint):
                list_reads += 1
                if list_reads <= 4:
                    return 0, []
                return 0, [{"id": 75500, "name": "issue-754-twin-release-2002"}]
            if method == "GET" and "/git/ref/tags/" in endpoint:
                return 1, {"message": "Not Found", "status": "404"}
            return 0, {}

        with mock.patch.object(self.settings, "gh_api", side_effect=fake_api), mock.patch.object(
            self.settings.time, "sleep", return_value=None
        ):
            with self.assertRaisesRegex(self.settings.SettingsError, "after cleanup"):
                self.settings.run_live_twin_probe(
                    args, ruleset, "repos/bluetape4k/bluetape4k-projects/rulesets",
                    "tag-token", "settings-token", "ordinary-token",
                )
        self.assertIn(("DELETE", "repos/bluetape4k/bluetape4k-projects/rulesets/75500"), calls)

    def test_preexisting_production_tag_is_never_deleted_by_rollback(self):
        with mock.patch.object(self.settings, "gh_api") as api:
            result = self.settings.rollback_created_tag(
                "repos/example/project/git/ref/tags/1.12.0",
                "repos/example/project/git/refs/tags/1.12.0",
                "tag-token",
                created_this_run=False,
                candidate="a" * 40,
            )
        self.assertEqual("pre-existing-tag-retained", result)
        api.assert_not_called()

    def test_prepare_rejects_preexisting_production_tag_even_at_exact_candidate(self):
        with self.assertRaisesRegex(self.settings.SettingsError, "pre-existing production tag"):
            self.settings.require_absent_production_tag_for_prepare(
                0,
                {"object": {"sha": "a" * 40}},
                "a" * 40,
            )

    def test_owned_tag_rollback_refuses_moved_candidate(self):
        with mock.patch.object(
            self.settings,
            "gh_api",
            return_value=(0, {"object": {"sha": "b" * 40}}),
        ) as api:
            result = self.settings.rollback_created_tag(
                "repos/example/project/git/ref/tags/1.12.0",
                "repos/example/project/git/refs/tags/1.12.0",
                "tag-token",
                created_this_run=True,
                candidate="a" * 40,
            )
        self.assertEqual("unknown-drift", result)
        api.assert_called_once_with(
            "repos/example/project/git/ref/tags/1.12.0",
            "tag-token",
            check=False,
        )

    def test_ambiguous_initial_tag_read_never_attempts_creation(self):
        args = self.settings.argparse.Namespace(
            repository="bluetape4k/bluetape4k-projects",
            workflow="release.yml",
            ref="develop",
            tag="1.12.0",
            candidate="a" * 40,
            candidate_validation_run_id=1001,
            candidate_validation_request_id="candidate-1001",
            ruleset="release-tags-1.12.0",
            request_id="release-2002",
            run_id=2002,
        )
        ruleset = {
            "id": 754,
            "name": "release-tags-1.12.0",
            "target": "tag",
            "enforcement": "active",
            "bypass_actors": [{"actor_id": 7541}],
            "conditions": {"ref_name": {"include": self.settings.RULESET_PATTERNS, "exclude": []}},
            "rules": [{"type": rule_type} for rule_type in self.settings.RULESET_RULE_TYPES],
            "updated_at": "2026-07-16T00:00:00Z",
        }
        calls = []

        def fake_api(endpoint, token, *, method="GET", payload=None, check=True, paginate=False):
            calls.append((method, endpoint))
            if endpoint.endswith("/rulesets/754"):
                return 0, ruleset
            if endpoint.endswith("/git/ref/tags/1.12.0"):
                return 1, {"message": "server error", "status": "500"}
            if method == "POST" and endpoint.endswith("/git/refs"):
                return 0, {"ref": "refs/tags/1.12.0", "object": {"sha": "a" * 40}}
            return 0, {}

        with tempfile.TemporaryDirectory() as directory:
            candidate_path = Path(directory) / "candidate.json"
            candidate_path.write_text("{}\n", encoding="utf-8")
            candidate_report = {
                "testedCodeTreeSha256": "b" * 64,
                "checksums": {".github/release-holds/1.12.0-issue-754.json": "c" * 64},
                "slices": self.valid_closeout()["candidateValidation"]["evidenceSlices"],
            }
            with mock.patch.object(self.settings, "require_token", return_value="token"), mock.patch.object(
                self.settings,
                "configured_actor",
                side_effect=[
                    {"appId": 7541, "installationId": 75401, "permissions": {"administration": "none", "contents": "write", "metadata": "read"}},
                    {"appId": 7542, "installationId": 75402, "permissions": {"administration": "write", "contents": "read", "metadata": "read"}},
                ],
            ), mock.patch.object(self.settings, "list_rulesets", return_value=[{"id": 754, "name": args.ruleset}]), mock.patch.object(
                self.settings, "download_candidate_artifact", return_value=(candidate_path, candidate_report)
            ), mock.patch.object(
                self.settings,
                "run_live_twin_probe",
                return_value={"ordinaryCreate": 1, "ordinaryUpdate": 1, "ordinaryDelete": 1, "tagUpdate": 1, "tagDelete": 1, "policyMatch": True, "cleanup": True},
            ), mock.patch.object(self.settings, "gh_api", side_effect=fake_api):
                with self.assertRaisesRegex(self.settings.SettingsError, "ambiguous"):
                    self.settings.live_immutable_closeout(args)
        self.assertFalse(any(method == "POST" and endpoint.endswith("/git/refs") for method, endpoint in calls))

    def test_same_request_annotated_tag_recovers_after_bypass_removal(self):
        candidate = "a" * 40
        tag_object_sha = "b" * 40
        args = self.settings.argparse.Namespace(
            repository="bluetape4k/bluetape4k-projects",
            workflow="release.yml",
            ref="develop",
            tag="1.12.0",
            candidate=candidate,
            candidate_validation_run_id=1001,
            candidate_validation_request_id="candidate-1001",
            ruleset="release-tags-1.12.0",
            request_id="release-2002",
            run_id=2002,
        )
        ruleset = {
            "id": 754,
            "name": args.ruleset,
            "target": "tag",
            "enforcement": "active",
            "bypass_actors": [],
            "conditions": {"ref_name": {"include": self.settings.RULESET_PATTERNS, "exclude": []}},
            "rules": [{"type": rule_type} for rule_type in self.settings.RULESET_RULE_TYPES],
            "updated_at": "2026-07-16T00:00:00Z",
        }
        tag_ref = {"ref": "refs/tags/1.12.0", "object": {"type": "tag", "sha": tag_object_sha}}
        tag_object = {
            "sha": tag_object_sha,
            "tag": "1.12.0",
            "message": self.settings.annotated_tag_message(args.request_id),
            "object": {"type": "commit", "sha": candidate},
        }
        calls = []

        def fake_api(endpoint, token, *, method="GET", payload=None, check=True, paginate=False):
            calls.append((method, endpoint))
            if endpoint.endswith("/rulesets/754"):
                return 0, ruleset
            if endpoint.endswith("/git/ref/tags/1.12.0"):
                return 0, tag_ref
            if endpoint.endswith(f"/git/tags/{tag_object_sha}"):
                return 0, tag_object
            return 0, {}

        with tempfile.TemporaryDirectory() as directory:
            candidate_path = Path(directory) / "candidate.json"
            candidate_path.write_text("{}\n", encoding="utf-8")
            candidate_report = {
                "testedCodeTreeSha256": "c" * 64,
                "checksums": {".github/release-holds/1.12.0-issue-754.json": "d" * 64},
                "slices": self.valid_closeout()["candidateValidation"]["evidenceSlices"],
            }
            with mock.patch.object(self.settings, "require_token", return_value="token"), mock.patch.object(
                self.settings,
                "configured_actor",
                side_effect=[
                    {"appId": 7541, "installationId": 75401, "permissions": {"administration": "none", "contents": "write", "metadata": "read"}},
                    {"appId": 7542, "installationId": 75402, "permissions": {"administration": "write", "contents": "read", "metadata": "read"}},
                ],
            ), mock.patch.object(self.settings, "list_rulesets", return_value=[{"id": 754, "name": args.ruleset}]), mock.patch.object(
                self.settings, "download_candidate_artifact", return_value=(candidate_path, candidate_report)
            ), mock.patch.object(
                self.settings,
                "run_live_twin_probe",
                return_value={"ordinaryCreate": 1, "ordinaryUpdate": 1, "ordinaryDelete": 1, "tagUpdate": 1, "tagDelete": 1, "policyMatch": True, "cleanup": True},
            ), mock.patch.object(self.settings, "gh_api", side_effect=fake_api):
                result = self.settings.live_immutable_closeout(args)

        self.assertEqual("PASS", result["conclusion"])
        self.assertEqual(candidate, result["tagTargetSha"])
        self.assertFalse(any(method in {"POST", "PUT", "PATCH", "DELETE"} for method, _ in calls))

    def test_response_lost_tag_creation_is_not_owned_or_deleted(self):
        self.assertFalse(self.settings.creation_owned_by_current_run(
            1,
            {"message": "gateway timeout", "status": "502"},
            tag="1.12.0",
            candidate="a" * 40,
        ))
        self.assertFalse(self.settings.creation_owned_by_current_run(
            0,
            {"ref": "refs/tags/1.12.0", "object": {"sha": "b" * 40}},
            tag="1.12.0",
            candidate="a" * 40,
        ))
        self.assertTrue(self.settings.creation_owned_by_current_run(
            0,
            {"ref": "refs/tags/1.12.0", "object": {"sha": "a" * 40}},
            tag="1.12.0",
            candidate="a" * 40,
        ))

    def test_cleanup_requires_explicit_not_found_and_owned_readback_failure_rolls_back(self):
        self.assertTrue(self.settings.confirmed_absent(1, {"message": "Not Found", "status": "404"}))
        self.assertFalse(self.settings.confirmed_absent(1, {"message": "server error", "status": "500"}))
        self.assertFalse(self.settings.confirmed_absent(1, None))
        with mock.patch.object(
            self.settings,
            "gh_api",
            side_effect=[
                (1, {"message": "gateway timeout", "status": "502"}),
                (0, {"object": {"sha": "a" * 40}}),
                (0, {}),
                (1, {"message": "Not Found", "status": "404"}),
            ],
        ) as api:
            with self.assertRaisesRegex(self.settings.SettingsError, "rolled back"):
                self.settings.reconcile_created_tag_readback(
                    "repos/example/project/git/ref/tags/1.12.0",
                    "repos/example/project/git/refs/tags/1.12.0",
                    "tag-token",
                    candidate="a" * 40,
                    created_this_run=True,
                )
        self.assertEqual("DELETE", api.call_args_list[2].kwargs["method"])

    def test_response_loss_exact_readback_preserves_ownership_for_transition_rollback(self):
        candidate = "a" * 40
        tag_object_sha = "b" * 40
        with mock.patch.object(
            self.settings,
            "gh_api",
            return_value=(0, {"object": {"type": "tag", "sha": tag_object_sha}}),
        ):
            _, created_this_run = self.settings.reconcile_created_tag_readback(
                "repos/example/project/git/ref/tags/1.12.0",
                "repos/example/project/git/refs/tags/1.12.0",
                "tag-token",
                candidate=tag_object_sha,
                created_this_run=False,
            )
        self.assertTrue(created_this_run)

        with mock.patch.object(
            self.settings,
            "gh_api",
            return_value=(0, {"object": {"type": "commit", "sha": candidate}}),
        ):
            with self.assertRaisesRegex(self.settings.SettingsError, "did not reconcile"):
                self.settings.reconcile_created_tag_readback(
                    "repos/example/project/git/ref/tags/1.12.0",
                    "repos/example/project/git/refs/tags/1.12.0",
                    "tag-token",
                    candidate=tag_object_sha,
                    created_this_run=False,
                )

        args = self.settings.argparse.Namespace(candidate=candidate)
        ruleset = {
            "id": 754,
            "target": "tag",
            "enforcement": "active",
            "conditions": {"ref_name": {"include": self.settings.RULESET_PATTERNS, "exclude": []}},
            "rules": [{"type": rule_type} for rule_type in self.settings.RULESET_RULE_TYPES],
        }
        with mock.patch.object(
            self.settings,
            "gh_api",
            side_effect=[
                (0, {}),
                self.settings.SettingsError("ruleset readback failed"),
                (0, {"object": {"type": "tag", "sha": tag_object_sha}}),
                (0, {}),
                (1, {"message": "Not Found", "status": "404"}),
            ],
        ) as api:
            with self.assertRaisesRegex(self.settings.SettingsError, "rolled back"):
                self.settings.apply_live_immutable_transition(
                    args,
                    ruleset,
                    "repos/example/project/rulesets/754",
                    "repos/example/project/git/ref/tags/1.12.0",
                    "repos/example/project/git/refs/tags/1.12.0",
                    "settings-token",
                    "tag-token",
                    created_this_run=created_this_run,
                    expected_ref_sha=tag_object_sha,
                )
        self.assertEqual("DELETE", api.call_args_list[3].kwargs["method"])

    def test_annotated_tag_object_binds_request_and_candidate(self):
        candidate = "a" * 40
        tag_object_sha = "b" * 40
        request_id = "release-2002"
        payload = {
            "sha": tag_object_sha,
            "tag": "1.12.0",
            "message": self.settings.annotated_tag_message(request_id),
            "object": {"type": "commit", "sha": candidate},
        }
        with mock.patch.object(self.settings, "gh_api", return_value=(0, payload)):
            validated = self.settings.validate_annotated_tag_object(
                "bluetape4k/bluetape4k-projects",
                "tag-token",
                tag_object_sha,
                tag="1.12.0",
                candidate=candidate,
                request_id=request_id,
            )
        self.assertEqual(payload, validated)

    def test_policy_denial_requires_ruleset_specific_response(self):
        self.assertTrue(self.settings.denied_by_ruleset(
            1,
            {"message": "Repository rule violations found", "status": "403"},
        ))
        self.assertFalse(self.settings.denied_by_ruleset(
            1,
            {"message": "Resource not accessible by integration", "status": "403"},
        ))
        self.assertFalse(self.settings.denied_by_ruleset(
            1,
            {"message": "server error", "status": "500"},
        ))
        self.assertFalse(self.settings.denied_by_ruleset(0, {}))

    def test_owned_tag_rolls_back_when_immutable_transition_raises(self):
        args = self.settings.argparse.Namespace(candidate="a" * 40)
        ruleset = {
            "id": 754,
            "target": "tag",
            "enforcement": "active",
            "conditions": {"ref_name": {"include": self.settings.RULESET_PATTERNS, "exclude": []}},
            "rules": [{"type": "deletion"}],
        }
        with mock.patch.object(
            self.settings,
            "gh_api",
            side_effect=[
                (0, {}),
                self.settings.SettingsError("ruleset readback failed"),
                (0, {"object": {"sha": "a" * 40}}),
                (0, {}),
                (1, {"message": "Not Found", "status": "404"}),
            ],
        ) as api:
            with self.assertRaisesRegex(self.settings.SettingsError, "rolled back"):
                self.settings.apply_live_immutable_transition(
                    args,
                    ruleset,
                    "repos/example/project/rulesets/754",
                    "repos/example/project/git/ref/tags/1.12.0",
                    "repos/example/project/git/refs/tags/1.12.0",
                    "settings-token",
                    "tag-token",
                    created_this_run=True,
                )
        self.assertEqual("DELETE", api.call_args_list[3].kwargs["method"])

    def test_prepare_run_authority_binds_live_run_job_and_artifact_identity(self):
        args = self.settings.argparse.Namespace(
            repository="bluetape4k/bluetape4k-projects",
            workflow="release.yml",
            ref="develop",
            prepare_run_id=2002,
            request_id="release-2002",
            candidate="a" * 40,
        )
        valid = {
            "run": {
                "repository": {"full_name": args.repository},
                "head_sha": args.candidate,
                "head_branch": args.ref,
                "status": "completed",
                "conclusion": "success",
                "path": ".github/workflows/release.yml",
                "display_title": "issue-754-release-prepare-release-2002",
            },
            "artifacts": {
                "artifacts": [{
                    "name": "issue-754-tag-immutability-release-2002",
                    "expired": False,
                }],
            },
            "jobs": {
                "jobs": [{"name": "issue-754-tag-immutability", "conclusion": "success"}],
            },
        }

        def fake_api(endpoint, token, **kwargs):
            if endpoint.endswith("/artifacts?per_page=100"):
                return 0, valid["artifacts"]
            if endpoint.endswith("/jobs?filter=latest&per_page=100"):
                return 0, valid["jobs"]
            return 0, valid["run"]

        with mock.patch.object(self.settings, "gh_api", side_effect=fake_api):
            self.assertEqual([], self.settings.verify_prepare_run_authority(args, "token"))
        invalid = copy.deepcopy(valid)
        invalid["run"]["repository"]["full_name"] = "attacker/project"

        def fake_invalid_api(endpoint, token, **kwargs):
            if endpoint.endswith("/artifacts?per_page=100"):
                return 0, invalid["artifacts"]
            if endpoint.endswith("/jobs?filter=latest&per_page=100"):
                return 0, invalid["jobs"]
            return 0, invalid["run"]

        with mock.patch.object(self.settings, "gh_api", side_effect=fake_invalid_api):
            errors = self.settings.verify_prepare_run_authority(args, "token")
        self.assertTrue(any("repository" in error for error in errors), errors)

    def test_rollback_accepts_recognized_intermediate_state(self):
        pre_state = self.settings.empty_state("bluetape4k/bluetape4k-projects")
        with self.assertRaises(self.settings.PartialUpdateError) as raised:
            self.settings.apply_fixture(pre_state, self.desired, fail_after="release-environment")
        restored = self.settings.rollback_fixture(
            raised.exception.state,
            pre_state,
            raised.exception.journal,
        )
        self.assertEqual(pre_state, restored)

    def test_rollback_blocks_unknown_drift(self):
        pre_state = self.settings.empty_state("bluetape4k/bluetape4k-projects")
        result = self.settings.apply_fixture(pre_state, self.desired)
        drifted = copy.deepcopy(result["state"])
        drifted["ruleset"]["id"] = 999999
        with self.assertRaises(self.settings.UnknownDriftError):
            self.settings.rollback_fixture(drifted, pre_state, result["journal"])

    def test_offline_cli_commands_write_sanitized_state_without_secret_values(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fixture = root / "fixture.json"
            output = root / "snapshot.json"
            fixture.write_text(json.dumps(self.desired), encoding="utf-8")
            exit_code = self.settings.main(
                [
                    "snapshot",
                    "--repository",
                    "bluetape4k/bluetape4k-projects",
                    "--fixture-state",
                    str(fixture),
                    "--output",
                    str(output),
                ]
            )
            self.assertEqual(0, exit_code)
            payload = output.read_text(encoding="utf-8")
            self.assertNotIn("secretValue", payload)
            self.assertNotIn("privateKeyValue", payload)

    def test_cli_level_snapshot_apply_verify_probe_rollback_isolation_and_closeout_commands(self):
        repository = "bluetape4k/bluetape4k-projects"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            def write(name, payload):
                path = root / name
                path.write_text(json.dumps(payload, sort_keys=True) + "\n", encoding="utf-8")
                return path

            empty = write("empty.json", self.settings.empty_state(repository))
            desired = write("desired.json", self.desired)
            snapshot = root / "snapshot.json"
            self.assertEqual(0, self.settings.main([
                "snapshot", "--repository", repository, "--fixture-state", str(desired), "--output", str(snapshot)
            ]))

            verify = root / "verify.json"
            self.assertEqual(0, self.settings.main([
                "verify", "--repository", repository, "--fixture-state", str(desired), "--output", str(verify)
            ]))
            self.assertEqual("PASS", json.loads(verify.read_text())["decision"])

            applied = root / "applied.json"
            journal = root / "journal.json"
            self.assertEqual(0, self.settings.main([
                "apply", "--repository", repository, "--fixture-state", str(empty),
                "--desired-state", str(desired), "--output", str(applied), "--journal", str(journal)
            ]))

            events = write("events.json", self.valid_transition_input()["fixtureEvents"])
            probe = root / "probe.json"
            self.assertEqual(0, self.settings.main([
                "probe", "--repository", repository, "--fixture-state", str(desired),
                "--fixture-events", str(events), "--output", str(probe)
            ]))
            self.assertEqual("PASS", json.loads(probe.read_text())["decision"])

            workflows = write("workflows.json", {
                "release.yml": ["release-tag-1.12.0"],
                "publish-snapshot.yml": ["snapshot-publish-1.12.0"],
            })
            isolation = root / "isolation.json"
            self.assertEqual(0, self.settings.main([
                "prove-workflow-isolation", "--repository", repository, "--fixture-state", str(desired),
                "--workflow-fixture", str(workflows), "--output", str(isolation)
            ]))

            restored = root / "restored.json"
            self.assertEqual(0, self.settings.main([
                "rollback", "--repository", repository, "--fixture-state", str(applied),
                "--pre-state", str(empty), "--journal", str(journal), "--output", str(restored)
            ]))
            rollback_report = root / "rollback-report.json"
            self.assertEqual(0, self.settings.main([
                "verify-rollback", "--repository", repository, "--fixture-state", str(restored),
                "--pre-state", str(empty), "--journal", str(journal), "--output", str(rollback_report)
            ]))
            self.assertEqual("PASS", json.loads(rollback_report.read_text())["decision"])

            closeout_input = write("closeout-input.json", self.valid_transition_input())
            closeout = root / "closeout.json"
            self.assertEqual(0, self.settings.main([
                "immutable-closeout", "--input", str(closeout_input), "--output", str(closeout)
            ]))
            closeout_payload = json.loads(closeout.read_text())
            self.assertEqual("PASS", closeout_payload["conclusion"])
            artifact_directory = root / "closeout-artifact"
            artifact_directory.mkdir()
            write("closeout-artifact/closeout.json", closeout_payload)

            expected_args = [
                "--repository", repository,
                "--workflow", "release.yml",
                "--ref", "develop",
                "--prepare-run-id", "2002",
                "--candidate-validation-run-id", "1001",
                "--candidate-validation-request-id", "candidate-1001",
                "--tag", "1.12.0",
                "--candidate", "a" * 40,
                "--request-id", "release-2002",
                "--artifact", str(artifact_directory),
            ]
            report = root / "verify-immutable-artifact.json"
            self.assertEqual(0, self.settings.main([
                "verify-immutable-artifact", *expected_args, "--output", str(report)
            ]))
            self.assertEqual("PASS", json.loads(report.read_text())["decision"])

    def test_verify_immutable_closeout_requires_candidate_artifact(self):
        repository = "bluetape4k/bluetape4k-projects"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            artifact = root / "closeout.json"
            artifact.write_text(json.dumps(self.valid_closeout()), encoding="utf-8")
            output = root / "verify.json"
            args = [
                "verify-immutable-closeout",
                "--repository", repository,
                "--workflow", "release.yml",
                "--ref", "develop",
                "--prepare-run-id", "2002",
                "--candidate-validation-run-id", "1001",
                "--candidate-validation-request-id", "candidate-1001",
                "--tag", "1.12.0",
                "--candidate", "a" * 40,
                "--request-id", "release-2002",
                "--artifact", str(artifact),
                "--output", str(output),
            ]
            with mock.patch.dict(self.settings.os.environ, {"GH_TOKEN": "token"}, clear=True), mock.patch.object(
                self.settings, "verify_live_immutable_state", return_value=[]
            ):
                self.assertEqual(2, self.settings.main(args))

    def test_verify_immutable_closeout_requires_live_github_token(self):
        repository = "bluetape4k/bluetape4k-projects"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = {
                "candidateSha": "a" * 40,
                "runId": 1001,
                "requestId": "candidate-1001",
                "conclusion": "PASS",
                "testedCodeTreeSha256": "b" * 64,
                "slices": [
                    {
                        "name": item["name"],
                        "path": self.settings.CANDIDATE_SLICE_PATHS[item["name"]],
                        "conclusion": item["conclusion"],
                        "sha256": item["sha256"],
                    }
                    for item in self.valid_closeout()["candidateValidation"]["evidenceSlices"]
                ],
                "checksums": self.valid_closeout()["candidateValidation"]["checksums"],
            }
            candidate_artifact = root / "candidate.json"
            candidate_artifact.write_text(json.dumps(candidate), encoding="utf-8")
            closeout = self.valid_closeout()
            closeout["checksums"]["candidateArtifact"] = self.settings.hashlib.sha256(
                candidate_artifact.read_bytes()
            ).hexdigest()
            closeout_artifact = root / "closeout.json"
            closeout_artifact.write_text(json.dumps(closeout), encoding="utf-8")
            output = root / "verify.json"
            args = [
                "verify-immutable-closeout",
                "--repository", repository,
                "--workflow", "release.yml",
                "--ref", "develop",
                "--prepare-run-id", "2002",
                "--candidate-validation-run-id", "1001",
                "--candidate-validation-request-id", "candidate-1001",
                "--tag", "1.12.0",
                "--candidate", "a" * 40,
                "--request-id", "release-2002",
                "--artifact", str(closeout_artifact),
                "--candidate-artifact", str(candidate_artifact),
                "--output", str(output),
            ]
            with mock.patch.dict(self.settings.os.environ, {}, clear=True), mock.patch.object(
                self.settings, "validate_candidate_report", return_value=[]
            ):
                self.assertEqual(2, self.settings.main(args))

    def test_cli_probe_and_workflow_isolation_reject_wrong_actor_or_workflow(self):
        repository = "bluetape4k/bluetape4k-projects"
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            state = root / "state.json"
            state.write_text(json.dumps(self.desired), encoding="utf-8")

            wrong_events = self.valid_transition_input()["fixtureEvents"]
            wrong_events[0]["actor"] = "settings"
            events = root / "events.json"
            events.write_text(json.dumps(wrong_events), encoding="utf-8")
            result = self.settings.main([
                "probe", "--repository", repository, "--fixture-state", str(state),
                "--fixture-events", str(events), "--output", str(root / "probe.json")
            ])
            self.assertNotEqual(0, result)

            workflows = root / "workflows.json"
            workflows.write_text(json.dumps({"extra.yml": ["release-tag-1.12.0"]}), encoding="utf-8")
            result = self.settings.main([
                "prove-workflow-isolation", "--repository", repository, "--fixture-state", str(state),
                "--workflow-fixture", str(workflows), "--output", str(root / "isolation.json")
            ])
            self.assertEqual(3, result)


if __name__ == "__main__":
    unittest.main()
