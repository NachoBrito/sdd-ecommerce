<!--
Sync Impact Report:
- Version change: Initial Draft -> 1.0.0
- List of modified principles:
  - Principle 1: Do Not Distribute by Default (Added)
  - Principle 2: Optimize for Deletion, Not Extension (Added)
  - Principle 3: Make Dependencies Explicit (Added)
  - Principle 4: Contract at the Boundary, Not in the Middle (Added)
  - Principle 5: Test the Transformation, Not the Plumbing (Added)
  - Principle 6: Emit Structured Events, Derive Everything Else (Added)
  - Principle 7: Recovery Over Prevention (Added)
  - Principle 8: Attention Is Finite (Added)
  - Principle 9: Value Is Realized at the User, Not at Merge (Added)
  - Principle 10: Commands Are Discoverable; Local Dev Matches CI (Added)
- Added sections:
  - Core Principles (Principles 1 through 10 with Rationale, Rule, and How To Apply)
  - Governance (Authority and Precedence, Amendment Procedure, Versioning Policy, Compliance Review Expectations)
- Removed sections: None
- Templates requiring updates:
  - ✅ `.specify/templates/plan-template.md` (Constitution Check gate aligned with 10 principles)
  - ✅ `.specify/templates/spec-template.md` (Scope, boundary contracts, and success criteria aligned)
  - ✅ `.specify/templates/tasks-template.md` (Observability, contract tests, and rollback tasks aligned)
- Follow-up TODOs: None (All placeholders resolved)
-->

# SDD E-Commerce Constitution

**Version**: 1.0.0  
**Ratified**: 2026-10-24  
**Last Amended**: 2026-10-25  

This constitution serves as the foundational "Source of Law" for the SDD E-Commerce project. Every architectural decision, code submission, pull request review, and automated quality gate must strictly adhere to these principles. Principles are decision-ready: a reviewer must be able to point to a diff and cite a violation unambiguously without subjective interpretation.

---

## Core Principles

### Principle 1: Do Not Distribute by Default

#### Rationale
In distributed systems, coordination is fundamentally constrained by network round trips and physical limits, not milliseconds of CPU execution. Introducing distributed processes introduces partial failure modes, network partitioning, data consistency lag, serialization overhead, distributed transactions, and severe operational cognitive load. Compute is cheap and scaling vertically on modern multi-core machines handles orders of magnitude more load than most systems ever encounter. A monolithic or single-deployable process eliminates network latency and distributed failure modes by construction.

#### Rule
Vertical scaling and a single deployable unit is the mandatory starting baseline for all architecture. Introducing a new network boundary, external microservice, background daemon process, or asynchronous message queue requires an explicit, written justification accepted in technical review. The justification MUST prove at least one of the following four conditions:
1. **Working-set overflow**: The memory, disk, or compute requirements genuinely exceed the physical capacity of the largest viable single compute instance.
2. **Genuinely independent compute lifecycle**: The workload requires specialized hardware (e.g., GPU accelerators), exhibits wildly divergent scaling dynamics, or carries catastrophic crash risk (e.g., untrusted sandbox execution) that threatens core system uptime.
3. **Geographic latency**: Compute MUST execute physically proximate to edge consumers across disparate geographic regions to satisfy non-negotiable latency constraints.
4. **Organizational independence**: Two distinct engineering teams with fully decoupled organizational ownership, domain boundaries, and disjoint release cadences require autonomous deployment.

Reviewers MUST reject any diff or technical plan that introduces an unapproved network boundary, external daemon, or message queue broker.

#### How To Apply
- Design all new features as modular internal packages or libraries within the primary monolith deployable, using in-memory function calls and strongly typed local interfaces.
- For asynchronous or delayed work, utilize in-process concurrency workers backed by transactions in the primary relational database before proposing an external queue broker.
- If distribution is proposed, document the specific exemption criteria and empirical resource metrics in an Architecture Decision Record (ADR) prior to implementation.

---

### Principle 2: Optimize for Deletion, Not Extension

