# 교훈 - 이슈 #852 Rule engine condition failure

## 배경

issue #852는 rule condition이 throw할 때 synchronous `DefaultRuleEngine`을 suspend engine과
맞췄다.

## 교훈

- rule failure policy는 condition evaluation과 action execution을 모두 다뤄야 한다.
  action exception만 catch하면 `skipOnFirstFailedRule`이 일관되지 않다.
- `check()`는 rule별 evaluation result를 만들어야 한다. condition exception 하나가 전체
  result map을 abort하지 말고 해당 rule에 `false`를 기록해야 한다.
- exception을 policy outcome으로 변환할 때 listener ordering이 중요하다. condition
  exception이 있어도 rule-set lifecycle은 완료되고 evaluation은 `false`로 보고되어야 한다.
- `CancellationException`은 ordinary rule failure가 아니다. sync/suspend engine은 넓은
  `Exception` handling 전에 이를 rethrow해야 한다.
- 같은 configuration semantic을 노출하는 sync/suspend engine은 behavior pair를 유지한다.

## 가드

rule engine failure policy를 수정할 때는 implementation을 바꾸기 전에 `fire()`와 `check()`
양쪽에 대한 sync/suspend parity test를 추가한다.
