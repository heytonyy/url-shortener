-- ========================================
-- URL SHORTENER - DATABASE SCHEMA
-- PostgreSQL 14+
-- ========================================

-- Extension for UUID generation (optional, if you want UUIDs for session tokens)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ========================================
-- 1. USERS TABLE
-- ========================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, -- bcrypt hash
    username VARCHAR(50) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Constraints
    CONSTRAINT email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT username_format CHECK (username ~* '^[a-zA-Z0-9_-]{3,50}$')
);

-- Indexes for users
CREATE INDEX idx_users_email ON users(email) WHERE is_active = TRUE;
CREATE INDEX idx_users_username ON users(username) WHERE username IS NOT NULL;
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- ========================================
-- 2. URLS TABLE (Core table)
-- ========================================
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    short_code VARCHAR(20) NOT NULL UNIQUE, -- The generated hash
    original_url TEXT NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL, -- NULL for guest URLs
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP, -- NULL means never expires
    click_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB DEFAULT '{}', -- For tags, description, etc.
    
    -- Constraints
    CONSTRAINT url_format CHECK (original_url ~* '^https?://'),
    CONSTRAINT short_code_format CHECK (short_code ~* '^[a-zA-Z0-9_-]+$'),
    CONSTRAINT positive_clicks CHECK (click_count >= 0)
);

-- Critical indexes for urls
CREATE UNIQUE INDEX idx_urls_short_code ON urls(short_code) WHERE is_active = TRUE;
CREATE INDEX idx_urls_user_id ON urls(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_urls_created_at ON urls(created_at DESC);
CREATE INDEX idx_urls_expires_at ON urls(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_urls_metadata_gin ON urls USING gin(metadata); -- For JSONB queries

-- Partial index for active, non-expired URLs (most common query)
-- Note: Removed time-based predicate since NOW() is not immutable
CREATE INDEX idx_urls_active_short_code ON urls(short_code) 
WHERE is_active = TRUE;

-- ========================================
-- 3. CUSTOM_ALIASES TABLE
-- Allows users to have custom short codes like "my-blog"
-- ========================================
CREATE TABLE custom_aliases (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    custom_code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Constraints
    CONSTRAINT custom_code_format CHECK (custom_code ~* '^[a-zA-Z0-9_-]{3,50}$'),
    
    -- One active custom alias per URL
    CONSTRAINT unique_active_custom_per_url UNIQUE (url_id, custom_code) 
        DEFERRABLE INITIALLY DEFERRED
);

-- Indexes for custom_aliases
CREATE UNIQUE INDEX idx_custom_aliases_code ON custom_aliases(custom_code) WHERE is_active = TRUE;
CREATE INDEX idx_custom_aliases_url_id ON custom_aliases(url_id);

-- ========================================
-- 4. ANALYTICS TABLE
-- Track every click for analytics
-- ========================================
CREATE TABLE analytics (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES urls(id) ON DELETE CASCADE,
    clicked_at TIMESTAMP NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45), -- IPv6 support
    user_agent TEXT,
    referer TEXT,
    country VARCHAR(2), -- ISO country code
    city VARCHAR(100),
    device_info JSONB DEFAULT '{}', -- Browser, OS, device type, etc.
    
    -- Partition key hint for future partitioning by date
    CONSTRAINT clicked_at_not_future CHECK (clicked_at <= NOW() + INTERVAL '1 minute')
);

-- Indexes for analytics (read-heavy queries)
CREATE INDEX idx_analytics_url_id ON analytics(url_id, clicked_at DESC);
CREATE INDEX idx_analytics_clicked_at ON analytics(clicked_at DESC);
CREATE INDEX idx_analytics_country ON analytics(country) WHERE country IS NOT NULL;

-- Composite index for common analytics queries
-- Note: Removed time-based predicate since NOW() is not immutable
CREATE INDEX idx_analytics_url_date_range ON analytics(url_id, clicked_at);

-- ========================================
-- 5. SESSIONS TABLE
-- For user authentication (JWT tokens)
-- ========================================
CREATE TABLE sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_token VARCHAR(255) NOT NULL UNIQUE, -- JWT or UUID
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    CONSTRAINT expires_after_creation CHECK (expires_at > created_at)
);

-- Indexes for sessions
-- Note: Removed time-based predicate since NOW() is not immutable
CREATE UNIQUE INDEX idx_sessions_token ON sessions(session_token) 
WHERE is_active = TRUE;
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires_at ON sessions(expires_at) WHERE is_active = TRUE;

