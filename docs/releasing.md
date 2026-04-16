# Release Procedure

This document describes the steps to cut a release and publish to Maven Central.

---

## Prerequisites

- GPG key configured and available (`gpg --list-secret-keys`)
- Maven Central credentials in `~/.m2/settings.xml` under `<server id="central">`
- Namespace `dev.silentcraft.tools` verified on [central.sonatype.com](https://central.sonatype.com)

---

## Steps

### 1. Finalize code

- All features and fixes for the release are committed on `main`
- No uncommitted changes (`git status` is clean)

### 2. Update `CHANGELOG.md`

Add a new entry at the top following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) conventions:

```markdown
## [x.y.z] - YYYY-MM-DD
### Added
- ...
### Fixed
- ...
```

Commit the changelog before tagging.

### 3. Bump version in `pom.xml`

```xml
<version>x.y.z</version>
```

Maven Central rejects `SNAPSHOT` versions. Commit the version bump.

### 4. Run `mvn verify`

Confirms GPG signing, sources jar, and Javadoc jar all produce without errors.

```shell
mvn clean verify
```

Fix any Javadoc warnings before proceeding.

### 5. Tag the release

```shell
git tag -s x.y.z -m "CacheAwareSpringBootTest x.y.z"
```

The `-s` flag creates a GPG-signed tag using your configured signing key.

### 6. Deploy to Maven Central

```shell
mvn deploy
```

Go to [central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments),
review the bundle, and click **Publish**.

### 7. Push commits and tag to GitHub

```shell
git push origin main
git push origin x.y.z
```

### 8. Create GitHub release

- Go to **Releases → Draft a new release**
- Select the tag `x.y.z`
- Copy the relevant section from `CHANGELOG.md` as the release description
- Attach the following artifacts from `target/`:
  - `spring-test-context-cache-metrics-x.y.z.jar`
  - `spring-test-context-cache-metrics-x.y.z-sources.jar`
  - `spring-test-context-cache-metrics-x.y.z-javadoc.jar`
  - `spring-test-context-cache-metrics-x.y.z.pom`
- Click **Publish release**

---

## Post-release

- Bump `pom.xml` to the next `SNAPSHOT` version on `main`
- Commit: `chore: prepare next development iteration`
