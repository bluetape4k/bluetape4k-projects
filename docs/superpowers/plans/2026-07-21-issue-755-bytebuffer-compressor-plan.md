# Issue #755 Caller-Owned ByteBuffer Compressor 구현 계획

> **agentic worker용:** 필수 sub-skill: 이 계획은 superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task별 구현한다. 진행 상태는 checkbox(`- [ ]`) syntax로 추적한다.

**목표:** `Compressor`에 caller-owned `ByteBuffer` 압축/복원 API를 추가하고 LZ4, Deflate, Snappy, Zstd의 안전한 저할당 경로와 재현 가능한 allocation 증거를 제공한다.

**아키텍처:** 공개 API와 allocating compatibility fallback을 먼저 독립 core slice로 전달한다. 이후 LZ4, Deflate, Snappy, Zstd를 각각 독립 PR로 추가해 backend별 실패 계약과 rollback을 격리하고, 마지막 cross-cutting slice에서 singleton concurrency, caller 예제, benchmark evidence와 문서 parity를 수렴한다. 모든 codec 호출은 source 상태를 보존하고 target position을 성공 시에만 commit하며, native/JDK 경계는 승인 명세의 exact bound와 예외 분류를 따른다.

**기술 스택:** Kotlin 2.4.0 / language-api 2.3, Java 21, Gradle 9.x, JUnit 5, `io.bluetape4k.assertions`, lz4-java 1.11.0, snappy-java 1.1.10.8, zstd-jni 1.5.7-11, JDK `Deflater`/`Inflater`, kotlinx-benchmark/JMH 1.37, Python 3 evidence validator.

---

## 1. 승인 기준과 실행 경계

- Work type: **Type A - Full Feature**
- Basis: broad public API, 네 개 codec backend, JNI/native 실패 계약, ABI fixture, concurrency, allocation benchmark와 다중 PR 전달 train.
- 승인 명세:
  `docs/superpowers/specs/2026-07-21-issue-755-bytebuffer-compressor-design.md`
- 승인 명세 commits:
  `0ef9f67aa1b743eea2787ad5b8b8ec9f4e6ff343`,
  `26313f9b9abe710f4d4c518269f477c4b9e42508`
- issue: `bluetape4k/bluetape4k-projects#755`, milestone `1.12.0`, labels
  `enhancement`, `performance`, `infra/io`, assignee `debop`.
- 현재 coordination/core branch:
  `feat/issue-755-bytebuffer-compressor`, base `origin/develop`.
- 2026-07-21 historical machine-readable workflow run:
  `20260721T115110Z-8e06d9a0`, manifest SHA-256
  `3e9a92a4ea0bef991d0613c8fad9b4fa90b27c520e16d1588005105aebd27638`, initial receipt checksum `9e1435c164dd203997fe9d41a49d8d95cb68d60de6f5074e76471e051262456e`. WF-04A fallback terminal 상태는 `blocked`, sequence `5`, checksum
  `b0458931bc1b8a3f2b173f04116b010c938d804f737aae081daeed292ef3cda1`이다. 이 checksum은
  과거 실행의 immutable audit evidence이며 현재 mutation, readiness 또는 completion
  authority가 아니다.
- 이번 계획 승인 전 stop condition: implementation, push, PR creation을 시작하지 않는다.
- 계획 승인 후 stop condition: 각 PR은 exact-head CI/review 수렴 후 fresh merge 승인을 별도로 받고, 승인 전에는 다음 slice branch를 만들지 않는다.
- merge approval은 누적되지 않는다. core 승인으로 LZ4 또는 이후 PR merge를 대신할 수 없다.
- stable tag, publish, release, workflow dispatch는 범위 밖이다.
- diagram/chart는 N/A다. 숫자 authority는 raw JSON/CSV와 표이며 새 visual asset을 만들지 않는다.

### 1.1 2026-07-30 실행 재개 상태

- 현재 기준은 `origin/develop@fa07277c8c123c1093299e10cf09504f13d177a1`이다.
- core/API/ABI Task 1–3은 PR #1067, LZ4 Task 4는 PR #1091로 반영되었다.
- 이번 실행의 machine-readable workflow run은
  `20260730T121803Z-0b53afa0`이며, verified receipt checksum은
  `dcb772cdb463c2c7eaa26f6eb6736c983016bb94644052c438a59d6aa48c8777`이다.
- 현재 격리 worktree와 branch는 각각
  `.worktrees/issue-755-bytebuffer-codecs`,
  `feat/issue-755-bytebuffer-codecs`이다. 사용자가 승인한 현재 head 이름은
  Task 5의 `feat/issue-755-bytebuffer-deflate` 이름을 대체하지만, Task 5의 파일 범위,
  TDD 순서, 문서 범위와 검증 계약은 변경하지 않는다.
- 현재 실행 범위는 Task 5 Deflate slice다. 이 PR의 exact-head CI/review와 별도 merge
  승인 전에는 Task 6 Snappy branch 또는 구현을 시작하지 않는다.
- 2026-07-21의 terminal blocked fallback command와 checksum은 과거 audit record다.
  현재 mutation authority, lifecycle 및 completion evidence는 위 fresh run에서만 가져온다.

### 1.2 2026-07-31 Snappy 실행 상태

- Deflate Task 5는 PR #1229로 반영되었고 현재 기준은
  `origin/develop@53f9b86c562c8fb5ba2ddd539abc10ac2d406b1f`이다.
- 현재 격리 worktree와 branch는 `.worktrees/issue-755-snappy-bytebuffer`,
  `feat/issue-755-snappy-bytebuffer`다.
- 현재 실행 범위는 Task 6 Snappy slice다. 이 PR의 exact-head CI/review와 별도 merge
  승인 전에는 Task 7 Zstd branch 또는 구현을 시작하지 않는다.

## 2. Broad Backend Matrix 전달 topology

| 순서 | 역할              | head branch                                     | base              | 독립 결과                                                         | 다음 단계 진입 조건               |
|-----:|-------------------|-------------------------------------------------|-------------------|-------------------------------------------------------------------|-----------------------------------|
|    1 | core/API/ABI      | `feat/issue-755-bytebuffer-compressor`          | `develop`         | JVM default API, fallback, 공통 contract, ABI fixture, 초기 docs  | PR merge와 local sync 완료        |
|    2 | LZ4 backend       | `feat/issue-755-bytebuffer-lz4`                 | updated `develop` | heap/direct 전체 조합, bounded payload slice                      | PR merge와 local sync 완료        |
|    3 | Deflate backend   | `feat/issue-755-bytebuffer-codecs`              | updated `develop` | JDK buffer loop와 deterministic cleanup                           | PR merge와 local sync 완료        |
|    4 | Snappy backend    | `feat/issue-755-snappy-bytebuffer`              | updated `develop` | bounded direct→direct native path와 validation-first              | PR merge와 local sync 완료        |
|    5 | Zstd backend      | `feat/issue-755-bytebuffer-zstd`                | updated `develop` | declared-size native bound와 exact exception taxonomy             | PR merge와 local sync 완료        |
|    6 | adoption/evidence | `perf/issue-755-bytebuffer-compressor-evidence` | updated `develop` | cross-codec concurrency, examples, two canonical runs, final docs | PR merge-ready 보고 후 fresh 승인 |

각 slice는 하나의 worktree만 쓰고 native/JNI test와 benchmark는 다른 worktree와 병렬 실행하지 않는다. PR이 merge되면 `develop`을 fast-forward한 다음 ancestry를 확인하고 해당 local worktree를 final documented checklist/PR train completion까지 보존한다. coordination/core worktree와 모든 local/remote branch 삭제는 그 completion 뒤 별도 명시 cleanup 승인을 받기 전까지 수행하지 않는다.

### 2.1 모든 slice의 merge-ready와 post-approval checkpoint

각 PR은 다음 readiness block으로 exact head, required checks와 unresolved thread 0을 증명한다.

```bash
set -euo pipefail
repo='bluetape4k/bluetape4k-projects'
branch="$(git branch --show-current)"
pr_number="$(gh pr list --repo bluetape4k/bluetape4k-projects \
  --head "$branch" --base develop --state open \
  --json number --jq 'if length == 1 then .[0].number else error("expected exactly one open PR") end')"
expected_head="$(git rev-parse HEAD)"
test "$(gh pr view "$pr_number" --repo "$repo" --json headRefOid --jq .headRefOid)" = "$expected_head"
gh pr checks "$pr_number" --repo "$repo" --required
threads_json="$(gh api graphql \
  -F owner=bluetape4k -F name=bluetape4k-projects -F number="$pr_number" \
  -f query='query($owner:String!,$name:String!,$number:Int!){repository(owner:$owner,name:$name){pullRequest(number:$number){reviewThreads(first:100){nodes{isResolved} pageInfo{hasNextPage}}}}}')"
test "$(jq -r '.data.repository.pullRequest.reviewThreads.pageInfo.hasNextPage' <<<"$threads_json")" = false
unresolved="$(jq '[.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved | not)] | length' <<<"$threads_json")"
test "$unresolved" -eq 0
gh pr view "$pr_number" --repo "$repo" \
  --json number,url,headRefName,headRefOid,baseRefName,mergeStateStatus,reviews,statusCheckRollup

candidate_key="${branch//\//-}"
candidate_stamp="$(date -u +%Y%m%dT%H%M%SZ)"
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
readiness_run_id=20260730T121803Z-0b53afa0
readiness_run_file="$state_root/runs/$readiness_run_id/run.json"
python3 "$flow" --state-root "$state_root" verify --run-id "$readiness_run_id"
test "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["state"])' "$readiness_run_file")" = running
readiness_receipt_checksum="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["last_checksum"])' "$readiness_run_file")"
test "${#readiness_receipt_checksum}" -eq 64
candidate_file="/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/merge-candidates/${candidate_key}-${expected_head}-${candidate_stamp}.json"
test ! -e "$candidate_file"
```

review thread가 100개를 넘으면 `hasNextPage=true`로 fail closed하며 cursor pagination을 구현하기 전에는 merge-ready로 보고하지 않는다. 이어서 shell redirection/move가 아니라 `apply_patch`의
`Add File`로 위 no-clobber `candidate_file`에 다음 concrete JSON을 만든다. 모든 값은 방금 조회한 actual value이며 설명용 문자열을 남기지 않는다.

```json
{
  "pr_number": 1234,
  "head_sha": "40 lowercase hexadecimal characters",
  "head_branch": "exact feature branch",
  "base_branch": "develop",
  "readiness_receipt_checksum": "64 lowercase hexadecimal characters",
  "observed_at": "UTC RFC3339"
}
```

예시의 PR number/문자열은 실행 시 actual value로 교체한다. `apply_patch`가 existing path 때문에 실패하면 덮어쓰지 않고 새 timestamp를 가진 readiness snapshot 이름을 사용한다. parent directory는
`mkdir -p`로만 만들고 file content는 반드시 `apply_patch`로 쓴다. 생성 후 absolute path를 literal로 다시 지정하고 `candidate_sha256="$(shasum -a 256 "$candidate_file" | awk '{print $1}')"`로 exact file SHA-256을 계산하고 candidate absolute path, file SHA-256, PR number, head SHA를 함께 사용자에게 보고한 뒤 fresh merge 승인을 받는다.

승인 후에는 shell 상태나 current branch에서 candidate 이름을 재도출하지 않는다. 직전 approval request에 보고되어 사용자가 승인한 absolute path와 SHA-256을 shell literal로 복원하고 다음 block이 file hash와 pair를 검증한다. drift가 있으면 새 immutable candidate를 만들고 fresh 승인을 다시 받는다.

```bash
set -euo pipefail
repo='bluetape4k/bluetape4k-projects'
# Export both values as exact shell literals copied from the approved request; never derive them here.
: "${APPROVED_CANDIDATE:?exact approved candidate absolute path is required}"
: "${APPROVED_CANDIDATE_SHA256:?exact approved candidate file SHA-256 is required}"
approved_candidate="$APPROVED_CANDIDATE"
approved_candidate_sha256="$APPROVED_CANDIDATE_SHA256"
candidate_json="$(python3 - "$approved_candidate" "$approved_candidate_sha256" <<'PY'
import hashlib
import json
import os
import pathlib
import re
import stat
import sys

path = pathlib.Path(sys.argv[1])
expected_hash = sys.argv[2]
expected_parent = pathlib.Path(
    "/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/merge-candidates"
).resolve(strict=True)
if not path.is_absolute() or path.parent.resolve(strict=True) != expected_parent:
    raise SystemExit("candidate parent mismatch")
if not re.fullmatch(r"[A-Za-z0-9._-]+\.json", path.name):
    raise SystemExit("candidate filename is invalid")
if not re.fullmatch(r"[0-9a-f]{64}", expected_hash):
    raise SystemExit("candidate SHA-256 is invalid")
if not stat.S_ISREG(os.lstat(path).st_mode) or path.is_symlink():
    raise SystemExit("candidate must be a regular non-symlink file")

parent_fd = os.open(expected_parent, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
try:
    fd = os.open(path.name, os.O_RDONLY | os.O_NOFOLLOW, dir_fd=parent_fd)
    try:
        if not stat.S_ISREG(os.fstat(fd).st_mode):
            raise SystemExit("opened candidate is not regular")
        raw = b""
        while True:
            chunk = os.read(fd, 65536)
            if not chunk:
                break
            raw += chunk
    finally:
        os.close(fd)
finally:
    os.close(parent_fd)

if hashlib.sha256(raw).hexdigest() != expected_hash:
    raise SystemExit("candidate hash mismatch")
value = json.loads(raw)
required = {
    "pr_number", "head_sha", "head_branch", "base_branch",
    "readiness_receipt_checksum", "observed_at",
}
if set(value) != required:
    raise SystemExit("candidate schema mismatch")
if not isinstance(value["pr_number"], int) or isinstance(value["pr_number"], bool) or value["pr_number"] <= 0:
    raise SystemExit("candidate PR number is invalid")
if not re.fullmatch(r"[0-9a-f]{40}", value["head_sha"]):
    raise SystemExit("candidate head SHA is invalid")
if not re.fullmatch(r"[A-Za-z0-9._/-]+", value["head_branch"]) or ".." in value["head_branch"]:
    raise SystemExit("candidate branch is invalid")
if value["base_branch"] != "develop":
    raise SystemExit("candidate base is invalid")
if not re.fullmatch(r"[0-9a-f]{64}", value["readiness_receipt_checksum"]):
    raise SystemExit("candidate receipt checksum is invalid")
if not re.fullmatch(r"[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z", value["observed_at"]):
    raise SystemExit("candidate timestamp is invalid")
print(json.dumps(value, separators=(",", ":"), sort_keys=True))
PY
)"
pr_number="$(jq -er '.pr_number' <<<"$candidate_json")"
expected_head="$(jq -er '.head_sha' <<<"$candidate_json")"
branch="$(jq -er '.head_branch' <<<"$candidate_json")"
candidate_receipt_checksum="$(jq -er '.readiness_receipt_checksum' <<<"$candidate_json")"
current_run_file=/Users/debop/work/bluetape4k/.bluetape/runs/20260730T121803Z-0b53afa0/run.json
test "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["state"])' "$current_run_file")" = running
current_receipt_checksum="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["last_checksum"])' "$current_run_file")"
test "$candidate_receipt_checksum" = "$current_receipt_checksum"
test "$(git branch --show-current)" = "$branch"
test "$(git rev-parse HEAD)" = "$expected_head"

pr_json="$(gh pr view "$pr_number" --repo "$repo" \
  --json state,baseRefName,headRefName,headRefOid,mergeStateStatus)"
test "$(jq -r .state <<<"$pr_json")" = OPEN
test "$(jq -r .baseRefName <<<"$pr_json")" = develop
test "$(jq -r .headRefName <<<"$pr_json")" = "$branch"
test "$(jq -r .headRefOid <<<"$pr_json")" = "$expected_head"
test "$(jq -r .mergeStateStatus <<<"$pr_json")" = CLEAN

gh pr checks "$pr_number" --repo "$repo" --required
threads_json="$(gh api graphql \
  -F owner=bluetape4k -F name=bluetape4k-projects -F number="$pr_number" \
  -f query='query($owner:String!,$name:String!,$number:Int!){repository(owner:$owner,name:$name){pullRequest(number:$number){reviewThreads(first:100){nodes{isResolved} pageInfo{hasNextPage}}}}}')"
test "$(jq -r '.data.repository.pullRequest.reviewThreads.pageInfo.hasNextPage' <<<"$threads_json")" = false
test "$(jq '[.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved | not)] | length' <<<"$threads_json")" -eq 0
main_repo=/Users/debop/work/bluetape4k/bluetape4k-projects
test "$(git -C "$main_repo" branch --show-current)" = develop
test -z "$(git -C "$main_repo" status --porcelain)"

gh pr merge "$pr_number" --repo "$repo" \
  --squash --match-head-commit "$expected_head"
merge_oid="$(gh pr view "$pr_number" --repo "$repo" \
  --json state,mergedAt,mergeCommit --jq 'select(.state == "MERGED" and .mergedAt != null) | .mergeCommit.oid')"
test -n "$merge_oid"
git -C "$main_repo" fetch origin develop
test "$(git -C "$main_repo" branch --show-current)" = develop
test -z "$(git -C "$main_repo" status --porcelain)"
git -C "$main_repo" pull --ff-only origin develop
test "$(git -C "$main_repo" rev-parse HEAD)" = "$(git -C "$main_repo" rev-parse origin/develop)"
git -C "$main_repo" merge-base --is-ancestor "$merge_oid" origin/develop
```

merge checkpoint shell tests는 `gh` stub으로 live PR `state`, `baseRefName`, `headRefName`,
`headRefOid`, required check, unresolved thread를 하나씩 drift시키고 각 경우 merge stub call count가 0인지 검증한다. candidate tests는 symlink, merge-candidates 밖 resolved parent, hash 확인 시점 교체, extra/missing key, boolean/non-integer PR number, malformed SHA/branch/base/timestamp를 모두 거부한다.

이 checkpoint 성공 전 다음 branch/worktree를 만들지 않는다. merge 승인은 cleanup, remote branch deletion, tag/release 권한을 포함하지 않는다.

### 2.2 review-fix exact-head protocol

어느 six-perspective/CI/review 단계에서든 수정이 생기면 Lore commit 후 affected targeted test와 해당 slice full verification을 다시 실행한다. 그 다음 `test -z "$(git status --porcelain)"`, current branch를 push하고 local `HEAD`,
`git ls-remote origin "refs/heads/$(git branch --show-current)"` SHA, PR `headRefOid` equality를 확인한 뒤 readiness block 전체를 재실행한다. final slice 수정은 Step 10.6의 descendant/diff-allowlist/ABI/evidence validator까지 새
`delivery_head`로 다시 실행한다. review 전 test 결과나 commit SHA를 재사용하지 않는다.

## 3. 파일 구조와 책임

### Core/API/ABI slice

| 경로                                                                                        | 변경           | 책임                                                                         |
|---------------------------------------------------------------------------------------------|----------------|------------------------------------------------------------------------------|
| `io/io/src/main/kotlin/io/bluetape4k/io/compressor/Compressor.kt`                           | 수정           | 두 JVM default public method와 한국어 KDoc                                   |
| `io/io/src/main/kotlin/io/bluetape4k/io/compressor/CompressorBufferSupport.kt`              | 생성           | preflight, overlap 탐지, fallback, commit/rollback wrapper, BE header helper |
| `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferTestSupport.kt`      | 생성           | buffer/mark/sentinel fixture와 모든 compressor provider                      |
| `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferContractTest.kt`     | 생성           | fallback 및 전체 compressor 공통 상태/실패/wire contract                     |
| `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorBufferAbiCompatibilityTest.kt` | 생성           | frozen manifest/hash/default-method/runtime fixture 검증                     |
| `io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaContractTest.java` | 생성           | Java null 순서와 public invocation                                           |
| `io/io/src/test/resources/abi/issue-755/`                                                   | 생성           | baseline source, classfile-only jar, negative fixture, provenance manifest   |
| `scripts/check-compressor-buffer-abi.sh`                                                    | 생성           | pinned baseline/current ABI compile/runtime 검증                             |
| `scripts/check-compressor-buffer-docs.py`                                                   | 생성           | README locale marker/matrix/example/caveat parity 검증                       |
| `io/io/README.md`, `io/io/README.ko.md`                                                     | 수정           | API contract, fallback 한계, provisional storage matrix                      |
| `CHANGELOG.md`                                                                              | 수정           | `1.12.0` opt-in API와 rollback 정책                                          |
| `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`                                | 생성/후속 수정 | dependency surprise, slice 결과, evidence guard 누적                         |

### Backend slices

| Slice   | production             | tests                                | 문서                                                  |
|---------|------------------------|--------------------------------------|-------------------------------------------------------|
| LZ4     | `LZ4Compressor.kt`     | `LZ4CompressorByteBufferTest.kt`     | 양쪽 README matrix와 lesson LZ4 section               |
| Deflate | `DeflateCompressor.kt` | `DeflateCompressorByteBufferTest.kt` | 양쪽 README matrix와 lesson lifecycle section         |
| Snappy  | `SnappyCompressor.kt`  | `SnappyCompressorByteBufferTest.kt`  | 양쪽 README matrix와 lesson native-validation section |
| Zstd    | `ZstdCompressor.kt`    | `ZstdCompressorByteBufferTest.kt`    | 양쪽 README matrix와 lesson native-bound section      |

각 production 파일의 test seam은 같은 파일의 `internal interface`와 `internal forTesting`
factory로 제한한다. public constructor/factory와 wire format은 바꾸지 않는다. 각 backend README row를 commit하기 직전에 `python3 scripts/check-compressor-buffer-docs.py`와
`git diff --check`를 실행하고, marker/locale drift가 있으면 해당 slice에서 수정한다.

### Adoption/evidence slice

| 경로                                                                                               | 변경 | 책임                                                                 |
|----------------------------------------------------------------------------------------------------|------|----------------------------------------------------------------------|
| `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferIntegrationTest.kt`         | 생성 | built-in singleton 병렬 success/overflow/corruption 및 retry         |
| `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferKotlinExampleTest.kt`       | 생성 | canonical Kotlin example와 bounded growth retry compile/contract     |
| `io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaExampleTest.java`         | 생성 | canonical Java example compile/contract                              |
| `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmark.kt`     | 생성 | thread-local mutable caller-owned benchmark state                    |
| `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmarkTest.kt` | 생성 | dispatch/eligibility/state reset 검증                                |
| `io/io/scripts/run-bytebuffer-compressor-evidence.py`                                              | 생성 | no-clobber run, provenance capture, validation, two-run comparison   |
| `io/io/scripts/test_run_bytebuffer_compressor_evidence.py`                                         | 생성 | evidence validator fail-closed unit tests                            |
| `docs/benchmarks/raw/issue-755/run-<UTC>-<id>/`                                                    | 생성 | immutable JMH JSON, metadata, argv, environment, summary, validation |
| `docs/benchmarks/raw/issue-755/comparison.csv`                                                     | 생성 | 두 canonical run matched comparison                                  |
| `docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md`                                   | 생성 | allocation 판정과 throughput guard 결과                              |
| `docs/benchmarks/README.md`                                                                        | 수정 | report index                                                         |
| `io/io/Benchmark.md`                                                                               | 수정 | allocation report link와 throughput 비약 금지                        |
| `io/io/README.md`, `io/io/README.ko.md`, `CHANGELOG.md`                                            | 수정 | 최종 지원 행렬, examples, 결과와 한계                                |

## 4. 공통 구현 불변식

1. public signature는 정확히 다음 두 개다.

```kotlin
fun compress(source: ByteBuffer, target: ByteBuffer): Int
fun decompress(source: ByteBuffer, target: ByteBuffer): Int
```

