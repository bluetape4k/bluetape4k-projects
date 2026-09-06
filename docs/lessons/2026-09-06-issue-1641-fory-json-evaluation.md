# #1641: codec 기능 검증과 공용 API 적합성을 분리한다

## 배경

Fory JSON 1.7.1은 Kotlin constructor default, nullability, value class,
generic type과 제한된 증분 decoder를 제공한다. 기능 목록만 보면 기존
`JsonSerializer` backend나 공용 NDJSON primitive로 바로 추가할 수 있어 보였다.

## 확인된 차이

- `jsonTypeRef<List<Account>>()`는 정확한 모델을 복원하지만
  `List::class.java`는 동적 `JsonObject`를 반환했다.
- sealed base는 명시적인 `@JsonSubTypes` schema가 필요했다.
- 증분 decoder는 한 stream을 소유하며 실패 뒤 재사용할 수 없는 terminal
  lifecycle을 갖는다.
- Fory와 Jackson은 `ByteArray`를 Base64로 쓰지만 Fastjson2 JSON text는 signed
  numeric array로 쓴다. cross-read도 비대칭이라 공통 wire 형식으로 볼 수 없다.
- 짧은 직렬화 diagnostic에서 latency, current-thread allocation과 process heap pool
  peak 증가량을 관찰했지만 parser, concurrency나 실제 payload는 포함하지 않았다.

## 결정

2.1.0에서는 production dependency와 public API를 추가하지 않는다. 독립 PoC와
평가 기록만 커밋하고 #1641을 평가 완료로 종결한다.

## 재발 방지 규칙

새 serializer를 공용 interface에 추가하기 전에 다음을 별도로 증명한다.

1. codec 자체가 대상 언어 모델을 처리하는가.
2. 공용 interface의 타입 토큰이 그 의미를 손실 없이 표현하는가.
3. wire representation 변경을 versioning과 fixture로 고정했는가.
4. streaming lifecycle과 caller ownership을 실제 consumer가 요구하는가.
5. latency, allocation, peak memory가 모두 관찰됐는지와 그 측정 한계를 분리했는가.

기능 검증 PASS는 공용 API 적합성 PASS를 뜻하지 않는다.

독립 리뷰는 최초 PoC가 nullable collection element와 실제 default-field 제거를
증명하지 못하고, backend 표현 비교와 allocation/peak memory 관찰도 부족하다는 P2
4건을 찾았다. 각 항목을 executable assertion과 제한된 JVM diagnostic으로 보강했다.
특히 Fastjson2의 numeric byte array와 Fory/Jackson의 Base64가 만드는 cross-read
비대칭을 크기 비교만으로 숨기지 않게 됐다.

## 검증

- `./gradlew -p docs/evidence/issue-1641/poc run --no-daemon`
- Kotlin model, nullable collection element, byte-split NDJSON, array,
  malformed/limit 항목 PASS
- backend 표현/cross-read와 latency/allocation/peak heap 관찰 결과는
  `docs/superpowers/research/2026-09-06-issue-1641-fory-json-evaluation.md`에 기록
- 독립 리뷰 최초 P0=0/P1=0/P2=4, 보완 후 재검토 대기
