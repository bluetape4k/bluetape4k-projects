# 교훈: R2DBC indexed binding (2026-06-26)

**이슈**: #826
**모듈**: `:bluetape4k-r2dbc`

## L1: 문서 예시는 API 계약 테스트다

### 문제

`bindIndexedMap`은 map key를 Spring R2DBC의 indexed binding API로 그대로
전달했지만, KDoc과 README 예시는 첫 두 index로 `1`과 `2`를 보여주었다.

### 교훈

Helper가 framework API를 값 변환 없이 그대로 반영한다면, 예시는 framework의
정확한 계약을 따라야 한다. Spring R2DBC indexed parameter의 계약은 zero-based다.

### 향후 방지책

Parameter binding helper에는 README/KDoc 스타일 예시를 그대로 실행하는 직접
테스트를 추가한다. 음수 index처럼 계약 밖의 입력도 framework layer로 넘기기 전에
검증한다.