2. Java null은 Kotlin parameter check가 먼저 raw `NullPointerException`을 낸다.
3. non-null preflight는 read-only target → detectable overlap → empty source 순서다.
4. source의 position/limit/mark/byteOrder는 성공과 실패 모두 보존한다.
5. target의 limit/mark/byteOrder는 보존하고, position만 성공 시 `start + written`으로 이동한다.
6. 실패 시 target position은 start이고 이미 덮어쓴 byte는 unspecified다.
7. returned `written`은 `0..initialTargetRemaining`이어야 하며 범위 밖이면 정확한
   `IllegalStateException`이다.
8. direct/read-only alias처럼 JVM API로 탐지할 수 없는 overlap은 unsupported caller precondition이다.
9. `Error`와 cancellation은 identity를 보존한다. Deflate는 operation failure가 있으면 그 throwable identity를 항상 primary로 유지하고 cleanup failure를 suppressed로 붙인다. operation failure가 없을 때만 cleanup failure 자체를 그대로 전파한다.
10. fallback은 correctness path이며 allocation 개선 대상으로 승격하지 않는다.

## 5. Step 3-P 위험 예측

| 위험                                                      | 조기 신호                                                        | 예방/완화                                                                                      | rollback 또는 rerun 지점                                                 |
|-----------------------------------------------------------|------------------------------------------------------------------|------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| JVM default overload가 legacy implementor/caller를 깨뜨림 | fixture compile/linkage failure, `isDefault=false`               | frozen baseline jar와 Java/Kotlin source/classfile runtime fixture를 core RED로 먼저 고정      | core PR 중단, interface method를 revert하고 spec 재개방                  |
| LZ4가 caller limit 뒤 capacity tail을 읽음                | truncated limit fixture가 성공하거나 consumed가 remaining 초과   | payload를 position 0, limit=capacity=remaining인 bounded slice로 전달                          | LZ4 override만 fallback으로 revert, LZ4 slice rerun                      |
| Snappy invalid input이 native crash 경계로 진입           | validation seam 뒤 uncompress 호출 count 증가                    | direct optimized 경로는 validation-first, exact range만 length/decode에 전달                   | Snappy override만 fallback으로 revert, forked targeted test rerun        |
| Zstd under-declared header가 declared size보다 많이 기록  | large target sentinel 변경, `dstSize` seam mismatch              | native destination을 `declaredOriginalSize`로 고정                                             | Zstd override만 fallback으로 revert, heap/direct corruption matrix rerun |
| Zstd retry 의미가 오분류됨                                | decompression mismatch가 `BufferOverflowException`               | compression `errDstSizeTooSmall`만 overflow; decompression은 cause-less exact ISE              | exception taxonomy RED부터 재실행                                        |
| Deflate loop가 무한 정지 또는 cleanup 누락                | zero-progress 반복, end count != 1                               | state table, progress snapshot, Deflate-local operation-primary cleanup helper, per-call codec | Deflate override revert, lifecycle seam tests부터 rerun                  |
| built-in singleton에 호출 간 mutable state 누출           | 병렬 roundtrip mismatch 또는 failure 이후 재시도 실패            | codec state는 per-call, dependency singleton은 documented thread-safe operation만 공유         | 해당 backend PR revert, concurrency test 반복                            |
| fallback target을 decompression resource bound로 오해     | highly-compressible payload가 target preflight 전에 큰 배열 생성 | KDoc/README/test에서 final-write-bound 한계를 명시                                             | 문서/contract test 미충족 시 core PR block                               |
| benchmark가 mutable buffers를 공유                        | `@State(Scope.Benchmark)` 또는 fork 간 오염                      | `@State(Scope.Thread)`, setup allocation, measured reset only                                  | evidence invalid, 같은 JAR로 smoke부터 rerun                             |
| allocation 개선을 fallback/mixed cell에 잘못 주장         | eligibility mismatch 또는 baseline pair 누락                     | fail-closed dispatch matrix와 two-run validator                                                | report promotion 거부, raw artifact 보존 후 새 run                       |
| benchmark rebase/JAR drift                                | commit/tree/JAR hash 불일치                                      | 두 run exact identity 일치 필수                                                                | 기존 evidence 무효화, 새 commit에서 두 run 재생성                        |
| throughput이 유의하게 퇴행                                | 두 run 모두 -20% 이하, error interval non-overlap                | allocation promotion 전 원인 분석과 design review                                              | evidence PR 중단, backend 또는 validation 설계 재개방                    |

---

## Task 0: 승인된 plan으로 workflow run과 core slice를 시작한다

**복잡도:** 낮음 **Dependency:** 이 계획의 사용자 승인 **Write
scope:** `.bluetape` coordinator state만 helper가 기록; repository source 변경 없음 **Pattern
skill:** `bluetape-workflow`, `bluetape-full-feature`

### Rejected 2026-07-22 coordinator reservation workaround

> **실행 금지:** 아래 reservation/active-lane workaround는 검토 과정에서 폐기했다. current runtime의
> `evaluate_run_completion`은 owner가 아닌 reservation lane도 `completed`가 아니면 run completion을
> 막고, pending reservation을 안전하게 완료할 native-work evidence가 없기 때문이다. 아래 기록은
> 검토된 대안과 폐기 근거를 보존할 뿐, Task 0 실행 authority가 아니다.

실행 중 `topology-register`가 아직 생성되지 않은 미래 owner lane을 거부한다는 current runtime contract가 확인되었다. 동시에 lane deadline은 `lane-create` 시점에 immutable이라, 사용자 merge 승인 뒤에 시작할 미래 lane을 미리 만든 뒤 그대로 dispatch하면 liveness evidence가 stale해진다. 이 보정은 product/API/ABI 설계를 바꾸지 않으며 아래 Task 0.3/0.4의 “현재 component만 lane-create” 문장과 generic lifecycle block보다 우선한다.

1. initial topology 등록 전 일곱 exact `*-lane`을 모두 **reservation
   lane**으로 생성한다. reservation은 native agent를 spawn하지 않고 `pending`으로만 유지한다. 현재 run에서 이미 생성된
   `core-api-lane`은 그대로 core reservation으로 취급한다.
2. initial topology는 reservation lane을 owner로 사용한다. `topology.json`의 component/check/dependency snapshot은 그대로 유지한다.
3. component dependency가 covered되고 실제 dispatch가 준비된 직전에 fresh timestamp로
   `<component>-exec-<attempt>` active lane을 생성한다. active lane은 reservation과 같은 write scope,
   `parent_lane_id=<component>-lane`, `replacement_count=0`, 30초 startup deadline, 10분 command deadline을 가진다.
4. `topology-<component>-active.json`으로 full seven-component snapshot을 재등록하되 해당 component의
   `owner_lane`만 active lane으로 바꾼다. runtime replay가 기존 coverage/evidence를 보존한다.
5. owner 교체가 성공한 뒤 reservation lane을 `lane-cancel`로 terminal 처리하고, active lane만
   `lane-start → native spawn → startup-ack`한다. native lane은 사용자 merge 승인 대기 전에 반드시 terminal 처리한다.
6. active lane이 15분 안에 완료되지 않으면 liveness contract에 따라 interrupt/main takeover를 수행하고 distinct attempt lane을 만든다. expired reservation deadline을 active evidence로 사용하지 않는다.

reservation agent id는 `<planned-agent-id>-reservation`, active agent id는 기존 artifact matrix의 planned agent id를 쓴다. current run의 `core-api-lane`만 이미 planned agent id로 생성됐으므로 그 id를 reservation identity로 유지하고 active core agent id를 `issue755-core-api-executor-1`로 쓴다. 이 예외는 coordinator identity에만 적용되며 source write scope와 review ownership은 변하지 않는다.

각 reservation input은
`/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator/reservations/<component>.json`, active input은 `coordinator/lanes/<component>-exec-<attempt>.json`, topology snapshot은
`/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology-<component>-active.json`에 둔다. 모든 JSON은 `apply_patch`로 만든다. 다음 순서를 exact authority로 사용한다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
receipt_head() {
  python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])'
}

for component_id in lz4 deflate snappy zstd benchmark-docs review-delivery; do
  python3 "$flow" --state-root "$state_root" lane-create \
    --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
    --input "$input_root/reservations/$component_id.json" \
    --evidence "$input_root/evidence/lane-reserve-$component_id.json"
done
python3 "$flow" --state-root "$state_root" topology-register \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --evidence /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology-evidence.json \
  --input /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology.json
python3 "$flow" --state-root "$state_root" verify --run-id "$run_id"
```

각 component dispatch는 active lane JSON/topology JSON/evidence를 먼저 생성한 뒤 다음 block을 사용한다.
`component_id`, `active_lane_id`, `active_agent_id`는 current component의 exact artifact identity다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
: "${component_id:?set exact component id}"
: "${active_lane_id:?set <component>-exec-<attempt>}"
: "${active_agent_id:?set exact active agent id}"
reservation_lane_id="$component_id-lane"
receipt_head() {
  python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])'
}

python3 "$flow" --state-root "$state_root" lane-create \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --input "$input_root/lanes/$active_lane_id.json" \
  --evidence "$input_root/evidence/lane-create-$active_lane_id.json"
python3 "$flow" --state-root "$state_root" topology-register \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --evidence "$input_root/evidence/topology-$component_id-active.json" \
  --input "/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology-$component_id-active.json"
cancelled_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
python3 "$flow" --state-root "$state_root" lane-cancel \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --lane-id "$reservation_lane_id" \
  --agent-id "$(python3 -c 'import json,sys; d=json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json")); print(d["lanes"][sys.argv[1]]["agent_id"])' "$reservation_lane_id")" \
  --at "$cancelled_at" --reason "reservation replaced by fresh-deadline active lane" \
  --evidence "$input_root/evidence/lane-cancel-$component_id-reservation.json"
started_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
python3 "$flow" --state-root "$state_root" lane-start \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --lane-id "$active_lane_id" --agent-id "$active_agent_id" --at "$started_at" \
  --evidence "$input_root/evidence/lane-start-$active_lane_id.json"
# Main session performs the native spawn now, then records startup-ack from the observed result.
```

### Active 2026-07-22 WF-04A fallback

current `bluetape-flow.py`는 seven-component topology의 모든 `owner_lane`이 initial
`topology-register` 전에 존재하도록 요구한다. 반면 future lane의 startup/command deadline은
`lane-create` 때 immutable이며, 이 plan은 각 PR merge 승인 뒤 다음 component를 dispatch하므로 future dispatch 시점에 deadline을 fresh하게 만들 수 없다. reservation을 cancel/block해도
`evaluate_run_completion`이 모든 lane의 `completed` 상태를 요구해 run을 닫을 수 없다.

따라서 current run `20260721T115110Z-8e06d9a0`은 helper의 `run-block`으로 terminal evidence를 남기고,
`bluetape-workflow` WF-04A failure branch에 따라 machine-readable coordinator 없이 이 문서의 router/common/Type-A checklist와 native subagent liveness contract로 계속한다. `.bluetape` receipt는 helper 외 경로로 수정하지 않는다. 이 fallback은 product/API/ABI, PR topology, merge 승인 gate를 변경하지 않는다. coordinator가 deferred owner lane 또는 lane-start deadline refresh를 지원하기 전에는 이 seven-component run을 재개하거나 새로 만들지 않는다.

`/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator/evidence/run-block-topology-deadline-incompatibility.json`
은 `apply_patch`로 다음 exact one-element list를 만든다.

```json
[{"kind":"tool","summary":"Block issue755 run because current coordinator cannot model deferred owner lanes with fresh deadlines","path":"docs/superpowers/plans/2026-07-21-issue-755-bytebuffer-compressor-plan.md"}]
```

다음 block만 active run terminalization authority다. `running`에서 중단되면 fresh receipt head로
`run-block`을 한 번 실행하고, 이미 `blocked`이면 mutation 없이 verify한다. 그 밖의 state는 exit 65로 fail closed한다. postcondition은 `state=blocked`, checksum chain valid, printed terminal checksum이다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
run_file=/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json
evidence=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator/evidence/run-block-topology-deadline-incompatibility.json
state="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["state"])' "$run_file")"
case "$state" in
  running)
    blocked_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    expected_head="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["last_checksum"])' "$run_file")"
    python3 "$flow" --state-root "$state_root" run-block \
      --run-id "$run_id" --owner-file "$owner" --expected-head "$expected_head" \
      --at "$blocked_at" \
      --reason "current coordinator cannot model deferred topology owners with fresh immutable deadlines" \
      --evidence "$evidence"
    ;;
  blocked) ;;
  *) exit 65 ;;
esac
python3 "$flow" --state-root "$state_root" verify --run-id "$run_id"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["state"] == "blocked"; print(d["last_checksum"])' "$run_file"
```

terminal block evidence는 sequence `5`, checksum
`b0458931bc1b8a3f2b173f04116b010c938d804f737aae081daeed292ef3cda1`로 확인됐다.

- [x] **Step 0.F — WF-04A fallback을 terminal evidence로 고정한다**
    - **Action:** helper-only `run-block`을 fresh receipt head에서 실행하고 receipt chain/state를 검증했다.
    - **Evidence:** run `20260721T115110Z-8e06d9a0`, sequence `5`, state `blocked`, checksum
      `b0458931bc1b8a3f2b173f04116b010c938d804f737aae081daeed292ef3cda1`, `verify` PASS.
    -
  **Failure:** checksum/state가 달라지면 source/PR progression을 멈추고 helper `verify`와 receipt diagnosis로 돌아간다. direct `.bluetape` mutation은 금지한다.

Task 0.1–0.5의 coordinator commands는 최초 계획과 실제 실패 지점을 보존하는 audit record다. **Active WF-04A fallback에서는 아래 Task 0.1–0.5
commands를 다시 실행하지
않는다.** source implementation은 CG-01–CG-10, A-01–A-09, TDD, native liveness, six-perspective review를 계속 적용한다.

#### Audit Step 0.1 — N/A under WF-04A: 최초 plan approval receipt record

repository edit 전에 `apply_patch`로 다음 두 regular JSON files를 만든다.

`/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/approval-evidence.json`:

```json
[{"kind":"user_approval","summary":"User approved the reviewed issue-755 implementation plan","path":"docs/superpowers/plans/2026-07-21-issue-755-bytebuffer-compressor-plan.md"}]
```

`/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/start-evidence.json`:

```json
[{"kind":"approved_plan","summary":"Begin core API slice from the approved issue-755 plan","path":"docs/superpowers/plans/2026-07-21-issue-755-bytebuffer-compressor-plan.md"}]
```

```bash
set -euo pipefail
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape \
  run-approve \
  --run-id 20260721T115110Z-8e06d9a0 \
  --owner-file /Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json \
  --expected-head 9e1435c164dd203997fe9d41a49d8d95cb68d60de6f5074e76471e051262456e \
  --evidence /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/approval-evidence.json
```

예상 결과: receipt가 `run-start`를 safe next로 반환하고 owner fencing value는 출력하지 않는다.

#### Audit Step 0.2 — N/A under WF-04A: 최초 run-start record

```bash
set -euo pipefail
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape \
  run-start \
  --run-id 20260721T115110Z-8e06d9a0 \
  --owner-file /Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json \
  --expected-head "$(python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])')" \
  --evidence /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/start-evidence.json