#### Rationale
Software maintenance costs scale with lines of code, cognitive indirection, and coupling. Code is a liability, not an asset. Designing for hypothetical future extension results in complex inheritance trees, generic abstractions, abstract factories, and configuration sprawl that rarely match actual future requirements. The easiest and safest codebase to evolve is one where modules are small, self-contained, and easily replaceable or erasable with minimal blast radius. Duplication below three occurrences is substantially cheaper than the wrong abstraction.

#### Rule
Modules MUST be designed so that an engineer can understand, delete, and rewrite the entire module within a single working day (nominally under 500 lines of code per cohesive module). Engineers MUST inline logic until duplication hurts. Abstractions MUST NOT be extracted prior to encountering at least three distinct, real-world occurrences (the Rule of Three). Speculative generalizations, plugin architectures for single implementations, and premature generic parameterization are strictly forbidden.

Reviewers MUST reject diffs introducing unused abstraction layers, single-implementation interfaces, premature dynamic configuration hooks, or monolithic modules that cannot be cleanly deleted without cross-cutting cascade edits.

#### How To Apply
- Prefer concrete, localized functions over generic class hierarchies or multi-layered indirection.
- When writing a new feature, write straightforward, self-contained code directly where it is consumed; do not preemptively invent a generic framework.
- If a business requirement changes or is deprecated, delete the obsolete code, tests, and configuration outright rather than leaving dead flags or backwards-compatibility shims.

---

### Principle 3: Make Dependencies Explicit

#### Rationale
Hidden coupling, global mutable state, singleton patterns, and import-time side effects destroy local reasoning. When a function or class reaches out to ambient context (global variables, hidden thread-locals, unpassed database pools), the caller cannot predict, verify, or safely isolate execution. Making all dependencies visible in signatures turns runtime surprises into explicit compilation or invocation contracts and guarantees painless unit testing.

#### Rule
All external dependencies—including database connections, network clients, system clocks, random number generators, environment configurations, and file systems—MUST be passed explicitly into functions or constructors (Dependency Injection). Singletons, global state stores, ambient thread-local contexts, and import-time side effects (such as auto-connecting databases or initiating network traffic upon module import) are strictly prohibited.

A reader MUST be able to identify every external resource and collaborator required by an object or function purely by reading its signature and constructor parameters. Reviewers MUST reject any diff accessing hidden global singletons or executing I/O during module initialization.

#### How To Apply
- Pass dependencies as constructor arguments or function parameters; avoid static accessors such as `Database.getInstance()` or `Config.GLOBAL`.
- Use interface/protocol types for dependencies when testing requires substitution, but keep dependency passing explicit.
- Ensure that importing any module or file in the codebase is purely declarative and executes zero side-effecting I/O or network connections.

---

### Principle 4: Contract at the Boundary, Not in the Middle

#### Rationale
Internal domain logic should manipulate clean, strongly typed domain representations. When raw external payloads (JSON dictionaries, query parameter maps, database driver rows) leak into business logic, domain rules become coupled to external transport details and wire formats. Boundaries (HTTP endpoints, event consumers, storage persistence, third-party APIs) are untrusted perimeter points that require strict schema validation, versioning, and bidirectional translation.

#### Rule
Every producer/consumer boundary MUST have an explicit, versioned schema definition (e.g., OpenAPI, JSON Schema, Protobuf, or strictly typed Data Transfer Objects). Semantic validation and transformation MUST happen strictly at the boundary adapter. Raw payload structures (e.g., untyped maps, dictionary bags, raw JSON) MUST NOT penetrate into domain services or business entities. The side of the boundary that understands both external transport and internal domain context OWNS the boundary adapter and reconciliation logic. Shared mutable schemas between independent subsystems are prohibited.

Reviewers MUST reject any diff where unvalidated external payloads bypass boundary schemas, where business logic inspects HTTP headers or raw query strings directly, or where boundary schema changes lack explicit versioning.

