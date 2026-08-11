# Items

Un item accepte notamment : `slot` ou `slots`, `material`, `data`, `amount`,
`display_name`, `lore`, `cit`, `head`, `head_owner`, `enchanted`,
`hide_attributes`, `priority`, `view_requirements`, `click_requirements`,
`click_actions`, `left_click_actions`, `right_click_actions`, `deny_actions` et
`cooldown_ticks`.

```yaml
items:
  profile:
    slots: [10, 11]
    material: SKULL_ITEM
    data: 3
    head_owner: "%player_name%"
    display_name: "&e%player_name%"
    lore:
      - "&7Page %page%/%max_page%"
    priority: 10
    view_requirements:
      visible: {type: permission, permission: example.view}
    left_click_actions: ["[message] &aClic gauche"]
    right_click_actions: ["[message] &eClic droit"]
    cooldown_ticks: 10
```

La plus haute priorité gagne lorsqu'il y a plusieurs candidats pour le même
slot. Un item de provider n'est jamais autorisé par son NBT client : l'autorité
reste le binding du slot rendu dans la session serveur.

