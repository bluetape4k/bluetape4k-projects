# clear와 이전 계산의 저장 순서를 보장한다

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1694

## 실패한 가정과 근거

Caffeine, Hazelcast, Redisson suspend/async 모두 clear 뒤 이전 값 10이 다시 저장됐다.

## 결정

계산은 잠금 밖에서 실행한다. 세대 확인과 저장, 세대 변경과 삭제를 동일한 순서 경계에 둔다. suspend는 Mutex, async는 완료 순서를 연결한 Future를 사용한다.

## 재발 방지

세대 번호 확인 직후 clear가 끼어드는 순서를 고려한다. 이전 호출 결과, backing cache 부재, 새 세대 값 보존을 별도로 검증한다. 보장 범위는 같은 memoizer 인스턴스이며 다중 JVM 무효화가 아니다.

## 검증

이 PR의 회귀 테스트에서 수정 전 동작 실패를 확인했다. 수정 후 테스트와 관련 모듈 검증 결과는 PR의 `DoD Status`에 기록한다.

## 리뷰에서 보완한 취소 계약

서버 연산의 순서를 지키려고 NonCancellable을 사용하면 원래 호출자의 취소가 자동으로 복구된다는 가정은 틀리다. 저장 중 취소 회귀 테스트에서 호출자가 값을 받은 것으로 확인됐다. 서버 연산 완료 후 ensureActive로 취소를 다시 전파하며 저장과 삭제 양쪽을 테스트한다. 원격 mutation 직렬화는 서로 다른 키의 쓰기 처리량을 제한하므로 성능 변경 시 순서 테스트와 별도 측정을 함께 수행한다.

## 테스트 fixture의 실패 시 종료

여러 테스트가 같은 mock 호출 이력을 공유하면 순서 검증이 오염된다. 수동 완료 Future를 NonCancellable 작업에 연결한 테스트는 assertion 실패 시에도 finally에서 Future를 완료해야 테스트 종료를 보장할 수 있다. 테스트별 mock과 명시적 timeout을 사용하고, success 경로 외의 정리 경로도 점검한다.