#### How To Apply
- Implement ingress boundary adapters: parse raw input immediately into validated DTOs/schemas, enforce validation rules, and map into clean domain models before calling domain logic.
- Implement egress boundary adapters: translate domain models into explicit, versioned response DTOs or event payloads at the controller/publisher boundary.
- Version all external API endpoints (`/v1/...`) and event payloads (`OrderCreated_v2`) to preserve forward and backward compatibility across releases.

---

### Principle 5: Test the Transformation, Not the Plumbing

#### Rationale
Business value, domain logic, and algorithmic correctness reside in pure transformations (converting inputs into outputs). Framework dispatching, ORM query builders, and serialization plumbing are commodity infrastructure. Mocking internal application code results in brittle tests that test implementation details rather than business behavior, giving false confidence while actively hindering refactoring. True confidence comes from isolated unit tests on pure logic and realistic integration tests at physical boundaries.

#### Rule
Unit tests MUST test pure transformation logic, state transitions, and business rules without mocks. Integration tests MUST test boundaries (databases, queues, external APIs, file systems) using real or containerized boundary instances (e.g., ephemeral test databases or Testcontainers). Engineers MUST NOT mock code they own; engineers MUST ONLY mock external third-party services they do not control. 

Every bug fix PR MUST include a newly added reproducing test that demonstrably fails before the fix and passes after. A green CI without a failing test for the bug being resolved is NOT green. Reviewers MUST reject bug-fix PRs lacking a reproducing regression test, as well as test diffs that extensively mock internal application classes.

#### How To Apply
- Structure business logic into pure, stateless functions that take explicit inputs and return outputs, allowing testing with fast, zero-dependency unit tests.
- Verify persistence, queries, and network boundaries using automated integration tests executing against isolated test database instances.
- When resolving a bug report, begin by writing a test case asserting the expected behavior that reproduces the defect in CI, commit the failing test, and then implement the solution.

---

### Principle 6: Emit Structured Events, Derive Everything Else

#### Rationale
Unstructured string log statements (`printf`, ad-hoc string formatting) are effectively write-only data: impossible to query reliably, aggregate across high volume, or correlate across transactions. Structured canonical events preserve high-cardinality dimensionality, capturing full context at the point of origin. Logs, metrics, alerts, and distributed traces are simply mathematical or visual projections derived downstream from canonical structured event streams.

#### Rule
Ad-hoc, unstructured string logs are forbidden in all new code. All telemetry MUST be emitted as structured, machine-parseable events (e.g., JSON key-value objects) with consistent schema fields. Every event MUST capture high-cardinality operational metadata, including:
- `timestamp` (ISO 8601 UTC)
- `event_type` (dot-notated domain event, e.g., `order.payment.failed`)
- `request_id` or `trace_id` (cross-boundary correlation identifier)
- `user_id` and `tenant_id` (when operating within authenticated contexts)
- `feature_flags` (active evaluation states relevant to the execution path)

Reviewers MUST reject diffs containing unstructured logging calls or omitting mandatory high-cardinality context fields.

#### How To Apply
- Use a structured logging library that automatically injects ambient contextual fields (request IDs, trace IDs, tenant context) into every emitted payload.
- Emit wide events at the conclusion of significant units of work (e.g., an HTTP request completion or queue message processing) containing timing, outcome status, and contextual attributes.
- Derive rate and count metrics in monitoring tools from structured event queries rather than sprinkling uncoordinated counters across application code.

---

### Principle 7: Recovery Over Prevention

#### Rationale
In complex software systems, incidents and bugs are inevitable regardless of pre-release ceremony. Systems optimized purely for prevention deploy slowly, accumulate large batches of risk, and fail catastrophically when defects inevitably slip through. High-performing engineering organizations prioritize MTTR (Mean Time to Recovery) over MTBF (Mean Time Between Failures). Rapid, automated rollback capabilities and runtime blast-radius containment make deployment safe and routine.

#### Rule
Every production change MUST be revertible in under five minutes without requiring a code commit, rebuild, or re-run of the CI test suite. All high-risk logic, architectural refactors, and critical business paths MUST be gated behind runtime feature flags. 

