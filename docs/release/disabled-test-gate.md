# Disabled Test Release Gate

Run the disabled-test gate before release:

```bash
./gradlew checkDisabledTests
```

The task writes:

```text
build/reports/disabled-tests/disabled-tests.md
```

Release checklist:

1. Open the generated report.
2. Confirm `Known-bug violations without tracking issue` is `0`.
3. Review `uncategorized` entries and either add a clearer annotation reason or
   create a tracking issue when the disabled test hides a real bug.
4. Keep unsupported capability, manual environment, slow optional, and
   conditional environment skips visible in the report.

Gate rule: any disabled test categorized as `known-bug` must include a GitHub
issue reference such as `#497` in the annotation reason.
