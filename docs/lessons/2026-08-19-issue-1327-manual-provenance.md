# 이슈 #1327 - 매뉴얼 release provenance 단일화

## 맥락

`docs/manual/manifest.yaml`은 `releaseRef: 1.12.1`과
`releaseCommit: 7cf0b73646af05c0f8872cc4f6a16983949c4e3e`를 선언했지만,
매뉴얼 workflow는 `1.11.0`과 `6187173b58e8b4c5c435c145e00e94708f31ef75`를
직접 전달했다. 두 입력이 모두 성공해 workflow가 green이어도 manifest가
선언한 release source를 검증했다는 증거가 아니었다.

## 결정

- `validate_release_manuals.rb --manifest PATH`가 manifest의
  `releaseRef`와 `releaseCommit`을 읽어 검증하도록 했다.
- 기존 `TAG EXPECTED_SHA` 호출은 유지하되, `--manifest`와 명시 인자를 함께
  주면 두 값이 다를 때 Git 조회 전에 fail-closed 한다.
- `manual-docs.yml`과 module documentation checklist는 manifest 모드만
  호출한다. 성공 로그에는 실제 tag, commit, 검사 링크 수를 함께 남긴다.

## 결과

매뉴얼 CI가 검증 대상 release를 workflow 파일의 복사된 값이 아니라
`docs/manual/manifest.yaml`에서 선택한다. manifest와 다른 stale 입력은
회귀 테스트에서 즉시 거부되므로, 오래된 tag를 검증하고도 green으로 남는
경로를 차단한다.

## 검증

- `ruby scripts/manual/release_contract_test.rb` — 22 runs, 56 assertions,
  0 failures, 0 errors.
- `ruby scripts/manual/validate_release_manuals.rb --manifest docs/manual/manifest.yaml`
  — `1.12.1` / `7cf0b73646af05c0f8872cc4f6a16983949c4e3e`, 5,107 links checked,
  0 missing.
- `ruby -c scripts/manual/validate_release_manuals.rb` 및
  `ruby -c scripts/manual/release_contract_test.rb` — `Syntax OK`.

## 향후 지침

매뉴얼 release 기준을 변경할 때는 먼저 manifest의 `releaseRef`와
`releaseCommit`을 갱신하고 생성된 `manifest.json`을 확인한다. workflow나
checklist에 tag와 SHA를 별도로 복사하지 말고
`validate_release_manuals.rb --manifest docs/manual/manifest.yaml`을
사용한다. 로컬 검증 성공은 hosted CI의 exact head 결과를 대신하지 않는다.
