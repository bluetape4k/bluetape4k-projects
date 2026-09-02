# Ecosystem Actions gate 계약

Issue: #1605

## 목적

Testcontainers 검증을 없애지 않고 같은 source SHA에서 동일한 runtime suite를 여러
workflow가 반복하는 비용을 제거한다. publication은 이미 성공한 exact-head Nightly
증거를 검증하며 container를 다시 기동하지 않는다.

## workflow별 단일 책임

| workflow | 책임 | Testcontainers 원칙 |
|---|---|---|
| PR CI | static/unit 및 변경 영향 module 검증 | 변경된 module/family만 실행 |
| develop CI | merge된 변경의 빠른 회귀 검증 | change-impact 범위만 실행 |
| Full Nightly | 전체 backend/image 호환성 증거 생성 | exact SHA에서 전체 matrix를 한 번 실행 |
| SNAPSHOT | 반복 발행 가능한 publication | runtime suite 재실행 없이 source SHA 기록 |
| stable release | tag provenance와 publication | 동일 tag SHA의 성공한 Nightly attestation 재사용 |
| CodeQL | extractor가 compile을 관찰하는 보안 분석 | Gradle build cache를 사용하지 않음 |

stable release에서 image availability나 backend startup을 다시 검사하지 않는다. 해당
증거는 Nightly가 소유한다. release는 Nightly run의 workflow path, exact head,
terminal success, required matrix, `publish_eligible=true`를 fail-closed로 검증한다.

## 저장소 inventory

2026-09-02의 각 `origin/develop` 기준이다.

| 저장소 | PR/develop CI | Full Nightly | SNAPSHOT/stable release |
|---|---|---|---|
| Projects | path-filtered module tests와 일부 container tests | 전체 module과 현재 지원 image family gate | Nightly attestation 재사용, runtime gate 없음 |
| AWS | emulator별 module tests | AWS backend 전체 | runtime gate 없음 |
| Exposed | H2 중심 + 변경 영향 DB/cache module | JDBC/R2DBC/Redis/DB 전체 | runtime gate 없음 |
| Graph | path router가 선택한 backend tests만 실행 | backend와 image gate 전체 | Nightly attestation 재사용, runtime gate 없음 |
| Image | image/native/OCR 영향 module | 전체 image/native matrix | Nightly attestation 또는 runtime gate 없음 |
| Javers | Redis/Kafka/Exposed 영향 module | persistence backend 전체 | runtime gate 없음 |
| Leader | backend 영향 module | 전체 leader backend matrix | runtime gate 없음 |
| Text | tokenizer/search unit 중심 | 전체 module | runtime gate 없음 |

Graph의 workflow·문서-only PR 과다 실행은 Graph #602/PR #608에서 path routing과
fail-fast로 제거했다. Projects release의 중복 image gate는 Projects #1598/PR #1599에서
제거했고, Ignite 2 전용 image/arm64 gate는 Projects #1600/PR #1607에서 지원 코드와
함께 제거했다.

## 배포 후 ecosystem audit

2026-09-02의 최종 `origin/develop`을 다시 확인했다. Projects, AWS, Exposed,
Graph, Image, Javers, Leader, Text의 stable release 및 SNAPSHOT publication workflow는
Testcontainers runtime suite나 image startup gate를 새로 실행하지 않는다. release
workflow에 남은 `Testcontainers` 문자열은 contract 검증이나 catalog alias 확인이며
container 기동이 아니다.

PR/develop CI와 Full Nightly에 같은 backend 이름이 존재하는 것은 중복 배포 gate가
아니다. CI는 path-filtered 변경 영향 범위를 빠르게 확인하고, Nightly는 전체 backend
matrix와 image compatibility의 canonical 증거를 만든다. Text는 두 경로 모두 별도
container job이 없다. 따라서 배포 후 추가로 runtime 검증을 제거하면 안전 경계를
약화시키며, 현재 최소 경계는 다음과 같다.

- 변경 영향 검증: PR/develop CI에서 한 번
- 전체 호환성 검증: exact-head Full Nightly에서 한 번
- SNAPSHOT/stable publication: 위 증거를 재사용하고 container 0개
- 문서/workflow-only PR: Graph path router처럼 heavy/container 0개

## catalog 전환 계약

- checked-in `settings.gradle.kts`의 immutable Dependencies commit SHA가 유일한 기본값이다.
- `workflow_dispatch.catalogRef`는 명시적 진단/복구 override로만 사용한다.
- repository variable을 숨은 두 번째 기본값으로 사용하지 않는다.
- workflow는 외부 catalog를 읽기 전에 선택값이 40자리 또는 64자리 SHA인지 검사한다.
- final 2.0.0 catalog commit은
  `3c203aa9f8ba80685aac766c5fb8f24e23d0058e`다.

따라서 catalog file과 repository variable을 순서에 맞춰 두 번 갱신하는 절차가 없다.
consumer PR 하나가 checked-in SHA와 CI checkout ref를 함께 바꾼다.

## 측정 결과

GitHub Actions API의 run/job timestamp를 사용했다. runner 시간은 각 job duration의
합이며 billing 반올림과는 다를 수 있다.

| 사례 | 결론 | 경과 | job | runner 분 | container job |
|---|---:|---:|---:|---:|---:|
| Projects 중복 release run `33527449585` | cancelled | 68분 | 6 | 74분 | 3 |
| Projects attestation-only run `33537327623` | success | 8분 | 3 | 8분 | 0 |
| Graph 과다 PR run `33537634192` | failure | 6분 | 21 | 37분 | 9 |
| Graph path-routed PR run `33616285791` | success | 68초 | 18 | 약 1분 | 0 |

Projects stable release 경로는 job 6→3, container job 3→0, 관측 runner 시간
74→8분으로 줄었다. 첫 run은 68분에 취소됐으므로 절감치는 보수적인 하한이다.
Graph의 workflow-only PR은 heavy job 11개를 명시적으로 skip하고 container job을
9→0으로 줄였으며, 전체 경과 시간은 6분→68초로 줄었다.

## 유지할 안전 경계

- stable tag와 publication source SHA 검증을 약화하지 않는다.
- Nightly required matrix와 `publish_eligible=true`를 유지한다.
- skipped job을 success로 가장하지 않고 aggregate status가 허용된 skip인지 판정한다.
- product failure를 retry하지 않는다. 일시적 infrastructure failure만 제한적으로 재시도한다.
- 실제 backend 호환성은 Nightly에서 계속 검증한다.
