# measured 및 인프라 구현 검증

## 범위와 전달 상태

- 대상: projects #1712 전기, #1713 전송률, #1714 힘·토크, #1715 OpenFGA, #1716 Qdrant, #1717 Temporal.
- 분류: 신규 인프라 모듈은 Type A, measured API 확장은 Type B. 사용자의 구현 지시와 승인된 설계·계획을 따른다.
- 기준: projects `6dcfa0b504401155c7f035e5859bab5c5908ffbd`, 격리 브랜치 `feat/measured-infra-1712-1717`.
- 중앙 SDK 원본은 dependencies의 `feat/projects-infra-sdks` 브랜치에서 별도 커밋한다. 소비자 검증은 `-Pbluetape4kDependenciesCatalogPath=<dependencies worktree>/gradle/libs.versions.toml`을 사용한다.
- 이번 결과는 로컬 구현·검증이다. 중앙 카탈로그 원격 통합, 일반 checkout의 원격 카탈로그 갱신, PR/CI·병합·발행은 아직 수행하지 않았다. 이슈를 닫지 않는다.

## 변경과 검토

설계 검토는 [별도 기록](2026-09-08-measured-infra-design-review.md)에 있다. 구현에서는 기존 독립 에이전트가 Qdrant, Temporal, OpenFGA를 교차 검토했다. 새 검토 에이전트 생성은 세션 thread limit으로 실패하여 주 세션의 `inline fallback review`로 공통 빌드·문서·배포 계약을 보완했다. 이를 별도 독립 검증으로 표시하지 않는다.

| 관점 | 확인한 계약 | 결과 및 보완 |
| --- | --- | --- |
| 성능 | 한 페이지 Flow, 제한된 순차 배치, 완료된 Future만 get | Qdrant의 항목 수와 전체 protobuf 크기 상한을 검증. 장주기 cursor는 무제한 이력 대신 maxPages로 제한 |
| 안정성 | 정규화, 오류 전파, 취소 경쟁, 반복 수집 | ms→s와 g→kg 혼합 스케일 회귀, MultithreadingTester로 Qdrant 완료/취소 경쟁, Temporal replay 검증 |
| 보안 | scope 격리, 요청 옵션 보존, 로그 노출 | OpenFGA 요청 복사, store/model 명시, 로그에는 operation/status만 기록. payload·token·SDK 예외 원문 제외 |
| 운영 | 기한, 부분 성공, 자원 소유권 | SDK 기한 사용, 배치 성공분 롤백 없음, 주입 client 미종료, Temporal worker 유한 종료 및 명시적 force |
| 개발 API | 공식 타입 재사용, 정적 의미, 재사용 유틸리티 | 별도 SDK DSL을 만들지 않고 기존 assertions/future/IO 도구 재사용. Torque는 torqueAt, 표시 정책은 명시적 선택 |
| 호출자 | 취소 의미, 재수집, 호환성, 문서 | cold Flow, 로컬 대기 취소와 원격 취소 분리, README 양 언어의 사용 예제·제약 동기화 |

P0/P1 발견 없음. 다음 P2 항목을 보완했다.

1. Qdrant: 느린 수집자, pending page 취소, 배치의 최대 동시 요청 1, 입력 실패 시 잔여 배치 미전송, UUID와 named vector 및 차원 오류 테스트 추가.
2. Temporal: RPC timeout의 실제 설정 위치를 WorkflowServiceStubsOptions로 수정. List<String> query/result 타입, CanceledFailure 원인, 실제 worker 반복 종료 검증. 종료 대기 중 취소되면 shutdownNow로 승격하지 않는다.
3. OpenFGA: SDK가 반환하는 dependent CompletableFuture 취소는 원본 HTTP 요청·retry 중단을 보장하지 않는다. 로컬 대기 취소와 transport의 차이를 README에 명시하고 SDK 요청 기한을 안내한다.
4. measured: toHuman 후보 선택을 HumanFormatting으로 분리해 기존 표시 계약을 보존한다. Measure.equals의 기존 차원 비교 제약은 문서화하며 단위 엔진 전체 변경으로 확대하지 않는다.

## 검증 명령과 증거

모든 projects Gradle 명령에는 위 로컬 카탈로그 옵션과 `--no-daemon --max-workers=1`을 사용했다. 서버 테스트는 모듈별로 순차 실행했다.

