# 공용 Server 설계·계획 검토

## 범위와 근거

#1722/#1723 설계 및 계획, 공식 Testcontainers 2.0.5 소스, ConsulServer/ChromaDBServer, image-gate manifest와 Nightly 등록 계약을 검토했다.

| 관점 | 설계 | 계획 | 조치 |
|---|---|---|---|
| 성능 | P0/P1 없음 | P0/P1 없음 | 추가 polling 없이 공식 readiness와 Launcher 재사용 |
| 안정성 | P0/P1 없음 | 순차 실행 P1 해소 | 각 Server/infra 모듈을 별도 invocation --no-parallel |
| 보안 | P2 | P2/P3 | Docker binding과 인증 없는 테스트 전용 제약 문서화, secret export 없음 |
| 운영 | readiness/등록 P1 해소 | P2 | gRPC TCP 도달성, family53/release49/shard 분배와 Nightly 등록, image gate 실제 실행 |
| 개발자/API | playground P1 반박 | P2 | 실제 javap에서 setExposedPorts(List) 확인, getter 중복 방지, exact dependency alias |
| 호출자 | P2 | 순차 실행 P1 해소 | 명시적/공유 수명 예시와 기존 pagination/named-vector/UUID/오류 회귀 보존 |

독립 리뷰: server_inventory(성능·안정성·운영·호출자 개별 검토), server_docs(보안), openfga_impl(API). 메인 통합은 중복 지적을 합치고 현재 파일 및 실제 2.0.5 jar API로 확인했다. 실행 명령 재조회로 오래된 계획을 참조한 지적도 정정했다.

## 작성 검증

SPW-01~05 PASS: 한국어 엔지니어 대상, 이슈/현재 소스 근거, 설계→수용 기준→계획 연결, 기술 토큰 보존, 최종 Markdown 재조회. Korean terminology audit PASS. 코드 리뷰와 실행 결과는 구현 후 추가한다.

## 판정

설계·계획 P0=0/P1=0. 구현·로컬 테스트·코드 리뷰 완료. PR의 새 head CI와 병합은 PENDING.

## 구현 검증

| 대상 | 결과 | 근거 |
|---|---|---|
| 공통 PropertyExportingServer 계약 | 12 PASS | 신규 클래스 부재 ClassNotFoundException RED 후 구현 GREEN |
| OpenFgaServerTest | 6 PASS | HTTP/gRPC mapped 포트, 실제 store/model/tuple/allow·deny, 종료, 고정 포트, Launcher |
| QdrantServerTest | 6 PASS | 실제 gRPC collection/upsert/query/filter/delete, 종료, 고정 포트, Launcher |
| OpenFGA infra | 18 PASS | 공용 Launcher로 기존 pagination 포함 전체 테스트 |
| Qdrant infra | 15 PASS | 공용 Launcher로 named vector/UUID/dimension 오류 포함 전체 테스트 |
| 이미지 게이트 | 각 1/1 PASS | `--scope changed --changed-path <Server.kt> --require-selection --max-attempts 1`로 각각 순차 실행 |
| Python 계약 | 65 PASS | `python3 -m unittest discover -s scripts -p 'test*testcontainers*.py'` |
| detekt | 신규 진단 0 | 세 모듈 검사 완료. testcontainers 수정하지 않은 파일의 기존 46건은 범위 밖 |
| 카탈로그 | PASS | 중앙 sync-shared-versions.py --check --summary, 실제 Testcontainers 2.0.5 해석 |
| 문서·workflow | PASS | actionlint, README 이미지/속성 양 언어 계약, Korean terminology audit, diff check |

57개 Kotlin 테스트를 이번 변경에서 검증했다. 공용 Server 검증은 한 번에 한 클래스, infra 검증은 한 번에 한 모듈로 실행했으며 모든 Gradle 컨테이너 검사에 `--no-parallel --no-configuration-cache`를 사용했다. 기존 measured/Temporal 구현과 검증은 앞선 전달 기록을 따른다.

## 최종 독립 리뷰

code-reviewer의 공용 production/build/README 검토 후, 공용 테스트·등록과 infra 소비자 adoption을 의존 순서대로 검토했다. 각 범위의 성능/안정성/보안/운영/API/호출자 관점 모두 P0=0/P1=0/P2=0/P3=0이다. 메인 세션은 실제 실행 결과, 기존 detekt 진단과 신규 진단 구분, 문서 계약을 확인했다. LSP 도구가 없어 실제 Gradle 컴파일과 detekt를 진단 근거로 사용했다.

A-VER-01~07 및 KT-FIN-01~11: 적용 항목 PASS. Exposed·Spring·신규 Gradle 모듈 등록은 변경하지 않아 N/A. 공용 클래스·기존 infra SDK API 호환성을 보존했다. 핫패스·직접 동시성 알고리즘 변경이 없어 별도 benchmark/stress harness는 N/A이고 Launcher singleton 및 자원 종료를 실제 테스트했다. 정리는 신규 테스트 함수 추출과 직접 container fixture 제거로 제한했다.

## 지식 보존

공식 소스 조사 요약은 wiki `research/2026-09-08-openfga-qdrant-testcontainers.md`에 저장하고 `chore/flow-measured-research`의 `fb389c1`로 push했다. gno update/embed는 실행했으나 대표 검색은 결과가 없다. 현재 검색 컬렉션은 병합 전 worktree 문서를 노출하지 않으므로 검색 가능 상태로 보고하지 않는다.
