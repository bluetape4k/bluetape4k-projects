## Issue 251 States Comparison

Context: Issue #251 asked for a comparison between `utils/states` and
`joost-klitsie/StateMachine`, then follow-up improvement issues.

Decision: Do not add the external library as a dependency. Treat it as a design
reference because it is KMP/UI-oriented, small, and has no detected GitHub
license metadata.

Outcome: Split the findings into focused follow-up issues: #436 for a reactive
event/effect runtime, #437 for nested state DSL, and #438 for README positioning
guidance.

Verification: Checked issue #251, qmd history, local `utils/states` APIs and
README files, and external repository README/source/release metadata.

Future agents: Keep `bluetape4k-states` JVM/backend-focused. Borrow event,
effect, side-effect lifecycle, and nested DSL ideas only when they fit the
existing sync/suspend FSM contracts.
