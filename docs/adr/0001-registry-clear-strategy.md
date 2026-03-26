# 0001 — Registry Clear Strategy

## Status
Accepted

## Context
`ContextCacheMetricsRegistry` holds a static `ConcurrentHashMap` that accumulates
cache miss data for the lifetime of the JVM. In IDEs (IntelliJ, VS Code), the JVM
is often reused between test runs, causing miss counts from a previous run to bleed
into the next report.

The natural fix was to clear the registry automatically at the start of each test
plan execution via `GlobalTestExecutionAnalyzer.testPlanExecutionStarted()`. However,
in multi-module projects where all modules run in the same JVM (e.g. Gradle with
`--parallel`, or Maven Surefire with `forkCount=0`), each module triggers its own
`TestPlan` execution. Auto-clearing at `testPlanExecutionStarted()` would wipe the
previous module's data before a combined report could ever be produced, reducing the
analyzer to per-module reporting only.

Since the library's primary value is identifying context cache inefficiencies across
a full test suite — including across modules — losing cross-module aggregation would
undermine the tool's purpose.

## Decision
Do not auto-clear the registry at any point in the `TestExecutionListener` lifecycle.
Instead, expose `ContextCacheMetricsRegistry.clear()` as a public static method,
leaving the decision of when to reset to the caller.

## Consequences
**Easier:**
- Multi-module projects running in the same JVM get a combined report across all
  modules, which is the most actionable view of context cache inefficiencies.
- Callers with specific isolation requirements (e.g. test suite setup, custom
  listeners) can reset the registry explicitly via `clear()`.

**Harder:**
- IDE re-runs in the same JVM will accumulate data across runs. A second run will
  report misses from both the first and second run combined.

**Must be remembered:**
- The IDE re-run accumulation issue is a known, accepted trade-off. If single-run
  isolation is needed, callers must invoke `ContextCacheMetricsRegistry.clear()`
  before the test suite starts (e.g. via a custom `TestExecutionListener` registered
  before this one).
