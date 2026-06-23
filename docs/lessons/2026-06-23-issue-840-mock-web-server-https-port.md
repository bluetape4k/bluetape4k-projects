# Issue 840 - mock-web-server HTTPS port

## Context

`testing/mock-web-server` runtime configuration uses HTTPS port `8443` and the
Jib container metadata exposes ports `80` and `8443`, but both README locales
still documented HTTPS port `443`.

## Decision

Update the English and Korean READMEs to document `8443` consistently in the
architecture text, feature list, configuration table, and Docker run command.

## Follow-up Guard

`ReadmeHttpsPortContractTest` now checks that README HTTPS documentation matches
`application.yml` and the Jib port list, while blocking stale standalone `443`
documentation patterns.
