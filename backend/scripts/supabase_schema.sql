-- Tables for Notification Prioritization Engine

-- Users Table (Simplified for Demo, usually managed by Supabase Auth)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY, -- REFERENCES auth.users ON DELETE CASCADE if using Supabase Auth
    email TEXT UNIQUE,
    role TEXT DEFAULT 'operator', -- admin, operator
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Events Table
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    message TEXT NOT NULL,
    source TEXT NOT NULL,
    priority_hint TEXT NOT NULL, -- HIGH, MEDIUM, LOW
    channel TEXT,
    metadata JSONB,
    dedupe_key TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Rules Table
CREATE TABLE IF NOT EXISTS rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    condition_json JSONB NOT NULL,
    action TEXT NOT NULL, -- NOW, LATER, NEVER
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- AI Analysis Table
CREATE TABLE IF NOT EXISTS ai_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID REFERENCES events(id),
    classification TEXT NOT NULL,
    confidence FLOAT NOT NULL,
    reason TEXT,
    model_used TEXT,
    fallback_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Audit Logs Table (Append-only)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID REFERENCES events(id),
    decision TEXT NOT NULL, -- NOW, LATER, NEVER
    rule_id UUID REFERENCES rules(id),
    reason TEXT NOT NULL,
    ai_analysis_id UUID REFERENCES ai_analysis(id),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- LATER Queue Table
CREATE TABLE IF NOT EXISTS later_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID REFERENCES events(id),
    retry_count INTEGER DEFAULT 0,
    next_run_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status TEXT DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Dead Letter Queue Table
CREATE TABLE IF NOT EXISTS dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID REFERENCES events(id),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Metrics Table
CREATE TABLE IF NOT EXISTS metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name TEXT NOT NULL,
    metric_value FLOAT NOT NULL,
    dimensions JSONB,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indices for performance
CREATE INDEX idx_events_dedupe_key ON events(dedupe_key);
CREATE INDEX idx_audit_logs_event_id ON audit_logs(event_id);
CREATE INDEX idx_later_queue_next_run ON later_queue(next_run_at) WHERE status = 'PENDING';