All database schema modifications MUST follow a two-phase expand-and-contract methodology (Phase 1: Expand schema additively, deploy application supporting both schemas; Phase 2: Contract schema after old code is decommissioned). Destructive or backward-incompatible schema migrations in a single deploy step are prohibited. Automated rollback execution MUST be verified during deployment workflows rather than assumed.

Reviewers MUST reject PRs containing non-backward-compatible database migrations, single-step renames of active columns, or risky architectural changes lacking feature flag protection.

#### How To Apply
- Decouple code deployment from feature release using feature flags and progressive rollouts.
- When altering data models: deploy the additive column or table first, write to both locations, backfill existing data, switch reads, and only then remove obsolete structures in a subsequent release.
- Maintain and test automated rollback runbooks that can instantly revert service traffic or toggle flags via CLI or deployment console.

---

### Principle 8: Attention Is Finite

#### Rationale
Human engineering attention, vigilance, and cognitive capacity are scarce, exhaustible operational resources. Alert fatigue caused by noisy, flaky, or non-actionable alerts directly results in engineers missing critical production outages. Dashboards cluttered with vanity charts distract on-call engineers during active incidents. A monitoring system must only summon human attention when genuine user harm is occurring and clear remediation steps exist.

#### Rule
Every automated alert that pages a human MUST correspond directly to a user-visible symptom (e.g., elevated 5xx error rates, p99 user-facing latency breaches, payment gateway processing failures, consumer lag causing SLA breaches) rather than internal causes (e.g., CPU usage > 80%). Every alert definition MUST link directly to an up-to-date runbook containing exact triage steps, diagnostic queries, and remediation actions. 

Dashboards MUST represent saved, purposeful investigative queries for incident triage, not decorative vanity graphs. Any alert or monitoring signal that has not fired a useful, actionable page within 90 days MUST be deleted or demoted to an unpaged diagnostic metric. Reviewers MUST reject any alerting PR lacking a verified runbook link or alerting on non-symptomatic internal metrics.

#### How To Apply
- Focus alert definitions on Service Level Indicators (SLIs) and Service Level Objectives (SLOs) tied to end-user journeys (checkout, login, search).
- Add a mandatory `runbook_url` attribute to all alerting rules; verify the runbook URL is reachable and details concrete resolution steps.
- Schedule a quarterly audit to prune stale dashboards, un-triggered alerts, and noisy notifications that produced zero operational actions over the preceding 90 days.

---

### Principle 9: Value Is Realized at the User, Not at Merge

#### Rationale
Code that is merged into the `main` branch but sits undeployed in a repository creates zero customer value, generates zero revenue, and incurs high inventory risk. Undeployed code hides integration bugs, accumulates merge drift, and creates false perceptions of velocity. A feature is only complete when it is running reliably in production, processing real traffic, emitting observable telemetry, and provably delivering value to end users.

#### Rule
A pull request or task is NOT complete ("done") when merged. The definition of "Shipped" strictly requires that the change is:
1. Deployed to the target environment (production or production-equivalent release tier).
2. Fully instrumented and actively emitting structured telemetry.
3. Verified against defined functional success criteria and SLOs under real traffic.
4. Confirmed to be safely revertible or toggleable via feature flags.

Engineers maintain ownership of their changes until production verification is confirmed. Reviewers MUST reject PRs that claim task completion without demonstrating deployment verification and telemetry confirmation plans.

#### How To Apply
- Ensure CI/CD pipelines automatically deploy passing merges to production or staging without manual gates whenever possible.
- Include a "Production Verification" section in PR descriptions, detailing how the author will confirm feature behavior and telemetry after deployment.
- Keep feature branches short-lived (<24 hours) and integrate continuously to avoid large-batch release inventory.

---

### Principle 10: Commands Are Discoverable; Local Dev Matches CI

