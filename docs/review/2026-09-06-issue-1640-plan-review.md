# #1640 구현 계획 리뷰

## 범위와 근거

- 대상: [구현 계획](../superpowers/plans/2026-09-06-issue-1640-jpa-executor-contracts-plan.md).
- 기준: 사용자 승인 명세 `57d0df0f6d1c6c4a21b38b1e73f7ea53ccb0a673c4508d81697cdc6de2ba1825`, 코드 `f8c87c4d58b62cf6142aff1275784a440045c8dc`.
- 최초 계획 SHA256: `b8950a7bef78d81292add0df8db1eeff1804d472296b35d764b5b66b325d956d`.
- 수정 계획 SHA256: `fc24c3b68a67e3bfbcae5ad9a668a7d48b59282dfd89d6ba288589ca58954617`.
- 성능·안정성은 별도 native agent의 읽기 전용 리뷰다. 보안·운영·API·호출자는 각각 신규 agent 생성이 `agent thread limit reached`로 실패하여 주 담당자가 직접 검토했다.
- 코드 실행 전 계획의 실행 가능성을 검토했다. 테스트·빌드·CI 통과를 증명하는 리뷰가 아니다.

## 지적과 수정

| 심각도 | 관점 | 근거 | 처리 |
|---|---|---|---|
| P1 | 성능 | compile/detekt 실패 뒤 diff check가 성공하면 shell 종료코드가 0이 될 수 있음 | 작업 4 Gradle 명령에 `|| exit $?` 추가 |
| P2 | 성능 | 작업 3의 재실행 시간 제한이 암시적임 | fixture와 controller 각각 기존 600초 명령을 그대로 사용한다고 명시 |
| P2 | 안정성 | 시작 실패의 원인 사슬 추출 방법이 부족함 | 작업 2에 XML·로그의 예외 및 bean 경로 검색 명령 추가 |
| P2 | 안정성 | timeout 진단 저장 위치·자식 JVM 부재 확인 누락 | 단계별 /tmp 로그와 해당 process group 사후 조회 명시 |
| P2 | 직접 API 검토 | caller 동일성을 같은 bean 이름 재조회만으로 확인할 위험 | caller 설정의 ownedExecutor와 직접 동일성 비교 |
| P2 | 직접 운영 검토 | process group이 먼저 종료되면 종료 신호가 실패할 수 있음 | 각 신호의 ProcessLookupError 처리, 소유 그룹만 진단 |

## 관점별 결과

| 관점 | 결과와 한계 |
|---|---|
| 성능 | 독립 재검토에서 수정 SHA 일치와 382행 실패 코드 보존, 356행 재실행 제한을 확인했다. P0/P1/P2 잔여 없음 |
| 안정성 | 독립 리뷰 P0/P1 없음. 원인 추출·진단 경로 P2도 문서에 반영 |
| 보안 | 직접 검토 P0/P1 없음. 무작위 H2 전용 DB, 최소 scan, Redis 미기동, 공유 자원 미종료, 비밀을 포함할 수 있는 전체 프로세스 command 출력 제외 |
| 운영 | 직접 검토 P0/P1 없음. graph 불일치 진단, 제품 변경 시 중단, 실행 상태 보존, CI와 병합 경계 명시 |
| API | 직접 검토 P0/P1 없음. Boot 4.1.1 JAR의 자동 구성 클래스와 프로젝트 assertion API 확인, core 연결은 test-only, 생산 API 불변 |
| 호출자 | 직접 검토 P0/P1 없음. caller 우선·property true/false·명시 등록 구분, 외부 executor 종료 계약을 추가하지 않음 |

## 명세 대응과 제외 범위

AC-01은 버전 RED와 4개 classpath graph, AC-02–04는 3+2+3 fixture,
AC-05는 자원 종료와 실행 제한, AC-06은 세 모듈 전체 검증,
AC-07은 진단·정적 검사·exact-head CI 및 리뷰로 연결했다.

모듈 추가·생산 API 변경·전역 catalog 변경·HTTP·coroutine API 변경은 없다.
따라서 모듈 등록, README locale 변경, Exposed·HTTP·suspend cancellation
확장은 해당하지 않는다. 실제 구현 범위가 바뀌면 이 판정을 재검토한다.

## 문서 DoD

- [x] SPW-01: 한국어 구현 계획·리뷰의 목적과 기준 SHA·출처·미검증 범위를 고정했다.
- [x] SPW-02: 파일·순서·완전한 fixture 예시·명령·위험·복구·AC 대응과 지적 처리 표를 확인했다.
- [x] SPW-03: 기술 식별자를 유지하고 한국어 기술 문체를 적용했다.
- [x] SPW-04: 지적과 수정 위치를 대조하고 문서 리뷰와 실제 실행 결과를 분리했다.
- [x] SPW-05: 최종 계획과 통합 리뷰를 재독했다. 명세 대응·검증 한계·명령의 실패 처리를 확인했다.
- [x] KO-01: SHA·버전·시간·명령·오류 이름의 의미를 유지했다.
- [x] KO-02: 추상적인 통과 주장 대신 관점별 근거와 한계를 적었다.
- [x] KO-03: 상투적·번역투 표현을 점검했다.
- [x] KO-04: executor·context·fixture 용어를 일관되게 사용했다.
- [x] KO-05: 홍보 문구와 유머를 넣지 않았다.
- [x] KO-06: 제목·표·목록·링크를 검토했다.
- [x] KO-07: 최종 두 문서 용어 감사 통과, findings=0.

## DoD Status

- 계획 리뷰: **PASS**, P0/P1 잔여 없음. 문서 SPW 5/5·KO 7/7 확인.
- 구현·검증·PR: **PENDING**, 이 리뷰로 실행 성공을 주장하지 않는다.
