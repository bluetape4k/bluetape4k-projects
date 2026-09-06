# #1640 최종 구현 검토

## 검토 대상과 출처

base `31ee966cf339f7b895284329c8fbd53638ab837c`, HEAD `aed17cf76` 위의
미커밋 구현과 새 `JpaExecutorContractTest.kt`를 함께 검토했다.
root → core → Spring Boot → QueryDSL 순서로 각 변경을 확인했다.
명세·계획의 추가 승인, [수락 기준 검증](2026-09-06-issue-1640-verification.md),
[실행 기록](2026-09-06-issue-1640-execution-checkpoint.md)을 기준으로 판단했다.

성능·안정성·보안의 독립 native 검토를 먼저 요청했으나 90초 이상 유효한
결과가 없었다. 후속 요청에도 지적을 받지 못했고 세 검토자를 중단했다.
각 native interrupt의 이전 상태는 running이며 실패 lane을 보존했다.
이 세 관점의 아래 결과는 **inline fallback review**다.
운영·API·호출자 검토도 별도 독립 시도에서 유효한 결과가 없어 중단했다.
이 세 관점 역시 inline fallback으로 수행했다. 독립 검토 완료나 특정 모델의
검토 보증을 주장하지 않는다.

## 관점별 근거

| 변경 단위 | 성능 | 안정성 | 보안 |
|---|---|---|---|
| root build:791 | hot path 구현 없음. 성능 향상 주장 없음 | v32는 승인된 기존 catalog alias. 대표 POM과 전역 test 확인 | 새 저장소·credential·실행 hook 없음. 외부 취약점 감사로 주장하지 않음 |
| core 테스트:47–55 | sleep 및 속도 대소 assertion 제거. worker 수나 가속 증거로 주장하지 않음 | 0·1·31·32·33·1000과 3개 predicate. 음성 대조 6개 실패 후 복구, 전체 1,679개 통과 | 고정된 로컬 정수 입력, 제품 처리 경로 변경 없음 |
| Spring Boot builds 및 JPA fixture:43–200 | 8개 고정 context, pool 1개. 새 stress·benchmark 없음 | 실제 EMF/Hikari/executor 종료, Future 5초, 원인 보존 및 suppressed 처리, 프로세스 외부 상한 | 무작위 이름의 메모리 H2, 고정 fixture 값, 좁은 entity/repository scan. 실제 credential이나 외부 입력 없음 |
| QueryDSL build:67,74 | 의존성 alias만 정렬 | implementation/kapt 및 4개 classpath 3.2.0. 44개 통과·기존 제외 1개 | 기존 catalog 경로 유지, 새 저장소·실행 코드 없음 |

각 셀은 해당 관점의 실제 변경 범위에 대한 주 세션 검토다. 새 제품
`src/main` 변경은 없으므로 제품 concurrency quick scan의 추가 대상도 없다.
전체 repository의 기존 코드나 외부 의존성을 보안·성능 인증한 결과가 아니다.

## 지적과 처리

| 중요도 | 위치 | 지적과 처리 |
|---|---|---|
| P2 | 계획 작업 5의 CHANGELOG N/A | root 발행 POM 변경 후 기존 N/A가 부적절했다. Unreleased에 3.2.0 정렬·소비자 override 안내를 추가해 해결했다. |
| P3 | 실행 결과 집계 | 전체 XML 합계와 이번에 실행한 테스트 수를 혼동할 수 있다. 증분 task·기존 제외 108개·custom task 집계 제외를 명시해 해결했다. |

### 운영·API·호출자 관점과 통합

| 변경 단위 | 운영 | 개발자/API | 호출자 |
|---|---|---|---|
| root build:791 | 실제 graph/POM 확인, 역방향 alias 복구 시 FindOption 누락 재발 위험 기록 | 새 제품 API 없음. 의존성 관리의 전역 영향은 전체 build/test로 확인 | CHANGELOG:13–18에 3.1.0 override 정렬 안내 |
| core 테스트:47–55 | 시간 부하에 따른 거짓 실패 제거, test 제외 추가 없음 | 기존 fastList·assertions·JUnit 사용, 제품 parCount 시그니처 그대로 | 정확한 count 계약만 고정. worker 수·속도 보장 추가 없음 |
| Spring Boot fixture:107–200 | 시작 실패의 cause 보존, 종료 실패 suppressed 연결, context 소유 자원만 검사 | 실제 구성과 bean 동일성·타입·back-off 확인. test-only core 연결이며 제품 의존 방향 그대로 | Projects 명시 등록과 Boot property 선택을 서로 다른 테스트로 구분. 호출자 executor 우선 계약 확인 |
| QueryDSL build:67,74 | 기존 force 블록은 범위 밖으로 보존, 실제 5개 graph 확인 | implementation/kapt의 동일 alias 사용, 새 버전 상수 없음 | 예제의 compile/runtime 불일치를 제거. 기존 insert 제외 1개를 명시 |

아래 수치는 각 변경 단위의 최종 **미해결 P0/P1/P2/P3**다.

| 변경 단위 | 성능 | 안정성 | 보안 | 운영 | API | 호출자 | 주 세션 통합 |
|---|---|---|---|---|---|---|---|
| root | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | PASS |
| core | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | PASS |
| Spring Boot | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | PASS |
| QueryDSL | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | 0/0/0/0 | PASS |

초기 P0/P1=0/0, P2/P3 문서 지적 1/1을 보완했고 최종 미해결 P0/P1=0/0이다.
제외 사례·기존 정적 지적·독립 검토 부재는 아래 검증 한계로 유지한다.

## 한계

기존 Detekt 지적과 configuration-cache 12건은 남아 있다.
Boot 4.1.0 upstream 순환 의존의 직접 RED, Full Nightly, 다른 CPU 아키텍처,
발행 및 exact-head CI는 이 로컬 검토의 증거가 아니다.
새로운 architecture 구현은 없고 전역 관리 버전 변경은 승인된 추가 범위다.

## 문서 DoD

유지보수자 대상 한국어 검토 문서로 소스 위치·시험 수치·검토 출처·심각도·
해결 상태를 대조했다(SPW-01–04, KO-01–06). 최종 통합 문서를 재독했고
용어 감사 findings=0으로 SPW-05·KO-07을 확인했다. SPW 5/5·KO 7/7이다.
검토 후 코드 변경은 없다.

## DoD Status

Step 6-R PASS — 6개 관점 inline fallback 및 주 세션 통합 완료, P0=0/P1=0.
전체 전달은 PENDING이다. lesson 커밋·PR·exact-head CI·새 병합 승인이 남아 있다.
