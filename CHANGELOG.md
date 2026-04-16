# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

---

## [0.1.0-alpha] - 2026-04-16

Initial release published to Maven Central.

### Added

- `@CacheAwareSpringBootTest` annotation as a drop-in replacement for `@SpringBootTest`, with full attribute parity (`classes`, `properties`, `args`, `webEnvironment`, `useMainMethod`); supports meta-annotation composition and `@Inherited` inheritance
- `ObservableContextCache` — decorator over Spring's `DefaultContextCache` that fires listener callbacks on every cache lookup (hit and miss)
- `ContextCacheMissesListener` interface with `onCacheMiss` and `onCacheHit` callbacks for custom event handling
- `DefaultContextCacheMissesListener` — built-in listener that records all events into `ContextCacheMetricsRegistry`
- `ContextCacheMetricsRegistry` — static registry tracking context load events per test class, accessible via `snapshot()` and resettable via `clear()`
- `BUILD / REBUILD / REUSE` event semantics in `EventType` — every context load is classified:
  - `BUILD` — first `ApplicationContext` created in the entire build execution
  - `REBUILD` — context discarded and rebuilt due to a configuration mismatch
  - `REUSE` — cache hit; existing context returned without rebuilding
- `TestContextHistory` — immutable per-test-class history of context load events with accessors for rebuild and initial build events
- `GlobalTestExecutionAnalyzer` — JUnit Platform `TestExecutionListener` that reports cache usage at the end of the test suite; auto-discovered via SPI, no manual registration required
- Rebuild warning includes active profile diff — shows the profiles of each offending class alongside the initial build's profiles so the mismatch is immediately visible
- ANSI-colored log output for fast scanning of cache reports
- `ContextCacheMetricsRegistry.clear()` public API for per-module isolation in multi-module builds and IDE re-run scenarios
- ADR 0001 — registry clear strategy (no auto-clear; explicit `clear()` only)

### Build

- All Spring Boot and JUnit dependencies marked `optional` to prevent transitive version conflicts in consumer projects
- GPG signing, sources jar, and Javadoc jar configured for Maven Central publishing
- Built and tested against Spring Boot 3.5.10 and JUnit 5
