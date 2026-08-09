# Pagination et scroll

Kgui possède un viewport serveur unique par session.

- `navigation: PAGE` déplace le contenu d'une capacité complète ;
- `navigation: ROW_SCROLL` déplace le contenu d'une ligne (`row_stride`) ;
- `[next_page]`/`[previous_page]` et `[scroll_down]`/`[scroll_up]` passent par
  le même moteur borné ;
- un verrou d'un tick empêche les doubles navigations ;
- le refresh modifie seulement les slots visuellement changés et ne rouvre
  l'inventaire que si le titre ou la taille change.

```yaml
type: scroll
pagination:
  enabled: true
  navigation: ROW_SCROLL
  content_slots: "10-16,19-25,28-34"
  row_stride: 7
  provider: "example:entries"
items:
  up: {slot: 48, material: ARROW, click_actions: ["[scroll_up]"]}
  down: {slot: 50, material: ARROW, click_actions: ["[scroll_down]"]}
```

La molette physique change normalement le slot de hotbar dans le protocole
Minecraft 1.8 ; elle n'est donc pas détournée. Le « scroll » Kgui est volontairement
piloté par des boutons/actions configurables, fiable avec tous les clients 1.8.

