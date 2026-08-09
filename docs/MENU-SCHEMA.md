# Schéma d'un menu

La référence exhaustive est [KGUI_MENU_SCHEMA_V2.md](KGUI_MENU_SCHEMA_V2.md).
Tout fichier V2 commence par `schema_version: 2`.

```yaml
schema_version: 2
id: example
title: "&8Exemple &7%page%/%max_page%"
size: 54
type: pagination
refresh: {policy: EVENT}
open_commands: [example]
pagination:
  enabled: true
  navigation: PAGE
  content_slots: "10-16,19-25,28-34"
  prev_button_slot: 48
  next_button_slot: 50
  max_pages: 3
items:
  next:
    slot: 50
    material: ARROW
    display_name: "&ePage suivante"
    click_actions: ["[next_page]"]
```

`id` doit être unique, `size` un multiple de 9 entre 9 et 54 et le titre est
limité à 32 caractères après résolution des placeholders sur Minecraft 1.8.
Les slots sont indexés à partir de 0. Une erreur de schéma bloque uniquement le
menu fautif et apparaît dans `kgui validate`/`kgui diagnose`.

