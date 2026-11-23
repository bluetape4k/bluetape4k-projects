# 공통 Digest API 설계 통합 리뷰

## 검토 범위와 근거

- 이슈: [Projects #1917](https://github.com/bluetape4k/bluetape4k-projects/issues/1917)
- 대상: [공통 Digest API 설계](../specs/2026-11-23-shared-digester-design.md)
- 검토한 명세 SHA-256: `a8926875835e2b3744c721936e0fcebf83961013f10774d4c5dbe6b1b3ebbd39`
- 사용자 요청으로 기존 초안의 `DigestUtils` 이름을 `Digesters`로 바꿨다. 최종 API는 `Digesters.SHA256`과 `Digesters.kt`이며, 미배포 신규 API에 이전 이름 별칭을 두지 않는다. 기존 `TinkDigester`와 `TinkDigesters` 표면은 그대로 유지한다.
- 근거: 최신 명세, 이슈 #1917, `io/tink`의 기존 구현·테스트, `io/io/Benchmark.md`의 실제 benchmark task.
- 성능, 안정성, 보안, 운영, 개발자 API/JVM ABI, 사용자·호출자 관점의 독립 리뷰 6건과 통합 검토를 같은 명세 해시에 대해 수행했다. 리뷰어는 명세만 읽었고 파일을 수정하거나 무거운 명령을 실행하지 않았다.

## 최종 판정

**통과 — 구현 계획 단계로 진행 가능.** 최신 명세 해시 기준 P0=0, P1=0, P2=0, P3=0이다. 설계 경계, 실패 동작, 호환성, acceptance와 검증 task가 서로 일치한다.

| 관점 | 독립 리뷰 역할 | 판정 | 확인 범위 |
|---|---|---|---|
| 성능 | `code-reviewer` | 통과, P0/P1/P2/P3=0 | 네 측정 경로, 입력 크기, 비용 경계, 실제 `testBenchmark` 및 `testBenchmarkCompile` task |
| 안정성 | `verifier` | 통과, P0/P1/P2/P3=0 | 동시 독립 세션, reset/reuse, 예외, 모듈별 migration 기준 manifest와 롤백 |
| 보안 | `code-reviewer` | 통과, P0/P1/P2/P3=0 | digest와 인증 경계, Base64 검증, 비교, 민감 메모리, Tink adapter 호환성 |
| 운영·이전 | `verifier` | 통과, P0/P1/P2/P3=0 | Projects 우선 구현, composite substitution, dependencyInsight, baseline 및 롤백 증거, release 경계 |
| 개발자 API/JVM ABI | `code-reviewer` | 통과, P0/P1/P2/P3=0 | `Digesters` 이름·package·Java descriptor, 기존 `TinkDigester` API, 의존 방향, testability |
| 사용자·호출자 | `writer` | 통과, P0/P1/P2/P3=0 | `Digesters`와 `TinkDigesters` 구별, Kotlin/Java 예제, framing/output 보존, migration 안내 |
| 통합 | main session | 통과, P0/P1/P2/P3=0 | 절 간 계약, 이슈 범위, 근거와 명세-acceptance traceability |

## 초기 리뷰 지적과 반영

| 초기 지적 | 우선순위 | 명세 반영 |
|---|---:|---|
| benchmark 실행 task가 저장소의 실제 task와 달랐다. | P1 | 실행 명령을 `:bluetape4k-io:testBenchmark`로 고치고 `:bluetape4k-io:testBenchmarkCompile` 검증을 추가했다. 저장소 기준은 `io/io/Benchmark.md`의 task 목록과 대조했다. |
| 공유 `Digesters.SHA256`에서 동시 생성한 세션의 독립성을 직접 확인하는 acceptance가 없었다. | P2 | 동시 독립 세션으로 서로 다른 입력을 계산하고 각각 독립 one-shot 벡터와 비교하도록 acceptance를 보강했다. |
| Workshop rollback의 기준 상태와 작업 소유 경로 구분이 묵시적이었다. | P2 | migration 전에 HEAD, 전체 status와 모듈별 변경 경로 manifest를 기록하고, 작업 소유 경로만 롤백하도록 설계 및 acceptance를 명시했다. |

수정된 명세 해시를 기준으로 여섯 독립 관점을 다시 실행했다. 재검토에서 추가 지적은 나오지 않았다.

## 통합 판단과 남은 경계

`io/io`는 알고리즘 독립 `Digester` 계약, 세션, JDK 구현과 `Digesters.SHA256` 접근점을 소유한다. `io/tink`는 기존 `TinkDigester` public/JVM surface와 결과를 보존하는 adapter가 된다. 일반 unkeyed digest는 인증 수단이 아니며 실제 Tink MAC과 분리한다.

Workshop 이전은 Projects API 구현과 검증 다음에 수행한다. 원격 `2.2.0-SNAPSHOT` artifact가 없으므로 `--include-build`와 `dependencyInsight`로 로컬 Projects `:bluetape4k-io` 선택을 검증한다. 외부 CI와 published artifact 검증은 이 단계에서 주장하지 않는다. Publication, PR, merge, tag, release는 이 작업에 포함하지 않는다.

이 리뷰는 설계 문서만 대상으로 했다. 구현 테스트, Java ABI fixture 실행, benchmark, Workshop migration과 rollback 실행은 구현 단계에서 검증한다.

## Writer DoD

- `SPW-01`: 독자, 검토 목적, 언어, 이슈 #1917, 명세 경로와 정확한 SHA-256, 저장소 task 및 기존 구현 근거를 고정했다.
- `SPW-02`: 검토 범위, 여섯 독립 관점, severity, 지적 근거와 반영, 남은 승인 경계를 기록했다.
- `SPW-03`: 한국어 기술 문장을 읽고 API, 명령, 식별자, URL과 심각도 표기를 보존했다. 변경 명세의 용어 감사는 0건이다.
- `SPW-04`: 명세 해시, benchmark task, 초기 지적의 반영과 최신 acceptance를 대조했다. 구현 검증은 아직 수행하지 않았다고 구분했다.
- `SPW-05`: 최종 Markdown을 다시 읽어 제목, 표, 링크, 목록과 용어를 확인했다. `git diff --check`와 용어 감사가 통과했다.
