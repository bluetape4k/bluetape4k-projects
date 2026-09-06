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
- `ByteArray` 기본 표현은 1.7.1에서 Base64로 바뀌었고 1.7.0의 numeric-array
  입력을 호환 방식으로 읽지 않는다.
- 짧은 직렬화 측정은 유망했지만 allocation, parser, concurrency나 실제 payload를
  포함하지 않았다.

## 결정

2.1.0에서는 production dependency와 public API를 추가하지 않는다. 독립 PoC와
평가 기록만 커밋하고 #1641을 평가 완료로 종결한다.

## 재발 방지 규칙

새 serializer를 공용 interface에 추가하기 전에 다음을 별도로 증명한다.

1. codec 자체가 대상 언어 모델을 처리하는가.
2. 공용 interface의 타입 토큰이 그 의미를 손실 없이 표현하는가.
3. wire representation 변경을 versioning과 fixture로 고정했는가.
4. streaming lifecycle과 caller ownership을 실제 consumer가 요구하는가.
5. benchmark가 production 순위가 아니라 결정 범위에 맞는 항목을 측정했는가.

기능 검증 PASS는 공용 API 적합성 PASS를 뜻하지 않는다.

## 검증

- `./gradlew -p docs/evidence/issue-1641/poc run --no-daemon`
- Kotlin model, NDJSON, array, malformed/limit 항목 PASS
- 3회 제한 측정 결과와 표현 크기는
  `docs/superpowers/research/2026-09-06-issue-1641-fory-json-evaluation.md`에 기록
