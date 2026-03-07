# Notification Prioritization Engine - System Workflow

## Overview

This document describes the end-to-end lifecycle of a notification event as it flows through the engine.

## Step-by-Step Flow

1.  **Ingestion (POST /api/events)**:
    - Receive raw event with `user_id`, `message`, `event_type`, etc.
    - Validate payload and persist to `events` table.

2.  **Deduplication Check**:
    - **Exact Match**: Check `dedupe_key` against recent events.
    - **Similarity Match**: Use cosine similarity for near-duplicate detection.
    - **Outcome**: If duplicate, decision = `NEVER`, reason = "duplicate".

3.  **Alert Fatigue Check**:
    - Check user's recent notification count (cached).
    - Limit: 5 notifications per 10 minutes.
    - **Outcome**: If threshold exceeded, decision = `LATER`, reason = "alert fatigue".

4.  **Rule Engine Evaluation**:
    - Match event against stored rules based on `event_type`, `priority_hint`, `source`.
    - Rules are evaluated by priority and return `NOW`, `LATER`, or `NEVER`.
    - **Outcome**: If rule matches, follow rule action.

5.  **AI Classification (Gemini/OpenAI)**:
    - If no rule matches, invoke LLM via asynchronous call (wrapped with resilience4j).
    - Prompt asks AI to categorize based on urgency.
    - **Circuit Breaker**: If AI service is down/slow, fallback to `priority_hint` heuristic.

6.  **Final Decision & Audit Log**:
    - Persistent record in `audit_logs` table including rationale.
    - If `LATER`, entry added to `later_queue`.

7.  **Deferred Processing**:
    - Scheduled job runs every 5 minutes to re-evaluate `LATER` tasks.

## Metrics and Dashboard Counters

The dashboard metrics endpoint (`GET /api/metrics`) is built from persisted decision history:

- `now` = count of `NOW` decisions in `audit_logs`
- `later` = count of `LATER` decisions in `audit_logs`
- `never` = count of `NEVER` decisions in `audit_logs`
- `total` = `max(events.count, now + later + never)`

This definition prevents under-reporting when `events` rows are cleaned up or drift from audit history.
