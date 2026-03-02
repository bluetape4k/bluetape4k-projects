# Kdoc Instruction

Bluetape4k 공개 API KDoc 작성 지침(압축판)입니다.
목표는 “짧고 정확한 계약 + 바로 이해되는 예제”입니다.

## 1) 적용 대상
- `public` API 전체
  - top-level function/property
  - extension function/property
  - class/interface/object/enum
  - public constructor/member/companion member

## 2) 작성 제외
- local 변수/지역 함수
- `private` 구현 디테일
- 자명한 override (`toString`, `equals`, `hashCode`)
- 의미 없는 `companion object` 설명(필요 시 factory 함수에 집중)

## 3) KDoc 기본 포맷(필수)
1. 첫 줄: 한국어 요약 1문장
2. `## 동작/계약`
3. Kotlin fenced 예제

```kotlin
/**
 * (요약 1문장)
 *
 * ## 동작/계약
 * - (실제 구현 기준 계약 2~4개)
 *
 * ```kotlin
 * // 최소 재현 예제 (3~8줄)
 * // result == ...
 * ```
 */
```

## 4) 동작/계약 작성 원칙
- 코드에 실제 존재하는 계약만 작성
- 필요 시만 작성:
  - 입력 경계(null/empty/blank/whitespace)
  - mutate vs allocate
  - 예외/사전조건(`require`/`check`/`assert` 근거)
  - 성능/할당/동시성(구현 근거가 있을 때만)
- Kotlin non-null 타입(`T`)에 대해 null 비허용 반복 설명 금지

## 5) 금지 사항
- 템플릿/상투 문구 금지
  - 예: “입력 검증과 예외 발생 조건은 구현 본문의 ...”
  - 예: “상태 변경 여부와 할당 특성은 함수별 구현 ...”
- 허위 예외/허위 성능 설명 금지
- placeholder 예제 금지
  - `TODO("입력 값")`, `[계산 결과]`, `[처리된 ...]`
- `@sample` 및 샘플 객체 추가 금지

## 6) 예제 규칙
- 반드시 ` ```kotlin ` 블록
- 3~8줄, 최소 재현
- 테스트 assertion 값을 반영한 결과 표현 권장
- 출력 표현은 다음 우선:
  - `// result == [...]`
  - `// value == true`
- `println`, `check`는 꼭 필요한 경우에만 사용

## 7) 타입별 요약 규칙
- Marker interface: 역할/호출 계약만 간결히
- Enum: enum 타입 설명 + 각 상수 의미 개별 설명
- Factory/static constructor: 목적, 인자 검증, 예외를 함수 단위로 명시

## 8) assert/contract 특이사항
- `assert` 기반 API:
  - `-ea` 필요 여부
  - 실패 시 `AssertionError`
- Kotlin contracts 사용 API:
  - 스마트 캐스트/계약 효과 1줄 명시

## 9) 리뷰 체크리스트
- 공개 API 누락 없는가?
- 불필요한 KDoc이 들어가 있지 않은가?
- 구현과 문서가 1:1로 맞는가?
- 예제가 함수 동작을 즉시 이해시키는가?
