# Feedback

## Why this project exists

Integration test suites in Spring Boot projects can become slow in ways that are hard to diagnose.
A single overlooked `@MockBean`, an inconsistent profile declaration, or an unintentional `@DirtiesContext`
can silently fragment the `ApplicationContext` cache and multiply startup time across the build.

`CacheAwareSpringBootTest` was built to surface that fragmentation — not to prescribe a fix, but to
trigger understanding. The goal is to help developers make **conscious choices** about how their tests
are sliced and configured, rather than discovering the problem indirectly through a slow CI pipeline.

The tool is educational as much as it is a convenience: knowing *what* causes a context rebuild is often
enough to write better-isolated tests without any annotation or configuration change at all.

---

## Early stage

This project is at a very early stage — effectively a proof of concept. The core mechanics work and
have been validated on real projects, but the reporting is still limited, the API surface is small,
and there are known gaps in what the tool can surface today.

!!! note "Current reporting scope"
    The rebuild report currently identifies **active profile differences** between contexts. Differences
    in other rebuild triggers — `@MockBean`, configuration classes, property overrides — are detected
    but not yet broken out individually. This is planned for a future release.

Your experience using it on real projects is the most valuable input at this stage.

---

## Share your feedback

If you hit a use case the tool doesn't handle well, found the output confusing, or have an idea for
how it could be more useful — please [:octicons-feed-issue-open-16: Open an issue on GitHub.](https://github.com/chris-carneiro/spring-test-context-cache-metrics/issues)
All feedback is welcome, including questions, edge cases, and suggestions that challenge the current design.
 [:octicons-feed-discussion-16: Community thread](https://github.com/chris-carneiro/spring-test-context-cache-metrics/discussions)