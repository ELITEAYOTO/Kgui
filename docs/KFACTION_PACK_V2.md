# Pack Kfaction 2.3 pour Kgui 2

## Contrat et cycle de vie

Kgui déclare uniquement un `softdepend` vers Kfaction. L'adaptateur est chargé
si les trois services publics suivants sont disponibles et compatibles :

- `KfactionApiV23` pour les snapshots de lecture ;
- `KfactionPlayerActions` pour les parcours joueur ;
- API majeure `2`, état de compatibilité `READY_2_3`.

Kgui démarre normalement sans Kfaction et ne tente alors pas de résoudre ses
classes. Les menus du pack restent compilables, mais leur requirement
`kfaction:available` refuse explicitement l'ouverture. Un disable/reload de
Kfaction ferme toutes les extensions enregistrées sous le propriétaire
Kfaction, sans conserver de handler ni de cache de provider.

Kfaction n'importe plus Kgui. `/f menu`, `/f logs` et `/f perms` conservent un
parcours textuel autonome.

## Menus livrés

Les fichiers sont extraits dans `plugins/Kgui/menus` au premier démarrage et
peuvent tous être modifiés :

| Menu | Provider | Navigation |
|---|---|---|
| `faction_menu` | aucun | hub |
| `faction_members` | `kfaction:members` | pages |
| `faction_claims` | `kfaction:claims` | pages |
| `faction_logs` | `kfaction:logs` | pages |
| `faction_warps` | `kfaction:warps` | pages |
| `faction_quests` | `kfaction:quests` | scroll par ligne |
| `faction_rewards` | `kfaction:rewards` | pages |
| `faction_relations` | `kfaction:relations` | pages |
| `faction_invites` | `kfaction:invites` | pages |
| `faction_zones` | `kfaction:zones` | scroll par ligne |

La commande d'ouverture livrée est `/fmenu`. La commande `/f menu` reste
volontairement la commande autonome de Kfaction.

Pour une installation qui possédait déjà les menus Kgui V1, remplacer
manuellement le dossier `plugins/Kgui/menus` par le pack V2 avant le premier
test. Kgui ne supprime et n'écrase jamais automatiquement une configuration
existante. Aucun convertisseur V1 n'est fourni, conformément au choix d'une
refonte sans données de production à préserver.

## Providers

Tous les providers travaillent avec des snapshots immuables, un `offset`, une
`limit` bornée et une révision. Aucun `Faction`, `FPlayer`, manager ou service
interne n'est importé. `kfaction:claims` et `kfaction:logs` utilisent directement
la pagination serveur de l'API 2.3. Les autres collections sont découpées après
lecture de leur snapshot immutable. Les textes, lores, attributs et matériaux
issus des snapshots sont bornés/validés avant publication à Kgui afin qu'une
valeur de configuration invalide ne fasse pas rejeter une page entière.

`kfaction:logs` accepte l'argument facultatif `type`. `kfaction:warps` délègue
un clic au parcours public `teleportWarp`; un warp protégé nécessite l'argument
de menu `password`. La liste des zones vient toujours de `getGlobalZones()` :
aucun ID `SAFEZONE`/`WARZONE` n'est codé dans Kgui.

## Requirements

| ID | Paramètres | Décision |
|---|---|---|
| `kfaction:available` | aucun | adaptateur 2.3 actif |
| `kfaction:has_faction` | aucun | le joueur appartient à une faction |
| `kfaction:role_at_least` | `role` | hiérarchie publique `FactionRole` |
| `kfaction:capability` | `permission` | ACL du rôle via l'API publique |
| `kfaction:relation` | `target_faction_id`, `relation` | relation exacte |
| `kfaction:can_afford` | `amount_minor` | solde de banque faction suffisant |

Exemple :

```yaml
open_requirements:
  api:
    type: "kfaction:available"
    deny_message: "&cKfaction 2.3 est indisponible."
  faction:
    type: "kfaction:has_faction"
    deny_message: "&cTu dois appartenir à une faction."
```

## Actions joueur

Une action de menu s'écrit par exemple :

```yaml
click_actions:
  - "[kfaction:claim]"
  - "[kfaction:invite_player] target=Notch"
  - "[kfaction:set_role_permission] role=MEMBER permission=build allowed=true"
```

IDs et paramètres :

| Action | Paramètres |
|---|---|
| `create_faction` | `name` |
| `invite_player`, `kick_member`, `transfer_leadership` | `target` ou `target_uuid` |
| `accept_invite`, `decline_invite` | `faction_id` |
| `leave_faction`, `disband_faction` | aucun |
| `change_role` | `target`/`target_uuid`, `role` |
| `claim`, `claim_fill`, `unclaim`, `unclaim_all` | position courante |
| `claim_radius`, `unclaim_radius` | `radius` (1 à 32) |
| `request_relation` | `target_faction_id`, `relation` |
| `accept_relation`, `decline_relation` | `source_faction_id`, `relation` |
| `set_neutral` | `target_faction_id` |
| `deposit_bank`, `withdraw_bank` | `amount_minor` strictement positif |
| `set_home`, `teleport_home` | aucun |
| `create_warp`, `delete_warp` | `name` |
| `teleport_warp` | `name`, `password` facultatif |
| `set_role_permission` | `role`, `permission`, `allowed` |
| `set_relation_permission` | `relation`, `permission`, `allowed` |
| `select_quest_category` | `category_id` |
| `claim_progression_reward` | `level` |

Tous ces IDs sont préfixés par `kfaction:`. Un nom de joueur n'est accepté que
s'il est en ligne ; un UUID reste accepté pour éviter toute résolution lente ou
ambiguë. Les positions de home/warp et chunks sont construites au moment du clic
et validées de nouveau par Kfaction.

## Résultats et invalidations

Les dix valeurs de `ApiResult.Status` sont traitées explicitement : `SUCCESS`
rafraîchit le snapshot, `NO_CHANGE` est un succès sans invalidation,
les refus métier donnent un message stable et `FAILED` devient une erreur sans
prétendre que la mutation a réussi.

`FactionSnapshotChangedEvent` est traduit par famille de données vers les seuls
menus concernés, pour chaque membre/acteur affecté. `PlayerFactionChangedEvent`
invalide uniquement les sessions du joueur. Le scheduler Kgui coalesce ensuite
les requêtes, conserve le viewport et n'applique à l'inventaire que le diff des
slots effectivement modifiés.

L'état de l'adaptateur apparaît dans `/kgui debug` sous `Kfaction API`.
