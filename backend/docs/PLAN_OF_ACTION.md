# Notification Prioritization Engine - Plan of Action

## Phase 1: Backend Setup
 - [x] Define PostgreSQL schema (Supabase).
 - [x] Create Spring Boot project structure.
 - [x] Implement Entity, DTO, Repository layers.
 - [x] Configure `application.properties` with Supabase/OpenAI credentials.

## Phase 2: Core Processing Logic
 - [x] Implement Ingestion Controller.
 - [x] Implement Deduplication (Exact Match and Cosine Similarity).
 - [x] Implement AI Service (OpenAI integration with Resilience4j).
 - [x] Implement Rule Engine (Dynamic rule matching).
 - [x] Implement Alert Fatigue logic (Caffeine Cache).

## Phase 3: Post-Processing & Persistence
 - [x] Implement Audit Logging (Append-only).
 - [x] Implement LATER Queue and Scheduler.
 - [x] Implement Health Endpoint.

## Phase 4: Frontend Development
 - [ ] Scaffold Next.js 14 App Router project.
 - [ ] Implement Auth (Supabase Auth).
 - [ ] Create Dashboard Home (Metrics and Health).
 - [ ] Create Event Simulator (Tool for testing engine).
 - [ ] Create Event Audit Log Viewer (Filterable/Searchable).
 - [ ] Create Rules Manager (CRUD for rules).
 - [ ] Implement Live Data Polling/WebSockets.

## Phase 5: Deployment
 - [ ] Backend: Deploy to Railway/Render with Environment Variables.
 - [ ] Frontend: Deploy to Vercel.
 - [ ] Database: Finalize Supabase Production project.
