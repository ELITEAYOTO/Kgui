# Kgui V2 — actions, conditions et extensions

Ce document décrit le contrat runtime introduit au Lot 4. Le schéma général des menus reste décrit
dans `KGUI_MENU_SCHEMA_V2.md`.

## Identifiants namespacés

Toute entrée possède un identifiant canonique `namespace:id`. Les noms courts historiques restent
acceptés uniquement comme alias explicites du cœur. Ils ne permettent pas à un plugin de remplacer
une autre extension.

Exemples :

```yaml
click_actions:
  - '[kgui:message] &aBonjour %player_name%'
  - '[kgui:open] profile'
  - '[vault:withdraw] 100'
  - '[playerpoints:take] 5'
  - '[kfaction:claim]'
```

Un plugin externe qui s'appelle `KjobsUltimate` peut enregistrer `kjobsultimate:members`, mais ne peut
pas revendiquer `kfaction:members`. Un owner désactivé n'est jamais résolu, et toutes ses inscriptions
sont retirées sur `PluginDisableEvent`.

## Actions du cœur

| ID canonique | Alias V1 | Rôle |
|---|---|---|
| `kgui:message` | `message` | Message au joueur |
| `kgui:actionbar` | `actionbar` | Action bar |
| `kgui:title` | `title` | Titre et sous-titre |
| `kgui:sound` | `sound` | Son Bukkit |
| `kgui:open` | `open` | Ouvre un menu |
| `kgui:open_menu` | `open_menu` | Alias d'ouverture historique |
| `kgui:back` | `back` | Retour à la vue précédente |
| `kgui:close` | `close` | Ferme la session |
| `kgui:refresh` | `refresh` | Invalide la vue courante |
| `kgui:next_page` | `next_page` | Page/ligne suivante |
| `kgui:prev_page` | `prev_page` | Page/ligne précédente |
| `kgui:set_page` | `set_page` | Définit page/offset |
| `kgui:scroll_up` | `scroll_up` | Remonte d'une ligne |
| `kgui:scroll_down` | `scroll_down` | Descend d'une ligne |
| `kgui:navigate` | `navigate` | Déplace le viewport ou choisit une position |
| `kgui:delay` | `delay` | Retarde les actions suivantes, lié au token de session |
| `kgui:confirm` | `confirm` | Confirmation consommable une fois |
| `kgui:input_text` | `input_text` | Saisie texte bornée |
| `kgui:input_number` | `input_number` | Saisie numérique bornée |
| `kgui:player_command` | `player` | Commande non privilégiée du joueur |
| `kgui:console_command` | `console` | Commande locale privilégiée validée |
| `kgui:teleport` | `teleport` | Téléportation configurée |
| `kgui:broadcast` | `broadcast` | Message global |
| `vault:withdraw` | `take_money` | Retrait transactionnel strictement positif |
| `vault:deposit` | `give_money` | Dépôt transactionnel strictement positif |
| `playerpoints:take` | `take_points` | Retrait de points strictement positif |
| `playerpoints:give` | `give_points` | Ajout de points strictement positif |

L'action unifiée accepte exactement `direction=next|previous|up|down` avec un `step=1..100`
facultatif, ou `page=N`. Ces formes ne peuvent pas être mélangées :

```yaml
click_actions:
  - '[kgui:navigate] direction=next step=2'
  - '[kgui:navigate] page=1'
```

`[op]` est définitivement interdit. Le compilateur refuse les nouveaux menus qui l'utilisent et le
runtime refuse aussi les anciens fichiers déjà matérialisés. Kgui ne modifie jamais l'état OP d'un joueur.

### Commandes console

`kgui:console_command` est réservé aux actions écrites dans les fichiers locaux du pack. Il est refusé
pour une action provenant d'un provider dynamique. Avant expansion, seuls `%player%` et
`%player_name%` sont admis, car le nom Minecraft est validé par le serveur. Les valeurs
`{kgui_data_*}`, les placeholders arbitraires, les retours ligne, `;`, `&&`, `||`, les contrôles et les
racines administratives (`op`, `deop`, `stop`, `reload`, gestion de permissions) sont refusés.

```yaml
# accepté dans un menu local
click_actions:
  - '[kgui:console_command] give %player_name% diamond 1'

# refusé : donnée saisie par le joueur dans une commande privilégiée
click_actions:
  - '[kgui:console_command] eco give %player% {kgui_data_amount}'
```

Les commandes `f`, `faction` et `kfaction` sont également bloquées dans `player_command` et
`console_command`. Une mutation de faction doit passer par une action typée `kfaction:*`.

## Conditions du cœur