| 검증 | 증거 | 결과 |
| --- | --- | --- |
| measured 회귀 | `:bluetape4k-measured:test`, JUnit XML | 기준 181개 → 204개, 실패·오류·제외 0 |
| Qdrant | `:bluetape4k-qdrant:test`, 실제 Qdrant 1.19.0 | 15개, 실패·오류·제외 0 |
| Temporal | `:bluetape4k-temporal:test`, 공식 테스트 환경과 replay | 16개, 실패·오류·제외 0 |
| OpenFGA | `:bluetape4k-openfga:test`, 실제 OpenFGA 1.8.2 | 18개, 실패·오류·제외 0 |
| 정적 검사 | 네 모듈 `detekt`, XML 진단 직접 확인 | 신규 모듈 3개 진단 0. measured 기존 진단 20개, 신규 진단 0 |
| 자동 등록 | `projects`, BOM 생성 POM | 신규 3개 Gradle project와 3개 BOM constraint 확인 |
| 모듈 발행 계약 | `generatePomFileForBluetape4kPublication`, `generateMetadataFileForBluetape4kPublication`, `validate_module_metadata.rb` | 3개 파일/6 variants/54 dependencies 통과, SDK POM 버전 확인 |
| 중앙 원본 | Python unittest 20개, Gradle check와 BOM POM 생성 | 통과. SDK OpenFGA 0.10.0, Qdrant 1.19.0, Temporal 1.38.0 |
| 생성 카탈로그 | sync-managed-catalog.py --write --check --summary | 188 aliases/8 sub-BOM. projects 82개. 기존 exposed-tenant-jdbc도 실제 원본에서 동기화 |
| CI 설정 | actionlint, validate-nightly-kover-isolation.rb | 통과. 신규 경로·테스트·Kover manifest를 같은 기존 job에 추가 |
| 문서 | README 두 언어, 한국어 용어 및 링크 검사 | 한국어 용어 6개 파일 진단 0, 변경된 파일 링크와 두 언어 확인 |

Nightly의 기존 matrix 검증기는 live run JSON을 요구하므로 로컬 YAML 검사와 혼동하지 않는다. 원격 실행 결과는 없다. 순차 테스트 loop에는 `set -euo pipefail`을 적용하여 앞선 모듈 실패를 숨기지 않는다.

POM 생성의 기존 configuration-cache 직렬화 경고를 확인했다. 후속 발행 메타데이터 검증은 `--no-configuration-cache`를 사용한다. 발행 코드의 범용 캐시 설계를 이번 모듈 추가 범위에서 바꾸지 않는다.

measured 진단의 Angle, EnergyPower, Motion, Temperature 파일은 HEAD와 해시가 동일하다. Units의 Measure 함수 수와 as/in 명명 진단은 기존 선언이다. 새 파일에는 진단이 없다. 전체 테스트는 **253개, 실패·오류·제외 0**이다. SDK nullable tuple 필드의 추가 테스트 컴파일 오류는 requireNotNull로 수정한 뒤 OpenFGA 18개를 다시 통과했다.

## 문서·시각 자료 및 알려진 범위

- root README의 기존 모듈 구조·개수 차트를 실제 디렉터리에서 재생성했다. 구조 그림은 루트 직계 infra 16개, 차트는 하위 Gradle 프로젝트를 포함하여 infra 17개로 집계 범위가 다르다.
- SVG/PNG 2개 쌍을 실제 렌더링하여 확인했다. 텍스트 정규화 위험 0, 구조 카드 11개, 의도된 연결선 0개, 카드 침범·교차·공유 선분 0개. geometry/endpoint 검사 통과. PNG 시각 검사도 통과했다.
- 구조 생성기의 사용하지 않는 marker/route 코드를 제거했다. 차트 전용 생성 옵션을 추가하고 전체 생성 경로도 현재 구조 생성기를 호출하게 맞췄다.
- 전체 기존 시각 자료 디렉터리에는 참조되지 않는 workflow 샘플과 benchmark 이미지가 있다. 이번에 바꾼 두 쌍의 링크·렌더는 검증했고, 무관한 기존 자산을 삭제하지 않았다.
- 성능 개선 수치나 benchmark 우위는 주장하지 않는다. 메모리·동시 요청 상한은 기능 테스트로 검증한다.
- Kotlin 검토: GlobalScope, production runBlocking, Thread.sleep, synchronized, !! 없음. 공개 API/KDoc, SDK 소유권, 취소 재전파, null 처리 및 프로젝트 테스트 도구 사용을 확인했다.
- [교훈과 재발 방지](../lessons/2026-09-08-measured-infra-boundaries.md)에 정규화·SDK 경계·카탈로그 전달 조건을 기록했다.

## DoD Status

- [x] 6개 이슈의 로컬 구현과 문서·CI 연결
- [x] 독립 교차 검토 및 주 세션 보완 검토, 알려진 P0/P1 없음
- [x] 마지막 리뷰 보완을 포함한 테스트·정적 검사·배포 메타데이터 검증
- [x] 변경 범위와 문서 최종 검사, 프로젝트 및 중앙 원본 커밋
- 원격 전달: PENDING — 중앙 카탈로그 통합과 소비자 고정, PR/CI는 별도 단계다.

로컬 구현·검증 최종 상태: **DONE**. 원격 통합·CI·발행 완료를 뜻하지 않는다.
