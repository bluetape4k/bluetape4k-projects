# 이슈 #756 Fory 후속 release 체크리스트

**대상 버전:** `1.12.0`
**관찰된 Maven Central 버전:** `1.11.0`
**저장소:** `https://repo.maven.apache.org/maven2`
**Publish 권한:** 이 체크리스트나 issue workflow로는 부여되지 않았다.

## Release 범위

Release 범위는 다음 artifact의 raw Fory/FastFory serializer와 Redis codec path로 제한한다.

| 대상 좌표 | known-good rollback 좌표 | known-good JAR SHA-256 |
|---|---|---|
| `io.github.bluetape4k:bluetape4k-io:1.12.0` | `io.github.bluetape4k:bluetape4k-io:1.11.0` | `e5d41857bb7196c7fac8ecdfa773deb658f649ccbb78608064807fea1a823ea5` |
| `io.github.bluetape4k:bluetape4k-lettuce:1.12.0` | `io.github.bluetape4k:bluetape4k-lettuce:1.11.0` | `bd38da234b3dcd586d5a5458a95c4996c49585f945146fedb411a8c0810b962a` |
| `io.github.bluetape4k:bluetape4k-redisson:1.12.0` | `io.github.bluetape4k:bluetape4k-redisson:1.11.0` | `a8018e61ac2c0d3e592efdcf694d2785c709269f22378d00c8f000dfffc628a1` |

`1.12.0` JAR hash는 publication 전에 release executor가 staged artifact에서 캡처해야
한다. 아직 Maven Central에 존재하지 않으며 local build output에서 invent하거나 infer하면
안 된다.

## 증거 고정

- [x] Aggregate benchmark manifest: `docs/benchmarks/raw/issue-756-fory-followup/manifest.json`
- [x] Aggregate benchmark manifest SHA-256: `68f81d30c406ab24770127b92c4bef2a11ebfc66169a7ccf648d02d7efd50aae`
- [x] Aggregate disposition은 20개 canonical method와 `encodeDisposition=rejected`를 기록한다. 따라서 Redisson encode는 compatibility path에 남는다.
- [x] Maven Central download는 known-good classpath에 들어가기 전에 checksum-gated다: `docs/benchmarks/raw/issue-756-fory-followup/release/artifact-manifest.json`
- [x] Current/known-good classpath가 기록되어 있다: `docs/benchmarks/raw/issue-756-fory-followup/release/classpath-manifest.json`
- [x] Fory와 FastFory 모두에서 old-write/new-read, new-write/old-read가 통과했다: `docs/benchmarks/raw/issue-756-fory-followup/release/compatibility-results.json`
- [x] Non-publishing rollback smoke 결과가 기록되어 있다. Codec-level result가 `limited`이면 publication gate는 계속 blocked다: `docs/benchmarks/raw/issue-756-fory-followup/release/rollback-smoke.json`
- [x] Release evidence file hash가 기록되어 있다: `docs/benchmarks/raw/issue-756-fory-followup/release/release-manifest.json`

## Consumer와 migration 경계

- [x] Raw `bluetape4k-io`, Lettuce, Redisson Fory codec을 사용하는 consumer가 범위다.
- [x] Compression wrapper와 compressed payload migration은 범위 밖이다.
- [x] Consumer가 같은 Fory mode를 유지하면 data migration은 필요 없다.
- [x] `FastForyCodec`은 기존 asymmetric fallback을 유지한다. Compatible-mode Fory payload는 읽을 수 있지만 `ForyCodec`은 FastFory payload를 읽을 수 없다.
- [x] Registration-off default는 trusted payload 전용이다.
- [x] Fory는 내부 reusable buffer를 유지한다. 이 release는 end-to-end zero-copy serializer를 주장하지 않는다.

## 게시 전 hold

- [ ] 명시적인 publication authority와 release executor를 확보한다.
- [ ] 승인된 release commit에서 정확한 `1.12.0` staging artifact를 build한다.
- [ ] 각 staged `1.12.0` JAR SHA-256을 기록하고 dependency/BOM version set을 검증한다.
- [ ] Exact release commit에서 aggregate validator, compatibility runner, rollback smoke를 다시 실행한다.
- [ ] Rollback evidence는 `mode=redis`, `status=passed`, `publicationGate=passed`여야 한다. Codec-level fallback은 documentation-only evidence다.
- [ ] Exact release commit의 CI와 review 상태를 검증한다.
- [ ] 모든 hold가 해제된 뒤 repository release workflow로만 publish한다.

## Rollback 소유권과 조치

**소유자:** 명시적으로 승인된 release executor.

Publication 전에 wire parity, ownership, exception behavior, benchmark validation, release
smoke가 실패하면 release commit에서 영향을 받은 direct candidate를 제거하고 검증을
반복한다.

Publication 뒤 regression이 발견되면 다음 순서를 따른다.

1. 영향을 받은 consumer를 위 세 `1.11.0` known-good 좌표와 정확한 hash로 pin한다.
2. `python3 infra/redisson/scripts/run-issue756-fory-compatibility.py`를 실행한다.
3. `python3 infra/redisson/scripts/run-issue756-fory-rollback-smoke.py`를 실행한다.
4. Smoke가 live Redis를 사용했는지, 명시적으로 제한된 deterministic codec-level fallback을 사용했는지 기록한다.
5. 실패 artifact version, hash, fixture result를 담은 rollback issue/PR을 연다. Fresh authority 없이 replacement를 publish하지 않는다.

## 현재 handoff 상태

Release evidence 준비는 완료되었지만, 명시적 authority와 staged `1.12.0` hash가
존재할 때까지 publication은 의도적으로 blocked다. 기록된 rollback smoke는 Redis가 없어
deterministic codec-level path를 사용했다. `limited` 상태는 known-good codec round trip만
증명하며 networked Redis SET/GET 결과를 주장하지 않고 publication gate를 해제할 수 없다.
