# Issue Authority Before Generated Plans

## Context

Issue #754 requests ByteBuffer-oriented serializer APIs and allocation evidence.
Its generated design and implementation plan expanded that work into release
holds, GitHub App credentials, repository rulesets, protected environments, tag
mutation, and GitHub Release creation. Those additions were implemented even
though the issue, its milestone, and its parent epic did not require them.

## Root Cause

The generated plan was treated as a source of product authority instead of a
derivative execution artifact. Review focused on whether the added release
mechanism was internally coherent, not whether it was authorized by the live
issue. That inverted the authority chain and let unrelated operational policy
enter a serializer feature.

## Decision

For issue-driven work, the live issue and explicit user direction define scope.
Specs and plans may clarify implementation details, but they cannot add release,
credential, repository-setting, publication, or other external side effects
without separate explicit authority.

When a generated artifact exceeds its authority:

1. preserve valid in-scope implementation and compatibility evidence;
2. remove the unauthorized operational machinery in a focused corrective PR;
3. rewrite the derivative spec and plan to match the live issue;
4. add a regression check for any repository policy that was accidentally
   changed;
5. keep publish, tag, release, settings, and merge actions behind their own
   fresh gates.

## Verification

The corrective proof checks that release workflows publish Maven artifacts only,
contain no issue-specific hold/App/ruleset behavior, and do not create GitHub
Releases. The retained serializer ABI check computes its own serializer/build
digest and no longer imports release-policy code.

## Future Guidance

At each stacked PR boundary, compare the proposed files and side effects with
the live issue before reviewing implementation detail. A technically sound plan
is still invalid when its scope is unauthorized.
