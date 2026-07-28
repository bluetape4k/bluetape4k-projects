# Disabled Test Release Gate

Release 전에 disabled-test gate를 실행한다.

```bash
./gradlew checkDisabledTests
```

Task는 다음 파일을 쓴다.

```text
build/reports/disabled-tests/disabled-tests.md
```

Release checklist:

1. 생성된 report를 연다.
2. `Known-bug violations without tracking issue`가 `0`인지 확인한다.
3. `uncategorized` entry를 검토하고, disabled test가 실제 bug를 숨기면 더 명확한 annotation
   reason을 추가하거나 tracking issue를 만든다.
4. Unsupported capability, manual environment, slow optional, conditional environment skip은
   report에 보이게 유지한다.

Gate rule: `known-bug`로 분류된 모든 disabled test는 annotation reason에 `#497` 같은 GitHub
issue reference를 포함해야 한다.
