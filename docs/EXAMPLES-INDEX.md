# Index des exemples

Les exemples de référence ne sont pas des pseudo-configurations : ils sont les
fichiers réellement validés dans `Config-plugin-coter-server/Kgui`.

- `menus/quest.yml` : pagination statique, requirements d'affichage et boutons ;
- `menus/kjobs_main.yml` : provider paginé et actions d'extension ;
- `menus/kjobs_quests.yml` : viewport `ROW_SCROLL` ;
- `menus/faction_members.yml` : provider Kfaction et pagination ;
- `menus/faction_menu.yml` : requirements/capabilities Kfaction ;
- `menus/grades.yml` : menu statique riche ;
- `menus/kits*.yml` : navigation entre menus et actions de commande ;
- `items/example.yml` : champs d'items réutilisables ;
- `templates/default_border.yml` : template V2.

Avant de copier un exemple, changer son `id` et ses `open_commands` pour éviter
les collisions, puis exécuter `kgui validate`.

