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
