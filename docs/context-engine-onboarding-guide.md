# Context Engine Onboarding Guide — multi-test-type-demo

This guide documents the process for onboarding repositories to the Tabnine Context Engine using the `/onboard` skill in Claude Code. It is based on the onboarding and analysis of `multi-test-type-demo` and includes lessons learned, pitfalls to avoid and before/after measurement comparisons.

For the general onboarding methodology, see the [upstream onboarding guide](https://github.com/codota/ctx-customer-pack-distributable/blob/main/docs/onboarding-guide.md). This document supplements that guide with project-specific commands, submodule handling, and team guidance.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Choosing Your Path](#choosing-your-path)
  - [Path A: Query an already-onboarded repo](#path-a-query-an-already-onboarded-repo)
  - [Path B: Full onboarding from scratch](#path-b-full-onboarding-from-scratch)
- [Path A: Set Up and Query](#path-a-set-up-and-query)
- [Path B: Step-by-Step Onboarding](#path-b-step-by-step-onboarding)
  - [Step 0: Initialize](#step-0-initialize)
  - [Step 1: Build Testing Lab](#step-1-build-testing-lab)
  - [Step 2: Load Project Data](#step-2-load-project-data)
  - [Step 3: Baseline Without MCP](#step-3-baseline-without-mcp)
  - [Step 4: Baseline With MCP](#step-4-baseline-with-mcp)
  - [Step 5: Domain Enrichment](#step-5-domain-enrichment)
  - [Step 6: Measure With Domain](#step-6-measure-with-domain)
  - [Step 7: Rollout Plan](#step-7-rollout-plan)
- [Before and After: Submodule Inclusion Impact](#before-and-after-submodule-inclusion-impact)
- [Key Lessons Learned](#key-lessons-learned)
- [Example Queries and Investigations](#example-queries-and-investigations)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

| Requirement | Description |
|---|---|
| Context Engine server | URL and API key (provided by Tabnine) |
| Claude Code | With the Tabnine customer pack installed |
| GitHub PAT | With `repo` scope for all repositories to be analyzed |
| Repository | Cloned locally with submodules initialized |

Verify the tools are installed:

```bash
which tabnine-ctx-onboard && which tabnine-ctx-loader
```

If not found:

```bash
curl -fsSL https://raw.githubusercontent.com/codota/ctx-customer-pack-distributable/main/installers/install.sh | bash -s -- --package all --agent claude
```

---

## Configuration

Create `.tabnine/ctx/ctx-settings.yaml` in the project root:

```yaml
# Context Engine connection (required)
CTX_API_URL: https://ctx.tabnine.com/
CTX_API_KEY: ctx_your_key_here
PROJECT_NAME: multi-test-type-demo-env

# GitHub
GH_PAT: ghp_xxxxxxxxxxxx
GITHUB_ORG: ddreakford
GITHUB_REPO: multi-test-type-demo

# Data volume (ultra-light | light | standard | full)
DATA_VOLUME: light
```

**Data volume options:**

| Volume | History | Events | Use Case |
|---|---|---|---|
| `ultra-light` | 1 day | Push only | Quick testing |
| `light` | 7 days | Push only | Fast evaluation |
| `standard` | 30 days | Push + PRs + Issues | Recommended |
| `full` | 90 days | Push + PRs + Issues + Releases | Most comprehensive |

> **Note:** Data volume controls event history depth, not code analysis scope. The `context-ingestor` agent scans the full repository tree regardless of volume setting.

---

## Choosing Your Path

There are two paths depending on whether the repository has already been onboarded to the Context Engine.

### Path A: Query an already-onboarded repo

**Use this when:** A teammate or CI pipeline has already loaded the repo's data into the Context Engine. You have the CTX API key, you know the GitHub repo, and you want to set up your local workspace to start querying.

**What you'll do:** Clone the repo, install Context Engine tools, create the settings file, verify data is present, and begin investigating.

**Skip to:** [Path A: Set Up and Query](#path-a-set-up-and-query)

### Path B: Full onboarding from scratch

**Use this when:** The repo has not been loaded into the Context Engine yet, or you need to re-onboard (e.g., after a tenant reset, or to include submodules that were missed).

**What you'll do:** Walk through the full 7-step onboarding process — validate connectivity, build a testing lab, load data, measure baseline performance, enrich with domain context, and generate a rollout plan.

**Skip to:** [Path B: Step-by-Step Onboarding](#path-b-step-by-step-onboarding)

> **Not sure which path?** Run the verification step from Path A first. If entities exist, you're on Path A. If the knowledge graph is empty, you need Path B.

---

## Path A: Set Up and Query

Follow these steps to set up a local workspace for querying a repo that has already been onboarded to the Context Engine.

### A1. Clone the repository

```bash
git clone https://github.com/<owner>/<repo>.git
cd <repo>
git submodule update --init --recursive   # if it has submodules
```

Having the local source is important — as our [example investigations](#example-queries-and-investigations) showed, many queries (blast radius, cloud migration, etc.) require both Context Engine data and local file analysis.

### A2. Install the Context Engine tools

```bash
curl -fsSL https://raw.githubusercontent.com/codota/ctx-customer-pack-distributable/main/installers/install.sh | bash -s -- --package all --agent claude
```

Verify:

```bash
which tabnine-ctx-onboard && which tabnine-ctx-loader
```

This installs the CLI tools and creates the `.claude/` skills directory and `.mcp.json` configuration in your workspace. These files make the Context Engine MCP tools and slash commands (like `/investigate-service`, `/blast-radius`, `/understand-flow`) available in Claude Code.

> **If the repo already has `.claude/` and `.mcp.json` checked in**, the installer may still be needed to install the CLI tools (`tabnine-ctx-loader`, `tabnine-ctx-onboard`) on your machine.

### A3. Create the settings file

Create `.tabnine/ctx/ctx-settings.yaml` in the project root:

```yaml
# Context Engine connection (required)
CTX_API_URL: https://ctx.tabnine.com/
CTX_API_KEY: <your API key>
PROJECT_NAME: <project name>

# GitHub (needed for CLI queries)
GITHUB_ORG: <owner>
GITHUB_REPO: <repo>
```

> **Note:** For Path A, you do not need `GH_PAT` or `DATA_VOLUME` — those are only required for data loading (Path B). You just need the CTX connection details to query existing data.

### A4. Verify data is loaded

This is the critical check — confirm the knowledge graph has data for this repo:

```bash
tabnine-ctx-loader query entities | python3 -c "
import sys,json; entities=json.load(sys.stdin); types={}
for e in entities: t=e['type']; types[t]=types.get(t,0)+1
for t,c in sorted(types.items()): print(f'{t}: {c}')
print(f'Total: {len(entities)}')"
```

**If you see entities** (Services, Flows, Libraries, CodeHotspots, CodeExperts, etc.), the data is loaded and you can proceed to step A5.

**If the result is empty or shows very few entities:**
- The repo may not have been onboarded to the tenant associated with your API key
- The onboarding may have been incomplete (e.g., submodules missed — see [Key Lessons Learned](#key-lessons-learned))
- You may need to follow [Path B](#path-b-step-by-step-onboarding) to load the data

### A5. Start querying

Open the project in VS Code with Claude Code. You now have three ways to query:

**Slash commands (easiest):**
- `/investigate-service` — deep-dive a specific service
- `/blast-radius` — assess impact of a change
- `/understand-flow` — trace a business flow end-to-end
- `/search-knowledge` — semantic search across the knowledge graph
- `/ctx` — general-purpose knowledge graph query

**MCP tools (from Claude Code):**
- `mcp__tabnine-ctx-cloud__find_entities` — semantic search for entities
- `mcp__tabnine-ctx-cloud__get_service` — service details and relationships
- `mcp__tabnine-ctx-cloud__get_flow` — flow details and participating services
- `mcp__tabnine-ctx-cloud__get_service_dependencies` — what a service depends on
- `mcp__tabnine-ctx-cloud__get_service_dependents` — what depends on a service
- `mcp__tabnine-ctx-cloud__list_services` — list all services

**CLI (fallback, always works):**
```bash
tabnine-ctx-loader query search "your question here"
tabnine-ctx-loader query entities
tabnine-ctx-loader query entities --type Service
tabnine-ctx-loader query entity <entity-id>
```

> **Tip from experience:** MCP cloud tools can intermittently return HTTP 400 errors. The CLI fallback (`tabnine-ctx-loader query`) has been consistently reliable. See [Troubleshooting](#troubleshooting) for more details.

### A6. Combine Context Engine with local analysis

The Context Engine provides the architectural map — services, flows, dependencies, ownership, code hotspots. But for deep investigations (blast radius, cloud migration, implementation planning), you'll also need to read the local source code.

**What the Context Engine is best at:**
- "What services exist and how do they interact?"
- "What flows does this change affect?"
- "Who are the code experts?"
- "What are the hotspots and couplings in this codebase?"

**What requires local source analysis:**
- "What exact code needs to change?"
- "How does the auth mechanism work at the code level?"
- "What are the database schemas?"
- "Is this system cloud-ready?"

See the [Example Queries and Investigations](#example-queries-and-investigations) section for real examples showing this combination in action.

---

## Path B: Step-by-Step Onboarding

The easiest way to run onboarding is to invoke `/onboard` in Claude Code, which walks through all steps interactively using MCP tools. The commands below document what runs under the hood, for reference and for cases where you need to run steps manually.

### Step 0: Initialize

Validate connectivity and detect server capabilities.

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_0
```

**What to check:** If `supportedCredentialTypes` is empty or `agentCount` is 0, the tenant may need AI provider configuration. See [Troubleshooting](#troubleshooting).

### Step 1: Build Testing Lab

Analyze the repository and generate test cases.

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_1
  repo_path: /path/to/your/repo
```

Produces `test-plan.yaml` with 5-10 test cases across architecture, incident response, code intelligence, documentation, and workflow categories.

> **Note: Output directory discrepancy.** The upstream onboarding guide and some skill docs (e.g., `.claude/skills/onboard-init/SKILL.md`) reference `.tabnine-ctx-onboarding/` as the output directory. However, the actual onboarder writes all state and artifacts to **`.tabnine/ctx/onboarder/`** instead. Files you will find there include `test-plan.yaml`, `domain-model.yaml`, `state.json`, and the step answer JSON files. If you are looking for onboarding output and `.tabnine-ctx-onboarding/` is empty or doesn't exist, check `.tabnine/ctx/onboarder/`.

### Step 2: Load Project Data

This is the most critical step. You must ensure all code sources are represented in the loader manifest.

#### Step 2a: Check for submodules

**Before creating the manifest**, check if your repo contains git submodules:

```bash
cat .gitmodules
```

If submodules exist, each must be added as a separate source in the loader manifest. The loader does **not** follow submodules automatically.

#### Step 2b: Create and configure the loader manifest

```
MCP tool: mcp__tabnine-ctx-loader__loader_init
  template: "minimal"
  output: "tabnine-ctx-loader.yaml"
  owner: "ddreakford"
  repo: "multi-test-type-demo"
  resolve: true
```

Then edit `tabnine-ctx-loader.yaml` to add submodule repos as additional sources:

```yaml
workspaces:
  main:
    sources:
      - name: code
        type: github
        credential: github
        config:
          owner: ddreakford
          repo: multi-test-type-demo
          events:
            - push
            - pull_request
            - issues
        agents:
          - context-ingestor
          - dependency-mapper
          - github-pr-ingestor
          - github-issues-ingestor
          - git-insights-analyzer

      # Each submodule needs its own source entry
      - name: restful-booker-platform
        type: github
        credential: github
        config:
          owner: mwinteringham
          repo: restful-booker-platform
          events:
            - push
            - pull_request
            - issues
        agents:
          - context-ingestor
          - dependency-mapper
          - github-pr-ingestor
          - github-issues-ingestor
          - git-insights-analyzer
```

#### Step 2c: Validate and load

```bash
tabnine-ctx-loader validate
```

Then start the load:

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_2_start
  manifest: "tabnine-ctx-loader.yaml"
```

Poll for completion:

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_2_status
```

#### Step 2d: Verify entity count (critical checkpoint)

**Always verify after loading.** This is not part of the standard onboarding flow but is essential:

```bash
tabnine-ctx-loader query entities | python3 -c "
import sys,json; entities=json.load(sys.stdin); types={}
for e in entities: t=e['type']; types[t]=types.get(t,0)+1
for t,c in sorted(types.items()): print(f'{t}: {c}')
print(f'Total: {len(entities)}')"
```

**What to expect:** For a multi-project repo like multi-test-type-demo, you should see 15-30+ entities across types like Repository, Library, Manifest, CodeHotspot, CodeExpert, CodeCoupling, ModuleBoundary, and GitInsightsSnapshot. If you see fewer than 10 entities, something was likely missed.

**Red flags:**
- Only Repository and Library entities → agents may have failed (check `tabnine-ctx-loader status`)
- Missing Library entities for a known dependency → the manifest for that project was not scanned
- No CodeHotspot or CodeExpert entities → git-insights-analyzer may have failed

### Step 3: Baseline Without MCP

The agent answers test questions without Context Engine tools.

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_3_get_questions
```

The agent writes answers to `.tabnine/ctx/onboarder/step3-answers.json`, then submits:

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_3_submit
  responses_file: ".tabnine/ctx/onboarder/step3-answers.json"
```

### Step 4: Baseline With MCP

The agent re-answers using Context Engine MCP tools (`find_entities`, `list_services`, `search_knowledge`, etc.).

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_4_get_questions
```

Query the knowledge graph to build answers:

```
MCP tools used:
  mcp__tabnine-ctx-cloud__find_entities
  mcp__tabnine-ctx-cloud__list_services
```

Submit and get comparison against Step 3:

```
CLI: tabnine-ctx-onboard step-4 --responses .tabnine/ctx/onboarder/step4-answers.json --json
```

### Step 5: Domain Enrichment

Analyze the repository for domain-specific concepts.

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_5
  repo_path: /path/to/your/repo
```

**For repos with submodules, run enrichment on each directory separately:**

```
# First: top-level repo (covers rbp-test-demo/, docs/, etc.)
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_5
  repo_path: /path/to/multi-test-type-demo

# Then: each submodule
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_5
  repo_path: /path/to/multi-test-type-demo/restful-booker-platform
```

The top-level scan only processes files tracked by the top-level git repo. Submodule files are in a separate git tree and require a dedicated enrichment pass.

### Step 6: Measure With Domain

Re-answer test questions with domain enrichment active. Submit via CLI:

```bash
tabnine-ctx-onboard step-6 --responses .tabnine/ctx/onboarder/step6-answers.json --json
```

Produces a 3-way comparison (no MCP → MCP → MCP + domain).

### Step 7: Rollout Plan

Generate a phased adoption plan.

```
MCP tool: mcp__tabnine-ctx-onboard__onboard_step_7
```

---

## Before and After: Submodule Inclusion Impact

This comparison shows how explicitly including the `restful-booker-platform` submodule and running domain enrichment on it changed the onboarding results.

### Before: Initial onboarding (top-level repo only)

The initial data load only ingested `ddreakford/multi-test-type-demo`. The `restful-booker-platform` submodule was completely missed. The knowledge graph contained 10 entities (8 Libraries, 1 Manifest, 1 Repository) — all from `rbp-test-demo/build.gradle`.

**3-way comparison (initial):**

| Dimension | No MCP (Step 3) | With MCP (Step 4) | MCP + Domain (Step 6) |
|---|---|---|---|
| Relevance | 1.0 | 1.0 | 1.0 |
| Depth | 1.6 | 2.6 | 3.0 |
| Actionability | 0.0 | 0.0 | 0.0 |
| Accuracy | 2.0 | 2.0 | 2.0 |
| **Overall** | **4.6** | **5.6** | **6.0** |

- MCP over baseline: **+21.7%**
- Domain over baseline: **+30.4%**

### After: Full onboarding (submodule + domain enrichment)

After adding `mwinteringham/restful-booker-platform` as a second source and running domain enrichment on the submodule directory, the knowledge graph grew to 20 entities including CodeHotspots, CodeExpert, CodeCouplings, ModuleBoundaries, and GitInsightsSnapshot. The domain model extracted 136 entities from the platform's 227 files, identifying service-level components (Room, Booking, Message, Report, Auth, Notification).

**3-way comparison (after submodule inclusion):**

| Dimension | No MCP (Step 3) | With MCP (Step 4) | MCP + Domain (Step 6) |
|---|---|---|---|
| Relevance | 1.0 | 1.0 | 1.0 |
| Depth | 1.6 | 3.0 | 3.0 |
| Actionability | 0.0 | 0.0 | 0.0 |
| Accuracy | 2.0 | 2.0 | 2.0 |
| **Overall** | **4.6** | **6.0** | **6.0** |

- MCP over baseline: **+30.4%** (was +21.7%)
- Domain over baseline: **+30.4%**

### What changed

| Metric | Before | After | Impact |
|---|---|---|---|
| Knowledge graph entities | 10 | 20 | 2x more entities |
| Domain entities extracted | 147 (top-level only) | 147 + 136 (with submodule) | Platform service structure captured |
| Step 4 (MCP only) depth | 2.6 | 3.0 | +15.4% — richer data improved MCP-only answers |
| Step 4 overall | 5.6 | 6.0 | MCP-only reached parity with domain-enriched |

The key insight: **including the submodule data raised MCP-only (Step 4) scores to match the domain-enriched (Step 6) scores.** The additional CodeHotspot, CodeExpert, CodeCoupling, and ModuleBoundary entities provided enough context that domain enrichment was no longer the differentiator — the data itself was.

---

## Key Lessons Learned

### 1. Git submodules are not automatically followed

The loader ingests repos via the GitHub API. Submodules are separate repositories and must each be added as an explicit source in `tabnine-ctx-loader.yaml`. **Always check for `.gitmodules` before creating the manifest.**

```bash
cat .gitmodules  # Check for submodules
git submodule status  # See which submodules exist and their repos
```

### 2. Run domain enrichment separately on submodule directories

Step 5 scans files tracked by the git repo at the specified path. The top-level repo's git tree does not include submodule file contents — it only records the submodule commit pointer. To analyze a submodule's code, run Step 5 again with `repo_path` pointed at the submodule directory.

### 3. Verify entity count after every data load

This is the single most important diagnostic step not included in the standard onboarding flow. After Step 2 completes, immediately check:

```bash
tabnine-ctx-loader query entities | python3 -c "
import sys,json; entities=json.load(sys.stdin)
types={}
for e in entities: t=e['type']; types[t]=types.get(t,0)+1
for t,c in sorted(types.items()): print(f'{t}: {c}')
print(f'Total: {len(entities)}')"
```

If the count is unexpectedly low, investigate before proceeding to Steps 3-4. Low entity counts produce low scores that misrepresent the Context Engine's value.

### 4. After a tenant factory reset, the AI provider must be reconfigured

If you reset the Context Engine tenant (`tabnine-ctx-loader reset --confirm`), all server-side configuration is wiped — including the AI provider (e.g., Anthropic API key). Agents will fail with **"Not logged in · Please run /login"** until the AI provider is reconfigured on the Tabnine side.

**Action:** Contact your Tabnine administrator to reconfigure the AI provider after any tenant reset.

### 5. Check agent status, not just load status

The load can report `status: completed` even when all agents failed. Always check the per-agent status:

```bash
tabnine-ctx-loader status --summary false
```

Look for `"status": "failed"` on individual agents. A load where all agents failed produces only a bare Repository entity shell.

### 6. Data volume does not limit code analysis scope

The `DATA_VOLUME` setting (`ultra-light`, `light`, `standard`, `full`) controls how much event history (commits, PRs, issues) is ingested. The `context-ingestor` agent always scans the full repository tree. Don't assume `light` means shallow analysis.

### 7. Include documentation directories

The `docs/` directory was recognized as a ModuleBoundary with high cohesion (0.75). Documentation files appeared as CodeHotspots (the Setup Guide had the highest churn in the entire repo). Including docs in the analysis provides valuable context for workflow and incident response questions.

---

## Example Queries and Investigations

After onboarding, the Context Engine knowledge graph can answer questions about your project's architecture, services, flows, dependencies, and code intelligence. This section summarizes real queries performed against the multi-test-type-demo knowledge graph. Full details — including the exact tools used, raw results, and lessons learned — are tracked in [context-engine-queries.md](context-engine-queries.md).

### Q1: Investigate the room reservation flow

**Question:** How does the flow to reserve a room work? It's initiated by clicking the Booking control on the UI and flows through backend services.

**Tools used:**
- `tabnine-ctx-loader query search "booking room reservation"` — found a `Flow` entity named "Create Booking"
- `tabnine-ctx-loader query search "Create Booking flow"` — returned full flow details plus related flows
- `tabnine-ctx-loader query entities` (filtered for Flow/Service types) — revealed the complete service map

**What the Context Engine returned:**

The knowledge graph contained a "Create Booking" flow entity showing that the flow is initiated by the UI Service (guest user), passes through `room-service` to get available rooms, then hits `booking-service` to create the reservation. It also revealed the UI Service orchestrates 10 total flows across 6 backend services (auth, booking, room, message, report, branding).

**Key takeaways:** 
- Flow-level architecture queries work well when the relevant repos are loaded. This data was only available after explicitly adding the `restful-booker-platform` submodule as a second source — see [Before and After: Submodule Inclusion Impact](#before-and-after-submodule-inclusion-impact). 
- When MCP tools return errors, the CLI fallback (`tabnine-ctx-loader query search`) is reliable. 
- Searching by specific entity names yields higher similarity scores than searching by concept.

### Q2: Blast radius — Block rooms for special events

**Question:** Assess the blast radius of adding a "block rooms for special events" feature to the booking service, with both a third-party API and admin UI.

**Data sources required:** Both Context Engine and local source analysis.

**What the Context Engine provided:**
- Service graph (6 services), flow map (7 flows), and UI Service orchestration (10 initiated flows) — gave the architectural overview in seconds
- Identified that booking-service is called by both room-service and report-service, making it a high-impact change target

**What required local source analysis:**
- Exact API endpoints and auth requirements per service
- The critical conflict detection SQL in `BookingDB.checkForBookingConflict()` (lines 142-173) that must be modified
- Cross-service HTTP call patterns (reconstructed from `*Requests.java` classes — the knowledge graph lacked explicit dependency edges)
- Database schemas, UI component structure, Next.js proxy routes
- An existing SQL injection vulnerability discovered during the analysis

**Blast radius summary:** 5 of 7 flows impacted, all 6 services touched (2 major, 2 moderate, 1 minor, 1 none). The highest-risk change is modifying the booking conflict detection logic. The test automation (23 tests in rbp-test-demo) will also need new and modified tests.

**Key takeaway:** The Context Engine provides the architectural map quickly; local source provides the code-level detail needed for a thorough blast radius assessment. Both are essential. The knowledge graph would benefit from explicit inter-service dependency edges (`CALLS_HTTP`, `DEPENDS_ON`) to reduce the need for local source analysis.

### Q3: Cloud migration feasibility — AWS or GCP

**Question:** Is the booking system deployable on cloud infrastructure (AWS/GCP) now? What aspects need to change, and what expertise is required?

**What the Context Engine provided:**
- Service inventory (7 services with roles and ports) and flow map (7 business flows) — gave the architectural overview of what needs to be deployed and how services interact

**What the Context Engine did NOT have:**
- No infrastructure entities (Docker, Kubernetes, Terraform, database configuration). The knowledge graph returned 0 results for deployment/infrastructure queries. This investigation was ~90% local source analysis.

**What local source analysis revealed:**
- **Not cloud-deployable as-is.** Critical blockers: H2 in-memory databases in all 6 backend services (data lost on restart), hardcoded ports in inter-service calls, auth tokens in ephemeral storage, logs to filesystem instead of stdout, no Kubernetes manifests or IaC.
- **Partially cloud-ready:** Dockerized services with Alpine images, environment-based service discovery, Spring Boot Actuator health endpoints, per-service CI/CD workflows.
- **9 roles identified** for the migration team: cloud/platform engineer, backend Java dev, frontend dev, DevOps, QA, security engineer, solutions architect, technical writer, and business analyst.

**Key takeaway:** The Context Engine is valuable for understanding *what services exist and how they interact* (the architectural map), but cloud migration feasibility requires deep analysis of infrastructure configuration (Dockerfiles, docker-compose.yml, database config, CI/CD, networking) that the knowledge graph does not currently capture. Infrastructure entity types (Container, Database, Deployment) would make the Context Engine significantly more valuable for this class of question.

> **For the full query log**, see [context-engine-queries.md](context-engine-queries.md). Add new entries there as you perform additional investigations — this builds a library of example queries that helps team members learn what the Context Engine can answer and how to ask effectively.

---

*Generated from the actual onboarding of multi-test-type-demo. For the general onboarding methodology, see the [upstream onboarding guide](https://github.com/codota/ctx-customer-pack-distributable/blob/main/docs/onboarding-guide.md).*

---

## Troubleshooting

### "Not logged in · Please run /login" on all agents

**Cause:** AI provider not configured on the tenant (common after factory reset).
**Fix:** Contact Tabnine to reconfigure the AI provider (Anthropic API key).

### Low entity count after loading

**Cause:** Submodules not included, or agents silently failed.
**Fix:**
1. Check `.gitmodules` for submodule repos
2. Add each submodule as a separate source in `tabnine-ctx-loader.yaml`
3. Check agent status: `tabnine-ctx-loader status --summary false`
4. Re-run: `tabnine-ctx-loader load`

### MCP tools return HTTP 400

**Cause:** Transient server issue or malformed query.
**Fix:** Retry the query. If persistent, use the CLI fallback: `tabnine-ctx-loader query search "your query"`

### Step 6 MCP tool returns instructions instead of scoring

**Cause:** The MCP tool for Step 6 expects answers to be submitted separately.
**Fix:** Use the CLI to submit: `tabnine-ctx-onboard step-6 --responses .tabnine/ctx/onboarder/step6-answers.json --json`
