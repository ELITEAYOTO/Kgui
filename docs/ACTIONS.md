# Actions

La syntaxe complète, les aliases et les règles d'extension sont dans
[KGUI_ACTIONS_REQUIREMENTS_EXTENSIONS_V2.md](KGUI_ACTIONS_REQUIREMENTS_EXTENSIONS_V2.md).

Familles principales :

- navigation : `[next_page]`, `[previous_page]`, `[set_page] 2`,
  `[scroll_up]`, `[scroll_down]`, `[kgui:navigate] direction=next step=1` ;
- menus : `[open] menu_id`, `[back]`, `[close]`, `[refresh]` ;
- joueur : `[message]`, `[sound]`, `[command]`, `[console]` ;
- économie : `[vault:withdraw]`, `[vault:deposit]` ;
- extensions : préfixes propriétaires `kfaction:*` et `kjobsultimate:*`.

Les actions différées capturent un token de session. Si le joueur ferme le
menu, change de session, quitte ou si le plugin propriétaire est désactivé,
l'action n'est pas exécutée.

