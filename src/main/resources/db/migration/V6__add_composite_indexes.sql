-- V6: Add composite indexes for enhanced filtering and sorting performance

-- Trade Order composite indexes for common filter combinations
CREATE INDEX  IF NOT EXISTS trade_order_portfolio_security_idx 
    ON trade_order (portfolio_id, security_id);

CREATE INDEX  IF NOT EXISTS trade_order_order_type_timestamp_idx 
    ON trade_order (order_type, trade_timestamp);

CREATE INDEX  IF NOT EXISTS trade_order_quantity_range_idx 
    ON trade_order (quantity, quantity_sent);

CREATE INDEX  IF NOT EXISTS trade_order_blotter_submitted_idx 
    ON trade_order (blotter_id, submitted);

CREATE INDEX  IF NOT EXISTS trade_order_submitted_timestamp_idx 
    ON trade_order (submitted, trade_timestamp);

-- Execution composite indexes for common filter combinations
CREATE INDEX  IF NOT EXISTS execution_status_blotter_idx 
    ON execution (execution_status_id, blotter_id);

CREATE INDEX  IF NOT EXISTS execution_trade_type_destination_idx 
    ON execution (trade_type_id, destination_id);

CREATE INDEX  IF NOT EXISTS execution_quantity_range_idx 
    ON execution (quantity_ordered, quantity_placed, quantity_filled);

CREATE INDEX  IF NOT EXISTS execution_timestamp_status_idx 
    ON execution (execution_timestamp, execution_status_id);

CREATE INDEX  IF NOT EXISTS execution_trade_order_status_idx 
    ON execution (trade_order_id, execution_status_id);

-- Additional indexes for sorting performance
CREATE INDEX  IF NOT EXISTS trade_order_id_desc_idx 
    ON trade_order (id DESC);

CREATE INDEX  IF NOT EXISTS execution_id_desc_idx 
    ON execution (id DESC);

CREATE INDEX  IF NOT EXISTS trade_order_timestamp_desc_idx 
    ON trade_order (trade_timestamp DESC);

CREATE INDEX  IF NOT EXISTS execution_timestamp_desc_idx 
    ON execution (execution_timestamp DESC); 