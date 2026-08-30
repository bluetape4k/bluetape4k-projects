# 리뷰 도구 적용 범위와 verdict 교훈

## 맥락

#1535는 Markdown manual 6개와 Ruby minitest 1개만 변경했다. 독립 inline review는
production source와 EN/KO 문서 계약을 대조해 P0부터 P3까지 finding이 없다고
판정했지만, 제공되지 않은 `lsp_diagnostics`를 실행하지 못했다는 이유만으로 최초
verdict를 `REQUEST_CHANGES`로 기록했다.

## 잘못된 판단과 증거

검증 도구의 부재와 검증 대상의 결함을 같은 상태로 취급한 것이 잘못이었다.
Markdown에는 이 저장소가 요구하는 LSP gate가 없고, Ruby 변경은 실행 가능한 minitest와
manual validator로 직접 검증할 수 있었다.

실제 증거는 다음과 같았다.

- `ruby scripts/manual/validate_manuals_test.rb`: `19 runs`, `160 assertions`,
  failures/errors/skips 모두 0
- release manual validator: `5067 checked`, `0 missing`
- EN/KO localization gap: 0
- 한국어 terminology finding: 0
- `git diff --check`: 통과
- 독립 inline review: P0/P1/P2/P3 모두 0

## 결정

review verdict는 변경 surface에 적용 가능한 필수 검증과 실제 finding을 기준으로 내린다.
적용할 계약이 없는 도구는 `N/A`로 분류하며, 도구가 노출되지 않았다는 사실만으로
`REQUEST_CHANGES`나 `PENDING`을 만들지 않는다. 반대로 적용 가능한 필수 도구가
누락됐다면 해당 계약과 영향을 명시하고 검증을 보완할 때까지 verdict를 보류한다.

#1535 reviewer에게 Markdown/Ruby surface와 대체 검증 증거를 다시 제시한 뒤, 실제 수정할
finding이 없음을 확인하고 verdict를 `APPROVE`로 바로잡았다.

## 결과

도구 가용성 중심의 형식적 미충족을 제거하면서도 inline review, 실행 가능한 테스트,
release compatibility, locale parity gate는 모두 유지했다. 검증 강도를 낮춘 것이 아니라
검증 도구를 실제 변경 surface에 맞게 정렬했다.

## 향후 지침

1. reviewer prompt에는 변경 언어와 artifact 종류, 적용 가능한 validator를 명시한다.
2. unchecked item을 merge blocker로 올리기 전에 그 도구가 해당 surface의 필수 계약인지
   확인한다.
3. finding이 0인데 도구 부재만으로 부정 verdict가 나오면 exact file/line의 수정 가능한
   결함을 요구한다.
4. 대체 검증을 사용할 때는 명령, 종료 코드, failure/error/skip 수를 남긴다.
5. 실제 필수 검증 누락과 적용 불가능한 검증은 각각 `PENDING`과 `N/A`로 구분한다.
