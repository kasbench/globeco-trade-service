-- V7: Performance optimization indexes for common query patterns
-- Based on TradeOrderSpecification and ExecutionSpecification analysis

-- Performance-critical indexes for trade_order table

-- Covering index for common SELECT operations with portfolio and security filtering
CREATE INDEX IF NOT EXISTS idx_trade_orders_portfolio_security_covering 
ON trade_order (portfolio_id, security_id) 
INCLUDE (id, order_id, quantity, quantity_sent, submitted, trade_timestamp, order_type);

-- Partial index for active (non-submitted) orders - frequently queried
CREATE INDEX IF NOT EXISTS idx_trade_orders_active_portfolio_security 
ON trade_order (portfolio_id, security_id, trade_timestamp DESC) 
WHERE submitted = false;

-- Partial index for submitted orders with timestamp ordering
CREATE INDEX IF NOT EXISTS idx_trade_orders_submitted_timestamp 
ON trade_order (trade_timestamp DESC, portfolio_id, security_id) 
WHERE submitted = true;

-- Composite index for order type and portfolio filtering
CREATE INDEX IF NOT EXISTS idx_trade_orders_order_type_portfolio 
ON trade_order (order_type, portfolio_id, trade_timestamp DESC);

-- Index for quantity range queries (min/max filtering)
CREATE INDEX IF NOT EXISTS idx_trade_orders_quantity_range 
ON trade_order (quantity, quantity_sent, portfolio_id);

-- Performance-critical indexes for execution table

-- Covering index for execution queries with trade_order relationship
CREATE INDEX IF NOT EXISTS idx_executions_trade_order_covering 
ON execution (trade_order_id, execution_status_id) 
INCLUDE (id, execution_timestamp, quantity_ordered, quantity_placed, quantity_filled, execution_service_id);

-- Index for execution service ID queries (frequently used for external service tracking)
CREATE INDEX IF NOT EXISTS idx_executions_service_id_timestamp 
ON execution (execution_service_id, execution_timestamp DESC) 
WHERE execution_service_id IS NOT NULL;

-- Composite index for status and timestamp filtering
CREATE INDEX IF NOT EXISTS idx_executions_status_timestamp_desc 
ON execution (execution_status_id, execution_timestamp DESC);

-- Index for quantity range queries on executions
CREATE INDEX IF NOT EXISTS idx_executions_quantities_range 
ON execution (quantity_ordered, quantity_placed, quantity_filled, execution_status_id);

-- Composite index for blotter and trade type filtering
CREATE INDEX IF NOT EXISTS idx_executions_blotter_trade_type_timestamp 
ON execution (blotter_id, trade_type_id, execution_timestamp DESC);

-- Cross-table query optimization indexes

-- Index for portfolio/security queries across trade_order and execution
CREATE INDEX IF NOT EXISTS idx_trade_orders_portfolio_security_submitted_timestamp 
ON trade_order (portfolio_id, security_id, submitted, trade_timestamp DESC);

-- Index for execution queries that join back to trade_order for portfolio/security info
CREATE INDEX IF NOT EXISTS idx_executions_trade_order_status_timestamp 
ON execution (trade_order_id, execution_status_id, execution_timestamp DESC);

-- Specialized indexes for JOIN operations

-- Index to optimize blotter joins in trade_order queries
CREATE INDEX IF NOT EXISTS idx_trade_orders_blotter_submitted_timestamp 
ON trade_order (blotter_id, submitted, trade_timestamp DESC) 
WHERE blotter_id IS NOT NULL;

-- Index to optimize destination joins in execution queries
CREATE INDEX IF NOT EXISTS idx_executions_destination_status_timestamp 
ON execution (destination_id, execution_status_id, execution_timestamp DESC);

-- Index to optimize trade type joins in execution queries
CREATE INDEX IF NOT EXISTS idx_executions_trade_type_status_timestamp 
ON execution (trade_type_id, execution_status_id, execution_timestamp DESC) 
WHERE trade_type_id IS NOT NULL;

-- Partial indexes for common filtered queries

-- Index for unfilled executions (quantity_filled = 0)
CREATE INDEX IF NOT EXISTS idx_executions_unfilled 
ON execution (execution_status_id, execution_timestamp DESC, trade_order_id) 
WHERE quantity_filled = 0;

-- Index for partially filled executions
CREATE INDEX IF NOT EXISTS idx_executions_partially_filled 
ON execution (execution_status_id, execution_timestamp DESC, trade_order_id) 
WHERE quantity_filled > 0 AND quantity_filled < quantity_ordered;

-- Index for fully filled executions
CREATE INDEX IF NOT EXISTS idx_executions_fully_filled 
ON execution (execution_status_id, execution_timestamp DESC, trade_order_id) 
WHERE quantity_filled = quantity_ordered;

-- Performance indexes for sorting and pagination

-- Optimized index for ID-based pagination (DESC order for recent-first queries)
CREATE INDEX IF NOT EXISTS idx_trade_orders_id_desc_portfolio 
ON trade_order (id DESC, portfolio_id);

CREATE INDEX IF NOT EXISTS idx_executions_id_desc_status 
ON execution (id DESC, execution_status_id);

-- Timestamp-based pagination indexes
CREATE INDEX IF NOT EXISTS idx_trade_orders_timestamp_desc_portfolio_security 
ON trade_order (trade_timestamp DESC, portfolio_id, security_id);

CREATE INDEX IF NOT EXISTS idx_executions_timestamp_desc_trade_order 
ON execution (execution_timestamp DESC, trade_order_id);