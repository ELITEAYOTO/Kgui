# Configuration globale

`plugins/Kgui/config.yml` contrôle le moteur, pas le contenu des menus.

## Sécurité

- `security.nbt_tag` : préfixe NBT réservé aux items temporaires de GUI ;
- `clean_on_close`, `clean_on_world_change`, `clean_on_quit` : nettoyages de
  défense. Les laisser à `true` ;
- ne jamais réutiliser le tag Kgui sur un item de gameplay persistant.

## Performance

- `placeholder_cache_ticks: 20` : cache local d'une seconde ;
- `provider_cache_entries: 2048` : LRU global borné ;
- `provider_slow_warning_ms: 2.0` : seuil d'alerte d'un provider ;
- `max_pending_refreshes: 2048` : capacité de la file coalescée ;
- `max_refreshes_per_tick: 32` : plafond de travaux par tick ;
- `refresh_budget_nanos: 2000000` : budget global de 2 ms/tick ;
- `use_protocol_lib: true` : mises à jour de slots si le hook existe, fallback
  Bukkit sinon.

Les limites sont globales et bornées. Les augmenter ne corrige pas un provider
lent : il faut d'abord publier des invalidations plus précises et réduire le
coût du provider.

## Structure des fichiers

- `menus/*.yml` : un menu par fichier ;
- `items/*.yml` : bibliothèques d'items réutilisables ;
- `templates/*.yml` : fragments avec `schema_version: 2` ;
- `messages.yml` : messages localisés.

Après une édition, utiliser `kgui validate`, puis `kgui reload`. Le reload de
configuration est accepté ; le hot-reload du JAR ne l'est pas.

