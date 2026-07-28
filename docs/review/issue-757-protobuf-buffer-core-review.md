# Issue 757 Protobuf Buffer Core 검토

## 검토 기준

- Base: `784e74c182ae97a6f2b89991bdf068832ecdbb4d`
- 검토 구현 HEAD: `c70ad5e1f24bb1ace26a434fb2fe1f0065081635`
- 검토 구현 tree: `794a8ff3ab01e776a1c3e13cbd98260fd90ac107`
- 측정 HEAD: `22b155fbed160e95a259a7c6695620139bbedda8`
- 측정 tree: `7a68ec6eff87f4e35446fa5096655e9822141a9f`
- 범위: caller-owned `ByteBuffer` API, Protobuf serializer/Redisson 경로,
  allocation benchmark와 fail-closed evidence pipeline, README/CHANGELOG.
- 중단 조건: 독립 6관점 검토와 main integration 검토에서 `P0=0`,
  `P1=0`; 모든 P2/P3에 명시적인 처리 방침이 있고 검증 명령이 통과할 것.

## 최종 판정

최종 통합 판정은 **APPROVE**이다. 독립 검토 결과는 총 `P0=0`,
`P1=0`, `P2=3`이며 P2는 아래에 비차단 잔여 위험으로 기록했다.

| 관점 | 최종 판정 | 근거 |
|---|---|---|
| Performance P0=0 P1=0 | PASS, P2=0 | 두 clean exact-head run에서 Redisson contiguous와 serializer encode heap/direct가 같은 방향의 allocation 감소를 보였다. Decode 회귀와 compatibility control은 positive claim에서 제외됐다. |
| Stability/resource P0=0 P1=0 | PASS, P2=1 | 임시 `ByteBuf` ref-count 변경과 직접/unwrap/composite view escape는 fail-closed 처리한다. 임의 holder/비동기 전달까지 탐지한다는 trusted-fallback 계약 표현은 실제 검사 범위보다 넓다. |
| Security P0=0 P1=0 | PASS, P2=1 | class resolution은 allowlist와 `initialize=false`를 사용하고 `SecurityException`은 fallback으로 우회하지 않는다. Hash-bound rollback archive에는 과거 operator 경로/identity가 남는다. |
| Operator/Ops P0=0 P1=0 | PASS, P2=1 | manifest/promotion symlink와 non-object JSON은 fail-closed이다. 현재 canonical `validation.json`과 `run.log` 일부에는 실행 worktree/build 절대경로가 남는다. |
| Developer/API P0=0 P1=0 | PASS, P2=0 | 기존 constructor와 byte-array API를 유지하며 `ByteBuffer` overload만 추가했다. 수동 `javap -public` 비교에서 기존 JVM surface가 유지됐다. |
| Caller P0=0 P1=0 | PASS, P2=0 | read-only output은 Protobuf 작업 전에 거부되고 heap/direct/slice/read-only 상태와 실패 경계가 회귀 테스트로 고정됐다. EN/KO README와 CHANGELOG가 trusted fallback migration 제약을 함께 설명한다. |

## 해결된 주요 발견

- P1: fallback이 임시 입력의 direct view 또는 `CompositeByteBuf` component
  graph를 반환하면 release 후 invalid view가 남을 수 있었다. `b0ba912f2`에서
  component graph traversal과 ref-count 검사를 추가하고 회귀 테스트를 고정했다.
- P1: `packMessageTo`가 read-only output을 Protobuf packing 이후에 거부했다.
  `b0ba912f2`에서 어떤 Protobuf 작업보다 먼저 검증하도록 순서를 바꾸고
  no-dispatch 회귀 테스트를 추가했다.
- P1: committed manifest와 promotion path의 최종 symlink를 거부하지 않았다.
  `b0ba912f2`에서 모든 path component와 terminal file을 `lstat` 기반으로
  검증하고 symlink fixture를 추가했다.
- P2: trusted fallback의 synchronous/no-retain/no-thread/no-view 제약이 사용자
  문서에 부족했다. `b0ba912f2`에서 English/Korean README와 CHANGELOG에
  migration contract를 추가했다.
- P2: top-level JSON이 object가 아니면 traceback이 노출됐다. `b0ba912f2`에서
  bounded CLI error로 변환하고 fixture를 추가했다.
- P2: persisted JVM identity에 `user.name`, home, tmp 경로가 포함됐다.
  `b0ba912f2`에서 allowlist identity로 축소했다.
- Main integration 검토가 최종 environment/argv에 남은 local build path를
  발견했다. `22b155fb`에서 pinned JAR SHA token과 repo-relative JMH path로
  정규화하고 legacy evidence read compatibility를 유지했다. 이 수정 뒤 두
  canonical run과 comparison/report를 모두 새로 생성했다.

## Allocation evidence

- Pinned JAR SHA-256:
  `e8731752bf7fb3177f4069552f74a209d152b7df38980bd80bc4e09ab0797042`
