-- Flyway migration: Initial data for destination table

INSERT INTO public.destination (abbreviation, description, version) VALUES
  ('ML', 'Merrill Lynch', 1),
  ('RBC', 'Royal Bank of Canada', 1),
  ('JPM', 'Chase', 1),
  ('IB', 'Interactive Brokers', 1),
  ('INST', 'Instinet', 1),
  ('POSIT', 'Posit', 1); 