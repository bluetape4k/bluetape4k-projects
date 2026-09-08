# measured·인프라 설계 검토

## 범위와 근거

2026-09-08 spec/plan, projects #1712–#1717, 현재 Units.kt 및 공식 SDK 소스를 검토했다. 성능·안정성·보안·운영·개발 API·호출자 관점의 독립 검토를 수행했다. 구현은 이후 단계이며 이 문서는 구현 완료 증거가 아니다.

## 판단과 수정

| 관점 | 발견 | 처리 |
| --- | --- | --- |
| 성능 | page/batch 상한 및 정량 테스트 누락 | pageSize/maxPages, batch 수/byte, maxInFlight=1, 취소 후 추가 요청=0 명시 |
| 성능 | IO 안의 suspend await가 스레드를 점유한다는 주장 | 부정확한 전제라 채택하지 않음. suspend 중 worker 미점유와 blocking 호출 경계를 명시 |
| 안정성 | timeout, partial success, Worker 종료 모호 | SDK timeout+withTimeout 구분, batch별 결과 Flow, Boolean 종료 결과/force 계약과 테스트 추가 |
| 보안 | 테넌트 인가 경계 불명확 | 범용 SDK 계층임을 명시. 인가는 호출자 책임, 요청 간 옵션 오염 방지 검증 |
| 보안·운영 | 민감한 값/예외 로그 및 rollback | 상태만 로깅, 원격 상태는 Git revert로 복원되지 않음, fixture finally 정리 |
| 개발 API | wildcard Measure.equals가 의미 타입을 구분하지 않음 | 기존 전역 제약. 신규 타입/표시와 구분하여 문서화하고 차원 엔진 변경은 범위 제외 유지 |
| 개발 API | cursor 장주기 무한 반복 | maxPages를 추가하고 상한 도달 시 오류 처리 |
| 호출자 | DataRate 포맷 이름/Force 역변환/nullable 모호 | DataRateFormat 3종, Force/Acceleration, nullable 신규 API 없음 확정 |

## 재검토와 제한

성능 재검토 agent 요청은 `agent thread limit reached`로 실패했다. 수정된 spec/plan에 대해 주 세션이 inline fallback review를 수행했다. 각 지적의 수정 문구와 테스트 대응을 다시 읽었으며 설계 범위의 P0=0/P1=0으로 정리했다. 기존 wildcard equals는 하위 호환성을 지키기 위해 이 변경에서 손대지 않는다. 구현 리뷰에서 새 단위 연산이 정적 타입 경계를 지키는지 확인한다.

## 문서 및 다음 게이트

SPW-01–05: 한국어 구현 협업 문서, 이슈·소스 근거, 대안/범위/실패 모드, 테스트 대응, 용어와 최종 Markdown을 검토했다. 기존 문서 용어 검사 2개 파일 통과. SDK 좌표는 Maven 저장소에서 확인했다. 다음 게이트는 RED 테스트, 구현, 모듈별 검증과 코드 리뷰다.
