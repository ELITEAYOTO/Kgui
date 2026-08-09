# Kgui — schéma de menus V2

Le compilateur V2 lit exclusivement les fichiers `*.yml` placés dans `plugins/Kgui/menus/` et
`plugins/Kgui/templates/`. Un fichier n'est publié dans le cache actif qu'après validation de
l'ensemble des menus et de leurs dépendances. En cas d'erreur, le dernier état valide reste actif.

## Commandes de contrôle

- `/kgui validate` compile et valide sans modifier le cache actif.
- `/kgui reload` recharge le plugin et ne publie les menus que si tout est valide.
- `/kgui reload <menu>` recompile le graphe de templates, puis ne remplace que ce menu.
- `/kgui dump <menu>` affiche la configuration canonique après héritage.

Chaque diagnostic contient une gravité, un code, le fichier, la ligne et le chemin YAML. Les fichiers
sans `schema_version` restent acceptés pendant la migration avec un avertissement. Tout nouveau fichier
doit commencer par :

```yaml
schema_version: 2
```

## Clés à la racine

| Clé | Type | Rôle |
|---|---|---|
| `schema_version` | entier | Doit valoir `2`. |
| `id` | chaîne | Métadonnée facultative; l'identifiant effectif reste le nom du fichier. |
| `title` | chaîne | Titre, `&8Menu` par défaut. |
| `size` | entier | `9`, `18`, `27`, `36`, `45` ou `54`. |
| `type` | chaîne | `normal`, `pagination`, `scroll` ou `dynamic`; les anciens alias restent acceptés. |
| `template` | chaîne | Template parent recommandé. `extends` et `inherit_from` sont des alias de migration. |
| `open_command` | chaîne | Commande d'ouverture unique. |
| `open_commands` | liste de chaînes | Commandes d'ouverture supplémentaires. |
| `open_actions`, `close_actions` | liste de chaînes | Actions exécutées à l'ouverture/fermeture. |
| `open_requirements` | section | Conditions globales nommées; chaque condition exige `type`. |
| `permission` | chaîne | Permission globale facultative. |
| `block_in_combat` | booléen | Interdit l'ouverture en combat. |
| `allowed_worlds`, `blocked_worlds` | liste de chaînes | Filtres de mondes. |
| `open_on_region_enter`, `allowed_regions`, `blocked_regions` | liste de chaînes | Intégration WorldGuard. |
| `cooldown` | entier positif ou nul | Délai d'ouverture en secondes. |
| `update_interval` | entier positif ou nul | Intervalle historique en ticks; le scheduler d'invalidation ciblée relève du Lot 5. |
| `pagination` | section | Configuration paginée recommandée. |
| `content_slots`, `prev_button_slot`, `next_button_slot`, `max_pages` | slots/entiers | Ancien format racine encore accepté. |
| `provider`, `provider_args` | chaîne/section scalaire | Fournisseur dynamique et arguments libres. |
| `empty_message`, `empty_item` | chaîne/item | État vide d'un fournisseur. |
| `items` | section | Items nommés du menu. |
| `animations` | section libre | Définitions d'animations consommées par le moteur existant. |

Une clé inconnue est une erreur. Il est interdit de combiner `template`, `extends` et `inherit_from`
dans un même fichier.

## Templates et fusion

Un template utilise exactement le même schéma qu'un menu et peut lui-même hériter d'un template.
Les sections sont fusionnées récursivement. Un item portant le même identifiant remplace uniquement
ses propriétés redéfinies. Les listes `open_actions` et `close_actions` sont concaténées, parent d'abord;
les autres listes sont remplacées par l'enfant. Les cycles et les parents absents sont refusés.

```yaml
schema_version: 2
template: default_border
title: '&6Mon menu'
size: 54
items:
  close:
    slot: 49
    material: BARRIER
    display_name: '&cFermer'
    click_actions: ['[close]']
```

## Pagination et providers

```yaml
type: pagination
pagination:
  enabled: true
  content_slots: '10-16,19-25,28-34'
  prev_button_slot: 45
  next_button_slot: 53
  max_pages: 0
  provider: kfaction_members
  provider_args:
    role: all
  empty_message: '&7Aucun membre.'
  empty_item:
    material: BARRIER
    display_name: '&cAucun résultat'
```

