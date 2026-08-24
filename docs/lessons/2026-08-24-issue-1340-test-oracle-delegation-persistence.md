# 이슈 #1340 테스트 oracle은 위임과 영속성을 분리해 증명한다 (2026-08-24)

관련 이슈: #1340 · Epic #1420 Slot 1
영향 module: `:bluetape4k-jdbc`, `:bluetape4k-cassandra`

## 맥락

JDBC delegation 테스트는 프록시가 모든 호출에 기본값을 반환하고, 예외를 넓게
무시해 실제 인자 전달이나 결과 반환이 끊겨도 green이 될 수 있었다. Cassandra
`SettableSupport` 테스트도 bound statement의 값만 읽어 실제 `session.execute`와
저장 결과를 검증하지 않았다.

## 결정

1. JDBC recording proxy는 호출 method와 exact arguments를 기록하고, 대표 setter와
   getter가 실제 delegated value와 column access를 남기는지 별도 assertion으로
   고정한다.
2. JDBC에서 지원하지 않는 Java default overload는 method 이름과 예상 예외 메시지를
   지정한 작은 helper로만 허용한다. 예외를 삼키는 broad fallback은 사용하지 않는다.
3. Cassandra는 name/index/`CqlIdentifier` 세 `setMap` overload를 각각 execute한 뒤
   별도 `SELECT`로 읽고, map key와 value type을 함께 확인한다. 테스트 간 상태는
   `TRUNCATE`로 격리한다.
4. no-op delegation 또는 no-op `setMap` 변이가 각각 exact-call/read-back assertion을
   실패시키는지 RED proof로 확인한다.

## 결과

두 테스트가 대표 JDBC 호출의 exact arguments와 전체 setter 호출 목록, Cassandra 저장
결과를 직접 관찰하므로 호출 누락과 bound statement 내부 상태만으로는 회귀를 숨길 수
없다. production API와 구현은 변경하지 않고 테스트 oracle만 강화했다.

## 검증

- `:bluetape4k-jdbc:test` 전체 163개 통과
- `:bluetape4k-cassandra:test` 전체 199개 통과
- targeted JDBC 5개, Cassandra 2개 통과
- no-op 변이에서 JDBC exact-call과 Cassandra persisted-map assertion 각각 실패
- `git diff --check` 통과

## 향후 지침

- delegation 테스트 double은 method, arguments, 중요한 반환값을 기록하고 검증한다.
  void 호출도 예상 method 목록과 대조하고, 예상하지 않은 반환값 호출은 즉시 실패시킨다.
- persistence 테스트는 bind 결과를 읽는 데서 멈추지 말고 실제 execute와 독립적인
  read-back을 수행한다. overload가 여러 개면 각 overload를 고유한 row로 검증한다.
- 의도적으로 지원하지 않는 API만 예외 종류와 메시지를 명시해 허용하고, 나머지
  예외는 테스트 실패로 남긴다.
