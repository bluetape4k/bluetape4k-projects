# Issue #497 비활성 test release gate 계획

1. 비활성 test 보고를 위한 `buildSrc` scanner와 Gradle task를 추가한다.
2. root `checkDisabledTests`를 등록하고 `check`에 연결한다.
3. known-bug 위반, issue reference, unsupported capability 분류, conditional
   disabled annotation을 검증하는 unit test를 추가한다.
4. 생성된 report와 gate 규칙을 maintainer가 확인하도록 release 문서를 추가한다.
5. `:buildSrc:test`, `checkDisabledTests`, root task 등록을 입증하는 task 목록
   또는 dry run으로 검증한다.
6. 향후 disabled-test triage를 위한 lesson note를 추가한다.
