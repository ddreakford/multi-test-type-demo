# Tabnine Context Engine

IMPORTANT: The customer pack is fully installed. Do not explore, search, or read files outside this directory. Do not look for the customer-pack source repository, installation scripts, or parent directories — they do not exist on customer machines. Everything you need is here and already loaded into your context.

## Setup

The only setup required is creating `ctx-settings.yaml` in this directory (if it doesn't already exist):

```yaml
CTX_API_URL: <Context Engine URL>
CTX_API_KEY: <your API key>
PROJECT_NAME: <project name>
GITHUB_ORG: <GitHub org or owner>
GITHUB_REPO: <repo name>
DATA_VOLUME: standard
GH_PAT: <GitHub PAT>
```

All CLIs read this file automatically. No environment variable exports needed.

## Tools available to you right now

Skills (invoke via slash command or Skill tool — do NOT read the skill files, they are already in your context):
`/ctx`, `/onboard`, `/investigate-service`, `/blast-radius`, `/review-pr`, `/search-knowledge`, `/incident-response`, `/understand-flow`, `/change-confidence-tools`, `/ownership-tools`, `/git-insights-tools`, `/architecture-tools`, `/code-migration`, `/dependency-check`

MCP tools (call directly — the MCP server is already configured):
`mcp__tabnine-ctx-cloud__search_knowledge`, `mcp__tabnine-ctx-cloud__query_entities`, `mcp__tabnine-ctx-cloud__blast_radius`, `mcp__tabnine-ctx-cloud__investigate_service`, `mcp__tabnine-ctx-cloud__get_change_confidence`, `mcp__tabnine-ctx-cloud__get_service`, `mcp__tabnine-ctx-cloud__get_service_dependencies`

For querying the knowledge graph, ALWAYS use `mcp__tabnine-ctx-cloud__*` tools (search_knowledge, query_entities, blast_radius, investigate_service, etc.)

For data loading: `mcp__tabnine-ctx-loader__loader_init`, `mcp__tabnine-ctx-loader__loader_load`, `mcp__tabnine-ctx-loader__loader_status`, `mcp__tabnine-ctx-loader__loader_diagnose`
For onboarding: `mcp__tabnine-ctx-onboard__onboard_step_0` through `mcp__tabnine-ctx-onboard__onboard_step_7`, `mcp__tabnine-ctx-onboard__onboard_status`

Fallback CLIs (if MCP is unavailable):
`tabnine-ctx-loader` (data loading), `tabnine-ctx-onboard` (onboarding), `tabnine-ctx-cli` (queries) — all read `ctx-settings.yaml` automatically.

## Onboarding a new project

Invoke `/onboard`. It walks through: validate connectivity → build test lab → load data → measure baseline → domain enrichment → rollout plan.

## Rules

- Credentials only from `ctx-settings.yaml` or environment variables. Never as CLI arguments.
- If data loading fails: `tabnine-ctx-loader diagnose --json`
- To collect debug logs for support: call `mcp__tabnine-ctx-loader__loader_collect_logs` or run `tabnine-ctx-loader collect-logs`
- Do not read files in `.claude/` — skills, hooks, MCP config, and scripts are already loaded and configured.
- Do not access parent directories or search for source repositories.