repo-status
git branch --show-current
git merge-base --is-ancestor origin/develop HEAD
```

예상 결과: branch는 `feat/issue-755-bytebuffer-compressor`, working tree clean, `origin/develop`가 ancestor다. 불일치하면 source edit 전에 중단한다.

#### Audit Step 0.3 — N/A under WF-04A: 실패한 topology registration record

`topology-register` input은 manifest의 일곱 component를 required로 등록한다.

| component         | owner lane             | dependencies     | required checks                                     |
|-------------------|------------------------|------------------|-----------------------------------------------------|
| `core-api`        | `core-api-lane`        | none             | `tests`, `abi`, `docs`, `review`, `ci`, `merge`     |
| `lz4`             | `lz4-lane`             | `core-api`       | `tests`, `docs`, `review`, `ci`, `merge`            |
| `deflate`         | `deflate-lane`         | `lz4`            | `tests`, `docs`, `review`, `ci`, `merge`            |
| `snappy`          | `snappy-lane`          | `deflate`        | `tests`, `docs`, `review`, `ci`, `merge`            |
| `zstd`            | `zstd-lane`            | `snappy`         | `tests`, `docs`, `review`, `ci`, `merge`            |
| `benchmark-docs`  | `benchmark-docs-lane`  | `zstd`           | `tests`, `benchmark`, `evidence`, `docs`, `review`  |
| `review-delivery` | `review-delivery-lane` | `benchmark-docs` | `tests`, `abi`, `evidence`, `review`, `ci`, `merge` |

coordinator artifact matrix는 다음 exact id/path를 사용한다.

| component         | lane id                | agent id                            | lane input                   | changed paths                        | check input/evidence prefix                                                          | component input/evidence                                                     |
|-------------------|------------------------|-------------------------------------|------------------------------|--------------------------------------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `core-api`        | `core-api-lane`        | `issue755-core-api-executor`        | `lanes/core-api.json`        | `changed-paths/core-api.json`        | `checks/core-api-<check>.json`, `evidence/check-core-api-<check>.json`               | `components/core-api.json`, `evidence/component-core-api.json`               |
| `lz4`             | `lz4-lane`             | `issue755-lz4-executor`             | `lanes/lz4.json`             | `changed-paths/lz4.json`             | `checks/lz4-<check>.json`, `evidence/check-lz4-<check>.json`                         | `components/lz4.json`, `evidence/component-lz4.json`                         |
| `deflate`         | `deflate-lane`         | `issue755-deflate-executor`         | `lanes/deflate.json`         | `changed-paths/deflate.json`         | `checks/deflate-<check>.json`, `evidence/check-deflate-<check>.json`                 | `components/deflate.json`, `evidence/component-deflate.json`                 |
| `snappy`          | `snappy-lane`          | `issue755-snappy-executor`          | `lanes/snappy.json`          | `changed-paths/snappy.json`          | `checks/snappy-<check>.json`, `evidence/check-snappy-<check>.json`                   | `components/snappy.json`, `evidence/component-snappy.json`                   |
| `zstd`            | `zstd-lane`            | `issue755-zstd-executor`            | `lanes/zstd.json`            | `changed-paths/zstd.json`            | `checks/zstd-<check>.json`, `evidence/check-zstd-<check>.json`                       | `components/zstd.json`, `evidence/component-zstd.json`                       |
| `benchmark-docs`  | `benchmark-docs-lane`  | `issue755-benchmark-docs-executor`  | `lanes/benchmark-docs.json`  | `changed-paths/benchmark-docs.json`  | `checks/benchmark-docs-<check>.json`, `evidence/check-benchmark-docs-<check>.json`   | `components/benchmark-docs.json`, `evidence/component-benchmark-docs.json`   |
| `review-delivery` | `review-delivery-lane` | `issue755-review-delivery-verifier` | `lanes/review-delivery.json` | `changed-paths/review-delivery.json` | `checks/review-delivery-<check>.json`, `evidence/check-review-delivery-<check>.json` | `components/review-delivery.json`, `evidence/component-review-delivery.json` |

모든 상대 path의 root는 `/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator/`다.
`review-delivery`는 read-only lane이라 write scope와 changed paths가 모두 `[]`다. review fix가 필요하면 Section 2.2에 따라 별도 correction lane을 만들고 verifier lane의 scope를 넓히지 않는다. 나머지 lane write scope는 다음 exact repository-relative array다.

| component        | exact write scope                                                                                                                                                                                                                                                                                                                                    |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `lz4`            | `io/io/src/main/kotlin/io/bluetape4k/io/compressor/LZ4Compressor.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/compressor/LZ4CompressorByteBufferTest.kt`, `io/io/README.md`, `io/io/README.ko.md`, `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`                                                                                      |
| `deflate`        | `io/io/src/main/kotlin/io/bluetape4k/io/compressor/DeflateCompressor.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/compressor/DeflateCompressorByteBufferTest.kt`, `io/io/README.md`, `io/io/README.ko.md`, `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`                                                                              |
| `snappy`         | `io/io/src/main/kotlin/io/bluetape4k/io/compressor/SnappyCompressor.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/compressor/SnappyCompressorByteBufferTest.kt`, `io/io/README.md`, `io/io/README.ko.md`, `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`                                                                                |
| `zstd`           | `io/io/src/main/kotlin/io/bluetape4k/io/compressor/ZstdCompressor.kt`, `io/io/src/test/kotlin/io/bluetape4k/io/compressor/ZstdCompressorByteBufferTest.kt`, `io/io/README.md`, `io/io/README.ko.md`, `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`                                                                                    |
| `benchmark-docs` | Task 8의 integration/example 3 files, Task 9의 benchmark/runner 4 files, `docs/benchmarks/raw/issue-755`, `docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md`, `docs/benchmarks/README.md`, `io/io/Benchmark.md`, `io/io/README.md`, `io/io/README.ko.md`, `CHANGELOG.md`, `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md` |

`benchmark-docs` lane JSON은 “Task 8/9 files” 축약을 쓰지 않고 해당 `Files` section의 일곱 exact file path를 펼친 array로 기록한다. core scope는 아래 JSON의 exact array다.
`changed-paths/*.json`은 lane 완료 직전 pinned base..HEAD diff와 `git status --porcelain=v1`을 합쳐 canonicalize한 실제 file array이며, lane input write scope 밖 path가 하나라도 있으면
`lane-complete`를 호출하지 않는다.

core lane은 `run-start`의 safe-next를 지키기 위해 topology보다 먼저 만든다. 다음 input/evidence는 execution-time RFC3339 timestamps와 Task 1–3의 exact `Files` array를 넣어 `apply_patch`로 생성한다.

```json
{
  "lane_id": "core-api-lane",
  "agent_id": "issue755-core-api-executor",
  "assignment": "Implement and verify issue 755 core API and ABI slice",
  "write_scope": [
    "io/io/src/main/kotlin/io/bluetape4k/io/compressor/Compressor.kt",
    "io/io/src/main/kotlin/io/bluetape4k/io/compressor/CompressorBufferSupport.kt",
    "io/io/src/test/kotlin/io/bluetape4k/io/compressor",
    "io/io/src/test/java/io/bluetape4k/io/compressor",
    "io/io/src/test/resources/abi/issue-755",
    "scripts/check-compressor-buffer-abi.sh",
    "scripts/check-compressor-buffer-docs.py",
    "io/io/README.md",
    "io/io/README.ko.md",
    "CHANGELOG.md",
    "docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md"
  ],
  "fallback": "main session reclaims the same bounded slice",
  "observed_at": "execution-time UTC RFC3339",
  "startup_ack_deadline": "observed_at plus 30 seconds",
  "command_deadline": "observed_at plus 10 minutes"
}
```

실제 JSON에는 위 설명 문자열 대신 계산된 UTC timestamp를 넣는다. assignment evidence는
`[{"kind":"plan","summary":"Assign issue755 core-api slice","path":"docs/superpowers/plans/2026-07-21-issue-755-bytebuffer-compressor-plan.md"}]`다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
python3 "$flow" --state-root "$state_root" lane-create \
  --run-id "$run_id" --owner-file "$owner" \
  --expected-head "$(python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])')" \
  --input "$input_root/lanes/core-api.json" \
  --evidence "$input_root/evidence/lane-create-core-api.json"
```

각 row는 exact topology fields
`id,required=true,description,owner_lane,required_checks,dependencies,evidence_refs=[],coverage_state="missing"`
로 `/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology.json`에 기록한다. 등록 evidence는
`kind=topology`, summary와 이 plan path를 가진 one-element JSON list다.

```bash
set -euo pipefail
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape \
  topology-register \
  --run-id 20260721T115110Z-8e06d9a0 \
  --owner-file /Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json \
  --expected-head "$(python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])')" \
  --evidence /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology-evidence.json \
  --input /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/topology.json
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape verify \
  --run-id 20260721T115110Z-8e06d9a0
```

core의 `lane-start → native spawn → startup-ack`은 topology verify 직후 Step 0.4 block으로 실행한다. 이후 component는 직전 dependency component가 covered이고 merge ancestry가 확인된 뒤 같은 block의
`lane-create`부터 시작한다. 모든 coordinator mutation의 `--expected-head`는 Git commit이 아니라 직전 receipt SHA-256이다. 첫 승인만 위 pinned checksum을 쓰고 이후에는
`run.json.last_checksum`을 fresh하게 읽는다.

#### Audit Step 0.4 — N/A under WF-04A: coordinator lifecycle record

각 component 시작 시 아래 case가 lane/agent/check set을 exact하게 고정한다. `core-api`만 Step 0.3에서 이미 `lane-create`했으므로 그 호출을 건너뛴다. 나머지는 동일 artifact matrix의 lane JSON과 fresh assignment evidence를 먼저 생성한다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
: "${component_id:?set to the exact next component id from the artifact matrix}"
case "$component_id" in
  core-api) lane_id=core-api-lane; agent_id=issue755-core-api-executor ;;
  lz4) lane_id=lz4-lane; agent_id=issue755-lz4-executor ;;
  deflate) lane_id=deflate-lane; agent_id=issue755-deflate-executor ;;
  snappy) lane_id=snappy-lane; agent_id=issue755-snappy-executor ;;
  zstd) lane_id=zstd-lane; agent_id=issue755-zstd-executor ;;
  benchmark-docs) lane_id=benchmark-docs-lane; agent_id=issue755-benchmark-docs-executor ;;
  review-delivery) lane_id=review-delivery-lane; agent_id=issue755-review-delivery-verifier ;;
  *) exit 64 ;;
esac

receipt_head() {
  python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])'
}

if test "$component_id" != core-api; then
  python3 "$flow" --state-root "$state_root" lane-create \
    --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
    --input "$input_root/lanes/$component_id.json" \
    --evidence "$input_root/evidence/lane-create-$component_id.json"
fi
started_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
python3 "$flow" --state-root "$state_root" lane-start \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --lane-id "$lane_id" --agent-id "$agent_id" --at "$started_at" \
  --evidence "$input_root/evidence/lane-start-$component_id.json"
# The main session now performs the recorded native spawn and confirms the exact agent id.
ack_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
python3 "$flow" --state-root "$state_root" startup-ack \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --lane-id "$lane_id" --agent-id "$agent_id" --at "$ack_at" \
  --evidence "$input_root/evidence/startup-ack-$component_id.json"
```

native agent의 구현/검증 turn이 끝나면 fresh merge 승인 대기 전에 actual diff를
`changed-paths/$component_id.json`에 고정하고 즉시 `lane-complete`를 실행해 15분 delegation deadline 안에서 lane을 terminal로 만든다. long build/JMH/CI poll은 main session이 별도 process로 계속하며 active native lane을 사람 승인 대기 동안 열어 두지 않는다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
: "${component_id:?set to the exact component id whose native lane finished}"
case "$component_id" in
  core-api) lane_id=core-api-lane; agent_id=issue755-core-api-executor ;;
  lz4) lane_id=lz4-lane; agent_id=issue755-lz4-executor ;;
  deflate) lane_id=deflate-lane; agent_id=issue755-deflate-executor ;;
  snappy) lane_id=snappy-lane; agent_id=issue755-snappy-executor ;;
  zstd) lane_id=zstd-lane; agent_id=issue755-zstd-executor ;;
  benchmark-docs) lane_id=benchmark-docs-lane; agent_id=issue755-benchmark-docs-executor ;;
  review-delivery) lane_id=review-delivery-lane; agent_id=issue755-review-delivery-verifier ;;
  *) exit 64 ;;
esac
receipt_head() {
  python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])'
}
completed_at="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
python3 "$flow" --state-root "$state_root" lane-complete \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --lane-id "$lane_id" --agent-id "$agent_id" --at "$completed_at" \
  --changed-paths "$input_root/changed-paths/$component_id.json" \
  --evidence "$input_root/evidence/lane-complete-$component_id.json"
```

각 `checks/$component_id-$check_id.json`은 exact
`{"component_id":"...","check_id":"...","passed":true}`이고, 대응 evidence JSON list는 actual command/exit status, PR/head 또는 merge OID를 담는다. lane-complete 직후 main session은 다음 self-contained block으로 non-merge checks만 기록한다. `benchmark-docs`는 merge check가 없으므로 이 시점에 component evidence까지 닫는다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
: "${component_id:?set to the exact completed component id}"
case "$component_id" in
  core-api) checks='tests abi docs review ci'; merge_required=true ;;
  lz4|deflate|snappy|zstd) checks='tests docs review ci'; merge_required=true ;;
  benchmark-docs) checks='tests benchmark evidence docs review'; merge_required=false ;;
  review-delivery) checks='tests abi evidence review ci'; merge_required=true ;;
  *) exit 64 ;;
esac
receipt_head() {
  python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])'
}
for check_id in $checks; do
  python3 "$flow" --state-root "$state_root" check-result \
    --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
    --input "$input_root/checks/$component_id-$check_id.json" \
    --evidence "$input_root/evidence/check-$component_id-$check_id.json"
done
if test "$merge_required" = false; then
  python3 "$flow" --state-root "$state_root" component-evidence \
    --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
    --input "$input_root/components/$component_id.json" \
    --evidence "$input_root/evidence/component-$component_id.json"
fi
python3 "$flow" --state-root "$state_root" verify --run-id "$run_id"
```

merge-required component는 이 상태에서 candidate path/hash를 보고하고 fresh 사용자 승인을 기다린다. 승인 후 Section 2.1 merge/local-sync/ancestry가 성공하면 root가 다음 독립 block으로 merge check와 component evidence만 기록한다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
owner=/Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json
input_root=/Users/debop/work/bluetape4k/.bluetape/inputs/issue755/coordinator
: "${component_id:?set to the exact just-merged component id}"
case "$component_id" in core-api|lz4|deflate|snappy|zstd|review-delivery) ;; *) exit 64 ;; esac
receipt_head() {
  python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])'
}
python3 "$flow" --state-root "$state_root" check-result \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --input "$input_root/checks/$component_id-merge.json" \
  --evidence "$input_root/evidence/check-$component_id-merge.json"
python3 "$flow" --state-root "$state_root" component-evidence \
  --run-id "$run_id" --owner-file "$owner" --expected-head "$(receipt_head)" \
  --input "$input_root/components/$component_id.json" \
  --evidence "$input_root/evidence/component-$component_id.json"
python3 "$flow" --state-root "$state_root" verify --run-id "$run_id"
```

`components/$component_id.json`은 exact `{"component_id":"<component_id>"}`다. component evidence는 그 component의 check evidence path들과 final commit/PR/merge identity를 fresh refs로 가진다. 모든 일곱 component에 대해 closure block과 `verify`가 성공해야 Task 11 `completion-check`로 간다.

#### Audit Step 0.5 — N/A under WF-04A: original coordinator safety block record

```bash
set -euo pipefail
smoke_dir="$(mktemp -d)"
trap 'rm -rf "$smoke_dir"' EXIT
if bash -c 'set -euo pipefail; false; touch "$1/unsafe-mutation"' _ "$smoke_dir"; then
  exit 1
fi
test ! -e "$smoke_dir/unsafe-mutation"
plan=docs/superpowers/plans/2026-07-21-issue-755-bytebuffer-compressor-plan.md
awk '
  /^```bash$/ {
    if ((getline first) <= 0) { print "unterminated bash fence" > "/dev/stderr"; bad=1; next }
    if (first != "set -euo pipefail" && first != "#!/usr/bin/env bash") {
      print "bash fence is not fail-fast at line " NR > "/dev/stderr"
      bad=1
    }
  }
  END { exit bad }
' "$plan"
```

예상 결과: deliberate failed precondition 뒤 marker mutation은 실행되지 않는다. readiness, approved merge, coordinator, verification, evidence, commit/push block은 모두 첫 명령이
`set -euo pipefail`이고, standalone script definition만 shebang을 첫 줄로 허용한다.

**Step
DoD:** WF-04A fallback에서 run의 blocked checksum과 valid receipt chain이 고정되고, core slice의 branch/base/clean boundary 및 documented checklist 실행 authority가 증명된다. initial seven-component topology registration은 current runtime incompatibility evidence로 FAIL/N/A가 아니라 WF-04A fallback 처리되며 source implementation을 block하지 않는다.

---

## Task 1: Core RED — public contract와 ABI authority를 먼저 고정한다

**복잡도:** 높음 **Dependency:** Task 0 WF-04A fallback DoD PASS **Write
scope:** core test/ABI resources와 `scripts/check-compressor-buffer-abi.sh`만 **Pattern
skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**파일:**

- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferTestSupport.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferContractTest.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorBufferAbiCompatibilityTest.kt`
- 생성: `io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaContractTest.java`
- 생성: `io/io/src/test/resources/abi/issue-755/src/java/LegacyCompressorCaller.java`
- 생성: `io/io/src/test/resources/abi/issue-755/src/java/LegacyCompressorImplementation.java`
- 생성: `io/io/src/test/resources/abi/issue-755/src/java/NewCompressorBufferCaller.java`
- 생성: `io/io/src/test/resources/abi/issue-755/src/java/AmbiguousNullCaller.java`
- 생성: `io/io/src/test/resources/abi/issue-755/src/kotlin/LegacyCompressorCaller.kt`
- 생성: `io/io/src/test/resources/abi/issue-755/src/kotlin/LegacyCompressorImplementation.kt`
- 생성: `io/io/src/test/resources/abi/issue-755/pre-change/legacy-compressor-fixtures.jar`
- 생성: `io/io/src/test/resources/abi/issue-755/pre-change/manifest.json`
- 생성: `scripts/check-compressor-buffer-abi.sh`

- [ ] **Step 1.1: 공통 buffer fixture와 fallback compressor를 작성한다**

```kotlin
package io.bluetape4k.io.compressor

import java.nio.ByteBuffer

internal object CompressorByteBufferTestSupport {
    val payload: ByteArray = "caller-owned compressor payload".repeat(32).encodeToByteArray()

    fun heap(bytes: ByteArray, prefix: Int = 7, suffix: Int = 11): ByteBuffer =
        ByteBuffer.allocate(prefix + bytes.size + suffix).apply {
            position(prefix)
            put(bytes)
            limit(position())
            position(prefix)
            mark()
        }

    fun direct(bytes: ByteArray, prefix: Int = 7, suffix: Int = 11): ByteBuffer =
        ByteBuffer.allocateDirect(prefix + bytes.size + suffix).apply {
            position(prefix)
            put(bytes)
            limit(position())
            position(prefix)
            mark()
        }

    fun writableTarget(capacity: Int, direct: Boolean, prefix: Int = 5): ByteBuffer =
        (if (direct) ByteBuffer.allocateDirect(prefix + capacity + 13)
         else ByteBuffer.allocate(prefix + capacity + 13)).apply {
            repeat(capacity + 13) { put(0x5A) }
            position(prefix)
            limit(prefix + capacity)
            mark()
        }

    fun bytes(buffer: ByteBuffer, start: Int, size: Int): ByteArray =
        ByteArray(size).also { buffer.duplicate().position(start).get(it) }
}

internal class ReversingFallbackCompressor: Compressor {
    override fun compress(plain: ByteArray?): ByteArray = plain.orEmpty().reversedArray()
    override fun decompress(compressed: ByteArray?): ByteArray = compressed.orEmpty().reversedArray()
}
```

- [ ] **Step 1.2: 공통 success/state/overflow RED를 작성한다**

```kotlin
class CompressorByteBufferContractTest {
    private val fallback = ReversingFallbackCompressor()

    companion object {
        @JvmStatic
        fun allCompressors(): Stream<Arguments> = Stream.of(
            Arguments.of("apache-deflate", Compressors.ApacheDeflate),
            Arguments.of("deflate", Compressors.Deflate),
            Arguments.of("apache-gzip", Compressors.ApacheGZip),
            Arguments.of("gzip", Compressors.GZip),
            Arguments.of("lz4", Compressors.LZ4),
            Arguments.of("block-lz4", Compressors.BlockLZ4),
            Arguments.of("framed-lz4", Compressors.FramedLZ4),
            Arguments.of("snappy", Compressors.Snappy),
            Arguments.of("framed-snappy", Compressors.FramedSnappy),
            Arguments.of("apache-zstd", Compressors.ApacheZstd),
            Arguments.of("zstd", Compressors.Zstd),
            Arguments.of("bzip2", Compressors.BZip2),
            Arguments.of("zip", Compressors.Zip),
            Arguments.of("test-fallback", ReversingFallbackCompressor()),
        )
    }

    @Test
    fun `fallback preserves source and commits only target position`() {
        val source = CompressorByteBufferTestSupport.direct(CompressorByteBufferTestSupport.payload)
        val sourcePosition = source.position()
        val sourceLimit = source.limit()
        val target = CompressorByteBufferTestSupport.writableTarget(4096, direct = true)
        val targetStart = target.position()
        val targetLimit = target.limit()

        val written = fallback.compress(source, target)

        written shouldBeEqualTo CompressorByteBufferTestSupport.payload.size
        source.position() shouldBeEqualTo sourcePosition
        source.limit() shouldBeEqualTo sourceLimit
        source.reset().position() shouldBeEqualTo sourcePosition
        target.position() shouldBeEqualTo targetStart + written
        target.limit() shouldBeEqualTo targetLimit
        target.reset().position() shouldBeEqualTo targetStart
    }

    @Test
    fun `overflow leaves source and target positions unchanged`() {
        val source = CompressorByteBufferTestSupport.heap(CompressorByteBufferTestSupport.payload)
        val target = CompressorByteBufferTestSupport.writableTarget(1, direct = false)
        val sourceStart = source.position()
        val targetStart = target.position()

        assertFailsWith<BufferOverflowException> { fallback.compress(source, target) }

        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart
        source.reset().position() shouldBeEqualTo sourceStart
        target.reset().position() shouldBeEqualTo targetStart
    }

    @ParameterizedTest(name = "{0} common caller-owned roundtrip and state")
    @MethodSource("allCompressors")
    fun `all built-ins obey the common contract`(name: String, compressor: Compressor) {
        val payload = CompressorByteBufferTestSupport.payload
        val wire = compressor.compress(payload)
        val source = CompressorByteBufferTestSupport.direct(wire)
        val target = CompressorByteBufferTestSupport.writableTarget(payload.size, direct = true)
        val sourceStart = source.position()
        val targetStart = target.position()

        compressor.decompress(source, target) shouldBeEqualTo payload.size

        source.position() shouldBeEqualTo sourceStart
        target.position() shouldBeEqualTo targetStart + payload.size
        CompressorByteBufferTestSupport.bytes(target, targetStart, payload.size) shouldBeEqualTo payload
    }
}
```

success/state/overflow/preflight matrix도 같은 `allCompressors` source를 사용한다. codec별 wire 크기 차이는 `compressor.compress(payload)`로 준비하고 fixed size를 가정하지 않는다. pre-created failure identity cases만 deterministic `ReversingFallbackCompressor` seam을 사용한다.

같은 test class에 다음 exact cases를 parameterized fixture로 추가한다.

| case                                                          | expected                                         |
|---------------------------------------------------------------|--------------------------------------------------|
| heap/direct source × heap/direct target roundtrip             | 반환량과 bounded result가 동일                   |
| non-zero position, bounded limit, slice                       | source 전체 상태 보존, limit 밖 `0x5A` 유지      |
| read-only target + same object/empty                          | raw `ReadOnlyBufferException` 우선               |
| writable same object + empty                                  | `IllegalArgumentException`                       |
| same backing array partial/full overlap                       | `IllegalArgumentException`                       |
| empty non-overlap source                                      | `0`, codec 미호출, target 불변                   |
| pre-created `Error`, `CancellationException`, runtime failure | `assertSame` identity                            |
| overflow/corruption 후 같은 target 재사용                     | valid retry 성공                                 |
| highly-compressible fallback decompress + tiny target         | 내부 ByteArray 생성 후 `BufferOverflowException` |
| 기존/new API 양방향 wire                                      | 양쪽 roundtrip 성공                              |

- [ ] **Step 1.3: Java null/preflight RED를 작성한다**

```java
package io.bluetape4k.io.compressor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class CompressorByteBufferJavaContractTest {
    private final Compressor compressor = new ReversingFallbackCompressor();

    @Test
    void nullArgumentsFailBeforeBufferPreflight() {
        ByteBuffer readOnly = ByteBuffer.allocate(8).asReadOnlyBuffer();
        assertThrows(NullPointerException.class, () -> compressor.compress(null, readOnly));
        assertThrows(NullPointerException.class, () -> compressor.compress(ByteBuffer.allocate(1), null));
        assertThrows(NullPointerException.class, () -> compressor.decompress(null, null));
    }
}
```

- [ ] **Step 1.4: ABI fixture source와 provenance를 고정한다**

`LegacyCompressorImplementation.java`는 두 `ByteArray` abstract method만 구현하고,
`LegacyCompressorCaller.java`/`.kt`는 pre-change API만 호출한다. classfile-only jar에는
`Compressor.class`를 넣지 않는다. `NewCompressorBufferCaller.java`는 current jar에서 두 신규 method를 호출한다. `AmbiguousNullCaller.java`는 `compress(null)` 한 줄로 의도적 overload ambiguity를 만든다.

fixture jar를 만든 직후 script가 실제 hash를 계산하고 다음 Python 호출로 manifest 전체를 결정적으로 생성한다. 사람이 hash 문자열을 손으로 치환하지 않는다.

```bash
set -euo pipefail
fixture_sha="$(sha256 "$FIXTURE_ROOT/pre-change/legacy-compressor-fixtures.jar")"
python3 - "$FIXTURE_ROOT/pre-change/manifest.json" "$fixture_sha" <<'PY'
import json
import pathlib
import sys

manifest_path = pathlib.Path(sys.argv[1])
fixture_sha = sys.argv[2]
if len(fixture_sha) != 64 or any(ch not in "0123456789abcdef" for ch in fixture_sha):
    raise SystemExit("fixture SHA-256 must be 64 lowercase hexadecimal characters")
manifest = {
    "schemaVersion": 1,
    "producer": {
        "commit": "a065a8e88cf246975660c68df2dd78dfb5b6dc4d",
        "tree": "50cf7789648c0091b6c16de6cf5eb495c26510f8",
    },
    "compiler": {
        "java": "21",
        "kotlin": "2.4.0",
        "languageVersion": "2.3",
        "apiVersion": "2.3",
        "jvmDefault": "enable",
    },
    "baselineJar": {
        "path": "bluetape4k-io-1.12.0.jar",
        "sha256": "34d280b0cb465ffca2a23a2aa57895cc3ba9c08ea18f57c706443b91a0eae6f1",
    },
    "fixtureJar": {
        "path": "legacy-compressor-fixtures.jar",
        "containsCompressorClass": False,
        "sha256": fixture_sha,
    },
}
manifest_path.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
PY
```

- [ ] **Step 1.5: ABI script를 issue-754 패턴에서 좁혀 구현한다**

```bash
#!/usr/bin/env bash
set -euo pipefail

readonly BASE_SHA="a065a8e88cf246975660c68df2dd78dfb5b6dc4d"
readonly BASE_TREE="50cf7789648c0091b6c16de6cf5eb495c26510f8"
readonly BASE_JAR_SHA="34d280b0cb465ffca2a23a2aa57895cc3ba9c08ea18f57c706443b91a0eae6f1"
ROOT="$(git rev-parse --show-toplevel)"
FIXTURE_ROOT="$ROOT/io/io/src/test/resources/abi/issue-755"
AUTH_DIR="$ROOT/.codex/compat/issue-755/$BASE_SHA"
EXPECTED_HEAD=""
BUILD_CURRENT=false

fail() { echo "ERROR: $*" >&2; exit 1; }
sha256() { shasum -a 256 "$1" | awk '{print $1}'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --build-current) BUILD_CURRENT=true; shift ;;
    --expected-head) EXPECTED_HEAD="$2"; shift 2 ;;
    *) fail "unknown argument: $1" ;;
  esac
done

[[ "$BUILD_CURRENT" == true ]] || fail "--build-current is required"
[[ "$EXPECTED_HEAD" =~ ^[0-9a-f]{40}$ ]] || fail "full --expected-head is required"
[[ "$(git -C "$ROOT" rev-parse HEAD)" == "$EXPECTED_HEAD" ]] || fail "head drift"
[[ -z "$(git -C "$ROOT" status --porcelain -- io/io/src scripts/check-compressor-buffer-abi.sh)" ]] ||
  fail "dirty compressor ABI paths"

ensure_base_worktree
verify_base_jar
compile_legacy_fixtures
write_and_verify_manifest
run_legacy_classfiles
verify_ambiguous_null
verify_jvm_defaults
compile_new_callers
echo "COMPRESSOR BUFFER ABI PASS head=$EXPECTED_HEAD"
```

함수 계약은 다음과 같이 고정한다.

| 함수                        | exact contract                                                                                                                                                                                 |
|-----------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ensure_base_worktree`      | detached baseline을 `BASE_SHA`로 만들고 `rev-parse HEAD^{tree}`가 `BASE_TREE`와 같은지 검증한다. trap으로 임시 worktree를 제거한다.                                                            |
| `verify_base_jar`           | baseline에서 `:bluetape4k-io:jar`를 실행하고 산출물 hash가 `BASE_JAR_SHA`인지 검증한다.                                                                                                        |
| `compile_legacy_fixtures`   | `javac --release 21`과 Gradle-resolved Kotlin compiler classpath로 legacy Java/Kotlin source를 compile하고, jar에 `Compressor.class`가 없음을 `jar tf`로 검증한다.                             |
| `write_and_verify_manifest` | 위 Python block으로 manifest를 생성하고 producer/compiler/jar hash를 다시 읽어 비교한다.                                                                                                       |
| `run_legacy_classfiles`     | fixture classfile을 재compile하지 않고 current jar와 함께 실행한다.                                                                                                                            |
| `verify_ambiguous_null`     | `LC_ALL=C`에서 baseline jar와 current jar 각각 `AmbiguousNullCaller.java` compile을 실패시키고 양쪽 normalized diagnostic에 `reference to compress is ambiguous`가 동일하게 존재함을 요구한다. |
| `verify_jvm_defaults`       | `javap -v Compressor`에서 두 descriptor가 존재하고 `ACC_ABSTRACT`가 아님을 검증한다.                                                                                                           |
| `compile_new_callers`       | `verify_jvm_defaults`가 PASS한 뒤에만 current jar로 신규 Java/Kotlin caller를 compile하고 두 default method를 실행한다.                                                                        |

구현은 `scripts/check-serializer-buffer-abi.sh`의 worktree/trap/compiler-resolution 함수를 동일한 구조로 재사용하되 serializer 경로와 symbol을 위 compressor contract로 좁힌다. 어느 단계도 PATH의 임의 `kotlinc`를 사용하지 않는다.

- [ ] **Step 1.6: RED를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferContractTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferJavaContractTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: compile FAIL. `Compressor.compress(ByteBuffer, ByteBuffer)`와
`decompress(ByteBuffer, ByteBuffer)`가 아직 없다. dirty fixture tree에서 ABI checker를 실행하지 않는다. 그 경우 intended descriptor RED가 아니라 dirty-path gate가 먼저 실패하기 때문이다.

- [ ] **Step 1.7: RED fixture를 Lore commit한다**

```bash
set -euo pipefail
git add io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferTestSupport.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferContractTest.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorBufferAbiCompatibilityTest.kt \
  io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaContractTest.java \
  io/io/src/test/resources/abi/issue-755 scripts/check-compressor-buffer-abi.sh
git commit -m 'Lock caller-owned compressor compatibility before adding defaults' \
  -m 'Constraint: Existing Java and Kotlin implementors must link without recompilation within JVM default-method rules
Rejected: Infer compatibility from source compilation alone | It misses classfile linkage and overload ambiguity
Confidence: high
Scope-risk: moderate
Directive: Keep the frozen baseline provenance and classfile-only fixture reproducible
Tested: Targeted contract tests fail because the two-argument API is absent
Not-tested: Exact-head ABI RED runs immediately after this fixture commit; production defaults and backend overrides are intentionally not implemented'
```

- [ ] **Step 1.8: committed exact HEAD에서 ABI RED를 실행한다**

```bash
set -euo pipefail
test -z "$(git status --porcelain -- io/io/src scripts/check-compressor-buffer-abi.sh)"
bash scripts/check-compressor-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
```

예상 결과: dirty-path gate와 baseline/current ambiguous-null diagnostic 비교는 PASS하고
`compile_new_callers`에 도달하기 전에 current two-argument JVM default descriptor 부재라는 ABI reason으로 non-zero 종료한다. 다른 이유로 실패하면 Task 2로 진행하지 않고 fixture/checker를 교정해 Lore commit 후 이 step을 재실행한다.

**Step DoD:** 공통 계약과 ABI가 의도한 missing-API 이유로 RED이며 baseline authority가 hash로 고정된다.

---

## Task 2: Core GREEN — fallback과 상태 commit wrapper를 구현한다

**복잡도:** 높음 **Dependency:** Task 1 **Write
scope:** `Compressor.kt`, 신규 `CompressorBufferSupport.kt`, Task 1 test 보정만 **Pattern
skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**파일:**

- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/compressor/Compressor.kt`
- 생성: `io/io/src/main/kotlin/io/bluetape4k/io/compressor/CompressorBufferSupport.kt`
- 수정: Task 1의 test/fixture files only when RED expectation needs exact import or generated hash update

- [ ] **Step 2.1: preflight와 fallback helper를 최소 구현한다**

```kotlin
package io.bluetape4k.io.compressor

import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

internal inline fun writeToCallerBuffer(
    source: ByteBuffer,
    target: ByteBuffer,
    operation: (
        sourcePosition: Int,
        sourceRemaining: Int,
        targetPosition: Int,
        targetRemaining: Int,
    ) -> Int,
): Int {
    if (target.isReadOnly) throw ReadOnlyBufferException()
    rejectDetectableOverlap(source, target)
    if (!source.hasRemaining()) return 0

    val sourcePosition = source.position()
    val sourceLimit = source.limit()
    val targetPosition = target.position()
    val targetLimit = target.limit()
    val sourceOrder = source.order()
    val targetOrder = target.order()

    try {
        val written = operation(
            sourcePosition,
            source.remaining(),
            targetPosition,
            target.remaining(),
        )
        check(written in 0..(targetLimit - targetPosition)) {
            "Compressor buffer operation returned invalid written=$written, targetRemaining=${targetLimit - targetPosition}"
        }
        check(source.position() == sourcePosition && source.limit() == sourceLimit && source.order() == sourceOrder) {
            "Compressor buffer operation modified caller source state"
        }
        check(target.limit() == targetLimit && target.order() == targetOrder) {
            "Compressor buffer operation modified caller target limit or byte order"
        }
        target.position(targetPosition + written)
        return written
    } catch (failure: Throwable) {
        target.position(targetPosition)
        throw failure
    }
}

internal inline fun writeFallback(
    source: ByteBuffer,
    target: ByteBuffer,
    transform: (ByteArray) -> ByteArray,
): Int = writeToCallerBuffer(source, target) { sourcePosition, sourceRemaining, targetPosition, targetRemaining ->
    val input = ByteArray(sourceRemaining).also { bytes ->
        source.duplicate().position(sourcePosition).limit(sourcePosition + sourceRemaining).get(bytes)
    }
    val output = transform(input)
    if (output.size > targetRemaining) throw BufferOverflowException()
    target.duplicate().position(targetPosition).limit(targetPosition + targetRemaining).put(output)
    output.size
}

private fun rejectDetectableOverlap(source: ByteBuffer, target: ByteBuffer) {
    if (source === target) throw IllegalArgumentException("source and target must not be the same buffer")
    if (!source.hasArray() || !target.hasArray() || source.array() !== target.array()) return

    val sourceStart = source.arrayOffset() + source.position()
    val sourceEnd = source.arrayOffset() + source.limit()
    val targetStart = target.arrayOffset() + target.position()
    val targetEnd = target.arrayOffset() + target.limit()
    if (sourceStart < targetEnd && targetStart < sourceEnd) {
        throw IllegalArgumentException("source and target array ranges must not overlap")
    }
}

internal fun putIntBigEndian(target: ByteBuffer, index: Int, value: Int) {
    target.put(index, (value ushr 24).toByte())
    target.put(index + 1, (value ushr 16).toByte())
    target.put(index + 2, (value ushr 8).toByte())
    target.put(index + 3, value.toByte())
}

internal fun getIntBigEndian(source: ByteBuffer, index: Int): Int =
    ((source.get(index).toInt() and 0xFF) shl 24) or
        ((source.get(index + 1).toInt() and 0xFF) shl 16) or
        ((source.get(index + 2).toInt() and 0xFF) shl 8) or
        (source.get(index + 3).toInt() and 0xFF)
```

helper는 logging, codec 선택, size-limit 정책을 소유하지 않는다.

- [ ] **Step 2.2: 두 public JVM default method를 추가한다**

`Compressor.kt`에 기존 single-argument `ByteBuffer` method를 그대로 둔 채 다음을 추가한다.

```kotlin
/**
 * Compresses the remaining bytes in [source] into caller-owned [target].
 *
 * The source position, limit, mark, and byte order are preserved. On success,
 * only the target position advances by the returned byte count. On failure,
 * the target position is restored; bytes already overwritten are unspecified.
 * The default implementation is a compatibility path and may allocate
 * payload-sized byte arrays.
 *
 * @throws ReadOnlyBufferException when [target] is read-only
 * @throws IllegalArgumentException when detectable source and target ranges overlap
 * @throws BufferOverflowException when [target] has insufficient remaining capacity
 */
