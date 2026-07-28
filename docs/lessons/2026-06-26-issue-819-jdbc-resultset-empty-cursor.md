# 교훈: JDBC ResultSet empty cursor 계약 (2026-06-26)

**이슈**: #819
**모듈**: `:bluetape4k-jdbc`

## L1: JDBC cursor predicate는 consuming API다

### 문제

`ResultSet.isEmpty()`와 `ResultSet.isNotEmpty()`는 단순 predicate처럼 보였지만
둘 다 `next()`를 호출해 JDBC cursor를 전진시켰다. 호출자가 `isNotEmpty()`를
확인한 뒤 같은 `ResultSet`에 `toList`를 사용하면 첫 row를 놓쳤다.

### 교훈

`ResultSet.next()`를 호출하는 helper는 이름, KDoc, 테스트, README 예시에서
cursor 이동을 명시해야 한다. Forward-only cursor를 재사용 가능한 collection처럼
다루면 안 된다.

### 향후 방지책

ResultSet helper를 review할 때는 반환된 boolean이나 mapped value뿐 아니라 helper
호출 뒤 cursor 위치를 검사하는 테스트를 추가한다.