-- ========================================
-- 6. HASH RANGE ALLOCATIONS TABLE
-- For distributed short code generation
-- ========================================
CREATE TABLE range_allocations (
    id BIGSERIAL PRIMARY KEY,
    service_instance_id VARCHAR(100) NOT NULL, -- Container/pod ID
    start_range BIGINT NOT NULL,
    end_range BIGINT NOT NULL,
    allocated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    exhausted_at TIMESTAMP, -- When this range was fully used
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXHAUSTED, EXPIRED
    
    CONSTRAINT valid_range CHECK (end_range > start_range),
    CONSTRAINT status_values CHECK (status IN ('ACTIVE', 'EXHAUSTED', 'EXPIRED')),
    
    -- Prevent overlapping ranges
    EXCLUDE USING gist (
        int8range(start_range, end_range, '[]') WITH &&
    ) WHERE (status = 'ACTIVE')
);

-- Indexes for range_allocations
CREATE INDEX idx_range_allocations_service ON range_allocations(service_instance_id);
CREATE INDEX idx_range_allocations_status ON range_allocations(status);

-- ========================================
-- 7. GLOBAL COUNTER TABLE
-- Single-row table for managing the global counter
-- ========================================
CREATE TABLE global_counter (
    id INTEGER PRIMARY KEY DEFAULT 1,
    current_counter BIGINT NOT NULL DEFAULT 0,
    range_size INTEGER NOT NULL DEFAULT 1000,
    last_updated TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT singleton_check CHECK (id = 1)
);

-- Initialize with first value
INSERT INTO global_counter (id, current_counter, range_size) 
VALUES (1, 0, 1000);

-- ========================================
-- FUNCTIONS & TRIGGERS
-- ========================================

-- Function to increment URL click count (can be used instead of manual UPDATE)
CREATE OR REPLACE FUNCTION increment_click_count(url_short_code VARCHAR)
RETURNS void AS $$
BEGIN
    UPDATE urls 
    SET click_count = click_count + 1 
    WHERE short_code = url_short_code AND is_active = TRUE;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup expired URLs (run as scheduled job)
CREATE OR REPLACE FUNCTION cleanup_expired_urls()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE urls 
    SET is_active = FALSE 
    WHERE expires_at IS NOT NULL 
      AND expires_at < NOW() 
      AND is_active = TRUE;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Function to cleanup expired sessions
CREATE OR REPLACE FUNCTION cleanup_expired_sessions()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    UPDATE sessions 
    SET is_active = FALSE 
    WHERE expires_at < NOW() 
      AND is_active = TRUE;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update last_login on user authentication
CREATE OR REPLACE FUNCTION update_last_login()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE users 
    SET last_login = NOW() 
    WHERE id = NEW.user_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_last_login
AFTER INSERT ON sessions
FOR EACH ROW
EXECUTE FUNCTION update_last_login();

-- ========================================
-- VIEWS FOR COMMON QUERIES
-- ========================================

-- View for active URLs with analytics
CREATE VIEW url_analytics_summary AS
SELECT 
    u.id,
    u.short_code,
    u.original_url,
    u.user_id,
    u.created_at,
    u.click_count,
    COUNT(a.id) as total_clicks_tracked,
    COUNT(DISTINCT a.country) as unique_countries,
    MAX(a.clicked_at) as last_clicked_at,
    MIN(a.clicked_at) as first_clicked_at
FROM urls u
LEFT JOIN analytics a ON u.id = a.url_id
WHERE u.is_active = TRUE
GROUP BY u.id;

-- View for user's URL management dashboard
CREATE VIEW user_url_dashboard AS
SELECT 
    u.id as user_id,
    u.email,
    u.username,
    COUNT(url.id) as total_urls,
    SUM(url.click_count) as total_clicks,
    MAX(url.created_at) as last_url_created
FROM users u
LEFT JOIN urls url ON u.id = url.user_id AND url.is_active = TRUE
WHERE u.is_active = TRUE
GROUP BY u.id, u.email, u.username;

-- ========================================
-- SAMPLE QUERIES FOR TESTING
-- ========================================

-- Find most popular URLs (for caching strategy)
-- SELECT short_code, original_url, click_count 
-- FROM urls 
-- WHERE is_active = TRUE 
-- ORDER BY click_count DESC 
-- LIMIT 100;

-- Get analytics for a specific URL in last 7 days
-- SELECT 
--     DATE(clicked_at) as date,
--     COUNT(*) as clicks,
--     COUNT(DISTINCT country) as countries
-- FROM analytics
-- WHERE url_id = $1 
--   AND clicked_at > NOW() - INTERVAL '7 days'
-- GROUP BY DATE(clicked_at)
-- ORDER BY date DESC;

-- Get user's URLs with their custom aliases
-- SELECT 
--     u.short_code,
--     u.original_url,
--     u.click_count,
--     ca.custom_code,
--     u.created_at
-- FROM urls u
-- LEFT JOIN custom_aliases ca ON u.id = ca.url_id AND ca.is_active = TRUE
-- WHERE u.user_id = $1 AND u.is_active = TRUE
-- ORDER BY u.created_at DESC;

-- ========================================
-- GRANT PERMISSIONS (adjust for your setup)
-- ========================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO url_shortener_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO url_shortener_app;