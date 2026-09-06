# JPA·executor 회귀 검증에서 버전 정렬과 테스트 계약을 분리하기

## 문제와 결정

Issue #1640은 catalog의 Boot 4.1.1에서 JPA·executor 결합을 검증하는 작업이다.
기존 테스트 force가 4.0.3을 선택했고, 이를 제거한 실제 JPA fixture에서는
root Jakarta Persistence 3.1.0 관리에 따른 `FindOption` 누락이 드러났다.
사용자 승인 후 기존 catalog v32로 정렬했으며 QueryDSL의 직접 선언도
별도 승인으로 정렬했다. 임시 test force로 production 불일치를 숨기지 않았다.

## 재발 방지 기준

| 단계와 잘못된 가정 | 드러난 증거와 결정 | 다음 작업에서 먼저 확인할 것 |
|---|---|---|
| 설계: context 시간 assertion만으로 hang을 제한할 수 있다 | 시작/종료 후 assertion은 영구 대기를 중단하지 못한다. 외부 프로세스 상한을 추가했다. | Future·JUnit·프로세스 상한의 역할을 나누고 소유 process group만 종료한다. |
| TDD: Boot 버전 불일치 RED가 upstream 순환 의존 RED다 | 실제 RED는 4.0.3과 4.1.1 불일치다. 4.1.0 순환 의존 재현과 구분했다. | 구버전 재현·버전 graph·assertion 음성 대조의 주장을 각각 기록한다. |
| 계획: root alias 변경만으로 모든 소비자가 정렬된다 | QueryDSL implementation/kapt의 직접 v31 선언이 남았다. 5개 configuration에서 선택 버전을 확인했다. | compile/runtime/test/kapt와 대표 발행 POM을 함께 검사한다. |
| 검증: demo에도 library Detekt task가 있다 | 해당 task가 없어 명령이 실패했다. root의 예제 제외 정책을 확인하고 compile/test와 library Detekt를 분리했다. | 실행 전에 실제 task와 plugin 적용 조건을 확인한다. |
| 환경: manual checkout은 예전 위치를 따른다 | cache-core가 BLUETAPE4K_MANUAL_ROOT 누락으로 실패했다. 사용자 지적과 GNO·현행 CI·reader를 대조했다. | 중앙 docs/manual 아래 repo 전용 root까지 확인하고 환경변수만 해당 명령에 지정한다. |
| 테스트: 병렬 count는 매번 순차 count보다 빨라야 한다 | 2386ms < 1246ms 실패 후 단독 재실행은 통과했다. 승인 후 속도 조건을 정확한 count·빈 입력·batch 경계로 대체했다. | 기능 테스트에서 속도 우위를 요구하지 않는다. 성능 주장이 필요하면 별도 반복 측정 근거를 만든다. |
| 문서: test 작업이므로 CHANGELOG는 계속 N/A다 | 추가 승인으로 root 관리 버전과 발행 POM이 바뀌었다. Unreleased에 소비자 override 안내를 추가했다. | 범위가 바뀌면 처음 정한 문서 N/A도 다시 판단한다. |

## 결과와 증거 한계

- JPA fixture 8개와 Spring Boot 세 모듈 295개는 실패·오류·제외 없이 통과했다.
- core count 기대값을 1 증가시킨 음성 대조는 6개 입력에서 실패했고 복구했다.
  core 전체 1,679개는 실패·오류·제외 없이 통과했다.
- 전역 테스트는 30분 상한 종료 후 완료 task를 재사용한 후속 실행에서
  종료코드 0으로 완료됐다. 전체 clean 실행이나 Full Nightly는 아니다.
- 현재 일반 test XML 합계 21,630개 중 제외 108개를 통과로 집계하지 않는다.
  기존 Detekt 지적과 configuration-cache 경고도 별도 제한으로 유지한다.
- 독립 core 검토 무응답은 실패 기록과 inline fallback으로 구분했다.
  검토자 생성 성공만으로 독립 검토 완료를 주장하지 않는다.

자세한 명령·실패·복구는 [실행 기록](../review/2026-09-06-issue-1640-execution-checkpoint.md),
수락 기준은 [검증표](../review/2026-09-06-issue-1640-verification.md)에 있다.
최종 리뷰의 문서 영향 P2와 집계 표현 P3는 위 문서 재판정 및 증거 한계에
반영했다. 6개 관점은 독립 시도 실패 후 inline fallback으로 통합했다.

## 문서 DoD

한국어 유지보수 문서로 실패 가정·승인·소스·로그를 대조했다(SPW-01–04,
KO-01–06). 최종 재독·용어 감사 findings=0과 리뷰 반영을 확인했다.
SPW 5/5·KO 7/7이며 커밋 결과는 전달 기록에서 별도로 확인한다.
