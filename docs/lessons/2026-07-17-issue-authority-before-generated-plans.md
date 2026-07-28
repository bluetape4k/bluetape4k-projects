# Generated plan보다 issue authority가 먼저다

## 배경

이슈 #754는 ByteBuffer-oriented serializer API와 allocation evidence를 요구했다. 그런데
generated design과 implementation plan은 이 작업을 release hold, GitHub App credential,
repository ruleset, protected environment, tag mutation, GitHub Release creation까지
확장했다. 이슈, milestone, parent epic이 요구하지 않았는데도 그 추가 항목이 구현되었다.

## 근본 원인

Generated plan을 derivative execution artifact가 아니라 product authority의 source로
다뤘다. Review는 추가된 release mechanism이 내부적으로 일관적인지에 집중했고, live
issue가 그것을 승인했는지는 확인하지 않았다. 그 결과 authority chain이 뒤집혔고,
serializer feature에 관련 없는 operational policy가 들어왔다.

## 결정

Issue-driven work에서는 live issue와 명시적인 user direction이 scope를 정의한다.
Spec과 plan은 implementation detail을 명확히 할 수 있지만, 별도 explicit authority 없이
release, credential, repository-setting, publication 또는 다른 external side effect를
추가할 수 없다.

Generated artifact가 authority를 넘어서면 다음 순서로 처리한다.

1. 유효한 in-scope implementation과 compatibility evidence를 보존한다.
2. Unauthorized operational machinery를 focused corrective PR에서 제거한다.
3. Derivative spec과 plan을 live issue에 맞게 다시 쓴다.
4. 실수로 변경된 repository policy가 있으면 regression check를 추가한다.
5. Publish, tag, release, setting, merge action은 각각의 fresh gate 뒤에 둔다.

## 검증

Corrective proof는 release workflow가 Maven artifact만 publish하고, issue-specific
hold/App/ruleset behavior를 포함하지 않으며, GitHub Release를 만들지 않음을 확인한다.
유지된 serializer ABI check는 자체 serializer/build digest를 계산하고 더 이상
release-policy code를 import하지 않는다.

## 향후 지침

각 stacked PR boundary에서 implementation detail을 review하기 전에 제안된 file과 side
effect를 live issue와 비교한다. 기술적으로 타당한 plan도 scope가 승인되지 않았다면
여전히 invalid다.
