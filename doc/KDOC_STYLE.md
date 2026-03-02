# KDOC_STYLE

Bluetape4k Projects 전반의 Kotlin 공개 API 문서(KDoc) 작성 규칙입니다.  
목표는 IDE 툴팁/문서 사이트에서 **짧고 정확한 “계약(behavior/contract)” + 복붙 가능한 최소 예제**를 제공하는 것입니다.

---

## 1. 적용 범위

### 1.1 필수 적용

- `public` (또는 공개적으로 노출되는) 모든 API
    - top-level function / property
    - extension function / property
    - public class / interface / enum / object
    - public constructor / public member / public companion member

### 1.2 선택 적용

- `internal`이라도 사실상 외부에서 널리 쓰는 유틸(예: cross-module internal)  
  → 팀 합의 후 적용 가능

---

## 2. KDoc 기본 포맷(필수)

모든 공개 API KDoc은 아래 3단 구성으로 작성합니다.

1) **요약 1문장 (첫 줄)**
2) **## 동작/계약** 섹션 (2~4개 bullet)
3) **```kotlin 예제```** (3~8줄)

### 2.1 템플릿

```kotlin
/**
 * (요약 1문장)
 *
 * ## 동작/계약
 * - (핵심 규칙 1: null/empty/blank 등)
 * - (핵심 규칙 2: mutate/allocate, thread-safety 등)
 * - (필요 시: 예외, 성능/할당, 경계조건)
 *
 * 
 * // 3~8줄의 최소 재현 예제
 * 
 */
```

---

## 3. 금지 규칙

### 3.1 `@sample` 금지

- `@sample` 태그를 사용하지 않습니다.
- 예제용 `*Samples` object를 프로덕션 코드에 추가하지 않습니다.

> 이유: 문서 때문에 런타임 코드/탐색 노이즈가 증가하는 것을 피하고, 예제는 짧은 코드펜스로 충분합니다.

### 3.2 문서 예제 과대화 금지

- 예제는 “대표 사용법 1개”만.
- 테스트/설정/컨테이너/환경 세팅 등 긴 준비 코드는 문서에서 제외합니다.

---

## 4. 용어/표현 통일

문장은 한국어로 작성하되, 아래 용어는 기술 키워드로 영어를 그대로 사용합니다.

- **empty**: 길이 0 (`""`, `emptyList()` 등)
- **blank**: 공백만 포함(문자열 기준 `isBlank`)
- **whitespace**: 공백/탭/개행 등 공백 문자 전반
- **mutate**: 수신 객체를 직접 변경
- **allocate**: 새 객체/배열/컬렉션 생성

권장 표현:

- “null 또는 empty이면 …”
- “수신 리스트를 mutate 합니다.”
- “조건에 따라 allocate 없이 this를 반환합니다.”

---

## 5. `## 동작/계약` 섹션 체크리스트

대상 API 특성에 따라 아래를 반영합니다(해당되는 것만).

### 5.1 null/문자열 규칙

- null 허용 여부
- empty/blank/whitespace 처리 기준(정확히)

### 5.2 mutate/allocate 규칙

- receiver를 변경하는지(mutate)
- 새 값을 만들어 반환하는지(allocate)
- `this`를 그대로 반환하는지(할당 없음) 등

### 5.3 예외/계약

- 어떤 입력에서 어떤 예외가 나는지
- 사전조건(예: 인덱스 범위, thread ownership, lock 보유 등)

### 5.4 성능/할당(중요한 경우만)

- O(1)/O(n) 같은 복잡도
- 배열/리스트 복사 발생 여부
- ByteArray/String 변환의 인코딩/복사 여부 등

### 5.5 동시성/블로킹(중요한 경우만)

- thread-safe 여부
- blocking 여부(IO, sleep/wait, lock 등)
- 코루틴 컨텍스트/취소 전파/컨텍스트 전파 여부(해당 시)

---

## 6. 예제 코드(` ```kotlin `) 규칙

- 반드시 ` ```kotlin ` fenced block 사용
- 3~8줄 유지
- “최소 입력 → 결과 확인(check/println)” 패턴 권장
- 불필요한 변수/임시 값 남발 금지

좋은 예:

```kotlin
val source = mapOf("retry" to "3")
val retry = source.int("retry")
check(retry == 3)
```

나쁜 예(너무 김):

```kotlin
val container = startContainer()
val client = createClient(container)
val ctx = buildContext(...)
...
```

---

## 7. Assertion/Require 계열 규칙

### 7.1 JVM `assert` 기반(예: AssertSupport)

- 계약에 반드시 포함:
    - "`assert(...)` 기반이며 JVM assertions 활성화(-ea)가 필요"
    - "비활성화 시 검증이 수행되지 않을 수 있음"
    - "실패 시 기본적으로 `AssertionError`"

권장 문구(짧게):

- “`assert` 기반이므로 `-ea`가 필요합니다.”
- “실패 시 `AssertionError`가 발생합니다.”

### 7.2 Kotlin contracts 사용

- contracts가 있는 API는 계약에 1줄 포함:
    - “Kotlin contracts로 스마트 캐스트 힌트를 제공합니다.”

---

## 8. Number/Range 규칙

- 비교가 내부적으로 `toDouble()` 기반이라면 계약에 1줄 포함:
    - “내부적으로 `toDouble()`로 비교하므로 NaN/Infinity에 주의하세요.”
- 범위는 닫힌/열린 범위를 정확히 구분:
    - 닫힌 범위: `start..endInclusive`
    - 열린 범위: `start..<endExclusive`

---

## 9. Collections/Arrays 규칙

- 가장 먼저 mutate/allocate를 명시
- 앞 삽입/앞 삭제는 보통 O(n) (요소 이동), 의미가 있으면 언급
- 배열 비교/해시는 “내용 기반(contentXxx)”인지 “참조 기반”인지 명시

---

## 10. java.time 규칙

- 기간/시간 범위 API는 inclusive/exclusive(포함/배타) 경계를 명시
- timezone/clock 의존성이 있으면 계약에 명시
- 파서/포맷터는:
    - 지원 형식
    - 무시되는 필드(예: year/month 무시) 등 “함정”을 반드시 명시

---

## 11. Deprecated 규칙

- deprecated KDoc에는 “대체 API”를 명확히 적습니다.
- `@Deprecated(replaceWith=...)`와 문서가 불일치하지 않게 유지합니다.

권장 표현:

- “대신 `xxx()`를 사용하세요.”

---

## 12. 리뷰 체크리스트(PR용)

- [ ] 첫 줄이 1문장 요약인가?
- [ ] `## 동작/계약`이 있고 bullet이 2~4개인가?
- [ ] 예제가 ` ```kotlin `이며 3~8줄인가?
- [ ] `@sample`이 없는가?
- [ ] mutate/allocate 여부가 정확한가?
- [ ] null/empty/blank/whitespace 규칙이 정확한가?
- [ ] 예외/경계조건이 중요한 경우 누락되지 않았나?
- [ ] Deprecated 대체 API가 실제와 일치하는가?
