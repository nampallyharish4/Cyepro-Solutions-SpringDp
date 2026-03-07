# Notification Prioritization Engine - Architecture Decisions

## Decisions and Rationale

1.  **Spring Boot + Java 17 Backend**:
    - Chosen for robustness, ecosystem support (Spring Data JPA, Resilience4j), and multi-threading capabilities for asynchronous ingestion.

2.  **Next.js 14 Frontend (App Router)**:
    - Leveraging React Server Components (RSC) for better performance and easier data fetching.
    - Mobile-first approach for accessibility.

3.  **Supabase PostgreSQL + JWT**:
    - Simplifies storage, authentication, and hosting.
    - Uses PG's robust JSONB for metadata and Rule Engine condition flexibility.

4.  **Caffeine Cache for Fatigue Check**:
    - In-memory cache for speed, minimizing DB lookups for high-frequency user checks. In a multi-instance production setup, would scale to Redis.

5.  **LLM Classification (Gemini/OpenAI)**:
    - Provides sophisticated prioritization for non-rule-based notifications.
    - Integrated with Resilience4j to ensure core functionality (NOW/LATER/NEVER) is maintained even during LLM outages.

6.  **Rule Engine (Database Persistent)**:
    - Enables "live" updates to logic without backend redeployments.
    - Logic uses a simple JSON DSL based on event fields.

7.  **Later Queue (Table-based)**:
    - Ensuring persistence for deferred notifications, robust across restarts.
    - Scheduled processing for asynchronous retry.
