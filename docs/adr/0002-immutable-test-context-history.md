# ADR 0002 — TestContextHistory uses immutable value object pattern

## Status
Accepted

## Context
`TestContextHistory` accumulates load events over the lifecycle of a test suite run.
It is stored as a value in a `ConcurrentHashMap` inside `ContextCacheMetricsRegistry`,
which uses `ConcurrentHashMap.compute()` to update entries atomically.

## Decision
`TestContextHistory` is an immutable value object. Every call to `withNew(Events)` returns
a new instance rather than mutating the existing one. The `ConcurrentHashMap.compute()`
call in `ContextCacheMetricsRegistry.recordEntry()` atomically replaces the map value
with the new instance.

## Consequences
- Thread safety is guaranteed by the map's atomic `compute()` operation, not by the object.
- No additional synchronization is needed on `TestContextHistory` itself.
- Adding a mutable `List` field to `TestContextHistory` would break this guarantee
  and require separate synchronization — do not do this.
