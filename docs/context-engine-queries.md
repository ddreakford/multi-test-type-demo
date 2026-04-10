# Context Engine Queries and Investigations

This document tracks questions, queries, and investigations performed against the Context Engine knowledge graph for the multi-test-type-demo project. Each entry records the question asked, the tools and queries used, the data returned, and what we learned.

These are examples of some of the kinds of questions the Context Engine can answer and how to formulate effective queries.

---

## Table of Contents

- [Q1: Investigate the room reservation flow](#q1-investigate-the-room-reservation-flow)
- [Q2: Blast radius — Block rooms for special events](#q2-blast-radius--block-rooms-for-special-events)
- [Q3: Cloud migration feasibility — AWS or GCP](#q3-cloud-migration-feasibility--aws-or-gcp)

---

## Q1: Investigate the room reservation flow

**Date:** 2026-04-08

**Question:** Investigate the flow to reserve a room. This is initiated by clicking the Booking control on the UI and flows through one or more backend services. Does the Context Engine have the data to help with this investigation?

### Tools and queries used

**Step 1 — Search for booking/room flow entities:**

```
Tool: tabnine-ctx-loader query search "booking room reservation"
```

Returned 1 result: a `Flow` entity named "Create Booking" with `services: ["room-service", "booking-service"]`, initiated from the UI by a guest user.

**Step 2 — Search for full flow details:**

```
Tool: tabnine-ctx-loader query search "Create Booking flow"
```

Returned 3 flows: "Create Booking", "Booking Management", and "Room Management".

**Step 3 — List all Flow and Service entities:**

```
Tool: tabnine-ctx-loader query entities (filtered for Flow and Service types)
```

Returned the complete set of flows and the UI Service entity, which lists all 10 flows it initiates.

### Results

**Create Booking flow:**

| Property | Value |
|---|---|
| Flow name | Create Booking |
| Initiator | UI (guest user) |
| Services involved | `room-service`, `booking-service` |

**How it works:**

1. **UI Service** (front-end orchestrator, `restful-booker-platform/assets/`) — initiates the flow when a guest clicks the Booking control. React components involved: `AdminBooking`, `BookingListing`, `BookingDates`.
2. **room-service** (`restful-booker-platform/room/`) — queried to get available room details. Domain entities: Room, RoomDetails, RoomState, RoomData.
3. **booking-service** (`restful-booker-platform/booking/`) — receives the reservation request and creates the booking. Domain entities: AdminBooking, BookingListing, BookingDates.

**Related flows discovered:**

| Flow | Services | User Type |
|---|---|---|
| **Create Booking** | room-service, booking-service | Guest |
| Booking Management | booking-service | Admin |
| Room Management | room-service | Admin |
| Admin Login | auth-service | Admin |
| View Reports | report-service | Admin |
| Contact Message | message-service | Guest |
| Manage Branding | branding-service | Admin |

**UI Service orchestration:** The UI Service initiates 10 flows total: View Rooms, Create Booking, Send Message, Admin Login, Admin Logout, Manage Rooms, Manage Bookings, View Reports, Manage Branding, View Messages.

### What we learned

- The Context Engine successfully captured flow-level architecture from the restful-booker-platform, including which services participate in each flow and who initiates them.
- This flow data was only available after we added `mwinteringham/restful-booker-platform` as a second source in the loader manifest (it was missing from the initial load that only covered the top-level repo).
- The `find_entities` MCP tool was intermittently returning HTTP 400 errors during this query session; the CLI fallback (`tabnine-ctx-loader query search`) worked reliably.
- Searching by flow name (e.g., "Create Booking flow") returned higher similarity scores (0.74) than searching by concept (e.g., "booking room reservation" at 0.50). Use specific entity names when known.

---

## Q2: Blast radius — Block rooms for special events

**Date:** 2026-04-09

**Question:** Assess the blast radius of adding new functionality to the booking service: a third-party API and analogous admin UI functionality that allows administrators to specify a block of rooms available for special events.

### Data sources used

This investigation required **both** the Context Engine and local file analysis.

**Context Engine provided:**
- Service graph: identified all 6 services and their roles (booking, room, auth, message, report, branding)
- Flow map: identified all 7 flows and which services participate in each
- UI Service orchestration: confirmed the UI initiates 10 flows, showing full scope of frontend impact
- Inter-service relationships: booking-service is called by both room-service (availability) and report-service (booking data)

**Local source analysis provided:**
- Exact API endpoints and their auth requirements (e.g., `POST /booking/` is public, `PUT /booking/{id}` requires token)
- The booking conflict detection SQL logic in `BookingDB.checkForBookingConflict()` (lines 142-173) — the critical code that must be modified
- The room availability query chain: room-service calls `GET /booking/unavailable` on booking-service
- Auth mechanism details: cookie-based token validated via `POST /auth/validate` on port 3004
- Database schemas: H2 in-memory databases, table structures, no shared DB across services
- UI component structure: AdminBooking.tsx, BookingListing.tsx, and Next.js proxy routes
- Report service N+1 query pattern in `ReportService.getAllRoomsReport()`
- An existing SQL injection vulnerability in `BookingDB.queryByDate()` (line 201)

### Tools and queries used

**Step 1 — Context Engine: Service and flow discovery:**

```
Tool: tabnine-ctx-loader query entities (filtered for Flow and Service types)
```

Returned all flows with their participating services and the UI Service entity with its 10 initiated flows.

**Step 2 — Context Engine: Dependency search:**

```
Tool: tabnine-ctx-loader query search "booking service dependencies"
MCP tools: mcp__tabnine-ctx-cloud__get_service_dependencies (booking, room)
MCP tools: mcp__tabnine-ctx-cloud__get_service_dependents (booking, room)
```

MCP cloud tools returned HTTP 400 errors. CLI search returned no results for dependency-specific queries — the knowledge graph has Flow and Service entities but not explicit dependency edges. The dependency chain was reconstructed from local source analysis.

**Step 3 — Local source: Deep code analysis (via Explore agents):**

Two parallel investigations analyzed:
- Booking service: API (BookingController), models (Booking, BookingDates, CreatedBooking), service layer (BookingService), DB layer (BookingDB), outbound requests (AuthRequests, MessageRequests)
- Room service: API (RoomController), models (Room), service layer (RoomService), outbound requests (AuthRequests, BookingRequests)
- Report service: API (ReportController), service layer (ReportService), outbound requests (RoomRequests, BookingRequests)
- Auth service: API (AuthController), token model, DB schema
- UI admin components: AdminBooking.tsx, BookingListing.tsx, BookingListings.tsx, Next.js API routes
- Docker Compose: service ports and networking

### Results

**Services impacted (6 of 6):**

| Service | Impact | Key Changes |
|---|---|---|
| **booking-service** | MAJOR | New DB table, modify conflict detection, new API endpoints for block CRUD and third-party API |
| **UI Service (assets)** | MAJOR | New ManageSpecialEvents admin component, new Next.js proxy routes, calendar indicators |
| **room-service** | MODERATE | Availability queries may need to reflect blocked periods (currently delegates to booking-service) |
| **report-service** | MODERATE | Reports should surface blocked periods; existing N+1 pattern worsens |
| **auth-service** | NONE | Reuse existing token validation |
| **message-service** | MINOR | Optional event block/unblock notifications |

**Flows impacted (5 of 7):**

| Flow | Impact | Reason |
|---|---|---|
| Create Booking | HIGH | Conflict detection must prevent booking blocked rooms |
| Booking Management | HIGH | Admin must see/manage blocked periods |
| Room Management | MEDIUM | Room details should show active blocks |
| View Reports | MEDIUM | Reports should include blocked periods |
| View Rooms | LOW | Public listing should reflect reduced availability |

**Critical code touch points:**

| File | Lines | Risk |
|---|---|---|
| `booking/db/BookingDB.java` | 142-173 | HIGH — `checkForBookingConflict()` is the most complex query; must also check BLOCKED_PERIODS |
| `booking/db/BookingDB.java` | 199-223 | MEDIUM — `queryByDate()` must include blocked rooms in unavailability; also has existing SQL injection vulnerability |
| `booking/api/BookingController.java` | New | New endpoints for block CRUD |
| `report/service/ReportService.java` | `getAllRoomsReport()` | MEDIUM — N+1 query pattern; adding block queries worsens it |

**Key risks identified:**

1. **Booking conflict detection regression** (HIGH) — the conflict SQL is the most complex query in the codebase
2. **Cross-service eventual consistency** (MEDIUM) — no distributed transactions; blocks must be immediately visible in room availability
3. **Report service performance** (MEDIUM) — existing N+1 pattern would worsen with additional block queries
4. **Existing SQL injection** (LOW, pre-existing) — `BookingDB.queryByDate()` uses string concatenation

### What we learned

- **Context Engine + local source is the right combination.** The Context Engine provided the architectural map (services, flows, who calls whom) in seconds. But the blast radius assessment required reading the actual code to understand conflict detection logic, auth mechanisms, SQL schemas, and UI component structure.
- **The knowledge graph lacks explicit dependency edges.** While Flow entities list participating services, there are no `CALLS_HTTP` or `DEPENDS_ON` relationships between services. The inter-service call chain (room → booking, report → room + booking, booking → auth + message) was reconstructed from reading `*Requests.java` classes in each service.
- **MCP cloud tools were unreliable during this session** (HTTP 400 errors). The CLI fallback (`tabnine-ctx-loader query`) worked consistently. This is worth documenting for team members.
- **The blast radius extends to the test automation.** The 23 tests in rbp-test-demo will need new tests and modified regression tests to cover blocked period scenarios.

---

## Q3: Cloud migration feasibility — AWS or GCP

**Date:** 2026-04-09

**Question:** I'm considering making the hotel booking system easier to deploy onto cloud infrastructure (AWS or GCP). What aspects do I need to consider? Is the system deployable now, or would I need architectural/implementation changes? What expertise do the people involved need?

### Data sources used

**Context Engine provided:**
- Service inventory: 7 services with their roles and ports
- Flow map: 7 business flows showing service participation
- UI Service orchestration: 10 flows initiated, confirming the frontend is the primary entry point
- CodeHotspot data: `.github/workflows/test.yml` identified as a frequently changed CI file

**Context Engine did NOT have:**
- Infrastructure entities (no Docker, Kubernetes, Terraform, or cloud configuration data)
- Database configuration details
- Inter-service communication patterns at the code level
- Deployment architecture or environment configuration

**Local source analysis provided (essential for this investigation):**
- Docker Compose configuration: 7 service containers, ports, restart policies, no health checks, no volumes
- Dockerfiles: Alpine-based JRE 21 images, JVM container flags, Spring profile support, Honeycomb APM integration
- Database layer: H2 in-memory databases in all 6 backend services (hardcoded connection strings and credentials)
- Service discovery: environment variable-based domain names with hardcoded ports in `*Requests.java` classes
- Auth token storage: in-memory H2 (non-persistent, non-distributable)
- Logging: plain text files to container filesystem (not stdout)
- CI/CD: per-service GitHub Actions workflows pushing to Docker Hub
- Frontend: Next.js with environment-driven API proxy routes, multi-stage Docker build
- Application config: Spring Boot Actuator health endpoints, optional Honeycomb APM, dev/prod profiles
- Database scheduler: periodic reset feature (destructive in production)
- Build scripts: Maven multi-module build, local run via parallel JAR execution

### Tools and queries used

**Step 1 — Context Engine: Infrastructure search:**

```
Tool: tabnine-ctx-loader query search "deployment infrastructure docker CI/CD pipeline"
```

Returned 0 results. The knowledge graph has no infrastructure-level entities.

**Step 2 — Context Engine: Entity inventory:**

```
Tool: tabnine-ctx-loader query entities (filtered for Flow, Service, CodeHotspot)
```

Returned the service graph and flow map (useful for understanding what needs to be deployed and how services interact).

**Step 3 — Local source: Deep infrastructure analysis (via Explore agents):**

Two parallel investigations analyzed:
- Deployment architecture: docker-compose.yml, all Dockerfiles, parent pom.xml, application.properties per service, CI/CD workflows, build/run scripts, IaC (none found)
- Cloud readiness per service: database configuration (H2 in-memory), token storage, logging, health checks, inter-service communication, frontend proxy config

### Results

#### Is the system deployable on cloud infrastructure now?

**No.** The system is designed for local development and Docker Compose only.

**Critical blockers:**

| Blocker | Current State | Required Change |
|---|---|---|
| Databases | H2 in-memory per service (data lost on restart) | Migrate to managed DB (RDS PostgreSQL or Cloud SQL) |
| Service discovery | Env var domain names but hardcoded ports (`:3004`, `:3000`, etc.) | Kubernetes DNS or service mesh |
| Auth tokens | Stored in ephemeral H2 in auth-service | Distributed session store (Redis) or cloud-native identity |
| Logging | Plain text files in container filesystem | Structured JSON to stdout for CloudWatch/Stackdriver |
| Kubernetes/IaC | None exists | Create K8s manifests, Helm charts, Terraform modules |
| Database scheduler | Periodic DB reset (dev feature) | Remove or disable for production |
| Secrets | Hardcoded in env vars and Dockerfiles | AWS Secrets Manager or GCP Secret Manager |

**What's already cloud-friendly:**
- Dockerized services with Alpine images and JVM container support
- Environment variable-based service discovery (domain names configurable)
- Spring Boot Actuator health endpoints
- Per-service CI/CD workflows (supports independent deployment)
- Next.js standalone output mode

#### Aspects to consider

| Aspect | Details |
|---|---|
| **Compute** | 7 containers: 6 Java (Spring Boot, 128-384MB heap) + 1 Node.js (Next.js). EKS/GKE cluster sizing needed. |
| **Databases** | 5 separate schemas (auth, booking, room, message, branding). Report service is read-only. Could consolidate to 1-2 managed DB instances with separate schemas, or keep independent for service isolation. |
| **Networking** | Internal service mesh for inter-service HTTP. Load balancer/ingress for frontend. TLS termination at load balancer. |
| **CI/CD** | Existing GitHub Actions per service. Need to add: cloud registry push (ECR/GCR), K8s deployment steps, environment promotion. |
| **Observability** | Replace Honeycomb with cloud-native APM (X-Ray/Cloud Trace). Add structured logging. Add distributed tracing (OpenTelemetry). |
| **Security** | Unsecured actuator endpoints. Cookie-based auth without OAuth2. SQL injection vulnerability in BookingDB. No network policies. |
| **Cost** | Managed DB instances dominate cost. Estimate: 2 RDS/Cloud SQL instances + small K8s cluster + load balancer + monitoring. |

#### Expertise required

| Role | Key Skills | Scope |
|---|---|---|
| **Cloud/Platform Engineer** | EKS/GKE, RDS/Cloud SQL, Terraform, K8s manifests, networking, IAM | Infrastructure provisioning, IaC, K8s config |
| **Backend Java Developer** | Spring Boot, JPA, Flyway/Liquibase, PostgreSQL, HikariCP, structured logging, Redis | Database migration, config externalization, fix hardcoded ports, health checks |
| **Frontend Developer** | Next.js, runtime env injection, API gateway, CDN | Refactor build-time config to runtime, cloud LB integration |
| **DevOps/CI-CD Engineer** | GitHub Actions, Docker registries, Helm, GitOps | Update pipelines for cloud registry + K8s deployment |
| **QA/Test Engineer** | REST Assured, Selenium, TestNG, cloud test execution | Validate post-migration behavior, extend tests for cloud env |
| **Security Engineer** | OAuth2/OIDC, cloud IAM, network policies, TLS, secrets management | Auth modernization, secure actuator, fix SQL injection, network policies |
| **Solutions Architect** | Microservice patterns, 12-factor app, cloud cost modeling, migration planning | Target architecture design, build-vs-buy decisions, migration plan |
| **Technical Writer** | Runbooks, operational procedures, architecture documentation | Deployment runbooks, monitoring/alerting guides, architecture overview |
| **Business Analyst / Finance** | Cloud cost modeling, TCO analysis, ROI, vendor comparison | Business case: current vs cloud costs, migration investment, break-even timeline |

**Minimum viable team:** Cloud/platform engineer + backend Java developer + QA engineer for a basic migration. Architect, security, and technical writer roles become essential for production readiness.

### What we learned

- **The Context Engine's value for this question was limited to the service/flow map.** It quickly provided the architectural overview (7 services, 7 flows, inter-service relationships) but had no infrastructure, deployment, or configuration data. The knowledge graph would benefit from infrastructure entity types (Container, Database, Deployment, Network).
- **This investigation was 90% local source analysis.** Docker Compose, Dockerfiles, application.properties, database configuration, CI/CD workflows, and inter-service communication code all required reading local files. The Context Engine gave us the "what" (service inventory) but not the "how" (deployment architecture).
- **The H2 in-memory database pattern is the single biggest blocker.** Every service uses it, every service loses data on restart, and there is no external database support. This affects horizontal scaling, high availability, disaster recovery, and data persistence — all fundamental cloud requirements.
- **Service discovery is partially cloud-ready.** The environment variable pattern for domain names is good, but hardcoded ports in every `*Requests.java` class would need to change for Kubernetes where services typically expose port 80.
- **For cloud migration assessments, the Context Engine would be significantly more valuable if it ingested Dockerfiles, docker-compose.yml, Kubernetes manifests, Terraform files, and CI/CD workflows as first-class entities** — with relationships like "Service X is deployed as Container Y" and "Database Z is used by Service X".

---

*Add new entries below as additional questions and investigations are performed.*
