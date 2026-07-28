# 이슈 #1038: Avro serializer ByteBuffer path

## 배경

Avro interface는 이미 호환 가능한 `ByteBuffer` default를 노출했지만, 그 default는 OCF
data를 읽거나 쓰기 전에 complete ByteArray를 통해 copy했다. Reflect, generic-record,
specific-record, list 구현은 schema, codec, sync-marker, framing, null/empty,
caller-state behavior를 바꾸지 않는 direct stream routing이 필요했다.

## 결정

- Caller target의 duplicate 위에 non-growing `ByteBufferOutputStream`을 두고 OCF data를 쓴 뒤, writer close가 성공한 뒤에만 caller position을 commit한다.
- Duplicate-backed `ByteBufferInputStream` 위의 `DataFileStream`으로 OCF data를 읽고, caller-visible source property를 모두 보존한다.
- Overflow는 fixed target stream에서 비롯된 경우에만 tag한다. Datum accessor 자체가 `BufferOverflowException`을 던질 수 있으며, 이는 false target-capacity signal이 아니라 일반 handled backend failure로 남는다.
- Primary cause chain에서 발견된 fatal error를 보존한다. Suppressed cleanup failure를 primary backend failure보다 승격하지 않는다.
- 새 handled-failure log는 metadata-only로 유지해 caller record가 render되지 않게 한다.

## 발견 / 실패

첫 overflow classifier는 전체 primary cause chain에서 모든 `BufferOverflowException`을
찾았다. 독립 Developer/API review는 Avro가 datum-writer failure를 wrap하므로 record
accessor가 full target buffer로 잘못 보고될 수 있음을 보여주었다. Stream-bound signal이
target capacity와 backend behavior를 분리했고 RED regression test가 그 구분을 잠갔다.

첫 SpecificRecord log는 legacy `graph=$graph` diagnostic을 복사했다. 이는 caller
`toString()`을 평가하고 record data를 노출할 수 있었다. Regression test는 실제
codec-close failure와 ERROR-level lazy message evaluation을 모두 강제해야 했다.
Logging helper가 message-render exception을 안전하게 대체하므로, throwing `toString()`
만으로는 충분하지 않았다.

ABI script도 dirty serializer path를 의도적으로 거부한다. 신뢰할 수 있는 순서는
implementation commit, clean-head ABI generation, evidence commit, 그리고 review-driven
source fix 이후마다 regeneration하는 것이다.

## 결과

네 Avro family는 이제 bounded buffer input과 fixed output에서 allocating ByteArray
sibling method를 우회한다. OCF data는 legacy method와 cross-read되고, configured codec은
authoritative하게 남으며, caller state는 보존되고, 실패한 call도 재사용 가능하며,
compatibility report는 review된 code에 묶인다.

## 검증

- The focused contract suite covers direct/heap/sliced/read-only input,
  exact-capacity output, overflow provenance, rollback, fatal identity, cleanup
  failure, retry, schema mismatch, codecs, null/empty lists, malformed input,
  sibling bypass, and caller-safe logging.
- The complete Avro module reports 221 passing tests.
- Legacy Java/Kotlin callers, implementation loading, JVM default dispatch,
  public symbols, and frozen fixtures pass the exact-head ABI gate.
- Root detekt is `NO-SOURCE`; Kotlin compilation, full tests, unsafe-pattern
  scanning, and `git diff --check` provide the available static proof.

## 향후 방지책

Qualifier 없는 nested exception에서 target capacity failure를 추론하지 말고, 그것을
만든 resource boundary를 표시한다. Handled failure를 설명하려고 serializer datum을
log하지 않는다. OCF 비교는 semantic하게 유지하고, allocation/throughput claim은 #1039가
반복 가능한 benchmark evidence를 제공할 때까지 미룬다.
