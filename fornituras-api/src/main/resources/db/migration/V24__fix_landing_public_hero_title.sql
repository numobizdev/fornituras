-- Corrige el título de la cara pública de la landing (016). El sistema se presenta como
-- "Sistema de Gestión de Blindajes" (README, Planeación y constitución), no por la expansión del
-- acrónimo SIGEFOR ("Sistema Integral de Gestión de Fornituras"), que había quedado sembrada en el
-- HERO público de algunos entornos. Se acota a la sección sembrada (scope PUBLIC, type HERO) con la
-- variante "Fornituras" para no tocar ediciones deliberadas del administrador.

UPDATE landing_section
SET titulo = 'Sistema de Gestión de Blindajes'
WHERE scope = 'PUBLIC'
  AND type = 'HERO'
  AND titulo LIKE '%Fornituras%';