`content_slots` accepte un entier, une liste d'entiers ou une chaîne de plages. Tous les slots doivent
être compris entre `0` et `size - 1`. `-1` désactive un bouton précédent/suivant. `provider_args` accepte
uniquement des valeurs scalaires afin que le contrat envoyé au provider reste déterministe.

## Items

Chaque item de `items` exige `slot` ou `slots`, puis une source : `item`/`item_id`, ou une source inline
parmi `material`, `skull`/`skull_owner`, `hdb`/`head_database`, `cit`/`cit_key`.

Clés reconnues :

- placement : `slot`, `slots`, `priority`;
- source : `item`, `item_id`, `material`, `data`, `skull`, `skull_owner`, `hdb`, `head_database`, `cit`, `cit_key`;
- rendu : `display_name`, `name`, `lore`, `amount`, `glow`, `hide_attributes`, `kgui_protected`;
- conditions : `view_requirements`, `view_requirement`, `click_requirements`, `click_requirement`;
- actions : `click_actions`, `left_click_actions`, `right_click_actions`, `shift_click_actions`, `middle_click_actions`, `deny_actions`;
- exécution : `cooldown`, `update`, `animation`.

Les quatre blocs de conditions acceptent soit des conditions nommées, soit la forme singulière, soit
une liste courte telle que `permission: kgui.menu.admin`. Toute condition structurée doit déclarer `type`.

```yaml
items:
  profile:
    slots: [10, 11]
    material: SKULL_ITEM
    data: 3
    skull_owner: '%player_name%'
    display_name: '&e%player_name%'
    lore:
      - ''
      - '&7Clique pour ouvrir.'
    priority: 10
    view_requirements:
      visible:
        type: permission
        permission: kgui.profile
        deny_message: '&cProfil indisponible.'
    click_requirements:
      - 'permission: kgui.profile.open'
    left_click_actions:
      - '[message] &aOuverture...'
      - '[open] profile_details'
    deny_actions:
      - '[message] &cAction refusée.'
    cooldown: 1
    update: true
```

Les lignes vides dans `lore` sont valides. Les identifiants de conditions et les arguments de conditions
sont extensibles; ils sont transmis au moteur de requirements et aux hooks.

## Garanties de compilation

- lecture UTF-8 explicite;
- limite de 4 Mio par fichier pour éviter les configurations pathologiques;
- rejet des YAML invalides, clés dupliquées, clés inconnues et types incorrects;
- validation des tailles, valeurs négatives et limites de slots;
- modèles et collections compilés non modifiables;
- résolution déterministe de l'héritage;
- publication atomique par un unique changement de snapshot;
- aucune fermeture ni modification du cache actif par `/kgui validate`;
- conservation du snapshot précédent si une compilation ou une matérialisation échoue.

Le modèle runtime `MenuData` reste temporairement généré depuis le modèle immuable pour l'adaptateur
d'items et la pagination historiques. Depuis le Lot 3, le rendu actif est un `RenderedSlot[]` lié à une
session/révision et un refresh à titre/taille stables ne crée plus de nouvel inventaire Bukkit.
La suppression finale de l'adaptateur `MenuData` et l'unification du viewport relèvent du Lot 5.

## Sécurité runtime

- le holder contient l'UUID propriétaire, l'identifiant de session et la révision de rendu;
- les actions de clic restent côté serveur dans le slot rendu et ne sont jamais sérialisées dans le NBT;
- seuls les clics gauche, droit, shift-gauche, shift-droit et milieu peuvent déclencher une action;
- number-key, double-clic, drop/control-drop, creative, clic extérieur, inventaire joueur et type inconnu
  sont annulés sans action;
- tous les drags et transferts impliquant un inventaire Kgui sont annulés;
- close, quit, kick, changement de monde, reload et disable invalident la session de façon idempotente;
- les refreshs, animations et actions retardées sont liés au jeton de session et annulés à sa fermeture;
- les saisies chat et confirmations utilisent leurs propres jetons consommables une seule fois.
