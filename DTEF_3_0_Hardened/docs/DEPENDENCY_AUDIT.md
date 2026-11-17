# Dependency and supply-chain controls

The dependency lockfiles are retained to make builds reproducible. A live vulnerability audit could not be completed in the current offline build environment: npm registry/DNS access was unavailable and Maven was not installed.

## Required CI/release checks

- Frontend: `npm ci`, `npm audit --audit-level=high`, `npm run typecheck`, `npm test`, `npm run build`.
- Backend: `mvn clean verify`, then run an OWASP Dependency-Check or approved SCA scanner against the resolved Maven dependency graph.
- Containers: scan the final backend and frontend images for OS/package vulnerabilities and misconfiguration before publishing to the government registry.
- Pin production base images by digest and keep a documented patch cadence.
- Review and update Next.js, React, Spring Boot, PostgreSQL, Redis, RabbitMQ, Node and Java patch versions only with compatibility tests and a reviewed lockfile/POM change.
- Generate and retain an SBOM (CycloneDX or SPDX) for each release artifact.

Do not interpret a passing source-level check as evidence that the currently pinned third-party packages have no known vulnerabilities. The CI `npm audit` job is intentionally a release gate and may fail until dependencies are updated against current advisories.


## Milestone 4 test tooling additions

- `@playwright/test` 1.65.1 is added as a development-only browser test dependency. It is used only by the Chromium end-to-end suite and is not shipped in the frontend runtime image.
- Testcontainers 1.21.4 JUnit, PostgreSQL and RabbitMQ modules are added as backend test-scope dependencies. Redis is launched using Testcontainers GenericContainer. These dependencies are not packaged into the production runtime.

## Release-blocking frontend framework update identified 17 November 2026

The current lockfile still resolves `next` 15.2.4 and `react` / `react-dom` 19.0.0. These are not the current patched releases. The Next.js 15 maintenance line has published security fixes in 15.5.28 and 15.5.29, including a critical image-optimization RCE fix in 15.5.28 and further fixes in 15.5.29. The Next.js support policy says the 15.x maintenance line is supported through 31 January 2027; 16.x is the active LTS line. The React release page lists 19.3.1 as the latest stable version. See the Next.js support policy and release history before choosing the upgrade target.

**Do not release with the current 15.2.4 lockfile.** In a network-enabled developer environment, choose and review one of these paths:

1. Lower-change patch path: update to the latest supported Next.js 15 maintenance release (currently 15.5.29), update React/React DOM to a compatible patched release (currently 19.3.1), update the corresponding type packages, regenerate `package-lock.json` using npm, then run the full build and E2E suite.
2. Strategic path: upgrade to the current Next.js 16 active LTS line (currently 16.4.6) with compatible React 19.3.1, regenerate the lockfile, and complete a major-version compatibility review and full test run.

This preparation environment cannot resolve `registry.npmjs.org` (DNS `EAI_AGAIN`), so the lockfile was not hand-edited to guess transitive dependency versions. The CI `npm audit --audit-level=high` gate is expected to flag unresolved advisories until the dependency upgrade is completed. This is an explicit release blocker, not a completed fix.

Reference pages checked on 17 November 2026:

- Next.js support policy: https://nextjs.org/support-policy
- Next.js release history: https://nextjs-changelog.vercel.app/
- Next.js npm release tags: https://www.npmjs.com/package/next
- React version history: https://react.dev/versions
- Playwright Test npm package: https://www.npmjs.com/package/@playwright/test
- Testcontainers 1.21.4 is used for the existing 1.x artifact coordinates and package names; Testcontainers 2.x has changed module coordinates and Java package locations, so a separate migration is required before adopting 2.x. See https://central.sonatype.com/artifact/org.testcontainers/junit-jupiter/versions and https://github.com/testcontainers/testcontainers-java/releases.

## Milestone 4.1 dependency refresh automation

A manual GitHub Actions workflow has been added at `.github/workflows/frontend-dependency-refresh.yml`. It defaults to Next.js `16.4.6`, React/React DOM `19.3.1`, and React type packages `19.3.0`. On a connected GitHub runner it:

1. Updates exact framework versions and regenerates `frontend/package-lock.json` from the npm registry.
2. Runs `npm audit fix` without forcing major changes beyond the explicitly requested framework versions.
3. Runs a clean `npm ci`, `npm audit --audit-level=high`, TypeScript typecheck, frontend tests and a production build.
4. Creates a dependency-refresh pull request only if every gate succeeds.

The local package lockfile has intentionally not been hand-edited. This environment cannot resolve `registry.npmjs.org`; fabricating transitive dependency versions or integrity hashes would make reproducible builds less safe. The workflow must be run in the connected repository, and its generated PR must pass the normal Playwright and container CI jobs before merge.

Current release references checked 17 November 2026:

- Next.js support policy: https://nextjs.org/support-policy
- Next.js release history: https://nextjs-changelog.vercel.app/
- React versions: https://react.dev/versions
- React release history: https://github.com/react/react/releases
- Playwright Test package: https://www.npmjs.com/package/@playwright/test

## Additional backend framework release blocker

`backend/pom.xml` still uses Spring Boot `3.4.3`. Spring Boot's official release notice identifies `3.4.13` as the final open-source support release for the 3.4 line. The current 3.4.3 parent is older still. Spring Boot 4.1.2 is the current stable active line as of this review and requires Java 17 or later, so the existing Java 21 baseline is compatible at the JVM level.

A backend framework upgrade has deliberately not been applied without a Maven build. Spring Boot 4 has a migration surface that includes renamed web and OAuth2 resource-server starters, new test starter modules, Jackson 3 package/API changes, and replacement of deprecated `@MockBean` usage with Spring's `@MockitoBean`. The official migration guide recommends first moving to the latest 3.5.x line before the Boot 4 migration.

Recommended controlled sequence:

1. Upgrade from 3.4.3 to the latest 3.5.x migration baseline and run the complete Maven/JUnit/Testcontainers suite.
2. Migrate the POM to the supported Spring Boot 4.1.x line and update starter coordinates.
3. Migrate Jackson 2 imports/exception handling to Jackson 3 and replace deprecated test annotations.
4. Run Flyway migrations, full JUnit, Testcontainers, Playwright, Docker image builds and security scans.
5. Merge only after all gates pass and no Critical/High dependency findings remain.

Do not treat the current backend dependency tree as production-cleared until this migration and a live Maven vulnerability scan are complete.

References:

- Spring Boot 3.4.13 end-of-open-source-support notice: https://spring.io/blog/2025/12/18/spring-boot-3-4-13-available-now/
- Spring Boot 4.1.2 release notice: https://spring.io/blog/2026/10/23/spring-boot-4-1-2-available-now/
- Spring Boot supported versions: https://github.com/spring-projects/spring-boot/wiki/Supported-Versions
- Spring Boot 4 migration guide: https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- Jackson 3 migration: https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md

## Maven vulnerability scanning in CI

The main CI workflow now runs OWASP Dependency-Check Maven `13.0.0` against the backend dependency tree and fails the job at CVSS 7.0 or higher. It retains HTML and JSON reports as artifacts. Configure the GitHub repository secret `NVD_API_KEY` to reduce NVD rate limiting and improve first-run reliability. A successful scan must be reviewed for both true positives and false positives; suppressions require a documented owner, rationale and expiry.

OWASP Dependency-Check is a software composition analysis tool for identifying known publicly disclosed vulnerabilities in project dependencies: https://owasp.org/projects/dependency-check/ and https://dependency-check.github.io/DependencyCheck/dependency-check-maven/summary.html.