Une condition absente, inconnue, mal formée, en erreur ou dépendante d'un plugin indisponible vaut
toujours `DENY`. `invert: true` (alias `negate`) inverse uniquement une décision valide.

| ID canonique | Alias principaux | Champs obligatoires |
|---|---|---|
| `kgui:permission` | `permission`, `has_permission` | `permission` |
| `kgui:permissions` | `permissions` | `permissions`, `minimum` optionnel (toutes par défaut) |
| `vault:balance` | `money`, `has_money` | `amount >= 0`, Vault actif |
| `playerpoints:balance` | `points`, `has_points` | `amount >= 0`, PlayerPoints actif |
| `kgui:world` | `world`, `in_world` | `world` |
| `worldguard:region` | `region`, `in_region` | `region`, WorldGuard actif |
| `combattagplus:not_in_combat` | `not_in_combat` | CombatTagPlus actif |
| `kgui:level` | `level`, `experience_level` | `level >= 0` |
| `kgui:experience` | `experience`, `exp` | `experience >= 0` |
| `kgui:gamemode` | `gamemode` | valeur Bukkit valide |
| `kgui:op` | `op`, `is_op` | aucun |
| `kgui:item` | `item`, `has_item` | `material`; `amount` et `data` optionnels |
| `kgui:distance` | `distance`, `location` | `world`, `x`, `y`, `z`, `max_distance` |
| `kgui:string_equals` | `string_equals` | `input`, `output`; `ignore_case` optionnel |
| `kgui:placeholder_equals` | `placeholder_equals` | `placeholder`, `value` |
| `kgui:placeholder_contains` | `placeholder_contains` | `placeholder`, `value` |
| `kgui:placeholder_number` | `placeholder_number` | `placeholder`, `operator`, `value` |
| `kgui:placeholder_regex` | `placeholder_regex`, `regex` | `placeholder`, `regex` |
| `kgui:value_type` | `value_type`, `type` | `value`, `expected` (`int`, `double`, `uuid`, `player`, `boolean`, `string`) |
| `kgui:expression` | `expression`, `javascript` | `expression` |

Les alias Kfaction V1 `has_faction`, `faction_role` et `faction_power` ne lisent plus aucun manager ou
objet métier. Ils délèguent respectivement à `kfaction:has_faction`, `kfaction:role` et
`kfaction:power`; en l'absence de ces extensions, la condition est refusée.

## Expressions sûres

`javascript` n'active aucun moteur JavaScript : il reste seulement un alias de migration vers
`kgui:expression`.

Formes acceptées :

```text
true
false
10 >= 3
2.5 != 4
Volkaria equals volkaria
Volkaria contains kari
officer not_equals member
prefix starts_with pre
filename ends_with .yml
```

Une expression est limitée à 512 caractères et à une comparaison. Il n'existe ni appel de méthode,
ni réflexion, ni exécution de script, ni `&&`/`||`. Les nombres doivent être entièrement numériques :
`12 emeralds > 3` est refusé au lieu d'être transformé silencieusement en `12 > 3`.

Les regex sont limitées à 256 caractères, l'entrée à 2 048 caractères, et les backreferences,
lookarounds et groupes à quantificateurs imbriqués sont refusés.

## Extensions publiques et capacités

Le contrat `KguiApi` reste gelé à `2.0.0`. Les quatre inscriptions existantes accordent uniquement la
capacité correspondante tant que leur owner est actif :

```text
registerProvider     -> PROVIDE_CONTENT
registerAction       -> EXECUTE_ACTION
registerRequirement  -> CHECK_REQUIREMENT
registerMenuPack     -> OWN_MENU_PACK
```

Les handles sont idempotents et `unregisterAll(owner)` retire toutes les références. Un pack est limité
à 512 identifiants de menus valides. Le registre interne peut vérifier qu'un menu appartient réellement
au pack déclaré. Les snapshots/révisions et le routage complet des clics providers sont traités au Lot 5.

## Cycle de vie des hooks

PAPI, Vault, PlayerPoints, HeadDatabase, WorldGuard, CombatTagPlus et ProtocolLib sont résolus par des
adaptateurs réfléchis étroits. Aucune de leurs classes n'apparaît dans un import ou une signature Kgui.
`HookManager` écoute les événements d'activation/désactivation, attache tardivement la dépendance et
appelle `close()` à sa disparition. Les objets API, méthodes réfléchies, listeners et caches sont alors
retirés afin de ne pas retenir le classloader du plugin optionnel.

Le vieux `KfactionHook` V1 et le hook spécifique zAuctionHouse ont été supprimés. L'intégration métier
Kfaction sera possédée par Kgui au Lot 6 et utilisera uniquement les contrats publics 2.2/2.3.