fun compress(source: ByteBuffer, target: ByteBuffer): Int =
    writeFallback(source, target) { bytes -> compress(bytes) }

/**
 * Decompresses the remaining bytes in [source] into caller-owned [target].
 *
 * This target is a final-write bound, not a decompression resource bound for
 * the allocating compatibility path. Apply an application-level maximum to
 * untrusted compressed input.
 *
 * @throws ReadOnlyBufferException when [target] is read-only
 * @throws IllegalArgumentException when detectable source and target ranges overlap
 * @throws BufferOverflowException when the decompressed result does not fit [target]
 */
fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
    writeFallback(source, target) { bytes -> decompress(bytes) }
```

두 KDoc은 공통 contract를 동일 문구 또는 `@see`로 연결해 target limit/capacity/byteOrder/mark 보존, 성공 시 position만 commit, 실패 시 position rollback과 overwritten byte unspecified, detectable overlap 거부와 undetectable direct/read-only alias caller precondition, 개별 mutable buffer thread confinement, runtime dispatch telemetry/log 부재와 privacy-safe caller-side codec/storage/size diagnostics 권고를 모두 명시한다. decompression KDoc은 fallback target이 final-write bound일 뿐 untrusted input resource bound가 아님을 추가한다.

- [ ] **Step 2.3: common contract GREEN을 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferContractTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferJavaContractTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: all contract cases PASS. Java null은 raw NPE, fallback overflow는 raw
`BufferOverflowException`, source/target mark reset assertion도 PASS한다.

- [ ] **Step 2.4: helper/default implementation을 Lore commit한다**

```bash
set -euo pipefail
git add io/io/src/main/kotlin/io/bluetape4k/io/compressor/Compressor.kt \
  io/io/src/main/kotlin/io/bluetape4k/io/compressor/CompressorBufferSupport.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferTestSupport.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferContractTest.kt \
  io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaContractTest.java
git commit -m 'Let callers own compressor output buffers without breaking implementors' \
  -m 'Constraint: Existing Compressor implementations need executable JVM defaults and unchanged one-shot APIs
Rejected: Require a new capability interface | It fragments factories and caller contracts
Confidence: high
Scope-risk: moderate
Directive: Treat the default path as allocating compatibility behavior, never as allocation evidence
Tested: Caller-owned fallback contract and Java null/preflight tests
Not-tested: Backend-native overrides and canonical allocation evidence remain in later slices'
```

- [ ] **Step 2.5: exact-head ABI proof를 실행한다**

```bash
set -euo pipefail
bash scripts/check-compressor-buffer-abi.sh \
  --build-current \
  --expected-head "$(git rev-parse HEAD)"
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.CompressorBufferAbiCompatibilityTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: legacy source와 classfile caller/implementor, new callers, manifest/hash, JVM default reflection PASS. ambiguous null fixture만 예상한 compiler diagnostic으로 실패한다.

**Step DoD:** 모든 compressor가 default fallback으로 공통 contract를 통과하고 legacy ABI가 exact baseline에 대해 증명된다.

---

## Task 3: Core slice 문서·검증·PR을 수렴한다

**복잡도:** 중간 **Dependency:** Task 2 **작성 범위:** 양쪽 README, CHANGELOG, issue lesson, core test/ABI artifact
**Pattern skill:** `bluetape-writer`, `verification-before-completion`, `requesting-code-review`

**파일:**

- 수정: `io/io/README.md`
- 수정: `io/io/README.ko.md`
- 수정: `CHANGELOG.md`
- 생성: `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`
- 생성: `scripts/check-compressor-buffer-docs.py`

- [ ] **Step 3.1: provisional fallback matrix와 sizing/retry 경계를 양쪽 README에 기록한다**

English/Korean 문서는 같은 표 행과 예제를 유지한다.

```markdown
| Codec | heap -> heap | direct -> direct | mixed storage | Allocation claim |
|---|---|---|---|---|
| LZ4 | compatibility fallback | compatibility fallback | compatibility fallback | none in the core slice |
| Deflate | compatibility fallback | compatibility fallback | compatibility fallback | none in the core slice |
| Snappy | compatibility fallback | compatibility fallback | compatibility fallback | none in the core slice |
| Zstd | compatibility fallback | compatibility fallback | compatibility fallback | none in the core slice |
| Other codecs | compatibility fallback | compatibility fallback | compatibility fallback | ineligible |
```

문서에는 source 보존, target success commit/failure rollback, read-only/overlap, direct alias precondition, thread confinement, raw overflow에 required size가 없다는 점, fallback decompression target은 final write bound일 뿐 resource bound가 아니라는 점을 각각 명시한다. 기존 one-argument API의 source-position 차이, erased-signature default collision caveat, optimized storage와 reusable target이 있는 caller만 opt-in한다는 migration 경계, 결함 시 public default/wire는 유지하고 override만 fallback으로 되돌리는 patch rollback, patch 전 allocating API/fallback storage 우회, runtime telemetry 부재와 privacy-safe caller diagnostics도 양쪽 README에 같은 marker key로 기록한다.

- [ ] **Step 3.2: CHANGELOG와 첫 lesson을 작성한다**

`CHANGELOG.md`의 `1.12.0 — Unreleased`에 다음 의미를 English로 기록한다.

```markdown
<!-- issue-755-migration:start -->
- Added opt-in caller-owned `ByteBuffer` compressor defaults with source-state
  preservation and target position commit/rollback contracts. Compatibility
  defaults may still allocate payload-sized arrays; codec-native paths are
  delivered and measured separately under #755. Existing callers do not need
  to migrate: unlike some existing one-argument `ByteBuffer` methods that may
  consume the source position, the new two-argument methods preserve all source
  state. Opt in only with reusable targets and an optimized storage pairing;
  fallback pairings are correctness-only. As with standard Java interface
  evolution, an implementation inheriting another erased-signature-equivalent
  default may require an explicit override and is not claimed conflict-free.
<!-- issue-755-migration:end -->
<!-- issue-755-rollback:start -->
- If a codec-native override proves defective, a patch keeps the public default
  methods and wire contract and reverts only that override to the compatibility
  fallback. Until that patch, use an existing allocating API or a documented
  fallback storage pairing; no runtime feature flag is provided.
<!-- issue-755-rollback:end -->
```

lesson에는 승인 후 dependency source 검증에서 발견된 LZ4 capacity-tail read와 zstd-jni throw-before-return, 그 결과 broad backend slice와 spec 재승인이 필요했던 사실을 증거 commit과 함께 기록한다.

`scripts/check-compressor-buffer-docs.py`는 `issue-755-contract`, `issue-755-storage-matrix`,
`issue-755-kotlin-example`, `issue-755-java-example`, `issue-755-sizing-retry`,
`issue-755-resource-bound`, `issue-755-telemetry` marker를 두 README에서 추출해 row/key parity와 필수 문구를 비교한다. CHANGELOG에서는 `issue-755-migration`과 `issue-755-rollback` marker를 각각 정확히 한 번 요구하고 source-position/default-collision/fallback/override-only rollback/ allocating-API workaround 필수 문구를 검사한다. marker 누락/duplicate/order drift를 fail closed로 거부한다.

- [ ] **Step 3.3: core full verification을 순서대로 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-build-cache --rerun-tasks
./gradlew :bluetape4k-io:compileKotlin \
  :bluetape4k-io:compileTestKotlin \
  :bluetape4k-io:compileTestJava \
  detekt detektMain detektTest \
  --no-build-cache --rerun-tasks
bash scripts/check-compressor-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
python3 scripts/check-compressor-buffer-docs.py
git diff --check
```

예상 결과: module tests 1,109 baseline + 신규 core tests PASS, compile/Detekt/ABI PASS, diff check clean. ABI script가 commit-only tested paths를 요구하므로 docs/lesson commit 뒤 exact head에서 다시 실행한다.

- [ ] **Step 3.4: core 문서와 lesson을 Lore commit한다**

```bash
set -euo pipefail
git add io/io/README.md io/io/README.ko.md CHANGELOG.md \
  docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md \
  scripts/check-compressor-buffer-docs.py \
  io/io/src/test/resources/abi/issue-755/pre-change/manifest.json \
  io/io/src/test/resources/abi/issue-755/pre-change/legacy-compressor-fixtures.jar
git commit -m 'Make the allocating fallback boundary explicit before native adoption' \
  -m 'Constraint: Public APIs must be documented before backend allocation claims exist
Rejected: Publish one optimistic matrix for all codecs | It would mislabel fallback paths as optimized
Confidence: high
Scope-risk: narrow
Directive: Update each codec row only in the backend slice that proves it
Tested: Full io tests, Kotlin and Java compile, Detekt, ABI fixture, README locale parity, diff check
Not-tested: Native backend allocation claims await their isolated slices'
```

- [ ] **Step 3.5: six-perspective code review와 exact-head verification을 수렴한다**

성능, 안정성, 보안, 운영, developer/API, caller/user 여섯 read-only lane과 main integration을 실행한다. P0/P1을 수정하고 affected lane/test를 재실행한다. latest result가 P0=0/P1=0일 때만 다음 단계로 간다.

```bash
set -euo pipefail
bash scripts/check-compressor-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
git diff --check
```

- [ ] **Step 3.6: core PR을 생성하고 merge-ready에서 멈춘다**

```bash
set -euo pipefail
git push -u origin feat/issue-755-bytebuffer-compressor
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head feat/issue-755-bytebuffer-compressor \
  --title 'Add caller-owned ByteBuffer compressor defaults' \
  --assignee debop \
  --milestone '1.12.0' \
  --label enhancement --label performance --label infra/io \
  --body $'Refs #755\n\n## Summary\n- add executable caller-owned ByteBuffer defaults\n- preserve source state and commit only successful target positions\n- freeze Java/Kotlin source and classfile ABI compatibility\n\n## Verification\n- full bluetape4k-io tests and Detekt\n- exact-head compressor ABI fixture check\n- six-perspective review\n\n## DoD Status\n- [x] Core API and compatibility fallback complete\n- [x] Backend optimization and allocation evidence intentionally remain in follow-up slices'
```

PR body는 English이며 issue #755를 연결하고 final `##` heading을 `## DoD Status`로 둔다.
`gh pr view --json body,headRefOid,mergeStateStatus,statusCheckRollup,reviews`로 live metadata를 검증한다. CI와 current review/thread가 수렴하면 exact PR/head를 보고하고
**fresh merge approval을 기다린다**. 승인 전 `gh pr merge`와 LZ4 branch 생성은 금지한다.

**Step DoD:** core PR이 independently reviewable/tested 상태로 merge-ready이고 CG-16에서 PENDING이다.

---

## Task 4: LZ4 slice — bounded payload를 사용하는 전체 storage override를 전달한다

**복잡도:** 높음 **Dependency:** core PR merge + updated `develop` sync **Write
scope:** `LZ4Compressor.kt`, LZ4 test, README locale rows, shared lesson LZ4 section **Pattern
skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**파일:**

- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/compressor/LZ4Compressor.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/LZ4CompressorByteBufferTest.kt`
- 수정: `io/io/README.md`
- 수정: `io/io/README.ko.md`
- 수정: `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`

- [ ] **Step 4.1: merge된 core에서 isolated worktree를 만든다**

```bash
set -euo pipefail
git -C /Users/debop/work/bluetape4k/bluetape4k-projects fetch origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects switch develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects pull --ff-only origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects worktree add \
  /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-issue-755-bytebuffer-lz4 \
  -b feat/issue-755-bytebuffer-lz4 origin/develop
```

예상 결과: 새 worktree clean, core default/API/ABI commit이 `HEAD` ancestor다.

- [ ] **Step 4.2: LZ4 exact failure/boundary RED를 작성한다**

```kotlin
class LZ4CompressorByteBufferTest {
    private val compressor = LZ4Compressor()

    @ParameterizedTest
    @MethodSource("storagePairs")
    fun `heap and direct paths preserve wire and caller state`(sourceDirect: Boolean, targetDirect: Boolean) {
        val payload = CompressorByteBufferTestSupport.payload
        val source = if (sourceDirect) CompressorByteBufferTestSupport.direct(payload)
                     else CompressorByteBufferTestSupport.heap(payload)
        val target = CompressorByteBufferTestSupport.writableTarget(payload.size * 2 + 64, targetDirect)
        val targetStart = target.position()

        val written = compressor.compress(source, target)
        val compressed = CompressorByteBufferTestSupport.bytes(target, targetStart, written)

        compressor.decompress(compressed) shouldBeEqualTo payload
        source.reset()
        source.position() shouldBeEqualTo 7
    }

    @Test
    fun `decompression never reads valid capacity tail beyond caller limit`() {
        val wire = compressor.compress(CompressorByteBufferTestSupport.payload)
        val source = ByteBuffer.allocateDirect(wire.size).put(wire).flip()
        source.limit(source.limit() - 1)
        val target = ByteBuffer.allocateDirect(CompressorByteBufferTestSupport.payload.size)

        assertFailsWith<LZ4Exception> { compressor.decompress(source, target) }
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `trailing compressed bytes are rejected by consumed length`() {
        val wire = compressor.compress(CompressorByteBufferTestSupport.payload) + byteArrayOf(0x5A)
        val source = ByteBuffer.wrap(wire)
        val target = ByteBuffer.allocate(CompressorByteBufferTestSupport.payload.size)

        assertFailsWith<LZ4Exception> { compressor.decompress(source, target) }
        target.position() shouldBeEqualTo 0
    }
}
```

추가 RED는 target remaining `0..3`, exactly `4`, high-compression small destination, negative/over-256MiB header, target-too-small-before-decode, heap/direct/slice/read-only source, non-zero target offset, target byte order `LITTLE_ENDIAN`, limit 밖 sentinel, existing/new wire, pre-created failure identity, overflow 후 retry를 포함한다.

복합 실패 fixture는 유효한 declared header + corrupt/truncated payload +
`targetRemaining < declaredSize`에서 decode보다 target preflight가 먼저 실행되어 raw
`BufferOverflowException`을 내고, 같은 target position에서 작은 valid wire 재시도가 성공해야 한다.

- [ ] **Step 4.3: LZ4 RED를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.LZ4CompressorByteBufferTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: default fallback 때문에 correctness 일부는 PASS하지만 native dispatch seam, capacity-tail regression, payload-sized allocation avoidance assertion은 FAIL한다.

- [ ] **Step 4.4: injectable operation과 optimized override를 구현한다**

```kotlin
internal interface LZ4BufferOperations {
    fun compress(
        source: ByteBuffer, sourceOffset: Int, sourceLength: Int,
        target: ByteBuffer, targetOffset: Int, targetLength: Int,
    ): Int

    fun decompress(
        source: ByteBuffer, sourceOffset: Int,
        target: ByteBuffer, targetOffset: Int, targetLength: Int,
    ): Int
}

class LZ4Compressor private constructor(
    private val bufferOperations: LZ4BufferOperations,
): AbstractCompressor() {
    constructor(): this(DefaultLZ4BufferOperations)

    override fun compress(source: ByteBuffer, target: ByteBuffer): Int =
        writeToCallerBuffer(source, target) { sourcePosition, sourceRemaining, targetPosition, targetRemaining ->
            if (targetRemaining < Int.SIZE_BYTES) throw BufferOverflowException()
            putIntBigEndian(target, targetPosition, sourceRemaining)
            try {
                val payloadCapacity = targetRemaining - Int.SIZE_BYTES
                val payloadWritten = bufferOperations.compress(
                    source, sourcePosition, sourceRemaining,
                    target, targetPosition + Int.SIZE_BYTES, payloadCapacity,
                )
                if (payloadWritten !in 1..payloadCapacity) {
                    throw IllegalStateException(
                        "LZ4 payload write count out of range: written=$payloadWritten, capacity=$payloadCapacity"
                    )
                }
                Math.addExact(Int.SIZE_BYTES, payloadWritten)
            } catch (failure: LZ4Exception) {
                if (failure.message == "maxDestLen is too small") throw BufferOverflowException()
                throw failure
            }
        }

    override fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
        writeToCallerBuffer(source, target) { sourcePosition, sourceRemaining, targetPosition, targetRemaining ->
            if (sourceRemaining < Int.SIZE_BYTES) throw IndexOutOfBoundsException("LZ4 header requires 4 bytes")
            val declaredSize = getIntBigEndian(source, sourcePosition)
            require(declaredSize >= 0) { "sourceSize must not be negative: $declaredSize" }
            require(declaredSize <= MAX_DECOMPRESSED_SIZE) { "sourceSize exceeds 256 MiB: $declaredSize" }
            if (declaredSize > targetRemaining) throw BufferOverflowException()

            val payload = source.duplicate().apply {
                position(sourcePosition + Int.SIZE_BYTES)
                limit(sourcePosition + sourceRemaining)
            }.slice()
            val consumed = bufferOperations.decompress(
                payload, 0, target, targetPosition, declaredSize,
            )
            if (consumed != payload.remaining()) {
                throw LZ4Exception(
                    "LZ4 compressed payload length mismatch: consumed=$consumed, remaining=${payload.remaining()}"
                )
            }
            declaredSize
        }

    internal companion object {
        const val MAX_DECOMPRESSED_SIZE: Int = 256 * 1024 * 1024
        fun forTesting(operations: LZ4BufferOperations): LZ4Compressor = LZ4Compressor(operations)
    }
}
```

`DefaultLZ4BufferOperations`는 기존 companion의 thread-safe compressor/decompressor에 정확히 위임한다. decompression source는 position `0`, limit=capacity=`payload.remaining()`인 slice만 받는다. compression fake seam은 `-1`, `0`, `Int.MAX_VALUE` 반환을 각각 고정해 범위 검사가
`Math.addExact`와 target position commit보다 먼저 실패하는지 검증한다. 기존
`doCompress`/`doDecompress`와 4-byte BE wire는 유지한다.

- [ ] **Step 4.5: LZ4 GREEN과 module regression을 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.LZ4CompressorByteBufferTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferContractTest' \
  --no-build-cache --rerun-tasks
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-build-cache --rerun-tasks
./gradlew detekt detektMain detektTest --no-build-cache --rerun-tasks
git diff --check
```

예상 결과: capacity-tail/trailing test가 LZ4Exception으로 PASS하고 heap/direct 네 조합, wire interop, target rollback, sentinel, module suite, Detekt가 PASS한다.

- [ ] **Step 4.6: README matrix와 lesson을 LZ4 증거로 갱신하고 Lore commit한다**

LZ4 행을 heap/direct/mixed 모두 `optimized`로 바꾸되 allocation claim은 final evidence 전
`eligible, not yet measured`로 둔다. lesson에는 `LZ4FastDecompressor`가 capacity를 읽는 dependency 근거와 bounded slice guard를 추가한다.

```bash
set -euo pipefail
git add io/io/src/main/kotlin/io/bluetape4k/io/compressor/LZ4Compressor.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/compressor/LZ4CompressorByteBufferTest.kt \
  io/io/README.md io/io/README.ko.md \
  docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md
git commit -m 'Bound LZ4 decompression to the caller-visible payload' \
  -m 'Constraint: lz4-java fast decompression derives input length from ByteBuffer capacity
Rejected: Pass the original source buffer | It can consume bytes beyond the caller limit
Confidence: high
Scope-risk: moderate
Directive: Preserve the zero-offset capacity-bounded payload slice and consumed-length check
Tested: LZ4 heap/direct matrix, capacity-tail regression, wire compatibility, full io tests, Detekt, diff check
Not-tested: Allocation promotion remains reserved for the final evidence slice'
```

- [ ] **Step 4.7: LZ4 PR을 exact head로 전달하고 fresh merge approval에서 멈춘다**

```bash
set -euo pipefail
git push -u origin feat/issue-755-bytebuffer-lz4
gh pr create --repo bluetape4k/bluetape4k-projects \
  --base develop --head feat/issue-755-bytebuffer-lz4 \
  --title 'Add bounded caller-owned LZ4 buffer paths' \
  --assignee debop --milestone '1.12.0' \
  --label enhancement --label performance --label infra/io \
  --body $'Refs #755\n\n## Summary\n- add heap/direct caller-owned LZ4 paths\n- bound fast decompression to the caller-visible payload capacity\n- preserve the existing four-byte header wire format\n\n## Verification\n- LZ4 storage matrix and capacity-tail regression\n- full bluetape4k-io tests and Detekt\n- six-perspective review\n\n## DoD Status\n- [x] LZ4 backend slice complete\n- [x] Allocation promotion remains pending final evidence'
```

Six-perspective review, CI, live threads가 P0=0/P1=0일 때 exact PR/head를 보고한다. 승인 전 Deflate branch를 만들지 않는다.

**Step DoD:** LZ4 PR이 독립적으로 검증되어 CG-16 PENDING이고 caller limit 뒤 byte를 읽지 않는다.

---

## Task 5: Deflate slice — bounded JDK loop와 deterministic cleanup을 전달한다

**복잡도:** 높음 **Dependency:** LZ4 PR merge + updated `develop` sync **Write
scope:** `DeflateCompressor.kt`, Deflate test, README locale rows, docs checker의 Deflate expected
row, lesson lifecycle section **Pattern skill:** `bluetape-kotlin-patterns`,
`test-driven-development`

**파일:**

- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/compressor/DeflateCompressor.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/DeflateCompressorByteBufferTest.kt`
- 수정: `io/io/README.md`, `io/io/README.ko.md`
- 수정: `scripts/check-compressor-buffer-docs.py`
- 수정: `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`

- [x] **Step 5.1: merge된 develop에서 현재 Deflate worktree를 검증한다**

```bash
set -euo pipefail
worktree=/Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/issue-755-bytebuffer-codecs
test "$(git -C "$worktree" branch --show-current)" = feat/issue-755-bytebuffer-codecs
test "$(git -C "$worktree" rev-parse HEAD)" = fa07277c8c123c1093299e10cf09504f13d177a1
git -C "$worktree" merge-base --is-ancestor \
  e2869bddee9c21bd6b9eee1f549c521b8e400f83 HEAD
test -z "$(git -C "$worktree" status --short)"
```

예상 결과: core와 LZ4 merge commit이 ancestor이고, 승인된 branch/기준 commit의
worktree가 clean이다.

- [x] **Step 5.2: Deflate state table와 cleanup RED를 작성한다**

```kotlin
class DeflateCompressorByteBufferTest {
    private class FailingDeflater(
        private val failure: Throwable,
    ): Deflater() {
        override fun deflate(output: ByteBuffer): Int = throw failure
    }