#### Rationale
Discrepancies between local developer workstations and CI/CD environments are a primary source of wasted engineering hours, build failures, and fragile release pipelines. When build steps, linter arguments, or test executions are hidden in bespoke shell scripts or CI YAML configurations, developers cannot reproduce pipeline results locally. Repeatability and developer efficiency require a single, discoverable command interface shared universally across all environments.

#### Rule
Every repeatable developer action—including `setup`, `dev`, `build`, `lint`, `format`, `test`, `migrate`, `seed`, and `deploy`—MUST be a single, named command documented in a single command runner file (e.g., `Makefile`, `Taskfile`, or root runner script) located at the repository root. Commands MUST execute with zero hidden flags, undocumented environment requirements, or CI-only assumptions. The exact same command executed in the CI pipeline MUST be executable locally by any developer on any supported machine.

If a new contributor cannot list, discover, and execute every project workflow in under 30 seconds (e.g., by executing `make help`), the development interface is broken. Reviewers MUST reject any PR introducing CI-only shell commands, undocumented scripts, or command mismatches between local environments and CI configurations.

#### How To Apply
- Maintain a standardized root `Makefile` (or `Taskfile.yml`) where every target includes a descriptive comment and `make help` displays a formatted menu of all available actions.
- In CI configuration files (`.github/workflows/*.yml`), directly invoke the repository commands (e.g., `make test`, `make lint`) rather than inline shell scripts.
- Ensure all commands accept environment overrides gracefully while providing sensible, zero-configuration local defaults (e.g., spinning up local dependencies via container tooling).

---

## Governance

### Authority and Precedence
This constitution is the supreme architectural authority for the SDD E-Commerce project. In the event of a conflict between this constitution and any other documentation, specification, or oral decision, the rules of this constitution prevail. All design documents (`spec.md`, `plan.md`), task breakdowns (`tasks.md`), and automated tooling configurations are subordinate to these principles.

### Amendment Procedure
Amendments to this constitution must follow a formal, deliberate process:
1. **Proposal**: An engineer or architect submits an amendment proposal via a Pull Request modifying `.specify/memory/constitution.md`.
2. **First-Principles Justification**: The proposal MUST include a detailed rationale grounded in first-principles reasoning explaining why the existing principle is insufficient, what trade-offs are introduced, and how the change affects the five core domains.
3. **Consensus & Review**: The proposal requires approval from designated project stewards and technical leads.
4. **Consistency Propagation**: Before merging, all subordinate templates (`spec-template.md`, `plan-template.md`, `tasks-template.md`) and command documentation MUST be updated simultaneously to reflect the amendment.
5. **Sync Impact Report**: The Sync Impact Report at the top of this document MUST be updated with the version change, modified sections, and template statuses.

### Versioning Policy
The constitution version strictly adheres to Semantic Versioning (SemVer):
- **MAJOR (`X.0.0`)**: Incompatible governance alterations, principle removals, or fundamental redefinitions of core architectural constraints.
- **MINOR (`x.Y.0`)**: Addition of a new principle, introduction of a new mandatory section, or material expansion of existing guidance.
- **PATCH (`x.y.Z`)**: Non-semantic refinements, wording clarifications, typo fixes, or link/reference updates.

### Compliance Review Expectations
Compliance with this constitution is enforced at four distinct gates:
1. **Specification Gate (`/speckit.specify`)**: Requirements and user stories must not mandate architectures that violate core principles (e.g., demanding unnecessary microservice boundaries or uncontracted data transfer).
2. **Technical Planning Gate (`/speckit.plan`)**: Implementation plans MUST include a completed "Constitution Check" section explicitly validating compliance across all 10 principles before design approval.
3. **Task Generation Gate (`/speckit.tasks`)**: Task lists must include explicit tasks for boundary contracts, structured telemetry emission, expand-and-contract migrations, and regression tests.
4. **Code Review Gate (PR Review)**: Reviewers are empowered and required to cite constitution violations (e.g., *"Violates Principle 3: Dependency injected via global singleton rather than constructor"*) as blocking review feedback without debate.
