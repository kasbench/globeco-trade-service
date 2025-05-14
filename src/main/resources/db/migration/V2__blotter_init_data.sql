-- Flyway migration: Initial data for blotter table

INSERT INTO public.blotter (abbreviation, name, version) VALUES
  ('Default', 'Default', 1),
  ('EQ', 'Equity', 1),
  ('FI', 'Fixed Income', 1),
  ('HOLD', 'Hold', 1); 