    @ParameterizedTest
    @MethodSource("storagePairs")
    fun `deflate round-trips heap and direct buffers`(sourceDirect: Boolean, targetDirect: Boolean) {
        val compressor = DeflateCompressor()
        val payload = CompressorByteBufferTestSupport.payload
        val source = if (sourceDirect) CompressorByteBufferTestSupport.direct(payload)
                     else CompressorByteBufferTestSupport.heap(payload)
        val compressed = CompressorByteBufferTestSupport.writableTarget(payload.size * 2, targetDirect)
        val compressedStart = compressed.position()

        val compressedSize = compressor.compress(source, compressed)
        val restored = CompressorByteBufferTestSupport.writableTarget(payload.size, sourceDirect)
        val restoredSize = compressor.decompress(
            compressed.duplicate().position(compressedStart).limit(compressedStart + compressedSize),
            restored,
        )

        restoredSize shouldBeEqualTo payload.size
    }

    @Test
    fun `operation failure keeps identity and cleanup failure is suppressed`() {
        val operationFailure = ZipException("operation")
        val cleanupFailure = IllegalStateException("cleanup")
        val compressor = DeflateCompressor.forTesting(
            deflaterFactory = { FailingDeflater(operationFailure) },
            inflaterFactory = ::Inflater,
            endDeflater = { throw cleanupFailure },
            endInflater = Inflater::end,
        )

        val thrown = assertFails {
            compressor.compress(ByteBuffer.wrap(byteArrayOf(1)), ByteBuffer.allocate(8))
        }
        thrown shouldBeSameInstanceAs operationFailure
        thrown.suppressed.single() shouldBeSameInstanceAs cleanupFailure
    }
}
```

추가 RED: incompressible/exact-fit, target exhaustion rollback, corrupt/truncated, dictionary, dictionary+zero target → raw overflow, deflater compression no-progress stable
`IllegalStateException`, inflater decompression no-progress stable `ZipException`, `end()`
exactly once on success/failure, cleanup-only failure, fatal/cancellation precedence, limit 밖 sentinel, failure 후 same target retry, singleton concurrency를 포함한다. compression zero-progress는
`IllegalStateException`, decompression zero-progress는 `ZipException`으로 구분한다. decompression 복합 실패는 deterministic inflater seam으로 corruption-first와 target-exhaustion-first를 각각 만들고, 처음 확정된 상태의 `ZipException` 또는
`BufferOverflowException` identity와 같은 target 재시도를 검증한다.

- [x] **Step 5.3: Deflate RED를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.DeflateCompressorByteBufferTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: test seam/optimized override가 없어 compile 또는 dispatch assertion FAIL.

- [x] **Step 5.4: per-call factories, operation-primary cleanup helper와 compression loop를 구현한다**

```kotlin
internal inline fun <R, T> useDeflateCodec(
    resource: R,
    cleanup: (R) -> Unit,
    block: (R) -> T,
): T {
    var operationFailure: Throwable? = null
    try {
        return block(resource)
    } catch (failure: Throwable) {
        operationFailure = failure
        throw failure
    } finally {
        try {
            cleanup(resource)
        } catch (cleanupFailure: Throwable) {
            val operation = operationFailure
            if (operation == null) {
                throw cleanupFailure
            }
            if (operation !== cleanupFailure && operation.suppressed.none { it === cleanupFailure }) {
                operation.addSuppressed(cleanupFailure)
            }
        }
    }
}

override fun compress(source: ByteBuffer, target: ByteBuffer): Int =
    writeToCallerBufferViews(source, target) {
            sourceView,
            targetView,
            sourcePosition,
            sourceRemaining,
            targetPosition,
            targetRemaining,
        ->
        val input = sourceView.position(sourcePosition).limit(sourcePosition + sourceRemaining)
        val output = targetView.position(targetPosition).limit(targetPosition + targetRemaining)
        useDeflateCodec(deflaterFactory(), endDeflater) { deflater ->
            deflater.setInput(input)
            deflater.finish()
            while (!deflater.finished()) {
                val inputBefore = deflater.bytesRead
                val outputBefore = deflater.bytesWritten
                val produced = deflater.deflate(output)
                if (deflater.finished()) break
                if (!output.hasRemaining()) throw BufferOverflowException()
                if (produced > 0 || deflater.bytesRead != inputBefore || deflater.bytesWritten != outputBefore) {
                    continue
                }
                if (deflater.needsInput()) throw IllegalStateException("Deflater needs input before finishing")
                throw IllegalStateException("Deflater made no progress")
            }
            output.position() - targetPosition
        }
    }
```

- [x] **Step 5.5: decompression state table를 exact 순서로 구현한다**

```kotlin
override fun decompress(source: ByteBuffer, target: ByteBuffer): Int =
    writeToCallerBufferViews(source, target) {
            sourceView,
            targetView,
            sourcePosition,
            sourceRemaining,
            targetPosition,
            targetRemaining,
        ->
        val input = sourceView.position(sourcePosition).limit(sourcePosition + sourceRemaining)
        val output = targetView.position(targetPosition).limit(targetPosition + targetRemaining)
        useDeflateCodec(inflaterFactory(), endInflater) { inflater ->
            inflater.setInput(input)
            while (!inflater.finished()) {
                val inputBefore = inflater.bytesRead
                val outputBefore = inflater.bytesWritten
                val produced = try {
                    inflater.inflate(output)
                } catch (failure: DataFormatException) {
                    throw ZipException("Invalid Deflate payload").apply { initCause(failure) }
                }
                if (inflater.finished()) break
                if (!output.hasRemaining()) throw BufferOverflowException()
                if (inflater.needsDictionary()) throw ZipException("Deflate preset dictionary is required")
                if (inflater.needsInput()) throw ZipException("Truncated Deflate payload")
                if (produced == 0 && inflater.bytesRead == inputBefore && inflater.bytesWritten == outputBefore) {
                    throw ZipException("Inflater made no progress")
                }
            }
            output.position() - targetPosition
        }
    }
```

`DeflateCompressor`는 public no-arg constructor를 보존한다. private primary constructor는
`deflaterFactory`, `inflaterFactory`, `endDeflater`, `endInflater`를 받고 `internal forTesting`
factory만 노출한다. production factory는 매 호출 새 `Deflater`/`Inflater`를 만든다. 기존 `io.bluetape4k.io.serializer.useWithCleanup`은 fatal cleanup을 operation보다 승격할 수 있어 승인 명세와 다르므로 재사용하지 않는다. helper는 `DeflateCompressor.kt` 안의 internal 구현으로 한정하고 공용 cleanup abstraction으로 확장하지 않는다.

| operation failure                     | cleanup failure                       | expected primary                             |
|---------------------------------------|---------------------------------------|----------------------------------------------|
| runtime/overflow/`Error`/cancellation | runtime/overflow/`Error`/cancellation | exact operation identity, cleanup suppressed |
| runtime/overflow/`Error`/cancellation | none                                  | exact operation identity                     |
| none                                  | runtime/overflow/`Error`/cancellation | exact cleanup identity                       |

각 조합은 operation identity와 suppressed relation을 test seam으로 고정한다. 같은 throwable이 operation과 cleanup 양쪽에서 던져지면 자기 자신을 suppress하지 않고, 이미 같은 identity가 suppressed면 중복 추가하지 않는다.

- [x] **Step 5.6: Deflate GREEN/full regression/정적 검증을 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.DeflateCompressorByteBufferTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferContractTest' \
  --no-build-cache --rerun-tasks
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-build-cache --rerun-tasks
./gradlew detekt detektMain detektTest --no-build-cache --rerun-tasks
repo-test-summary -- ./gradlew :bluetape4k-io:compileKotlin --no-build-cache --rerun-tasks
python3 scripts/check-compressor-buffer-docs.py
git diff --check
```

예상 결과: state table, exact-fit, dictionary/zero-target precedence, no-progress, end-once와 suppressed identity가 PASS하고 전체 module regression이 없다.

2026-07-30 fresh evidence: Deflate 전용 12개와 공통 contract를 포함한 모듈 전체
1,215개 테스트가 통과했고 `:bluetape4k-io:compileKotlin`, 문서 checker,
`git diff --check`가 통과했다. 저장소 root의 `detekt`, `detektMain`, `detektTest`는
`NO-SOURCE`였고 `:bluetape4k-io`에는 Detekt task가 등록되어 있지 않아 Kotlin
compile과 전체 모듈 test를 현재 정적·동적 검증 근거로 사용한다.

- [ ] **Step 5.7: docs/lesson/Lore commit과 Deflate PR을 수렴한다**

README Deflate 행은 heap/direct/mixed capability cell을 `optimized`로 변경하고,
`Allocation claim` cell만 `eligible, not yet measured`로 변경한다. 영문/한국어
matrix의 cell 의미와 순서를 exact parity로 유지한다.
`DeflateCompressor`와 신규 internal seam/helper KDoc은 `$bluetape-writer` 기준의 한국어
기술 문체로 source/target 상태, per-call resource 수명, failure/cleanup precedence와
fallback 경계를 설명한다. 영문/한국어 README는 capability와 allocation claim을 같은
의미로 유지하고, lesson은 state order와 cleanup precedence를 기록한다. Lore intent는
`Keep Deflate buffer loops bounded and disposable per call`로 하고 1,215개 module test,
compile, docs validator, diff check와 Detekt `NO-SOURCE`/module task 미등록 사실을 trailers에
정확히 기록한다. PR metadata: base `develop`, head
`feat/issue-755-bytebuffer-codecs`, English title
`Add bounded caller-owned Deflate buffer paths`. Six-perspective review/CI 후 fresh merge approval에서 멈추며 Snappy branch를 미리 만들지 않는다.

```bash
set -euo pipefail
git push -u origin feat/issue-755-bytebuffer-codecs
gh pr create --repo bluetape4k/bluetape4k-projects \
  --base develop --head feat/issue-755-bytebuffer-codecs \
  --title 'Add bounded caller-owned Deflate buffer paths' \
  --assignee debop --milestone '1.12.0' \
  --label enhancement --label performance --label infra/io \
  --body $'Refs #755\n\n## Summary\n- add bounded JDK Deflater and Inflater ByteBuffer loops\n- fail closed on no-progress states\n- end every per-call codec with deterministic failure precedence\n\n## Verification\n- Deflate state-table and cleanup tests (12 passing)\n- full bluetape4k-io tests (1,215 passing)\n- compileKotlin, docs validator, and diff check\n- Detekt root tasks are NO-SOURCE; no bluetape4k-io Detekt task is registered\n- six-perspective review\n\n## DoD Status\n- [x] Deflate backend slice complete\n- [x] Allocation promotion remains pending final evidence'
```

**Step DoD:** Deflate PR이 무한 loop와 cleanup leak 없이 독립 merge-ready이고 CG-16 PENDING이다.

---

## Task 6: Snappy slice — validation-first matched-storage native path를 전달한다

**복잡도:** 높음 **Dependency:** Deflate PR merge + updated `develop` sync **Write
scope:** `SnappyCompressor.kt`, Snappy test, README locale rows, validator, Snappy design/plan
correction, lesson native-validation section **Pattern skill:** `bluetape-kotlin-patterns`,
`test-driven-development`

**파일:**

- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/compressor/SnappyCompressor.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/SnappyCompressorByteBufferTest.kt`
- 수정: `io/io/README.md`, `io/io/README.ko.md`
- 수정: `scripts/check-compressor-buffer-docs.py`
- 수정: `docs/superpowers/specs/2026-07-21-issue-755-bytebuffer-compressor-design.md`
- 수정: `docs/superpowers/plans/2026-07-21-issue-755-bytebuffer-compressor-plan.md`
- 수정: `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`

### 2026-07-31 실행 정정

resolved snappy-java 1.1.10.8 source를 다시 확인한 결과 array compression API는 target
offset만 받고 caller target length를 받지 않는다. 따라서 아래 초기 승인 예시의
heap→heap native dispatch와 max-bound 부족 즉시 overflow는 구현하지 않는다. 실제 실행
계약은 다음과 같다.

- direct→direct만 native `ByteBuffer` 경로 후보이며 heap/mixed는 fallback이다.
- compression은 max bound가 충분할 때만 native 경로를 사용하고, 부족하면 실제 압축 결과가
  들어갈 수 있는 compatibility fallback으로 내려간다.
- direct decompression은 exact range validation → 복원 크기/256 MiB → target bound →
  native decode 순서를 유지한다.
- invalid direct payload는 `IllegalArgumentException`이며 native decode를 호출하지 않는다.
- Step 6.2, 6.4, 6.5는 이 정정에 맞춘 실행 결과와 검증 계약만 기록한다.

- [x] **Step 6.1: merge된 develop에서 Snappy worktree를 만든다**

```bash
set -euo pipefail
git -C /Users/debop/work/bluetape4k/bluetape4k-projects fetch origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects switch develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects pull --ff-only origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects worktree add \
  /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/issue-755-snappy-bytebuffer \
  -b feat/issue-755-snappy-bytebuffer origin/develop
```

예상 결과: core/LZ4/Deflate merge commits가 ancestor이고 worktree clean.

- [x] **Step 6.2: dispatch와 native validation ordering RED를 작성한다**

RED는 direct pair backend dispatch, caller state/wire 호환성, max bound보다 작은 target의
fallback, invalid direct payload의 decode 차단, 256 MiB 경계, backend throwable identity,
inspection view 격리, exact output bound, singleton concurrency를 포함한다.

- [x] **Step 6.3: Snappy RED를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.SnappyCompressorByteBufferTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: `forTesting`과 native dispatch가 없어 compile/ordering assertion FAIL.

- [x] **Step 6.4: bounded direct storage dispatch와 compression을 구현한다**

public no-arg constructor와 기존 ByteArray methods는 유지하고 private primary constructor +
`internal forTesting` factory로 direct-only `SnappyBufferOperations`를 주입한다. common
preflight 안에서 positive max bound를 확인한다. bound가 충분하면 output duplicate limit을
그 bound로 고정해 native compression을 실행하고, 부족하면 compatibility fallback을
실행한다.

- [x] **Step 6.5: validation-first decompression을 구현한다**

validation, uncompressed-length 확인, native decode에는 각각 fresh input duplicate를 전달한다.
invalid payload는 `IllegalArgumentException`으로 거부하고, 복원 크기는 `0..256 MiB`와
target remaining에 대해 검증한다. native output limit은 exact 복원 크기로 고정하며 반환량이
그 크기와 같을 때만 원본 target position을 commit한다.

- [x] **Step 6.6: Snappy GREEN/full regression/Detekt를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.SnappyCompressorByteBufferTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferContractTest' \
  --no-build-cache --rerun-tasks
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-build-cache --rerun-tasks
./gradlew detekt detektMain detektTest --no-build-cache --rerun-tasks
git diff --check
```

실행 결과: Snappy 전용 10건, 공통 target suite 48건, 전체 모듈 1,225건이 PASS했다.
문서 검증기와 `git diff --check`도 PASS했다. root Detekt 진입점은 모두 `NO-SOURCE`이며
`bluetape4k-io` 전용 Detekt task는 등록되어 있지 않다.

- [ ] **Step 6.7: docs/lesson/Lore commit과 Snappy PR을 수렴한다**

README Snappy 행은 direct→direct만 `optimized`, heap/mixed는 `compatibility fallback`,
allocation은 `eligible for direct pair, not yet measured`로 변경한다. lesson은 validation-first
native crash 경계와 array API가 caller limit을 강제하지 못하는 이유를 기록한다. Lore intent는
`Keep invalid Snappy ranges outside the native decoder`. PR metadata: base `develop`, head
`feat/issue-755-snappy-bytebuffer`, English title `Add validated caller-owned Snappy buffer paths`.
Six-perspective review/CI 후 fresh merge approval에서 멈추며 Zstd branch를 미리 만들지 않는다.

```bash
set -euo pipefail
git push -u origin feat/issue-755-snappy-bytebuffer
gh pr create --repo bluetape4k/bluetape4k-projects \
  --base develop --head feat/issue-755-snappy-bytebuffer \
  --title 'Add validated caller-owned Snappy buffer paths' \
  --assignee debop --milestone '1.12.0' \
  --label enhancement --label performance --label infra/io \
  --body $'Refs #755\n\n## Summary\n- add bounded direct-pair Snappy caller-owned paths\n- validate exact source ranges before native length and decode calls\n- keep heap, mixed, and undersized compression bounds on the compatibility fallback\n\n## Verification\n- Snappy validation-order and storage-dispatch tests\n- full bluetape4k-io tests and Detekt\n- six-perspective review\n\n## DoD Status\n- [x] Snappy backend slice complete\n- [x] Allocation promotion remains pending final evidence'
```

**Step DoD:** Snappy PR이 invalid range를 native decode에 전달하지 않고 CG-16 PENDING이다.

---

## Task 7: Zstd slice — declared-size destination bound와 예외 의미를 전달한다

**복잡도:** 높음 **Dependency:** Snappy PR merge + updated `develop` sync **Write
scope:** `ZstdCompressor.kt`, Zstd test, README locale rows, lesson native-bound section **Pattern
skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**파일:**

- 수정: `io/io/src/main/kotlin/io/bluetape4k/io/compressor/ZstdCompressor.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/ZstdCompressorByteBufferTest.kt`
- 수정: `io/io/README.md`, `io/io/README.ko.md`
- 수정: `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`

- [ ] **Step 7.1: merge된 develop에서 Zstd worktree를 만든다**

```bash
set -euo pipefail
git -C /Users/debop/work/bluetape4k/bluetape4k-projects fetch origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects switch develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects pull --ff-only origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects worktree add \
  /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/feat-issue-755-bytebuffer-zstd \
  -b feat/issue-755-bytebuffer-zstd origin/develop
```

예상 결과: 앞선 네 slice merge commits가 ancestor이고 worktree clean.

- [ ] **Step 7.2: destination bound와 exact taxonomy RED를 작성한다**

```kotlin
private data class ZstdCall(val targetLength: Int)

private class RecordingZstdOperations(
    private val calls: MutableList<ZstdCall>,
): ZstdBufferOperations {
    var decompressFailure: ZstdException? = null

    override fun decompressDirect(
        target: ByteBuffer,
        targetOffset: Int,
        targetLength: Int,
        source: ByteBuffer,
        sourceOffset: Int,
        sourceLength: Int,
    ): Long {
        calls += ZstdCall(targetLength)
        decompressFailure?.let { throw it }
        return targetLength.toLong()
    }

    override fun compressDirect(
        target: ByteBuffer, targetOffset: Int, targetLength: Int,
        source: ByteBuffer, sourceOffset: Int, sourceLength: Int, level: Int,
    ): Long = error("compressDirect is not used by these taxonomy tests")

    override fun compressHeap(
        target: ByteArray, targetOffset: Int, targetLength: Int,
        source: ByteArray, sourceOffset: Int, sourceLength: Int, level: Int,
    ): Long = error("compressHeap is not used by these taxonomy tests")

    override fun decompressHeap(
        target: ByteArray, targetOffset: Int, targetLength: Int,
        source: ByteArray, sourceOffset: Int, sourceLength: Int,
    ): Long = error("decompressHeap is not used by these taxonomy tests")
}

class ZstdCompressorByteBufferTest {
    private fun directWire(declaredSize: Int, payload: ByteArray): ByteBuffer =
        ByteBuffer.allocateDirect(Int.SIZE_BYTES + payload.size).apply {
            putInt(declaredSize)
            put(payload)
            flip()
        }

    private fun validHeaderWire(declaredSize: Int): ByteBuffer =
        directWire(
            declaredSize,
            byteArrayOf(0x28.toByte(), 0xB5.toByte(), 0x2F.toByte(), 0xFD.toByte()),
        )

    @Test
    fun `under-declared direct header never expands beyond declared destination`() {
        val calls = mutableListOf<ZstdCall>()
        val operations = RecordingZstdOperations(calls).apply {
            decompressFailure = ZstdException(Zstd.errDstSizeTooSmall(), "Destination too small")
        }
        val compressor = ZstdCompressor.forTesting(ZstdCompressor.DEFAULT_LEVEL, operations)
        val source = directWire(declaredSize = 8, payload = ByteArray(64))
        val target = ByteBuffer.allocateDirect(1024)

        val failure = assertFailsWith<IllegalStateException> { compressor.decompress(source, target) }

        failure.message shouldBeEqualTo "Zstd decompressed payload exceeds declared size=8"
        failure.cause.shouldBeNull()
        calls.single().targetLength shouldBeEqualTo 8
        target.position() shouldBeEqualTo 0
    }

    @Test
    fun `non-destination ZstdException keeps identity`() {
        val expected = ZstdException(Zstd.errCorruptionDetected(), "corrupt")
        val compressor = ZstdCompressor.forTesting(
            ZstdCompressor.DEFAULT_LEVEL,
            RecordingZstdOperations(mutableListOf()).apply { decompressFailure = expected },
        )

        val thrown = assertFails { compressor.decompress(validHeaderWire(32), ByteBuffer.allocateDirect(32)) }
        thrown shouldBeSameInstanceAs expected
    }
}
```

`ZstdBufferOperations`는 위 네 heap/direct offset method만 가진 production-internal interface로 정의하고, `DefaultZstdBufferOperations`가 zstd-jni static API에 1:1 위임한다. test fake가 반환이나 예외를 결정하므로 native library의 우연한 payload parsing에 taxonomy test를 의존하지 않는다.

추가 RED: heap→heap/direct→direct native, mixed/read-only heap fallback, arrayOffset/non-zero direct offset, compression target remaining `0..3`와 `4`, payload length=`remaining-4`, compression
`errDstSizeTooSmall`만 raw overflow, target-too-small preflight before decode, negative/over-limit header, under-declared large target heap/direct, over-declared successful mismatch, truncated/corrupt payload, target byte order independence, sentinel, retry, singleton concurrency, existing/new wire를 포함한다. fake가 compression/decompression에서 `-1L`, `Long.MAX_VALUE`, `2^32 + declaredSize`를 반환하면 축소 전 거부하고 heap/direct caller position과 sentinel을 보존해야 한다. 유효한 declared header + corrupt payload + `targetRemaining < declaredSize` 복합 입력은 native decode 전에 raw `BufferOverflowException`이어야 하며, 같은 target position에서 작은 valid wire 재시도를 검증한다.

- [ ] **Step 7.3: Zstd RED를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.ZstdCompressorByteBufferTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: operation seam/optimized dispatch가 없어 compile/target-length/taxonomy assertion FAIL.

- [ ] **Step 7.4: matched storage compression과 error normalization을 구현한다**

```kotlin
private fun compressOptimized(source: ByteBuffer, target: ByteBuffer, direct: Boolean): Int =
    writeToCallerBuffer(source, target) { sourcePosition, sourceRemaining, targetPosition, targetRemaining ->
        if (targetRemaining < Int.SIZE_BYTES) throw BufferOverflowException()
        putIntBigEndian(target, targetPosition, sourceRemaining)
        val payloadCapacity = targetRemaining - Int.SIZE_BYTES
        try {
            val written = if (direct) {
                operations.compressDirect(
                    target, targetPosition + Int.SIZE_BYTES, payloadCapacity,
                    source, sourcePosition, sourceRemaining, level,
                )
            } else {
                operations.compressHeap(
                    target.array(), target.arrayOffset() + targetPosition + Int.SIZE_BYTES, payloadCapacity,
                    source.array(), source.arrayOffset() + sourcePosition, sourceRemaining, level,
                )
            }
            check(written in 0L..payloadCapacity.toLong()) {
                "Zstd compression returned invalid size=$written, payloadCapacity=$payloadCapacity"
            }
            Math.addExact(Int.SIZE_BYTES, Math.toIntExact(written))
        } catch (failure: ZstdException) {
            if (failure.errorCode == Zstd.errDstSizeTooSmall()) throw BufferOverflowException()
            throw failure
        }
    }
```

public override는 `source.hasArray() && target.hasArray()`이면 heap, 둘 다 direct면 direct, 그 밖은 `super.compress/decompress` fallback을 사용한다. public companion `invoke(level)`와 level clamping은 그대로 두고 private constructor에 operations를 추가한다.

- [ ] **Step 7.5: declared-size-bounded decompression을 구현한다**

```kotlin
private fun decompressOptimized(source: ByteBuffer, target: ByteBuffer, direct: Boolean): Int =
    writeToCallerBuffer(source, target) { sourcePosition, sourceRemaining, targetPosition, targetRemaining ->
        if (sourceRemaining < Int.SIZE_BYTES) throw IndexOutOfBoundsException("Zstd header requires 4 bytes")
        val declaredSize = getIntBigEndian(source, sourcePosition)
        require(declaredSize >= 0) { "sourceSize must not be negative: $declaredSize" }
        require(declaredSize <= MAX_DECOMPRESSED_SIZE) { "sourceSize exceeds 256 MiB: $declaredSize" }
        if (declaredSize > targetRemaining) throw BufferOverflowException()

        val payloadOffset = sourcePosition + Int.SIZE_BYTES
        val payloadLength = sourceRemaining - Int.SIZE_BYTES
        val actual = try {
            if (direct) {
                operations.decompressDirect(
                    target, targetPosition, declaredSize,
                    source, payloadOffset, payloadLength,
                )
            } else {
                operations.decompressHeap(
                    target.array(), target.arrayOffset() + targetPosition, declaredSize,
                    source.array(), source.arrayOffset() + payloadOffset, payloadLength,
                )
            }
        } catch (failure: ZstdException) {
            if (failure.errorCode == Zstd.errDstSizeTooSmall()) {
                throw IllegalStateException("Zstd decompressed payload exceeds declared size=$declaredSize")
            }
            throw failure
        }

        if (actual != declaredSize.toLong()) {
            throw IllegalStateException(
                "Zstd decompressed size mismatch: expected=$declaredSize, actual=$actual"
            )
        }
        Math.toIntExact(actual)
    }
```

두 `IllegalStateException`은 cause를 설정하지 않는다. `DefaultZstdBufferOperations`는 현재 zstd-jni static offset API에 위임하며 반환 후 `Zstd.isError` 분기를 추가하지 않는다. 현재 resolved API가 error를 반환 전에 `ZstdException`으로 바꾸기 때문이다.

- [ ] **Step 7.6: Zstd GREEN/full regression/Detekt를 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.ZstdCompressorByteBufferTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferContractTest' \
  --no-build-cache --rerun-tasks
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-build-cache --rerun-tasks
./gradlew detekt detektMain detektTest --no-build-cache --rerun-tasks
git diff --check
```

예상 결과: under-declared large target의 native target length가 declared size이고 stable cause-less ISE, compression overflow와 기타 ZstdException identity, mixed fallback, full suite/Detekt가 PASS한다.

- [ ] **Step 7.7: docs/lesson/Lore commit과 Zstd PR을 수렴한다**

README Zstd 행은 heap→heap/direct→direct `optimized, evidence pending`, mixed/read-only heap
`fallback, ineligible`로 변경한다. lesson은 throw-before-return과 operation별
`errDstSizeTooSmall` 의미를 기록한다. Lore intent는
`Keep Zstd decompression inside the wire-declared size bound`. PR metadata: base `develop`, head `feat/issue-755-bytebuffer-zstd`, English title
`Add bounded caller-owned Zstd buffer paths`. Six-perspective review/CI 후 fresh merge approval에서 멈추며 evidence branch를 미리 만들지 않는다.

```bash
set -euo pipefail
git push -u origin feat/issue-755-bytebuffer-zstd
gh pr create --repo bluetape4k/bluetape4k-projects \
  --base develop --head feat/issue-755-bytebuffer-zstd \
  --title 'Add bounded caller-owned Zstd buffer paths' \
  --assignee debop --milestone '1.12.0' \
  --label enhancement --label performance --label infra/io \
  --body $'Refs #755\n\n## Summary\n- add matched-storage Zstd caller-owned paths\n- bind native decompression to the declared output size\n- preserve operation-specific overflow and corruption semantics\n\n## Verification\n- Zstd destination-bound and exception-taxonomy tests\n- full bluetape4k-io tests and Detekt\n- six-perspective review\n\n## DoD Status\n- [x] Zstd backend slice complete\n- [x] Allocation promotion remains pending final evidence'
```

**Step DoD:** Zstd PR이 declared size를 native write bound로 강제하고 CG-16 PENDING이다.

---

## Task 8: Adoption RED/GREEN — singleton concurrency와 caller examples를 수렴한다

**복잡도:** 높음 **Dependency:** Zstd PR merge + updated `develop` sync **Write
scope:** cross-codec integration tests와 public examples only **Pattern
skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**파일:**

- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferIntegrationTest.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferKotlinExampleTest.kt`
- 생성: `io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaExampleTest.java`

- [ ] **Step 8.1: evidence worktree를 merged develop에서 만든다**

```bash
set -euo pipefail
git -C /Users/debop/work/bluetape4k/bluetape4k-projects fetch origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects switch develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects pull --ff-only origin develop
git -C /Users/debop/work/bluetape4k/bluetape4k-projects worktree add \
  /Users/debop/work/bluetape4k/bluetape4k-projects/.worktrees/perf-issue-755-bytebuffer-compressor-evidence \
  -b perf/issue-755-bytebuffer-compressor-evidence origin/develop
```

예상 결과: core와 네 backend merge commit이 모두 ancestor이고 worktree clean.

- [ ] **Step 8.2: built-in singleton concurrency/integration RED를 작성한다**

```kotlin
class CompressorByteBufferIntegrationTest {
    @ParameterizedTest(name = "{0} shared singleton isolates success, overflow, and corrupt calls")
    @MethodSource("optimizedCompressors")
    fun `shared singleton calls are isolated`(name: String, compressor: Compressor) {
        val payload = "$name concurrent caller-owned payload".repeat(64).encodeToByteArray()
        val wire = compressor.compress(payload)

        MultithreadingTester()
            .workers(8)
            .rounds(20)
            .add {
                val compressedTarget = ByteBuffer.allocateDirect(payload.size * 2 + 64)
                val compressedSize = compressor.compress(ByteBuffer.wrap(payload), compressedTarget)
                val generatedWire = ByteArray(compressedSize).also { bytes ->
                    compressedTarget.flip()
                    compressedTarget.get(bytes)
                }
                compressor.decompress(generatedWire) shouldBeEqualTo payload

                val overflowTarget = ByteBuffer.allocateDirect(1)
                assertFailsWith<BufferOverflowException> {
                    compressor.decompress(ByteBuffer.wrap(wire), overflowTarget)
                }
                overflowTarget.position() shouldBeEqualTo 0
                val oneByteWire = compressor.compress(byteArrayOf(1))
                compressor.decompress(ByteBuffer.wrap(oneByteWire), overflowTarget) shouldBeEqualTo 1

                val retryTarget = ByteBuffer.allocateDirect(payload.size)
                assertCorruptFailure(name) {
                    compressor.decompress(ByteBuffer.wrap(wire.copyOf(wire.size - 1)), retryTarget)
                }
                retryTarget.position() shouldBeEqualTo 0
                val restoredSize = compressor.decompress(ByteBuffer.wrap(wire), retryTarget)
                restoredSize shouldBeEqualTo payload.size
                retryTarget.flip()
                ByteArray(retryTarget.remaining()).also(retryTarget::get) shouldBeEqualTo payload
            }
            .run()
    }

    companion object {
        @JvmStatic
        fun optimizedCompressors(): Stream<Arguments> = Stream.of(
            Arguments.of("lz4", Compressors.LZ4),
            Arguments.of("deflate", Compressors.Deflate),
            Arguments.of("snappy", Compressors.Snappy),
            Arguments.of("zstd", Compressors.Zstd),
        )

        private fun assertCorruptFailure(name: String, block: () -> Unit) {
            when (name) {
                "lz4" -> assertFailsWith<LZ4Exception>(block)
                "deflate" -> assertFailsWith<ZipException>(block)
                "snappy" -> assertFailsWith<SnappyException>(block)
                "zstd" -> assertFailsWith<ZstdException>(block)
                else -> error("Unknown compressor: $name")
            }
        }
    }
}
```

각 worker는 buffer를 공유하지 않는다. truncated wire가 dependency 버전에 따라 위 taxonomy와 다르면 backend test에서 검증한 deterministic corrupt mutation을 이 helper에 그대로 재사용한다. timing retry로 완화하지 않는다. Testcontainers나 외부 service는 없다.

- [ ] **Step 8.3: canonical Kotlin example와 bounded growth retry test를 작성한다**

```kotlin
class CompressorByteBufferKotlinExampleTest {
    @Test
    fun `caller-owned Kotlin example preserves source and returns a bounded view`() {
        val payload = "caller-owned Kotlin example".repeat(32).encodeToByteArray()
        val source = ByteBuffer.wrap(payload)
        val sourceStart = source.position()
        var remaining = 4
        val maximum = 1024 * 1024
        var overflowCount = 0
        var encoded: ByteBuffer? = null

        while (encoded == null) {
            val target = ByteBuffer.allocateDirect(16 + remaining).apply {
                position(16)
                limit(16 + remaining)
            }
            val start = target.position()
            try {
                val written = Compressors.LZ4.compress(source, target)
                source.position() shouldBeEqualTo sourceStart
                encoded = target.duplicate().position(start).limit(start + written).slice()
                checkNotNull(encoded).remaining() shouldBeEqualTo written
            } catch (_: BufferOverflowException) {
                overflowCount++
                source.position() shouldBeEqualTo sourceStart
                target.position() shouldBeEqualTo start
                require(remaining <= maximum / 2) { "application maximum exceeded" }
                remaining = Math.multiplyExact(remaining, 2)
            }
        }

        overflowCount.shouldBeGreaterThan(0)
        val restored = ByteBuffer.allocate(payload.size)
        Compressors.LZ4.decompress(checkNotNull(encoded), restored) shouldBeEqualTo payload.size
        restored.flip()
        ByteArray(restored.remaining()).also(restored::get) shouldBeEqualTo payload
    }
}
```

- [ ] **Step 8.4: canonical Java example를 작성한다**

```java
package io.bluetape4k.io.compressor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class CompressorByteBufferJavaExampleTest {
    @Test
    void callerOwnedJavaRoundTrip() {
        byte[] payload = "caller-owned Java example".repeat(32).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuffer source = ByteBuffer.wrap(payload);
        int sourceStart = source.position();
        ByteBuffer compressed = ByteBuffer.allocateDirect(16 + payload.length * 2);
        compressed.position(16);
        int compressedStart = compressed.position();
        int compressedSize = Compressors.INSTANCE.getLZ4().compress(source, compressed);
        org.junit.jupiter.api.Assertions.assertEquals(sourceStart, source.position());

        ByteBuffer encoded = compressed.duplicate();
        encoded.position(compressedStart).limit(compressedStart + compressedSize);
        encoded = encoded.slice();
        org.junit.jupiter.api.Assertions.assertEquals(compressedSize, encoded.remaining());
        ByteBuffer restored = ByteBuffer.allocate(8 + payload.length);
        restored.position(8);
        int restoredStart = restored.position();
        int restoredSize = Compressors.INSTANCE.getLZ4().decompress(encoded, restored);

        byte[] actual = new byte[restoredSize];
        ByteBuffer restoredView = restored.duplicate();
        restoredView.position(restoredStart).limit(restoredStart + restoredSize);
        restoredView.slice().get(actual);
        assertArrayEquals(payload, actual);
    }
}
```

`javap -classpath io/io/build/classes/kotlin/main io.bluetape4k.io.compressor.Compressors`로 현재 `INSTANCE.getLZ4()` accessor를 다시 검증하고 README Java example도 같은 형태로 고정한다.

- [ ] **Step 8.5: integration/examples RED 또는 regression-sensitive GREEN을 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferIntegrationTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferKotlinExampleTest' \
  --tests 'io.bluetape4k.io.compressor.CompressorByteBufferJavaExampleTest' \
  --no-build-cache --rerun-tasks
```

예상 결과: merged backend 구현이 정확하면 first run부터 GREEN이다. 실패하면 timing retry로 넘기지 않고 해당 backend slice의 lifecycle/state bug를 진단해 수정하고 전체 세 test를 처음부터 재실행한다.

- [ ] **Step 8.6: integration/examples를 Lore commit한다**

```bash
set -euo pipefail
git add io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferIntegrationTest.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/compressor/CompressorByteBufferKotlinExampleTest.kt \
  io/io/src/test/java/io/bluetape4k/io/compressor/CompressorByteBufferJavaExampleTest.java
git commit -m 'Prove caller-owned compressor adoption across shared singletons' \
  -m 'Constraint: Built-in singleton use must isolate success, failure, and retry state across threads
Rejected: Rely on backend unit tests alone | They do not prove factory singleton integration or caller examples
Confidence: high
Scope-risk: moderate
Directive: Keep mutable buffers thread-confined and bound retry growth with an application maximum
Tested: Cross-codec concurrency and Kotlin/Java caller examples
Not-tested: Allocation evidence is produced by the next task'
```

**Step DoD:** 네 built-in singleton과 Kotlin/Java examples가 merged API/backend contract를 함께 증명한다.

---

## Task 9: Benchmark RED/GREEN — thread-local harness와 fail-closed evidence tool을 만든다

**복잡도:** 높음 **Dependency:** Task 8 **작성 범위:** io existing benchmark source와 `io/io/scripts` only **Pattern
skill:** `bluetape-kotlin-patterns`, `test-driven-development`

**파일:**

- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmark.kt`
- 생성: `io/io/src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmarkTest.kt`
- 생성: `io/io/scripts/run-bytebuffer-compressor-evidence.py`
- 생성: `io/io/scripts/test_run_bytebuffer_compressor_evidence.py`

이 harness는 production module에 새 dependency를 넣지 않고 이미 `kotlinx.benchmark` plugin이 적용된 `:bluetape4k-io`의 `testBenchmarkJar`를 재사용한다. repository benchmark hazard의 “existing benchmark module” branch다.

- [ ] **Step 9.1: Gradle task authority를 다시 확인한다**

```bash
set -euo pipefail
./gradlew :bluetape4k-io:tasks --all --no-configuration-cache | \
  grep -E '^(testBenchmark|testBenchmarkCompile|testBenchmarkJar)'
```

예상 결과: `testBenchmark`, `testBenchmarkCompile`, `testBenchmarkJar`가 존재한다. task 이름이 바뀌면 plan/spec의 command를 먼저 갱신하고 plan review를 재실행한다.

- [ ] **Step 9.2: benchmark dispatch/state unit RED를 작성한다**

```kotlin
internal object CallerOwnedCompressionDispatch {
    fun source(storagePath: String, bytes: ByteArray): ByteBuffer = when (storagePath) {
        "direct", "directToHeap" -> ByteBuffer.allocateDirect(bytes.size).put(bytes).flip()
        "heap", "heapToDirect" -> ByteBuffer.wrap(bytes)
        else -> error("Unknown storagePath=$storagePath")
    }

    fun target(storagePath: String, capacity: Int): ByteBuffer = when (storagePath) {
        "direct", "heapToDirect" -> ByteBuffer.allocateDirect(capacity)
        "heap", "directToHeap" -> ByteBuffer.allocate(capacity)
        else -> error("Unknown storagePath=$storagePath")
    }

    fun eligible(codec: String, storagePath: String): Boolean = when (codec) {
        "lz4", "deflate" -> storagePath in setOf("heap", "direct", "heapToDirect", "directToHeap")
        "snappy", "zstd" -> storagePath in setOf("heap", "direct")
        else -> false
    }
}

class CallerOwnedByteBufferCompressorBenchmarkTest {
    @Test
    fun `mutable benchmark state is thread scoped`() {
        val annotation = CallerOwnedByteBufferCompressorBenchmark::class.java.getAnnotation(State::class.java)
        annotation.value shouldBeEqualTo Scope.Thread
    }

    @Test
    fun `eligibility matches backend storage capabilities`() {
        CallerOwnedCompressionDispatch.eligible("lz4", "heapToDirect") shouldBeEqualTo true
        CallerOwnedCompressionDispatch.eligible("deflate", "directToHeap") shouldBeEqualTo true
        CallerOwnedCompressionDispatch.eligible("snappy", "heapToDirect") shouldBeEqualTo false
        CallerOwnedCompressionDispatch.eligible("zstd", "directToHeap") shouldBeEqualTo false
    }

    @Test
    fun `setup validates every candidate against the byte array wire`() {
        CallerOwnedByteBufferCompressorBenchmark().apply {
            compressorName = "zstd"
            payloadSize = "small"
            storagePath = "direct"
            setup()
            validateRoundTrip()
        }
    }

}
```

같은 test file의 `measured methods contain no allocation or payload copy calls` test는 module working directory 기준 `src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmark.kt`를 읽고 여섯 `@Benchmark` 함수 body를 brace-depth로 추출한다. caller-owned 두 body에는 저장된 limit/position reset과 codec call만, allocating baseline 네 body에는 필요한 source limit/position reset과 codec call만 허용하고 `allocate`, `allocateDirect`, `wrap`, `ByteArray(`, `.copy`, `.slice(`를 forbidden token으로 검사한다. 별도 RED는 모든 measured invocation이 position뿐 아니라 setup에서 캡처한 limit도 복원하는지 source body를 검사한다.

- [ ] **Step 9.3: thread-local benchmark state를 구현한다**

```kotlin
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
class CallerOwnedByteBufferCompressorBenchmark {
    @Param("lz4", "deflate", "snappy", "zstd")
    lateinit var compressorName: String

    @Param("small", "medium", "large")
    lateinit var payloadSize: String

    @Param("heap", "direct", "heapToDirect", "directToHeap")
    lateinit var storagePath: String

    private lateinit var compressor: Compressor
    private lateinit var payload: ByteArray
    private lateinit var wire: ByteArray
    private lateinit var plainSource: ByteBuffer
    private lateinit var compressedSource: ByteBuffer
    private lateinit var compressedTarget: ByteBuffer
    private lateinit var restoredTarget: ByteBuffer
    private var plainStart: Int = 0
    private var plainLimit: Int = 0
    private var compressedStart: Int = 0
    private var compressedLimit: Int = 0
    private var compressedTargetStart: Int = 0
    private var compressedTargetLimit: Int = 0
    private var restoredTargetStart: Int = 0
    private var restoredTargetLimit: Int = 0

    @Setup(Level.Trial)
    fun setup() {
        compressor = SameConditionCompressionPayloads.commonCompressor(compressorName).compressor
        val size = SameConditionPayloadSize.valueOf(payloadSize.replaceFirstChar(Char::uppercaseChar))
        payload = SameConditionCompressionPayloads.payload(SameConditionPayloadKind.Json, size)
        wire = compressor.compress(payload)
        plainSource = CallerOwnedCompressionDispatch.source(storagePath, payload)
        compressedSource = CallerOwnedCompressionDispatch.source(storagePath, wire)
        compressedTarget = CallerOwnedCompressionDispatch.target(
            storagePath,
            maxOf(payload.size * 2 + 64, wire.size * 2 + 64),
        )
        restoredTarget = CallerOwnedCompressionDispatch.target(storagePath, payload.size)
        plainStart = plainSource.position()
        plainLimit = plainSource.limit()
        compressedStart = compressedSource.position()
        compressedLimit = compressedSource.limit()
        compressedTargetStart = compressedTarget.position()
        compressedTargetLimit = compressedTarget.limit()
        restoredTargetStart = restoredTarget.position()
        restoredTargetLimit = restoredTarget.limit()
        validateRoundTrip()
    }

    fun validateRoundTrip() {
        plainSource.limit(plainLimit).position(plainStart)
        compressedTarget.limit(compressedTargetLimit).position(compressedTargetStart)
        val compressedSize = compressor.compress(plainSource, compressedTarget)
        val candidateWire = ByteArray(compressedSize).also { bytes ->
            compressedTarget.duplicate()
                .position(compressedTargetStart)
                .limit(compressedTargetStart + compressedSize)
                .get(bytes)
        }
        check(compressor.decompress(candidateWire).contentEquals(payload))

        resetCallerOwned()
        val written = compressor.decompress(compressedSource, restoredTarget)
        check(written == payload.size)
        val restored = ByteArray(written).also { bytes ->
            restoredTarget.duplicate()
                .position(restoredTargetStart)
                .limit(restoredTargetStart + written)
                .get(bytes)
        }
        check(restored.contentEquals(payload))
    }

    @Benchmark
    fun compressByteArrayBaseline(): ByteArray = compressor.compress(payload)

    @Benchmark
    fun compressByteBufferBaseline(): ByteBuffer {
        plainSource.limit(plainLimit).position(plainStart)
        return compressor.compress(plainSource)
    }

    @Benchmark
    fun compressCallerOwned(): Int {
        plainSource.limit(plainLimit).position(plainStart)
        compressedTarget.limit(compressedTargetLimit).position(compressedTargetStart)
        return compressor.compress(plainSource, compressedTarget)
    }

    @Benchmark
    fun decompressByteArrayBaseline(): ByteArray = compressor.decompress(wire)

    @Benchmark
    fun decompressByteBufferBaseline(): ByteBuffer {
        compressedSource.limit(compressedLimit).position(compressedStart)
        return compressor.decompress(compressedSource)
    }

    @Benchmark
    fun decompressCallerOwned(): Int {
        resetCallerOwned()
        return compressor.decompress(compressedSource, restoredTarget)
    }

    private fun resetCallerOwned() {
        compressedSource.limit(compressedLimit).position(compressedStart)
        restoredTarget.limit(restoredTargetLimit).position(restoredTargetStart)
    }
}
```

`CallerOwnedCompressionDispatch`는 setup에서만 buffer를 allocate/copy한다. measured methods에는
`allocate`, `allocateDirect`, `wrap`, payload copy가 없어야 한다. primary matched baseline은 동일 params의 `*ByteBufferBaseline`, `ByteArrayBaseline`은 secondary context다.

- [ ] **Step 9.4: benchmark RED/GREEN을 실행한다**

```bash
set -euo pipefail
repo-test-summary -- ./gradlew :bluetape4k-io:test \
  --tests 'io.bluetape4k.io.benchmark.CallerOwnedByteBufferCompressorBenchmarkTest' \
  --no-build-cache --rerun-tasks
./gradlew :bluetape4k-io:testBenchmarkCompile --no-build-cache --rerun-tasks
./gradlew :bluetape4k-io:testBenchmarkJar --no-build-cache --rerun-tasks
```

예상 결과: unit test, generated JMH compile, exactly one `*-JMH.jar` build PASS.

- [ ] **Step 9.5: evidence validator unit RED를 작성한다**

Python `unittest`는 아래 deterministic fixture factory를 test file 안에 먼저 정의하고, 각 invalid fixture를 각각 거부해야 한다.

```python
def metric(score=100.0, error=1.0, unit="B/op"):
    return {"score": score, "scoreError": error, "scoreUnit": unit}

PAYLOAD_BYTES = {"small": 1147, "medium": 65718, "large": 524349}

def result(method, payload_size, allocation):
    return {
        "benchmark": f"CallerOwnedByteBufferCompressorBenchmark.{method}",
        "params": {
            "compressorName": "lz4",
            "payloadSize": payload_size,
            "storagePath": "heap",
        },
        "primaryMetric": metric(score=10_000.0, error=100.0, unit="ops/s"),
        "secondaryMetrics": {"gc.alloc.rate.norm": metric(allocation)},
    }

def valid_run(*, state_scope="Thread", run_id="run-20260721T120000Z-00000001"):
    return {
        "metadata": {
            "runId": run_id,
            "commit": "a" * 40,
            "tree": "b" * 40,
            "jarSha256": "c" * 64,
            "stateScope": state_scope,
            "jdk": "21.0.8+9",
            "jvm": "OpenJDK 64-Bit Server VM",
            "gc": "G1 Young Generation + G1 Concurrent GC + G1 Old Generation",
            "os": "macOS 15.5 aarch64",
            "cpu": "Apple M4 Max",
            "dependenciesSha256": "d" * 64,
        },
        "dependencies": ["lz4-java=1.11.0", "snappy-java=1.1.10.8", "zstd-jni=1.5.7-11"],
        "sourceInspection": [{
            "codec": "lz4",
            "operation": "compress",
            "storagePath": "heap",
            "path": "io/io/src/main/kotlin/io/bluetape4k/io/compressor/LZ4Compressor.kt",
            "symbol": "compress(ByteBuffer, ByteBuffer)",
            "sha256": "e" * 64,
            "payloadIntermediateFree": True,
        }],
        "argv": ["-t", "1", "-f", "2", "-wi", "3", "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json"],
        "jmh": [
            row
            for payload_size in PAYLOAD_BYTES
            for row in (
                result("compressByteBufferBaseline", payload_size, PAYLOAD_BYTES[payload_size] + 512.0),
                result("compressCallerOwned", payload_size, 128.0),
            )
        ],
    }

def valid_pair(*, codec="lz4", storage="heap", candidate_delta=None):
    baseline = valid_run()
    candidate = valid_run(run_id="run-20260721T121000Z-00000002")
    for run in (baseline, candidate):
        for result in run["jmh"]:
            result["params"]["compressorName"] = codec
            result["params"]["storagePath"] = storage
    if candidate_delta is not None:
        for run in (baseline, candidate):
            for baseline_index in range(0, len(run["jmh"]), 2):
                baseline_score = run["jmh"][baseline_index]["secondaryMetrics"]["gc.alloc.rate.norm"]["score"]
                run["jmh"][baseline_index + 1]["secondaryMetrics"]["gc.alloc.rate.norm"] = metric(
                    baseline_score * (1.0 + candidate_delta / 100.0), 1.0
                )
    return [baseline, candidate]

def valid_runs(*, allocation_delta=None, throughput_delta=0.0, non_overlapping=True):
    runs = valid_pair(candidate_delta=allocation_delta)
    error = 1.0 if non_overlapping else 20.0
    for run in runs:
        run["jmh"][1]["primaryMetric"] = metric(
            10_000.0 * (1.0 + throughput_delta / 100.0), error, "ops/s"
        )
    return runs
```

```python
class EvidenceValidationTest(unittest.TestCase):
    def test_rejects_missing_allocation_metric(self):
        run = valid_run()
        del run["jmh"][0]["secondaryMetrics"]["gc.alloc.rate.norm"]
        with self.assertRaisesRegex(ValueError, "gc.alloc.rate.norm"):
            validate_run(run)

    def test_rejects_non_thread_state(self):
        run = valid_run(state_scope="Benchmark")
        with self.assertRaisesRegex(ValueError, "Scope.Thread"):
            validate_run(run)

    def test_rejects_fallback_positive_promotion(self):
        rows = valid_pair(codec="snappy", storage="heapToDirect")
        with self.assertRaisesRegex(ValueError, "ineligible"):
            compare_runs(rows)

    def test_requires_two_unique_runs_with_same_identity(self):
        first, second = valid_runs()
        second["metadata"]["jarSha256"] = "0" * 64
        with self.assertRaisesRegex(ValueError, "JAR identity"):
            compare_runs([first, second])

    def test_blocks_significant_throughput_regression(self):
        runs = valid_runs(throughput_delta=-25.0, non_overlapping=True)
        self.assertEqual("design-review-required", compare_runs(runs)[0]["verdict"])

    def test_one_run_throughput_regression_does_not_block(self):
        runs = valid_runs()
        runs[1]["jmh"][1]["primaryMetric"] = metric(7_500.0, 1.0, "ops/s")
        self.assertNotEqual("design-review-required", compare_runs(runs)[0]["verdict"])

    def test_accepts_monotonic_payload_scaled_savings_in_both_runs(self):
        self.assertEqual("accepted", compare_runs(valid_runs())[0]["verdict"])

    def test_five_percent_gate_without_payload_scaling_is_not_demonstrated(self):
        runs = valid_runs(allocation_delta=-10.0)
        self.assertEqual("not-demonstrated", compare_runs(runs)[0]["verdict"])

    def test_requires_each_run_to_contain_its_own_matched_pair(self):
        runs = valid_runs()
        runs[1]["jmh"].pop(0)
        with self.assertRaisesRegex(ValueError, "matched baseline"):
            compare_runs(runs)
```

추가 unit tests: no-clobber duplicate run ID, dirty tree, commit/tree mismatch, argv/JDK/JVM/GC/OS/CPU/ dependency metadata 누락, non-finite score/error, unit != `B/op`, canonical argv exact order/duplicate/missing/ extra argument 거부, fork/warmup/measurement/thread mismatch, baseline pair 누락/duplicate/reversal/params mismatch, medium 또는 large pair 누락, run1만 개선, run2만 개선, error interval overlap, 한 run만 5% 개선, 두 run 모두 5%/CI gate를 통과하지만 payload scaling이 부족하면 `not-demonstrated`,
`sourceInspection` 누락 또는 production source hash mismatch, 음수·boolean·numeric-string score/error, extra raw file/directory, output/run/file symlink, resolved-parent 이탈, shell metacharacter literal argv, log exit mismatch, >16MiB log abort. smoke가 생성한 실제 JMH JSON도 validator에 다시 입력해 synthetic fixture와 실제 schema의 차이를 차단한다. payload byte 수는 존재하지 않는 JMH parameter로 위조하지 않고 현재 JSON generator의 실제 UTF-8 결과인
`small=1147`, `medium=65718`, `large=524349` exact mapping에서만 파생하며 unknown payloadSize를 거부한다. benchmark unit test는 세 `SameConditionCompressionPayloads.payload(Json, size).size`가 이 mapping과 정확히 같은지 검증해 generator drift가 evidence에 진입하기 전에 실패시킨다. authority bootstrap tests는 missing `docs/benchmarks/raw/issue-755`에서 first prepare/run 성공, existing real root 재사용, root symlink/non-directory 거부를 고정한다. publication race tests는 staging root symlink, pending directory symlink, 같은 run ID의 두 concurrent publisher, final absence check 직후 경쟁자가 만든 destination을 고정한다. 정확히 한 publisher만 성공하고 기존 final artifact bytes는 어떤 경우에도 교체되지 않아야 한다.

delivery provenance RED는 docs-only allowlist descendant의 성공과 다음 실패를 각각 고정한다:
`evidence_head`의 non-descendant, production/benchmark/runner/dependency 변경 descendant, allowlist 밖 추가 file, rebuilt JMH JAR SHA mismatch. 허용 diff라도 symlink 또는 raw evidence rewrite면 거부한다.

- [ ] **Step 9.6: focused fail-closed runner를 구현한다**

```python
SCHEMA_VERSION = 1
MAX_RUN_LOG_BYTES = 16 * 1024 * 1024
JVM_ARGS = ["-Xms1g", "-Xmx1g", "-XX:+UseG1GC"]
PROFILE_ARGS = {
    "smoke": ["-t", "1", "-f", "1", "-wi", "1", "-i", "1", "-w", "100ms", "-r", "100ms", "-prof", "gc", "-rf", "json"],
    "canonical": ["-t", "1", "-f", "2", "-wi", "3", "-i", "5", "-w", "1s", "-r", "1s", "-prof", "gc", "-rf", "json"],
}
PAYLOAD_BYTES = {"small": 1147, "medium": 65718, "large": 524349}
ELIGIBLE = {
    (codec, storage, operation): (
        codec in {"lz4", "deflate"} or storage in {"heap", "direct"}
    )
    for codec in ("lz4", "deflate", "snappy", "zstd")
    for storage in ("heap", "direct", "heapToDirect", "directToHeap")
    for operation in ("compress", "decompress")
}

def allocation_interval(metric):
    score = finite(metric["score"], "allocation score")
    error = finite(metric["scoreError"], "allocation scoreError")
    if metric["scoreUnit"] != "B/op":
        raise ValueError("gc.alloc.rate.norm unit must be B/op")
    if score < 0 or error < 0:
        raise ValueError("allocation score and scoreError must be non-negative")
    return score - error, score + error

def finite(value, label):
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ValueError(f"{label} must be a JSON number")
    number = float(value)
    if not math.isfinite(number):
        raise ValueError(f"{label} must be finite")
    return number

def allocation_accepted(baseline, candidate):
    baseline_low, baseline_high = allocation_interval(baseline)
    candidate_low, candidate_high = allocation_interval(candidate)
    return candidate["score"] <= baseline["score"] * 0.95 and candidate_high < baseline_low

def scaling_demonstrated(per_payload):
    saved = {
        payload: finite(baseline["score"], f"{payload} baseline allocation")
        - finite(candidate["score"], f"{payload} candidate allocation")
        for payload, (baseline, candidate) in per_payload.items()
    }
    return (
        saved["small"] < saved["medium"] < saved["large"]
        and saved["medium"] / PAYLOAD_BYTES["medium"] >= 0.50
        and saved["large"] / PAYLOAD_BYTES["large"] >= 0.50
    )

def throughput_regressed(baseline, candidate):
    baseline_score = finite(baseline["score"], "throughput score")
    candidate_score = finite(candidate["score"], "throughput score")
    baseline_error = finite(baseline["scoreError"], "throughput scoreError")
    candidate_error = finite(candidate["scoreError"], "throughput scoreError")
    if min(baseline_score, candidate_score) <= 0 or min(baseline_error, candidate_error) < 0:
        raise ValueError("throughput scores must be positive and errors non-negative")
    baseline_low = baseline_score - baseline_error
    candidate_high = candidate_score + candidate_error
    return candidate_score <= baseline_score * 0.80 and candidate_high < baseline_low

def ensure_authority_root(repo_root):
    repo_root = repo_root.resolve(strict=True)
    raw_parent = repo_root / "docs/benchmarks/raw"
    if raw_parent.is_symlink() or not stat.S_ISDIR(os.lstat(raw_parent).st_mode):
        raise ValueError("raw benchmark parent must be a real directory")
    parent_fd = os.open(raw_parent, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
    try:
        try:
            os.mkdir("issue-755", 0o755, dir_fd=parent_fd)
        except FileExistsError:
            pass
        root_stat = os.stat("issue-755", dir_fd=parent_fd, follow_symlinks=False)
        if not stat.S_ISDIR(root_stat.st_mode):
            raise ValueError("issue-755 authority root must be a real directory")
        root_fd = os.open("issue-755", os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=parent_fd)
        try:
            os.fsync(root_fd)
            os.fsync(parent_fd)
        finally:
            os.close(root_fd)
    finally:
        os.close(parent_fd)
    root = raw_parent / "issue-755"
    if root.parent.resolve(strict=True) != raw_parent.resolve(strict=True):
        raise ValueError("issue-755 authority parent mismatch")
    return root

def create_staging_directory(final_root, run_id):
    if not re.fullmatch(r"run-[0-9]{8}T[0-9]{6}Z-[0-9a-f]{8}", run_id):
        raise ValueError("invalid run id")
    if final_root.is_symlink() or not stat.S_ISDIR(os.lstat(final_root).st_mode):
        raise ValueError("final root must be a real directory")
    final_root = final_root.resolve(strict=True)
    staging_root = final_root.parent / ".issue-755-staging"
    staging_root.mkdir(mode=0o700, parents=True, exist_ok=True)
    if staging_root.is_symlink() or not stat.S_ISDIR(os.lstat(staging_root).st_mode):
        raise ValueError("staging root must be a real directory")
    if staging_root.parent.resolve(strict=True) != final_root.parent:
        raise ValueError("staging root parent mismatch")
    path = staging_root / f"{run_id}.pending"
    path.mkdir(mode=0o700, exist_ok=False)
    if path.is_symlink() or not stat.S_ISDIR(os.lstat(path).st_mode):
        raise ValueError("pending run must be a real directory")
    if path.parent.resolve(strict=True) != staging_root.resolve(strict=True):
        raise ValueError("pending run parent mismatch")
    return path

@contextlib.contextmanager
def exclusive_run_lock(staging_root, run_id):
    lock_path = staging_root / f".{run_id}.lock"
    fd = os.open(lock_path, os.O_RDWR | os.O_CREAT | os.O_NOFOLLOW, 0o600)
    try:
        fcntl.flock(fd, fcntl.LOCK_EX)
        yield
    finally:
        fcntl.flock(fd, fcntl.LOCK_UN)
        os.close(fd)

def rename_noreplace(source, destination):
    libc = ctypes.CDLL(None, use_errno=True)
    at_fdcwd = -2
    if sys.platform == "darwin":
        result = libc.renameatx_np(
            at_fdcwd, os.fsencode(source), at_fdcwd, os.fsencode(destination), 0x00000004,
        )  # RENAME_EXCL
    elif sys.platform.startswith("linux"):
        result = libc.renameat2(
            at_fdcwd, os.fsencode(source), at_fdcwd, os.fsencode(destination), 0x00000001,
        )  # RENAME_NOREPLACE
    else:
        raise RuntimeError("atomic no-replace rename is unsupported on this platform")
    if result != 0:
        error = ctypes.get_errno()
        raise OSError(error, os.strerror(error), destination)

def publish_run_directory(staging, final_root, run_id):
    if final_root.is_symlink() or not stat.S_ISDIR(os.lstat(final_root).st_mode):
        raise ValueError("final root must be a real directory")
    final_root = final_root.resolve(strict=True)
    staging_root = staging.parent.resolve(strict=True)
    if staging.is_symlink() or not stat.S_ISDIR(os.lstat(staging).st_mode):
        raise ValueError("staging run must be a real directory")
    if staging_root != final_root.parent / ".issue-755-staging":
        raise ValueError("staging parent mismatch")
    lock_root = pathlib.Path("io/io/build/issue-755-evidence/locks")
    lock_root.mkdir(mode=0o700, parents=True, exist_ok=True)
    if lock_root.is_symlink() or not stat.S_ISDIR(os.lstat(lock_root).st_mode):
        raise ValueError("lock root must be a real directory")
    lock_root = lock_root.resolve(strict=True)
    final = final_root / run_id
    with exclusive_run_lock(lock_root, run_id):
        if final.exists() or final.is_symlink():
            raise FileExistsError(final)
        validate_complete_run(staging)
        rename_noreplace(staging, final)
        fsync_directory(final_root)
    try:
        staging_root.rmdir()
    except OSError as cleanup_failure:
        if cleanup_failure.errno not in {errno.ENOTEMPTY, errno.ENOENT}:
            warnings.warn(f"published run; staging-root cleanup skipped: {cleanup_failure}")
    return final
```

CLI subcommands는 `prepare`, `smoke`, `run --profile canonical`, `compare`, `validate-delivery`로 고정한다. 각 run은 authority 밖의 same-filesystem sibling `.issue-755-staging/<run-id>.pending`에
`jmh.json`, `metadata.json`, `argv.json`, `environment.json`, `dependencies.txt`, `run.log`,
`summary.csv`, `source-inspection.json`, `validation.json`을 atomic/no-clobber regular file로 기록한다. JMH exit, size cap, schema, source inspection과 file set 전체가 PASS한 뒤 per-run exclusive lock 아래 destination 부재를 다시 검사하고 OS-native no-replace rename으로만 directory를
`docs/benchmarks/raw/issue-755/<run-id>`에 publish한 뒤 parent directory를 fsync한다. rename+fsync 뒤 shared staging root 정리는 publication contract 밖의 best-effort다. 다른 pending run 때문에 `ENOTEMPTY`이거나 이미 정리된 `ENOENT`이면 성공을 뒤집지 않고, 다른 cleanup 오류도 warning으로 기록하되 이미 publish된 immutable final artifact를 실패/재시도 대상으로 오분류하지 않는다. 실패/interrupt 시 final run directory는 존재하지 않으며 owned `.pending` staging만 정리 또는 진단 대상이다. final authority 안에 불완전 run을 먼저 만들지 않는다.
`source-inspection.json`은 eligible codec/operation/storage별 production source path, symbol, file SHA와 payload-sized `ByteArray`/`ByteBuffer.allocate*` 부재 검사 결과를 exact tree에서 캡처한다. runner는 clean exact commit/tree, single JMH jar SHA, literal argv, JDK/JVM/GC, OS/CPU, dependency output를 먼저 캡처하고 JMH를
`subprocess.run(argvList, shell=False)`로 한 번만 실행한다. compare는 run ID를 제외한 identity, params/method set, matched baseline, eligibility와 small/medium/large 완전성을 검증한 뒤 두 run 모두 accepted일 때만 `accepted`를 낸다. 각 comparison row는 `PAYLOAD_BYTES`에서 파생한 actual payload bytes, baseline/candidate B/op, absolute B/op saved, payload 대비 saved ratio를 기록한다. 각 eligible codec/operation/storage의 각 canonical run에서 saved bytes가 small < medium < large이고 medium/large saved ratio가 각각 0.50 이상일 때만 `scaling_demonstrated`다. 어느 run에서든 이 predicate가 false면 5%/CI gate가 PASS해도 final verdict는 `not-demonstrated`다.

- [ ] **Step 9.7: Python unit tests와 JMH smoke를 실행한다**

```bash
set -euo pipefail
python3 -m unittest -v io/io/scripts/test_run_bytebuffer_compressor_evidence.py
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py smoke \
  --jar io/io/build/benchmarks/test/jars/*-JMH.jar \
  --output-root io/io/build/issue-755-evidence \
  --include '.*CallerOwnedByteBufferCompressorBenchmark.*' \
  --param compressorName=lz4 --param payloadSize=small --param storagePath=heap
```

예상 결과: Python tests PASS. Smoke는 `gc.alloc.rate.norm` B/op와 primary throughput을 가진 valid JMH JSON을 생성하지만 `docs/benchmarks/raw`에는 아직 promotion하지 않는다.

- [ ] **Step 9.8: harness와 runner를 Lore commit한다**

```bash
set -euo pipefail
git add io/io/src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmark.kt \
  io/io/src/test/kotlin/io/bluetape4k/io/benchmark/CallerOwnedByteBufferCompressorBenchmarkTest.kt \
  io/io/scripts/run-bytebuffer-compressor-evidence.py \
  io/io/scripts/test_run_bytebuffer_compressor_evidence.py
git commit -m 'Make compressor allocation claims reproducible and fail closed' \
  -m 'Constraint: Allocation promotion needs two identical-provenance runs and thread-confined mutable buffers
Rejected: Reuse short-window throughput results | They do not prove B/op reductions or evidence identity
Confidence: high
Scope-risk: moderate
Directive: Invalidate both runs after any benchmark input or JAR change and never promote fallback cells
Tested: Benchmark state/dispatch tests, benchmark compile/JAR, evidence validator unit tests, JMH GC-profiler smoke
Not-tested: Canonical two-run results are produced only from the committed exact JAR in Task 10'
```

**Step
DoD:** committed harness는 `Scope.Thread`, measured allocation-free setup discipline와 fail-closed evidence schema를 증명한다.

---

## Task 10: Canonical evidence·문서·최종 PR을 exact head에서 수렴한다

**복잡도:** 높음 **Dependency:** Task 9 commit **Write
scope:** raw evidence, benchmark report/index, module benchmark/readmes, CHANGELOG, shared lesson **Pattern
skill:** `bluetape-writer`, `verification-before-completion`, `requesting-code-review`

**파일:**

- 생성: `docs/benchmarks/raw/issue-755/run-<UTC>-<id>/` exactly twice
- 생성: `docs/benchmarks/raw/issue-755/comparison.csv`
- 생성: `docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md`
- 수정: `docs/benchmarks/README.md`
- 수정: `io/io/Benchmark.md`
- 수정: `io/io/README.md`
- 수정: `io/io/README.ko.md`
- 수정: `CHANGELOG.md`
- 수정: `docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md`

- [ ] **Step 10.1: committed exact JMH jar와 clean tracked tree를 고정한다**

```bash
set -euo pipefail
test -z "$(git status --porcelain)"
evidence_head="$(git rev-parse HEAD)"
./gradlew :bluetape4k-io:testBenchmarkCompile \
  :bluetape4k-io:testBenchmarkJar --no-build-cache --rerun-tasks
jmh_jars=()
while IFS= read -r jar; do
  jmh_jars+=("$jar")
done < <(find io/io/build/benchmarks/test/jars -type f -name '*-JMH.jar' -print | sort)
test "${#jmh_jars[@]}" -eq 1
jmh_jar="${jmh_jars[0]}"
jar_sha="$(shasum -a 256 "$jmh_jar" | awk '{print $1}')"
test -n "$jar_sha"
test "$evidence_head" = "$(git rev-parse HEAD)"
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py prepare \
  --jar "$jmh_jar" \
  --expected-head "$evidence_head" \
  --output-root docs/benchmarks/raw/issue-755 \
  --receipt io/io/build/issue-755-evidence/input.json
```

이 block은 Bash에서 실행한다. `prepare`는 repository-relative exact parent
`docs/benchmarks/raw`를 real/non-symlink directory로 검증하고 `ensure_authority_root`로 missing
`issue-755` child를 dir-fd `mkdir`한 뒤 child/parent를 fsync한다. existing symlink/non-directory는 거부한다. runner는 첫 run 전에는 완전 clean tree를 요구한다. 두 번째 run은 자신이 receipt로 반환한 첫 번째 run ID 하나만 exact allowlist로 허용한다. `lstat` 기준 symlink를 output root, run directory, 내부 file 어디서도 허용하지 않고 resolved parent가 repository의
`docs/benchmarks/raw/issue-755`인지 확인한다. 두 번째 run 전에 다른 file/directory 또는
`comparison.csv`가 있으면 거부한다. `HEAD`, `HEAD^{tree}`와 JAR SHA는 두 run에서 동일해야 한다.

- [ ] **Step 10.2: build-only smoke 후 canonical run 두 개를 순차 실행한다**

```bash
set -euo pipefail
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py smoke \
  --input-receipt io/io/build/issue-755-evidence/input.json \
  --output-root io/io/build/issue-755-evidence \
  --include '.*CallerOwnedByteBufferCompressorBenchmark.*' \
  --param compressorName=lz4 --param payloadSize=small --param storagePath=heap

run_one="$(python3 io/io/scripts/run-bytebuffer-compressor-evidence.py run \
  --profile canonical \
  --input-receipt io/io/build/issue-755-evidence/input.json \
  --output-root docs/benchmarks/raw/issue-755 \
  --include '.*CallerOwnedByteBufferCompressorBenchmark.*')"

run_two="$(python3 io/io/scripts/run-bytebuffer-compressor-evidence.py run \
  --profile canonical \
  --input-receipt io/io/build/issue-755-evidence/input.json \
  --output-root docs/benchmarks/raw/issue-755 \
  --include '.*CallerOwnedByteBufferCompressorBenchmark.*')"

test "$run_one" != "$run_two"
test -d "$run_one"
test -d "$run_two"
```

각 `run`은 stdout에 생성한 absolute run directory 한 줄만 출력한다. canonical argv는 정확히
`-t 1 -f 2 -wi 3 -i 5 -w 1s -r 1s -prof gc -rf json`이고 JVM args는
`-Xms1g -Xmx1g -XX:+UseG1GC`다. 두 run 사이에 source, benchmark parameter, dependency, JDK/JVM, GC, OS/CPU, JAR가 바뀌면 둘 다 폐기하고 Step 10.1부터 다시 실행한다.

- [ ] **Step 10.3: 두 run을 비교하고 fail-closed promotion gate를 실행한다**

```bash
set -euo pipefail
run_dirs=()
while IFS= read -r run_dir; do run_dirs+=("$run_dir"); done < <(
  find docs/benchmarks/raw/issue-755 -mindepth 1 -maxdepth 1 -type d -name 'run-*' -print | sort
)
test "${#run_dirs[@]}" -eq 2
run_one="${run_dirs[0]}"
run_two="${run_dirs[1]}"
evidence_head="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["commit"])' "$run_one/metadata.json")"
test "$evidence_head" = "$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["commit"])' "$run_two/metadata.json")"
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py compare \
  --run "$run_one" --run "$run_two" \
  --output docs/benchmarks/raw/issue-755/comparison.csv
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py validate-delivery \
  --run "$run_one" --run "$run_two" \
  --comparison docs/benchmarks/raw/issue-755/comparison.csv \
  --expected-head "$evidence_head"
```

positive allocation 판정은 eligible cell의 matched `ByteBuffer` baseline 대비 두 run 각각
`gc.alloc.rate.norm`이 5% 이상 감소하고 error interval이 겹치지 않을 때만 허용한다. fallback 또는 mixed-storage cell은 수치와 무관하게 `ineligible`이다. throughput은 개선을 주장하지 않는다. 두 run 모두 20% 이상 퇴행하고 error interval이 겹치지 않는 cell이 하나라도 있으면 verdict는
`design-review-required`이고 문서 promotion/PR을 중단해 해당 backend 설계를 재개방한다. eligible codec/operation/storage마다 1 KiB, 64 KiB, 512 KiB 세 row가 모두 있어야 하며 report는 absolute B/op saved와 payload 대비 saved ratio의 scaling을 함께 해석한다. scaling 또는
`source-inspection.json` 근거가 payload-sized intermediate 제거를 뒷받침하지 않으면 5% gate를 통과해도 verdict를 `not-demonstrated`로 낮추며 positive claim을 금지한다.

- [ ] **Step 10.4: raw authority에서 Korean report와 locale-parity docs를 작성한다**

`docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md`는 다음 순서를 사용한다.

1. 목적과 비주장: allocation 검증이며 일반 throughput 우위 주장이 아님.
2. exact commit/tree/JAR SHA, JDK/JVM/GC/OS/CPU와 canonical argv.
3. codec × operation × payload × storage별 baseline/candidate 두 run, absolute B/op saved와 payload 대비 saved ratio 표.
4. `accepted`, `not-demonstrated`, `ineligible`, `design-review-required` 판정 근거.
5. 세 payload 크기의 scaling 분석과 exact source symbol/hash code-inspection 결과.
6. fallback allocation과 decompression resource-bound 한계.
7. raw run directory와 `comparison.csv` 상대 링크.

`docs/benchmarks/README.md`와 `io/io/Benchmark.md`는 report를 링크한다. 양쪽 README는 최종 storage matrix와 Kotlin/Java caller-owned 예제, growth retry/application maximum, singleton thread-safety와 개별 buffer thread confinement을 같은 의미로 기록한다. `CHANGELOG.md`는 Step 3.2의
`issue-755-migration`/`issue-755-rollback` 문구를 그대로 보존하고 evidence-pending 표현만 실제 verdict로 교체하며, 검증된 eligible cell만 allocation evidence로 표현한다. lesson은 실제 unexpected dependency/backend 결과, 재실행 조건, 채택하지 않은 throughput claim을 기록한다. chart/image는 만들지 않는다.

- [ ] **Step 10.5: 전체 delivery verification을 fresh exact head에서 실행한다**

```bash
set -euo pipefail
run_dirs=()
while IFS= read -r run_dir; do run_dirs+=("$run_dir"); done < <(
  find docs/benchmarks/raw/issue-755 -mindepth 1 -maxdepth 1 -type d -name 'run-*' -print | sort
)
test "${#run_dirs[@]}" -eq 2
run_one="${run_dirs[0]}"
run_two="${run_dirs[1]}"
evidence_head="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["commit"])' "$run_one/metadata.json")"
repo-test-summary -- ./gradlew :bluetape4k-io:test --no-build-cache --rerun-tasks
./gradlew :bluetape4k-io:compileKotlin \
  :bluetape4k-io:compileTestKotlin \
  :bluetape4k-io:compileTestJava \
  :bluetape4k-io:testBenchmarkCompile \
  :bluetape4k-io:testBenchmarkJar \
  detekt detektMain detektTest \
  --no-build-cache --rerun-tasks
python3 -m unittest -v io/io/scripts/test_run_bytebuffer_compressor_evidence.py
python3 scripts/check-compressor-buffer-docs.py
bash scripts/check-compressor-buffer-abi.sh \
  --build-current --expected-head "$(git rev-parse HEAD)"
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py validate-delivery \
  --run "$run_one" --run "$run_two" \
  --comparison docs/benchmarks/raw/issue-755/comparison.csv \
  --expected-head "$evidence_head"
git diff --check
rg -n 'TBD|TODO|PLACEHOLDER|implement later|fill in later' \
  io/io/src io/io/scripts docs/benchmarks io/io/README.md io/io/README.ko.md \
  io/io/Benchmark.md CHANGELOG.md docs/lessons && exit 1 || true
```

예상 결과: 모든 unit/integration/benchmark compile/Detekt/ABI/Python/evidence validation PASS, placeholder와 whitespace error 0. ABI script의 dirty-path gate를 위해 production/test/script를 먼저 commit한 뒤 exact-head ABI를 실행한다. evidence validation은 canonical metadata의
`$evidence_head`를 계속 사용한다.

- [ ] **Step 10.6: evidence와 최종 문서를 Lore commit한다**

```bash
set -euo pipefail
git add docs/benchmarks/raw/issue-755 \
  docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md \
  docs/benchmarks/README.md io/io/Benchmark.md \
  io/io/README.md io/io/README.ko.md CHANGELOG.md \
  docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md
git commit -m 'Publish only reproducible caller-owned allocation evidence' \
  -m 'Constraint: Promotion requires two identical-provenance canonical runs and excludes fallback cells
Rejected: Generalize short-run throughput numbers | The issue only authorizes allocation evidence with a regression guard
Confidence: high
Scope-risk: moderate
Directive: Regenerate both canonical runs after any source, dependency, benchmark, JVM, or JAR drift
Tested: Two canonical JMH GC-profiler runs, fail-closed comparison, io tests, benchmark compile/JAR, Detekt, ABI, Python validator, locale parity
Not-tested: Other hardware and JVM configurations are outside this evidence claim'
```

commit 직후 ABI만 final `HEAD`로 다시 검증한다. `delivery_head`는 `$evidence_head`의 descendant여야 하고 다음 diff allowlist 밖 변경이 없어야 한다.

```bash
set -euo pipefail
run_dirs=()
while IFS= read -r run_dir; do run_dirs+=("$run_dir"); done < <(
  find docs/benchmarks/raw/issue-755 -mindepth 1 -maxdepth 1 -type d -name 'run-*' -print | sort
)
test "${#run_dirs[@]}" -eq 2
run_one="${run_dirs[0]}"
run_two="${run_dirs[1]}"
evidence_head="$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1]))["commit"])' "$run_one/metadata.json")"
delivery_head="$(git rev-parse HEAD)"
git merge-base --is-ancestor "$evidence_head" "$delivery_head"
test -z "$(git diff --name-only "$evidence_head..$delivery_head" -- \
  ':(exclude)docs/benchmarks/raw/issue-755/**' \
  ':(exclude)docs/benchmarks/2026-07-21-bytebuffer-compressor-allocation.md' \
  ':(exclude)docs/benchmarks/README.md' \
  ':(exclude)io/io/Benchmark.md' \
  ':(exclude)io/io/README.md' \
  ':(exclude)io/io/README.ko.md' \
  ':(exclude)CHANGELOG.md' \
  ':(exclude)docs/lessons/2026-07-21-issue-755-bytebuffer-compressor.md')"
bash scripts/check-compressor-buffer-abi.sh \
  --build-current --expected-head "$delivery_head"
python3 io/io/scripts/run-bytebuffer-compressor-evidence.py validate-delivery \
  --run "$run_one" --run "$run_two" \
  --comparison docs/benchmarks/raw/issue-755/comparison.csv \
  --expected-head "$evidence_head" \
  --delivery-head "$delivery_head"
```

validator는 final rebuild JAR SHA가 recorded JAR SHA와 같고 두 head 사이 변경이 위 allowlist에만 있음을 독립 재검증한다. code/benchmark/runner/dependency가 바뀐 descendant는 거부한다.

- [ ] **Step 10.7: six-perspective final review와 PR을 수렴한다**

성능, 안정성, 보안, 운영, developer/API, caller/user 여섯 read-only lane을 독립 실행한다. 각 lane은 P0/P1/P2/P3 또는 PASS, file:line, required fix와 rerun command를 반환한다. main integration이 중복을 제거하고 P0/P1을 모두 해결하며 affected lane과 Task 10.5를 재실행한다. P0=0/P1=0 전에는 push하지 않는다.

```bash
set -euo pipefail
git push -u origin perf/issue-755-bytebuffer-compressor-evidence
gh pr create \
  --repo bluetape4k/bluetape4k-projects \
  --base develop \
  --head perf/issue-755-bytebuffer-compressor-evidence \
  --title 'Publish caller-owned ByteBuffer compressor allocation evidence' \
  --assignee debop \
  --milestone '1.12.0' \
  --label enhancement --label performance --label infra/io \
  --body $'Closes #755\n\n## Summary\n- add cross-codec singleton concurrency and caller examples\n- add a thread-scoped JMH harness and fail-closed two-run evidence validator\n- publish exact-provenance allocation results without a general throughput claim\n\n## Verification\n- full bluetape4k-io tests and Detekt\n- compressor ABI exact-head check\n- benchmark compile/JAR and Python validator tests\n- two canonical JMH GC-profiler runs and delivery validation\n\n## DoD Status\n- [x] Caller-owned APIs and all four backend slices merged\n- [x] Heap/direct contracts, concurrency, docs, and allocation evidence verified'
gh pr view \
  --repo bluetape4k/bluetape4k-projects \
  --json number,url,headRefName,headRefOid,baseRefName,body,mergeStateStatus,statusCheckRollup,reviews
```

CI와 current review/thread가 exact head에서 수렴하면 PR number/head SHA를 보고하고 fresh merge 승인에서 멈춘다. 승인 전 merge, issue close, local/remote branch deletion은 하지 않는다.

**Step DoD:** final PR은 재현 가능한 두-run evidence와 전체 DoD를 포함해 merge-ready이고 CG-16 PENDING이다.

---

## Task 11: final merge 승인 후 documented checklist를 닫고 cleanup gate에서 멈춘다

**복잡도:** 중간 **Dependency:** final PR exact-head fresh merge approval **Write
scope:** approved GitHub merge와 local sync only; terminal blocked `.bluetape` receipt는 read-only **Pattern
skill:** `bluetape-workflow`, `finishing-a-development-branch`

> **Active WF-04A fallback:** Step 11.2의 original `completion-check`/`complete` commands는 audit
> record이며 실행하지 않는다. terminal blocked receipt를 그대로 보존하고 documented checklist와
> exact-head PR train을 final completion authority로 사용한다.

- [ ] **Step 11.1: approved final PR을 exact head로 merge하고 local develop을 sync한다**

Section 2.1의 post-approval checkpoint를 final PR number와 approved `expected_head`에 그대로 실행한다. merge state, merge OID와 `origin/develop` ancestry가 모두 증명되지 않으면 coordinator completion으로 넘어가지 않는다. Active fallback에서는 이 문장의 coordinator completion을 documented checklist closeout으로 읽는다.

- [ ] **Step 11.2: 남은 component evidence와 main verification을 닫는다**

Active WF-04A fallback에서는 Task 10.6의 committed evidence와 final PR merge 뒤 documented router/common/Type-A checklist를 actual artifact/PR evidence로 닫는다. coordinator component/lane을 추가하거나 terminal blocked run을 `complete`로 전이하지 않는다. 아래 original completion block은 실행 금지 audit record다.

```bash
set -euo pipefail
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape completion-check \
  --run-id 20260721T115110Z-8e06d9a0
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape complete \
  --run-id 20260721T115110Z-8e06d9a0 \
  --owner-file /Users/debop/work/bluetape4k/.bluetape/handles/issue755-plan-owner.json \
  --expected-head "$(python3 -c 'import json; print(json.load(open("/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json"))["last_checksum"])')" \
  --evidence /Users/debop/work/bluetape4k/.bluetape/inputs/issue755/completion-evidence.json
python3 /Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py \
  --state-root /Users/debop/work/bluetape4k/.bluetape verify \
  --run-id 20260721T115110Z-8e06d9a0
```

Original machine path에서 `completion-evidence.json`은 final merge OID, exact final PR/head, full verification summary와 evidence report path를 가진 fresh refs였다. 위 original block과 이 설명은 Active fallback에서 실행 authority가 없으며, run state `completed`를 요구하지 않는다.

Active fallback의 대체 proof는 `verify` 성공, `run.json.state == "blocked"`, Task 0에 고정한 terminal blocked checksum 불변, final PR merge OID/ancestry, 그리고 documented checklist의 전 항목 수렴이다.

```bash
set -euo pipefail
flow=/Users/debop/.codex/skills/bluetape-workflow/scripts/bluetape-flow.py
state_root=/Users/debop/work/bluetape4k/.bluetape
run_id=20260721T115110Z-8e06d9a0
run_file=/Users/debop/work/bluetape4k/.bluetape/runs/20260721T115110Z-8e06d9a0/run.json
python3 "$flow" --state-root "$state_root" verify --run-id "$run_id"
python3 -c 'import json,sys; d=json.load(open(sys.argv[1])); assert d["state"] == "blocked"; assert d["last_checksum"] == "b0458931bc1b8a3f2b173f04116b010c938d804f737aae081daeed292ef3cda1"' "$run_file"
```

- [ ] **Step 11.3: local sync 결과를 보고하고 cleanup 승인을 별도로 기다린다**

main `develop == origin/develop`, merged PR state, terminal blocked receipt checksum과 completed documented checklist를 보고한다. cleanup 전
`worktree-list`로 issue #755의 exact 6개 worktree (core, LZ4, Deflate, Snappy, Zstd, evidence)를 다시 열거한다. 이 6개 worktree와 대응 local/remote branches는 이 단계에서 삭제하지 않는다. 사용자가 worktree/branch cleanup 범위를 명시적으로 승인한 뒤에만 targeted cleanup을 수행한다.

**Step
DoD:** issue #755 final PR이 merged, local develop synced, Task 0의 terminal blocked receipt checksum이 그대로 보존되고 documented checklist/PR train이 완료됐으며 cleanup은 별도 승인 대기다.

---

## 12. 요구사항 추적성

| AC | 승인 명세 acceptance criterion                           | task      | file/test authority                              | final command                        |
|---:|----------------------------------------------------------|-----------|--------------------------------------------------|--------------------------------------|
|  1 | 두 caller-owned default method                           | 1–2       | `Compressor.kt`, Java/Kotlin contract            | targeted contract tests              |
|  2 | existing implementor/caller source·binary compatibility  | 1–3       | frozen fixture, `check-compressor-buffer-abi.sh` | exact-head ABI script                |
|  3 | 모든 compressor 공통 correctness                         | 1–8       | `allCompressors()` contract + integration        | full `:bluetape4k-io:test`           |
|  4 | LZ4/Deflate heap·direct no payload-sized intermediate    | 4–5, 9–10 | backend tests, source inspection, JMH            | targeted tests + `validate-delivery` |
|  5 | Snappy/Zstd matched-storage optimized, 나머지 fallback   | 6–7, 9–10 | dispatch tests, eligibility CSV                  | targeted tests + `validate-delivery` |
|  6 | source/target/read-only/overflow 상태 계약               | 1–8       | common/backend state/retry/sentinel tests        | full module test                     |
|  7 | old/new wire 양방향 복원                                 | 4–8       | codec existing/new wire tests                    | four backend targeted tests          |
|  8 | decompression limit와 corrupt-input 방어                 | 4–8       | backend compound-failure/concurrency tests       | backend + integration tests          |
|  9 | codec/storage eligibility와 two canonical runs           | 9–10      | raw JMH run 2개, comparison CSV, report          | Python tests + `validate-delivery`   |
| 10 | English/Korean README와 KDoc 정확성                      | 2–10      | KDoc, README markers, docs checker               | `check-compressor-buffer-docs.py`    |
| 11 | Kotlin/Java example와 bounded growth retry               | 8         | example compile/contract tests                   | three example/integration tests      |
| 12 | exact commit/tree/JAR/environment와 append-only evidence | 9–10      | run metadata, no-clobber/symlink tests           | Python tests + `validate-delivery`   |
| 13 | fallback target은 resource bound가 아님                  | 1–3, 10   | high-ratio tiny-target test, KDoc/README markers | contract test + docs checker         |

## 13. Repository hazard applicability

| Hazard/gate                                | 판정 | 계획 반영                                                       |
|--------------------------------------------|------|-----------------------------------------------------------------|
| broad backend matrix                       | 적용 | core + backend 4개 + adoption/evidence 1개로 분리, 순차 merge   |
| existing `io/io` benchmark target          | 적용 | 기존 kotlinx-benchmark/JMH task와 exact committed JAR 사용      |
| public JVM ABI                             | 적용 | frozen pre-change classfile/source manifest와 exact-head script |
| JNI/native crash boundary                  | 적용 | Snappy validation-first, Zstd/LZ4 bounded range seam            |
| concurrency/shared singleton               | 적용 | per-call state와 8-worker integration test                      |
| README locale/KDoc/CHANGELOG               | 적용 | 각 slice parity, final cross-check                              |
| module registration/catalog/workflow/Kover | N/A  | module 추가·이동, dependency/version, workflow, Kover 변경 없음 |
| Testcontainers/external service            | N/A  | 모든 codec/test/benchmark가 process-local                       |
| diagram/chart/image                        | N/A  | raw JSON/CSV와 Markdown 표가 numeric authority                  |
| publish/release/tag                        | N/A  | milestone implementation PR까지만, 배포 side effect 없음        |

## 14. 계획 검토 convergence 기록

2026-07-21 freeze candidate를 동일 문서 기준으로 독립 재검토했다.

| 관점          | P0 | P1 | P2 | P3 | 최종 판정 |
|---------------|---:|---:|---:|---:|-----------|
| performance   |  0 |  0 |  0 |  0 | PASS      |
| stability     |  0 |  0 |  0 |  0 | PASS      |
| security      |  0 |  0 |  0 |  0 | PASS      |
| operator      |  0 |  0 |  0 |  0 | PASS      |
| developer/API |  0 |  0 |  0 |  0 | PASS      |
| caller/user   |  0 |  0 |  0 |  0 | PASS      |

main integration은 Deflate operation-primary cleanup, LZ4/Snappy/Zstd native bounds, ABI RED order, thread-local JMH reset, actual payload scaling, immutable candidate 승인 binding, coordinator lifecycle, fail-fast shell gates와 atomic no-replace evidence publication을 반영했다. 최종 재검토 뒤 남은 P0–P3는 없다.

2026-07-30에는 현재 Task 5와 fresh workflow authority를 기준으로 계획을 다시
검토했다.

| 관점          | 최초 판정 | 수정·재검토 결과 |
|---------------|-----------|------------------|
| performance   | CLEAN     | CLEAN            |
| stability     | timeout 후 회수 | 좁은 재검토 CLEAN |
| security      | timeout 후 회수 | 좁은 재검토 CLEAN |
| operator      | P1 1건    | fresh run readiness authority로 수정 후 VERIFIED |
| developer/API | CLEAN     | CLEAN            |
| caller/user   | P2 2건    | capability/claim 분리와 한국어 retry 문구 수정 후 VERIFIED |

main integration은 실제 helper 이름 `writeToCallerBufferViews`, 승인된 현재 branch,
Task 5 PR command, 한국어 KDoc 및 docs validator 누락을 P1로 추가 확인해 수정했다.
현재 Task 5 계획의 최신 결과는 P0=0, P1=0이며 남은 P2/P3는 없다. Task 6 이후의
과거 snippet은 각 slice 시작 전 현재 source/API에 다시 정합해야 하며, 이번 Task 5의
실행 authority로 사용하지 않는다.

## 15. 구현 승인 handoff

이 plan commit 전에는 문서만 변경한다. 사용자 승인 후 Task 0에서 machine run을 approve/start하고 Task 1의 RED부터 실행한다. 기본 실행 방식은 `subagent-driven-development`이며 각 task마다 implementer와 reviewer write scope를 분리한다. 한 slice의 PR이 merge되고 local `develop` sync와 ancestry 검증이 끝나기 전에는 다음 branch/worktree를 만들지 않는다. 모든 merge는 exact PR/head, CI, current review/thread를 보고한 뒤 별도의 fresh 사용자 승인을 요구한다.
