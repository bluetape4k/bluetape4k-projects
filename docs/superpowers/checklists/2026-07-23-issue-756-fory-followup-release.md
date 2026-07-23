# Issue #756 Fory Follow-up Release Checklist

**Target version:** `1.12.0`
**Observed Maven Central version:** `1.11.0`
**Repository:** `https://repo.maven.apache.org/maven2`
**Publish authority:** Not granted by this checklist or issue workflow.

## Release scope

The release scope is limited to the raw Fory/FastFory serializer and Redis codec
paths in these artifacts:

| Target coordinate | Known-good rollback coordinate | Known-good JAR SHA-256 |
|---|---|---|
| `io.github.bluetape4k:bluetape4k-io:1.12.0` | `io.github.bluetape4k:bluetape4k-io:1.11.0` | `e5d41857bb7196c7fac8ecdfa773deb658f649ccbb78608064807fea1a823ea5` |
| `io.github.bluetape4k:bluetape4k-lettuce:1.12.0` | `io.github.bluetape4k:bluetape4k-lettuce:1.11.0` | `bd38da234b3dcd586d5a5458a95c4996c49585f945146fedb411a8c0810b962a` |
| `io.github.bluetape4k:bluetape4k-redisson:1.12.0` | `io.github.bluetape4k:bluetape4k-redisson:1.11.0` | `a8018e61ac2c0d3e592efdcf694d2785c709269f22378d00c8f000dfffc628a1` |

The `1.12.0` JAR hashes must be captured by the release executor from the
staged artifacts before publication. They do not exist in Maven Central yet
and must not be invented or inferred from local build output.

## Evidence pins

- [x] Aggregate benchmark manifest:
  `docs/benchmarks/raw/issue-756-fory-followup/manifest.json`
- [x] Aggregate benchmark manifest SHA-256:
  `68f81d30c406ab24770127b92c4bef2a11ebfc66169a7ccf648d02d7efd50aae`
- [x] The aggregate disposition records 20 canonical methods and
  `encodeDisposition=rejected`; Redisson encode therefore stays on the
  compatibility path.
- [x] Maven Central downloads are checksum-gated before they enter the
  known-good classpath:
  `docs/benchmarks/raw/issue-756-fory-followup/release/artifact-manifest.json`
- [x] Current and known-good classpaths are recorded:
  `docs/benchmarks/raw/issue-756-fory-followup/release/classpath-manifest.json`
- [x] Old-write/new-read and new-write/old-read pass for both Fory and FastFory:
  `docs/benchmarks/raw/issue-756-fory-followup/release/compatibility-results.json`
- [x] The non-publishing rollback smoke result is recorded:
  `docs/benchmarks/raw/issue-756-fory-followup/release/rollback-smoke.json`
- [x] Release evidence file hashes are recorded:
  `docs/benchmarks/raw/issue-756-fory-followup/release/release-manifest.json`

## Consumer and migration boundary

- [x] Consumers using raw `bluetape4k-io`, Lettuce, or Redisson Fory codecs are
  in scope.
- [x] Compression wrappers and compressed payload migration are out of scope.
- [x] No data migration is required when a consumer keeps the same Fory mode.
- [x] `FastForyCodec` keeps the existing asymmetric fallback: it can read
  compatible-mode Fory payloads, while `ForyCodec` cannot read FastFory
  payloads.
- [x] Registration-off defaults are for trusted payloads only.
- [x] Fory retains its internal reusable buffer; this release does not claim
  an end-to-end zero-copy serializer.

## Pre-publication hold

- [ ] Obtain explicit publication authority and identify the release executor.
- [ ] Build the exact `1.12.0` staging artifacts from the approved release
  commit.
- [ ] Record each staged `1.12.0` JAR SHA-256 and verify the dependency/BOM
  version set.
- [ ] Re-run the aggregate validator, compatibility runner, and rollback smoke
  from the exact release commit.
- [ ] Verify CI and review state for the exact release commit.
- [ ] Publish only through the repository release workflow after every hold is
  cleared.

## Rollback ownership and action

**Owner:** The explicitly authorized release executor.

If wire parity, ownership, exception behavior, benchmark validation, or release
smoke fails before publication, remove the affected direct candidate from the
release commit and repeat validation.

If a regression is found after publication:

1. Pin affected consumers back to the three `1.11.0` known-good coordinates
   and exact hashes above.
2. Run `python3 infra/redisson/scripts/run-issue756-fory-compatibility.py`.
3. Run `python3 infra/redisson/scripts/run-issue756-fory-rollback-smoke.py`.
4. Record whether the smoke used live Redis or the explicitly limited
   deterministic codec-level fallback.
5. Open a rollback issue/PR with the failing artifact version, hashes, and
   fixture result. Do not publish a replacement without fresh authority.

## Current handoff status

Release evidence preparation is complete, but publication is blocked by design
until explicit authority and staged `1.12.0` hashes exist. The recorded rollback
smoke used the deterministic codec-level path because Redis was unavailable;
this proves known-good codec round trips but does not claim a networked Redis
SET/GET result.
