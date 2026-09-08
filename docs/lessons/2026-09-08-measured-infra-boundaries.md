# 단위 정규화와 SDK 코루틴 경계

## 맥락

projects #1712–#1717에서 전기·전송률·힘·토크 단위와 OpenFGA·Qdrant·Temporal 모듈을 구현했다.
기존 measured 기준 단위는 시간 ms, 질량 g이므로 물리식에 그대로 대입하면 1000배 오차가 발생할 수 있다.

## 결정과 발견

- 전송률과 전기식은 s, 힘은 kg와 m/s²로 환산한 뒤 계산한다. 회귀 테스트에는 서로 다른 접두어를 섞는다.
- Torque는 명시적 `torqueAt(perpendicularArm)`으로 생성한다. Energy와 정적 의미를 구분하되 기존 `Measure.equals`의 차원 구분 제약은 변경하지 않는다.
- OpenFGA SDK의 `CheckRequest.trace`는 setter가 없는 생성자 속성이다. 생성된 DTO의 fluent 메서드를 추정하지 않고 실제 소스와 compile로 확인한다.
- Qdrant의 `PointId`와 `Filter`는 `Points`가 아니라 `Common`에 있다. 공식 요청을 보존하면 일관성·벡터·필터 옵션을 별도 DTO로 재구현할 필요가 없다.
- Future 대기를 취소해도 이미 반영된 원격 쓰기는 취소되지 않는다. Temporal의 로컬 결과 대기 취소와 원격 workflow cancel은 별도 API 계약이다.
- Temporal 테스트 환경에서 callback으로 Future를 기다리는 경로는 blocking get의 자동 시간 진행에 의존할 수 없다. `environment.sleep`으로 workflow timer를 명시적으로 진행한다.
- 코루틴 스택 복원은 예외 객체를 복제할 수 있다. 타입·메시지·원인 의미를 검사하며 무조건 객체 동일성으로 오류 전파를 판단하지 않는다.
- 중앙 카탈로그 aliases와 BOM constraints는 별개다. 양쪽을 변경하고 생성 POM의 실제 버전과 새 모듈 aliases를 확인해야 한다.
- PR 전달에서 settings의 catalog ref만 갱신하면 CI 환경변수가 이전 SHA를 덮어쓴다. settings와 CI checkout ref를 함께 확인한다. 신규 모듈을 Nightly manifest에 추가할 때는 `.github/scripts/test-aggregate-kover-coverage.py`의 기대 목록도 갱신하고 전체 계약 테스트를 실행한다. 첫 원격 CI에서 두 누락이 드러났으며, 모듈 단위 테스트 통과로 CI 등록 검증을 대신할 수 없다.

## 재발 방지와 검증

| 원인 | 방지 위치 | 검증 | 실패 시 조치 |
| --- | --- | --- | --- |
| g/ms 기준 혼동 | measured 연산 확장 | ElectricalTest, DataRateTest, ForceTorqueTest의 혼합 스케일 | 환산 기준을 고치고 전체 measured 회귀 실행 |
| SDK API 추정 | SDK 소스 및 모듈 테스트 | compileTestKotlin과 실제 서버 테스트 | 소스 시그니처를 다시 확인하고 원본 오류 보존 |
| 취소와 원격 상태 혼동 | Future bridge, Temporal 외부 client 확장 | pending Future 취소, timeout, 취소 후 workflow 계속 실행 | 자동 remote cancel을 제거하고 소유권 문서 동기화 |
| 페이징·배치 무제한 적재 | scroll/read Flow와 upsertBatches | 느린 소비자, take, 페이지 상한, 요청 바이트 상한 | 추가 요청·메모리 누적이 없는 경계로 복구 |
| 카탈로그만 등록 | dependencies BOM과 생성 카탈로그 | checksum, 188 aliases, 생성 POM SDK 좌표 | 중앙 producer 검증 전 downstream 전달 중단 |

실행 증거와 남은 전달 제약은 [구현 검증 기록](../review/2026-09-08-measured-infra-implementation-review.md)에 모은다.
공통 운영 규칙 자체를 바꾸는 작업은 이번 범위에 포함하지 않았다. 위 회귀 테스트와 모듈 README가 구체적인 방지 장치다.
