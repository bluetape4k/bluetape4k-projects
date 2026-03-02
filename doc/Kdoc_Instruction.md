# KDoc 작성 지침

Bluetape4k 공개 API KDoc 작성 지침(압축판)입니다.
목표: **짧고 정확한 계약 + 바로 이해되는 예제**

---

## 1. 적용 대상

- `public` API 전체
  - top-level function/property
  - extension function/property
  - class/interface/object/enum
  - public constructor/member/companion member

## 2. 작성 제외

- local 변수/지역 함수
- `private` 구현 세부
- 자명한 override (`toString`, `equals`, `hashCode`)
- 의미 없는 `companion object` (factory 함수는 개별 문서화)

## 3. 기본 포맷 (필수)

1. 첫 줄: 한국어 요약 1문장
2. `## 동작/계약` 섹션
3. `kotlin` fenced 예제

```kotlin
/**
 * (요약 1문장)
 *
 * ## 동작/계약
 * - (실제 구현 기준 계약 2~4개)
 *
 * ```kotlin
 * // 최소 재현 예제 (3~8줄)
 * val result = ...
 * // result == ...
 * ```
 *
 * @param name 파라미터 의미가 이름만으로 불충분할 때만 작성
 * @return 반환 타입 이름만으로 설명이 부족할 때만 작성
 * @throws IllegalArgumentException 명시적으로 발생 가능한 예외
 */
```

## 4. 동작/계약 작성 원칙

- **코드에 실제 존재하는 계약만** 작성
- 다음 항목은 해당할 때만 작성:
  - 입력 경계 (null/empty/blank 허용 여부)
  - 상태 변이(mutate) vs 새 객체 반환(allocate)
  - 예외/사전조건 (`require`/`check`/`assert` 근거)
  - 성능·동시성 특성 (구현 근거가 있을 때만)
  - `assert` 기반 API: `-ea` 필요 여부, 실패 시 `AssertionError`
  - Kotlin contracts 사용 API: 스마트 캐스트 효과 1줄
- Kotlin non-null 타입 `T`에 대해 null 비허용을 반복 설명하지 않는다

## 5. KDoc 태그 사용 기준

| 태그 | 사용 기준 |
|------|----------|
| `@param` | 파라미터 의미가 이름만으로 불충분할 때 |
| `@return` | 반환 타입만으로 설명이 불충분할 때 |
| `@throws` | 명시적으로 선언하거나 문서화할 예외가 있을 때 |
| `@property` | 생성자 파라미터로 선언된 프로퍼티 문서화 시 |
| `[TypeName]` | 프로젝트 내 커스텀 타입·함수 인라인 참조 시 (기본 타입은 불필요) |

- `@since`, `@author` 등은 사용하지 않는다

## 6. 금지 사항

- 템플릿/상투 문구
  - `"입력 검증과 예외 발생 조건은 구현 본문의 ..."`
  - `"상태 변경 여부와 할당 특성은 함수별 구현 ..."`
- 구현에 없는 허위 예외·성능 설명
- placeholder 예제: `TODO("입력 값")`, `[계산 결과]`, `[처리된 ...]`
- `@sample` 및 샘플 객체 추가

## 7. 예제 규칙

- 반드시 ` ```kotlin ` 블록 사용
- 3~8줄, 최소 재현
- 테스트 assertion 값을 반영한 결과 표현 권장
- 출력 표현 우선순위:
  1. `// result == [구체적 값]`
  2. `// value == true`
- `println`, `check`는 꼭 필요한 경우에만
- 기존 KDoc에 외부 링크(URL, 레퍼런스 링크)가 있으면 보존

## 8. 타입별 요약 규칙

- **Marker interface**: 역할·호출 계약만 간결히
- **Enum**: 타입 설명 + 각 상수 의미 개별 설명
- **Factory / companion 생성자**: 목적, 인자 검증, 예외를 함수 단위로 명시
- **data class 프로퍼티**: `@property` 태그 또는 생성자 `@param`으로 문서화

## 9. 리뷰 체크리스트

- 공개 API 누락 없는가?
- 불필요한 KDoc이 들어가 있지 않은가?
- 구현과 문서가 1:1로 맞는가?
- 예제가 함수 동작을 즉시 이해시키는가?
- 금지 문구·placeholder가 포함되어 있지 않은가?
