# README 다이어그램의 장식 path와 route 분류

## 배경

Issue #768의 전체 README 다이어그램 검증은 `274`개 asset 중 `132`개에서
실패했다. Okio 다이어그램 4개는 모두 `content outside frame=-34/...`를
보고했지만, 렌더링된 장식 아이콘은 frame 내부에 있었다.

## 원인

`scripts/validate-readme-diagram-assets.mjs`는 SVG `path`의 `class` 문자열에서
`route`, `flow`, `line` 같은 단어를 찾는다. 기존 정규식은 단어 경계를
사용하므로 `icon-line-green`의 하이픈 앞뒤를 경계로 해석하고, 장식 path를
route로 분류했다.

해당 path는 `transform="translate(...)"`가 적용된 아이콘 그룹 안에서
`M -10 -8 H 10` 같은 로컬 좌표를 사용한다. validator는 route 좌표에 상위
transform을 적용하지 않으므로, 잘못 분류된 음수 좌표가 frame 밖의 content로
계산됐다.

## 판단

이번 수정은 route 좌표계 전체를 다시 구현하지 않는다. SVG `class`는
공백으로 구분된 token이라는 계약에 따라 `icon-line`과 `icon-line-*` token만
route 후보에서 제외한다. 기존 `route`, `flow-*`, `line` 분류와 connector
검사는 그대로 유지한다.

회귀 fixture에는 실제 `flow-green` route와 transform 내부의
`icon-line-green`을 함께 둔다. 검증 결과는 카드 2개와 route 1개여야 한다.
따라서 장식 path만 제외하면서 실제 연결선 검사가 유지되는지도 한 테스트에서
증명한다.

## 결과

`icon-line*`을 사용하는 SVG 8개만 결과가 달라졌다. Okio 01/02는 실패가
사라졌고, Okio 03/04와 일부 Ktor asset에서는 잘못된 outside-frame 실패가
사라진 뒤 기존 세로 여백 또는 source metadata 실패가 드러났다. 전체 실패
asset 수는 `132`에서 `130`으로 줄었다.

분류기 수정이 새로 드러난 asset 결함까지 해결한 것으로 간주하지 않는다.
이번 변경은 false positive 제거에 한정하고, 남은 margin과 metadata 실패는
Issue #768의 후속 slice에서 별도로 다룬다.

## 검증

- `node --test scripts/validate-readme-diagram-assets_test.mjs`
- `DIAGRAM_VALIDATION_TARGETS='io-okio-diagram-01.svg,io-okio-diagram-02.svg,io-okio-diagram-03.svg,io-okio-diagram-04.svg' node scripts/validate-readme-diagram-assets.mjs`
- `DIAGRAM_VALIDATION_REPORT=/tmp/issue-768-icon-lines-fixed.json node scripts/validate-readme-diagram-assets.mjs`
- `node --check scripts/validate-readme-diagram-assets.mjs`
- `node --check scripts/validate-readme-diagram-assets_test.mjs`
- `git diff --check`

## 향후 지침

route parser 변경 전후를 비교할 때 전체 실패 수만 보지 않는다. 영향받은
각 asset의 `paths`와 `failures`를 함께 비교하고, 잘못된 경계 실패가 사라진
뒤 새로 드러난 검증 결과는 별도 결함으로 분리한다. transform이 필요한
실제 route가 확인되기 전에는 장식 path 한 사례를 이유로 범용 transform
해석기를 추가하지 않는다.
