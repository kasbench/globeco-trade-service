-- Add 'submitted' column to trade_order table
ALTER TABLE trade_order
    ADD COLUMN submitted BOOLEAN DEFAULT FALSE; 