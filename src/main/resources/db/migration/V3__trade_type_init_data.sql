-- Flyway migration: Initial data for trade_type table

INSERT INTO public.trade_type (abbreviation, description, version) VALUES
  ('BUY', 'Buy', 1),
  ('SELL', 'Sell', 1),
  ('SHORT', 'Sell to Open', 1),
  ('COVER', 'Buy to Close', 1),
  ('EXRC', 'Exercise', 1); 