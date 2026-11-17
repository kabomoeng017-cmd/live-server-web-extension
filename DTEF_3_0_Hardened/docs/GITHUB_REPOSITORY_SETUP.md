# DTEF 3.0 — GitHub repository and protected environment setup

This project is prepared for GitHub Actions, but this workspace is not connected to the user's GitHub account. Creating a remote repository, pushing source code, configuring environment approvals and starting hosted workflows require an authenticated repository owner or an authorized GitHub connection.

## 1. Create the repository and push the project

1. Create an **empty private repository** in the intended GitHub organization/account. Do not initialize it with a README, licence or `.gitignore` because those files are already in this project.
2. Extract the DTEF 3.0 package and open a terminal in the project root (the folder containing `README.md`, `frontend/` and `backend/`).
3. Run the following, replacing the URL with the repository's real HTTPS or SSH URL:

```bash
git init -b main
git add .
git status --short
git commit -m "chore: prepare DTEF 3.0 milestone 4.2 validation"
git remote add origin https://github.com/OWNER/REPOSITORY.git
git push -u origin main
```

Configure the local Git commit name/email before `git commit` if Git asks for them. Review `git status` and ensure no `.env`, token, personal information, or real government/bank data is staged. `.gitignore` excludes local secrets, build output and load-test token files.

## 2. Enable Actions and dependency pull requests

- In **Settings → Actions → General**, allow the required GitHub Actions workflows to run.
- Allow GitHub Actions to create pull requests. The dependency-refresh workflow needs the workflow's `contents: write` and `pull-requests: write` permissions plus this repository setting.
- Keep the default branch protected. Require the main CI checks before merging changes.
- Review dependency PRs and their generated lockfile; never auto-merge a framework major-version update.

## 3. Create protected environments

Create these environments in **Settings → Environments**:

### `security-testing`

Add required reviewers and set these environment variables:

- `DTEF_PENTEST_ALLOWED_HOST` — one exact, owner-approved HTTPS staging hostname, without a scheme or path.
- `DTEF_PENTEST_AUTHORIZATION_REFERENCE` — ticket, approval, or written authorization reference.

The active ZAP workflow is manual only. It also requires its explicit written-authorization confirmation and a target host matching the protected variable. Do not target live DTEF, gov.bw, identity, banking, or other government production systems.

### `load-testing`

Add required reviewers and set these environment variables:

- `DTEF_LOAD_ALLOWED_HOST` — one exact, owner-approved HTTPS staging hostname.
- `DTEF_LOAD_AUTHORIZATION_REFERENCE` — ticket or written approval reference.
- `DTEF_LOAD_JWT_ISSUER` — only if using the dedicated test-JWT mode.

Configure the appropriate environment secrets:

- `DTEF_LOAD_JWT_SECRET` — only for an isolated staging deployment explicitly configured to trust the dedicated test issuer and secret.
- `DTEF_LOAD_BEARER_TOKEN` — alternatively, a short-lived staging identity-provider token for the pre-issued bearer mode.

For Maven vulnerability scanning, an optional repository secret `NVD_API_KEY` reduces NVD rate limiting. Never place secrets in source files or workflow inputs.

## 4. Execute in controlled order

1. Run **Refresh patched frontend dependencies** manually. Review the PR only after npm install, audit, typecheck, tests and production build pass.
2. Run the main **DTEF 3.0 build and verification** workflow. Resolve all frontend, JUnit/Testcontainers, Playwright, Docker image and security dependency failures before proceeding.
3. Run passive ZAP baseline against the isolated demo stack. Active staging scans require written authorization and the protected `security-testing` environment.
4. Start load testing at **100 VUs**. Review error rate, p95/p99 latency, PostgreSQL connection/pool metrics, Redis latency, RabbitMQ queue depth, CPU, memory and recovery before moving to 500 and 1,000. The 2,000/5,000/10,000 stages require separate approval flags and infrastructure sign-off.
5. Run government-provider simulator contracts. Official Omang, FNBB, institution and welfare integrations remain separate and require approved provider sandboxes and owner-signed contracts.

## 5. Evidence and release decision

Retain GitHub workflow logs and artifacts for JUnit XML, Playwright reports/traces, Docker/Compose logs, dependency audits, ZAP reports and k6 summaries. Update `docs/MILESTONE_4_TEST_RESULTS.md` only from actual workflow or staging evidence. A configured workflow, static source check or simulator is not proof of a production build, official integration, penetration-test certification or capacity.
