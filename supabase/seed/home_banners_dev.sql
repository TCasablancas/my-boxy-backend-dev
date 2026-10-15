-- Seed SÓ DE DEV (Supabase local): 3 banners fictícios com imagem placeholder.
-- Nunca aplicar no projeto compartilhado com produção.
insert into public.home_banners (image_url, alt_text, internal_route, external_url, is_active, sort_order)
values
  ('https://placehold.co/1200x900/webp?text=Busca', 'Banner de teste: abre a busca', '/search', null, true, 1),
  ('https://placehold.co/1200x900/webp?text=Externo', 'Banner de teste: abre um site externo', null, 'https://example.com', true, 2),
  ('https://placehold.co/1200x900/webp?text=Sem+link', 'Banner de teste sem link', null, null, true, 3);
