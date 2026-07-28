## Issue 251 States 비교

배경: Issue #251은 `utils/states`와 `joost-klitsie/StateMachine` 비교, 그리고
후속 개선 issue를 요구했다.

결정: 외부 library를 dependency로 추가하지 않는다. KMP/UI 지향이고 규모가 작으며
감지된 GitHub license metadata가 없으므로 design reference로만 다룬다.

결과: 발견 사항을 집중된 후속 issue로 분리했다. reactive event/effect runtime은
#436, nested state DSL은 #437, README positioning guidance는 #438에서 다룬다.

검증: issue #251, GNO history, 로컬 `utils/states` API와 README 파일, 외부
repository README/source/release metadata를 확인했다.

향후 에이전트: `bluetape4k-states`는 JVM/backend 중심으로 유지한다. event,
effect, side-effect lifecycle, nested DSL 아이디어는 기존 sync/suspend FSM
contract에 맞을 때만 차용한다.
