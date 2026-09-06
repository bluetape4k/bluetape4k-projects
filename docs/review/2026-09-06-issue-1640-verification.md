# #1640 구현 수락 기준 검증

## 범위와 판정

기준은 승인 명세·구현 계획 및 root Jakarta, QueryDSL, core count에 대한
후속 `추가해` 승인이다. base `31ee966cf339f7b895284329c8fbd53638ab837c`,
HEAD `aed17cf76` 위의 현재 미커밋 변경을 검증했다.
상세 실행 근거는 [실행 기록](2026-09-06-issue-1640-execution-checkpoint.md)에 있다.

**Step 5: PASS — 로컬 구현 검증 범위.** 최종 리뷰·lesson·PR·CI는 후속
단계이며 전체 이슈 완료나 exact-head CI 통과 판정이 아니다.

| 기준 | 구현과 증거 | 상태 |
|---|---|---|
| AC-01 | 두 Spring Boot force 제거, 실제 graph, root v32 및 QueryDSL 5개 configuration의 3.2.0 선택, 대표 POM | 완료 |
| AC-02 | JpaExecutorContractTest의 Projects 기본 context: 실제 H2 repository 저장/조회 및 executor 제출 | 완료 |
| AC-03 | EmfDependentExecutorConfiguration 생성자의 실제 EMF 의존, 단일 caller bean, 기본 bean 부재 | 완료 |
| AC-04 | Projects 3개·caller 2개·Boot 3개에서 실제 실행 스레드 확인 | 완료 |
| AC-05 | Future 5초·context 단계 시간 검사·외부 실행 상한, 종료 후 EMF/Hikari 닫힘과 제출 거부, 음성 대조 | 완료 |
| AC-06 | Spring Boot core 250·Hibernate Lettuce 38·demo 7개 통과, core 라이브러리 1,679개 통과 | 완료 |
| AC-07 로컬 | compileTestKotlin·Detekt·diff 검사와 전역 test 종료코드 0; 새 두 테스트 파일의 정적 지적 0 | 완료 |
| AC-07 전달 | 최종 리뷰, 커밋된 exact head의 PR·CI·thread 확인 | 후속 단계 |

## verifier 체크

| 항목 | 근거 | 상태 |
|---|---|---|
| A-VER-01 | 위 AC 대응표와 승인된 추가 범위 | PASS |
| A-VER-02 | 계획의 구현 검증 후 작업 상태 표. 코드 커밋은 최종 검토 뒤로 이월, 전달 작업은 후속 단계로 명시 | PASS |
| A-VER-03 | 변경은 root·두 Spring Boot build·QueryDSL build·core 테스트·새 JPA fixture·작업 문서·CHANGELOG. 제품 구현·catalog ref·module 등록 변경 없음 | PASS |
| A-VER-04 | 새 제품 API 없음. root 발행 POM 변경은 CHANGELOG의 3.2.0 정렬·override 안내로 반영 | PASS |
| A-VER-05 | 실제 자원 종료와 제출 거부, 8개 context matrix, count의 빈 입력·batch 경계·혼합/전체/불일치 및 음성 대조 | PASS |
| A-VER-06 | 코드 변경 후 core 전체 및 전역 증분 test 성공. 전역 build 뒤의 테스트 전용 변경은 해당 compile/test/Detekt로 재검증 | PASS |
| A-VER-07 | 아래 미검증·기존 경고와 전달 보류 항목을 분리 | PASS |

## 한계와 후속 작업

- 전역 명령은 완료 task를 재사용한 증분 실행이다. XML 21,630개 사례 중
  제외 108개는 통과로 집계하지 않는다. 새 core/JPA 사례에는 제외가 없다.
- root의 Detekt ignoreFailures로 기존 지적이 남는다. 새 두 테스트 파일의
  지적 0과 저장소 전체 lint 무결함은 서로 다른 주장이다.
- configuration-cache 12건은 현재 검증에서 유지된 제한이다.
- Boot 4.1.0 구버전 순환 의존을 직접 재현하지 않았다. 버전 불일치 RED와
  assertion 음성 대조는 현재 계약의 검출 능력을 확인한 결과다.
- 명세의 bootstrap 키 표기는 계획과 코드에서 정확한
  `spring.data.jpa.repositories.bootstrap-mode=default`로 정정했다.
- 실제 병렬 worker 수·속도 향상·다른 CPU 아키텍처·Full Nightly·발행은
  이번 로컬 검증 결과로 주장하지 않는다.
- 최종 6개 관점 리뷰·lesson 커밋·wiki 보존/색인·PR·exact-head CI는 미완료다.

## 문서 검증

SPW-01–04는 유지보수자 대상 한국어 검증 문서로 승인·소스·실행 기록을
대조했다. KO-01–06은 수치·식별자·증거 한계·후속 상태를 보존하여 재독했다.
SPW-05는 완성 문서를 재독하여 확인했고 KO-07 용어 감사는 이 문서·계획·
실행 기록 3개 파일에서 findings=0으로 통과했다. SPW 5/5·KO 7/7이다.
CHANGELOG 전체 감사의 과거 문구 2건(`immutable event snapshot`,
`key-only snapshot`)은 변경 범위 밖의 기존 event 의미를 보존한 예외로
기록한다. 이번 Jakarta 안내에서는 지적이 없으며 일괄 치환하지 않았다.

## DoD Status

로컬 구현 검증 PASS. 전체 전달 상태 PENDING이며 최종 리뷰로 진행한다.
