# Pack KjobsUltimate pour Kgui V2

Le pack Kjobs est une intégration optionnelle. Kgui reste autonome et n'importe aucune classe du
plugin principal KjobsUltimate. Quand les deux plugins sont actifs, Kjobs récupère `KguiApi` dans le
`ServicesManager`, vérifie la majeure `2`, puis possède et ferme toutes ses inscriptions.

## Installation

1. Placer Kgui V2 et KjobsUltimate sur le serveur.
2. Conserver `Kgui` dans `softdepend` de KjobsUltimate.
3. Laisser `hooks.kgui.enabled: true` dans `plugins/KjobsUltimate/config.yml`.
4. Copier les six fichiers `kjobs_*.yml` du dossier de menus Kgui dans
   `plugins/Kgui/menus/` si une ancienne configuration existe déjà.
5. Exécuter `/kgui validate`, puis `/kgui reload`.

La commande `/jobs` reste possédée par KjobsUltimate. Kgui ne l'enregistre pas et ne peut donc pas
entrer en collision avec le plugin métier. Si Kgui est absent, incompatible ou désactivé dans la
configuration, `/jobs` revient au GUI interne de secours.

## Menus fournis

| Menu | Rôle | Provider |
|---|---|---|
| `kjobs_main` | six métiers, niveau, XP, état et favori | `kjobsultimate:jobs` |
| `kjobs_detail` | action typée sur un métier et raccourcis contextualisés | `kjobsultimate:job_detail` |
| `kjobs_quests` | quêtes filtrables avec scroll par ligne | `kjobsultimate:quests` |
| `kjobs_top` | classement global ou filtré, chargé hors thread serveur | `kjobsultimate:ranking` |
| `kjobs_settings` | HUD, BossBar et ActionBar | aucun |
| `kjobs_confirm_leave` | confirmation consommable et bornée dans le temps | `kjobsultimate:leave_confirmation` |

Les détails, quêtes et classements reçoivent `job_id` par les arguments d'ouverture. Le provider
revalide toujours l'identifiant avec le `JobRegistry`; une valeur absente ou inconnue renvoie un état
vide et n'exécute aucune mutation.

## Actions typées


Les actions disponibles sont :

```text
kjobsultimate:open_main
kjobsultimate:open_detail       job_id=<id>
kjobsultimate:open_quests       job_id=<id facultatif>
kjobsultimate:open_top          job_id=<id facultatif>
kjobsultimate:open_settings
kjobsultimate:unlock_job        job_id=<id>
kjobsultimate:favorite_job      job_id=<id>
kjobsultimate:request_leave     job_id=<id>
kjobsultimate:confirm_leave
kjobsultimate:cancel_leave
kjobsultimate:claim_quest       quest_id=<id>
kjobsultimate:toggle_hud
kjobsultimate:toggle_bossbar
kjobsultimate:toggle_actionbar
```

Exemple d'un filtre configurable :

```yaml
items:
  miner_quests:
    slot: 2
    material: IRON_PICKAXE
    display_name: '&bQuêtes Mineur'
    click_actions:
      - '[kjobsultimate:open_quests] job_id=mineur'
```

Les mutations passent par `SlotManager`, `QuestManager` et `HudManager`, jamais par une commande
texte ou une valeur NBT fournie par le client. Les clics provider restent liés côté serveur à la
session, au slot, à la révision et à l'identifiant logique de l'item.

## Conditions

| Condition | Paramètres | Décision |
|---|---|---|
| `kjobsultimate:available` | aucun | autorisée tant que l'extension est enregistrée |
| `kjobsultimate:data_loaded` | aucun | refuse tant que les données joueur ne sont pas en mémoire |
| `kjobsultimate:job_active` | `job_id` | job présent dans un slot actif |
| `kjobsultimate:job_inactive` | `job_id` | job valide mais non actif |
| `kjobsultimate:has_free_slot` | aucun | au moins un slot déverrouillé et libre |
| `kjobsultimate:quest_claimable` | `quest_id` | quête terminée et récompense non récupérée |

Toutes les conditions refusent en cas d'argument absent, de données non chargées, d'extension
indisponible ou d'erreur métier.

## Refresh et performance

- Les menus métiers utilisent `refresh.policy: EVENT`; il n'existe aucun scan global des GUI.
- Une mutation publie une invalidation ciblée pour le joueur et les menus concernés.
- Les gains XP et progressions de quête coalescent une seule invalidation par opération.
- Le classement exécute la requête SQL en asynchrone, limite le résultat à 50 entrées, partage les
  chargements simultanés et conserve un cache à durée bornée.
- Le scroll des quêtes utilise `ROW_SCROLL` et les boutons `kgui:navigate`. La molette physique n'est
  pas interceptée sur 1.8.8, car le protocole d'inventaire ne fournit pas d'événement fiable.
- Au disable de Kgui ou Kjobs, les providers, actions, conditions, caches et handles sont fermés de
  façon idempotente.

## Personnalisation sûre

Les titres, tailles, slots, matériaux, CIT, lores, filtres et boutons peuvent être modifiés librement.
Il faut conserver les identifiants de menus du tableau si `/jobs` doit continuer à les ouvrir, et les
providers/actions doivent garder le namespace `kjobsultimate:`. Une action inconnue ou un provider
absent échoue fermé.

Après chaque changement :

```text
/kgui validate
/kgui reload
```

Le dossier de déploiement Volkaria utilise les mêmes fichiers que les ressources testées du dépôt.
