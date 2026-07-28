# Manual Bilingual Pair Parity Audit

Issue: #1100

## 범위

`docs/manual/en`과 `docs/manual/ko`는 이미 bilingual pair로 관리되는 public manual이다.
따라서 이번 Korean localization Epic의 primary rewrite 대상이 아니라 parity-only 검증
대상으로 분류한다.

## 검증 결과

- English manual file count: 254
- Korean manual file count: 254
- English file missing Korean pair: 0
- Korean file missing English pair: 0
- Manual image asset count: 30
- Guard command: `python3 scripts/docs-localization-inventory.py --check`

## 결정

Manual pair는 기존 bilingual contract를 유지한다. 이번 Epic에서는 단일 언어 문서와
Kotlin/KTS KDoc을 한국어화하고, manual pair에는 basename parity guard만 적용한다.

## 재현

```bash
python3 scripts/docs-localization-inventory.py --check
```

Expected output:

```text
Korean localization guardrail
- manual EN missing KO: 0
- manual KO missing EN: 0
- English-KDoc policy drift: 0
```
