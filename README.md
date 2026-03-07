# Notification Prioritization Engine

A high-performance system for prioritizing notifications using a multi-stage decision pipeline (Deduplication, Rule Engine, Alert Fatigue, AI Classification).

## High-Level Architecture

- **Backend**: Java 17 + Spring Boot 3 + JPA + Resilience4j + Caffeine Cache
- **Frontend**: Next.js 14 (App Router) + TailwindCSS + React Query
- **Database**: Supabase PostgreSQL + Auth
- **AI**: OpenAI/Gemini for categorization
- **Deployment**: Vercel (Frontend), Railway/Render (Backend)

## Core Decision Pipeline

1.  **Deduplication**: Exact and Similarity Matches.
2.  **Rule Engine**: Domain-specific logic (e.g., Security -> NOW).
3.  **Alert Fatigue**: Limits per user (5 per 10m).
4.  **AI Analysis**: LLM-based categorization with fallback logic.
5.  **Audit Log**: Full traceability of every decision.

## Getting Started (Backend)

### Prerequisites

- Java 17+
- Maven
- Supabase/PostgreSQL instance

### Environment Variables

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `AI_API_KEY`

### Running Locally

```bash
./mvnw clean install
./mvnw spring-boot:run
```

## Getting Started (Frontend)

### Prerequisites

- Node.js 18+
- npm/yarn

### Environment Variables

- `NEXT_PUBLIC_API_URL`
- `NEXT_PUBLIC_SUPABASE_URL`
- `NEXT_PUBLIC_SUPABASE_ANON_KEY`

### Running Locally

```bash
cd frontend
npm install
npm run dev
```

## Documentation

- [System Workflow](./backend/docs/SYSTEM_WORKFLOW.md)
- [Architecture Decisions](./backend/docs/ARCHITECTURE_DECISIONS.md)
- [Deployment Guide](./backend/docs/DEPLOYMENT.md)
- [Database Schema](./backend/scripts/supabase_schema.sql)

## Metrics Contract

`GET /api/metrics` returns these dashboard counters:

- `now`: total decisions marked `NOW` from `audit_logs`
- `later`: total decisions marked `LATER` from `audit_logs`
- `never`: total decisions marked `NEVER` from `audit_logs`
- `total`: max of:
  - persisted rows in `events`
  - `now + later + never`

This keeps total events accurate even when legacy/cleanup behavior causes `events` row counts to differ from processed audit history.

## Troubleshooting

If the dashboard card **Total Events** still shows `0`:

1. Restart backend on the active project path: `D:\Cyepro Spring\backend`
2. Verify API response:
   - `http://127.0.0.1:5000/api/metrics`
   - confirm `total` is non-zero in JSON
3. Refresh the frontend page after backend restart.