- Run A: `run-20260718T210358.790209Z-d5bbb3c1`
- Run B: `run-20260718T210757.236799Z-cd971cfd`
- Redisson contiguous: `-28.22916174984595%`, `-28.2291619975462%`
- Serializer encode direct: `-30.733935166406052%`, `-30.733934590742862%`
- Serializer encode heap: `-31.422009043801285%`, `-31.422008172846454%`
- Serializer decode heap/direct: `removed_after_regression`, positive claim 제외.
- Composite/fallback compatibility cells: `ineligible`, positive claim 제외.
- Retained regression count: `0`.

이 결과는 JDK 21, macOS arm64, G1, 단일 payload와 기록된 JMH 설정의
`gc.alloc.rate.norm` evidence다. Zero-copy 또는 throughput 개선을 주장하지 않는다.

## P2/P3 처리 방침과 잔여 공백

1. Trusted fallback의 런타임 검사는 반환된 `ByteBuf`의 direct, `unwrap()`,
   `CompositeByteBuf` component graph만 추적한다. 임의 holder나 다른 thread에
   숨긴 참조는 reflection 없이 검출할 수 없다. 이 profile은 trusted-internal 전용이고
   문서가 synchronous/no-retain/no-thread를 요구하므로 P2로 수용한다. 향후 변경은
   KDoc 보장 범위를 실행 가능한 검사 범위와 더 정확히 맞춰야 한다.
2. Rollback archive의 과거 environment/argv/validation/log에는 operator identity와
   절대경로가 남는다. 해당 byte는 hash-bound regression provenance이며 직접
   redaction하면 bundle 무결성이 깨진다. 배포 차단은 아니지만 외부 공개 전에는
   privacy-safe rollback 재증명과 새 bundle generation으로 교체해야 한다.
3. 최종 canonical run의 `validation.json`과 `run.log`에는 실제 실행 경로가 일부
   남는다. 환경 identity와 argv는 이미 canonical token으로 정규화됐으며 현재
   validator와 manifest hash는 정상이다. 후속 hardening에서는 validator path를
   canonical ref/repo-relative schema로 바꾸고 알려진 JMH result path를 bounded
   normalization한 다음 evidence를 다시 수집해야 한다.
4. `lstat` 뒤 hash/open 사이에는 작은 TOCTOU window가 남는다. 로컬 trusted evidence
   workflow의 P3로 수용하며, 더 강한 경계가 필요하면 fd 기반 open/hash 계약을 별도
   설계한다.
5. 기존 published artifact를 기준으로 하는 자동 ABI baseline과 Java downstream
   compile gate는 없다. 이번 검토는 base-source 비교와 현재 artifact의 수동
   `javap -public` 검사로 대체했다.
6. JMH 수치는 환경과 payload에 민감하다. 다른 OS/JDK/GC/payload로 일반화하지 않는다.

## 검증 근거

- `./gradlew :bluetape4k-protobuf:test --no-configuration-cache`:
  `248` tests PASS.
- Performance/Stability reviewer의 no-build-cache 재검증:
  protobuf `248`, benchmark `4` tests PASS.
- Security/Ops reviewer의 targeted Kotlin security/Redisson 검증:
  `76` tests PASS.
- Developer/Caller reviewer의 강제 targeted 검증:
  `91` tests PASS.
- `./gradlew :protobuf-codec-benchmark:test
  :protobuf-codec-benchmark:benchmarkBenchmarkCompile
  :protobuf-codec-benchmark:benchmarkBenchmarkJar --no-configuration-cache`:
  benchmark `4` tests PASS.
- `python3 -m unittest
  benchmark/protobuf-codec-benchmark/scripts/test_validate_jmh.py
  benchmark/protobuf-codec-benchmark/scripts/test_run_evidence.py`:
  `100` tests PASS.
- `run-evidence.py validate-committed`: PASS.
- `run-evidence.py validate-report`: PASS.
- `./gradlew :bluetape4k-protobuf:build --no-configuration-cache`: PASS.
- `:bluetape4k-protobuf:detekt` task는 없으며 root `detekt`는 `NO-SOURCE`였다.
- `git diff --check origin/develop...HEAD`: PASS.
- Scope checker: `66` paths, approved Gradle declarations `3`, unexpected path/line `0`.
- Exact reviewed worktree: clean.

## Main integration review

Main integration은 독립 결과를 exact canonical worktree에 다시 대조했다. Reviewer가
main checkout `develop`을 feature HEAD로 오인한 경보는 절대경로 worktree와 reflog를
교차 확인해 경로 혼동으로 판정했으며 feature worktree에는 checkout/reset/edit가
없었다. Production/benchmark source는 측정 HEAD 이후 변경되지 않았고, final evidence
commit은 measurement provenance와 report/index만 고정한다.

최종 결론은 `P0=0`, `P1=0`이다. 위 P2/P3는 claim, privacy, automation 범위의
명시적 잔여 항목이며 현재 core delivery의 correctness/security/resource gate를
차단하지 않는다. Remote CI, GitHub review threads, current-head human approval은 PR의
외부 gate로 남는다.
