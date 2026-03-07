# Notification Prioritization Engine - Deployment

## Prerequisites

- **Supabase PostgreSQL URL**
- **OpenAI/Gemini API Key**
- **Vercel account**
- **Railway/Render account**

## Backend Deployment (Railway/Render)

1. Connect your repository.
2. Set the following Environment Variables:
   - `DB_URL`: `jdbc:postgresql://<supabase_host>:5432/postgres`
   - `DB_USERNAME`: `postgres`
   - `DB_PASSWORD`: `<your-supabase-password>`
   - `AI_API_KEY`: `<your-openai-api-key>`
3. Set the build command: `./mvnw clean install -DskipTests`
4. Set the start command: `java -jar target/cyepro-stack2-0.0.1-SNAPSHOT.jar`

## Backend Deployment (Render - Recommended)

Use this repository as a monorepo and deploy from `backend` root.

1. In Render, create a **Web Service** from `nampallyharish4/Cyepro-Spring`.
2. Configure:
   - Root Directory: `backend`
   - Runtime: `Java` (or Render native build)
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -jar target/cyepro-stack2-0.0.1-SNAPSHOT.jar`
   - Health Check Path: `/health`
3. Add environment variables:
   - `DB_URL` = `jdbc:postgresql://aws-1-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require`
   - `DB_USERNAME` = `postgres.llvedrcxpocdpguhxkql`
   - `DB_PASSWORD` = `<supabase-db-password>`
   - `AI_API_KEY` = `<groq-api-key>`
   - `AI_API_URL` = `https://api.groq.com/openai/v1/chat/completions`
   - `AI_MODEL` = `llama-3.3-70b-versatile`
   - `SUPABASE_URL` = `https://llvedrcxpocdpguhxkql.supabase.co`
   - `SUPABASE_SERVICE_ROLE_KEY` = `<supabase-service-role-key>`
   - `SUPABASE_JWT_SECRET` = `<supabase-jwt-secret>`
4. Deploy and verify endpoints:
   - `GET /health`
   - `GET /api/metrics`

### Optional: Blueprint Deploy

This repository now includes `render.yaml` at root. You can use **Blueprint** deploy in Render to auto-create the backend service with the same build/start/health settings.

## Frontend Deployment (Vercel)

1. Connect your repository's `frontend` folder.
2. Set the following Environment Variables:
   - `NEXT_PUBLIC_API_URL`: `<your-backend-railway-url>`
   - `NEXT_PUBLIC_SUPABASE_URL`: `<your-supabase-url>`
   - `NEXT_PUBLIC_SUPABASE_ANON_KEY`: `<your-supabase-anon-key>`
3. Deploy!

## Database Setup

1. Run the `supabase_schema.sql` script (located in `scripts/`) in your Supabase SQL Editor.
2. (Optional) Run the rule seeding script to initialize the Rule Engine logic.

## Runtime Verification

After deployment/startup, validate service health and metrics:

1. Health endpoint: `GET /health`
2. Metrics endpoint: `GET /api/metrics`
3. Ensure `total` in metrics response reflects activity and is not unexpectedly `0`.

## Common Issue: Total Events = 0

If `now/later/never` are non-zero but `total` is `0`, verify you are running the latest backend build from `D:\Cyepro Spring\backend` and restart the service. The current metrics contract calculates `total` as `max(events.count, now + later + never)`.
