# Kgui V2 — providers, viewport et invalidation

Ce document décrit le contrat runtime du Lot 5. Il s'applique à tout plugin optionnel, notamment
Kfaction, sans créer de dépendance dure vers celui-ci. Kgui reste utilisable seul; le branchement des
providers et actions `kfaction:*` appartient à l'adaptateur Kfaction du Lot 6.

## Principes

- un seul `ViewportState` par session, pour la page ou le défilement par ligne;
- un provider renvoie uniquement la tranche demandée, jamais toute sa collection;
- chaque tranche est immutable, révisionnée et composée d'identifiants d'items stables;
- une invalidation cible des sessions indexées, sans parcourir toutes les GUI ouvertes;
- les demandes simultanées sont fusionnées dans une file bornée;
- un refresh stable construit un frame neutre puis applique seulement le diff de slots;
- un nouvel `Inventory` Bukkit n'est créé que si le titre ou la taille change;
- les clics providers sont vérifiés avec provider, génération, révision, item et slot rendus.

## Obtenir et conserver l'API

Le plugin consommateur déclare Kgui en `softdepend`, puis récupère le service Bukkit quand Kgui est
présent. Il conserve aussi le handle afin de pouvoir le fermer explicitement.

```java
KguiApi api = Bukkit.getServicesManager().load(KguiApi.class);
if (api == null || api.getApiMajor() != 2) {
    return; // Kgui absent ou contrat incompatible
}

ProviderRegistration registration = api.registerProvider(
    this,
    "kfaction:members",
    new MembersProvider()
);
```

Le namespace doit être celui du plugin owner, normalisé en minuscules. Un plugin ne peut pas
enregistrer une extension dans le namespace d'un autre. Une inscription est unique et reçoit une
génération interne monotone. `registration.close()`, `api.unregisterAll(this)` et le disable Bukkit
retirent l'inscription de façon idempotente, vident son cache et invalident ses sessions.

Kgui exécute actuellement `getContent` et `onClick` sur le thread principal. Le provider ne doit donc
faire ni accès réseau, ni lecture disque, ni requête SQL bloquante. Il prépare ses données en amont et
ne fait ici qu'une lecture mémoire bornée.

## Requête et snapshot

`ContentRequest` contient :

| Champ | Contrat |
|---|---|
| `playerId` | joueur propriétaire de la session |
| `menuId` | menu rendu |
| `providerId` | identifiant canonique namespacé |
| `offset` | premier élément demandé, `>= 0` |
| `limit` | capacité exacte du viewport, entre `1` et `100` |
| `knownRevision` | dernière révision connue par Kgui |
| `arguments` | fusion déterministe des `provider_args` YAML et arguments d'ouverture |

Le provider renvoie un `ContentSnapshot(revision, items, totalItems)`. Le résultat est refusé si :

- il est nul ou sa révision régresse sous `knownRevision`;
- il contient plus de `limit` éléments;
- sa tranche est incohérente avec `offset` et `totalItems`;
- deux éléments partagent le même `itemId`;
- une chaîne, une lore ou une map d'attributs dépasse les limites runtime.

Limites retenues : `itemId` 128 caractères, material 64, nom 512, 64 lignes de lore de 1024
caractères, 64 attributs, clé 64 et valeur 4096. `ContentItem` impose aussi une quantité de `1..64`.
Un snapshot refusé conserve la tranche précédente exacte si elle existe; il ne mélange jamais deux
offsets.

La révision représente l'état logique du sujet `(provider, génération, joueur, menu, arguments)`. Elle
doit être monotone pour ce sujet. Si rien n'a changé, le provider rend la même révision. Si un élément,
l'ordre ou le total change, il l'incrémente. Les `itemId` doivent identifier l'objet métier, pas sa
position (`member:<uuid>` est correct, `slot:4` ne l'est pas).

Exemple minimal :

```java
public final class MembersProvider implements ContentProvider {
    @Override
    public ContentSnapshot getContent(ContentRequest request) {
        MembersView view = memberViews.get(request.getPlayerId());
        int from = Math.min(request.getOffset(), view.size());
        int to = Math.min(from + request.getLimit(), view.size());
        List<ContentItem> items = new ArrayList<>();

        for (MemberView member : view.slice(from, to)) {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("skull_owner", member.getName());
            attributes.put("member_name", member.getName());
            items.add(new ContentItem(
                "member:" + member.getUuid(),
                "SKULL_ITEM", (short) 3, 1,
                "&e%member_name%",
                Arrays.asList("&7Rôle: " + member.getRole()),
                attributes
            ));
        }
        return new ContentSnapshot(view.getRevision(), items, view.size());
    }
}
```

Kgui remplace les placeholders habituels puis les attributs `%nom_attribut%`. Les attributs réservés
du rendu sont `glow`, `skull_owner`, `head_database` et `cit`. Les actions optionnelles sont conservées
côté serveur sous `actions`, `left_actions`, `right_actions` et `shift_actions`; plusieurs actions sont
séparées par des retours à la ligne. Elles s'exécutent avec l'origine `PROVIDER`, qui interdit les
actions console privilégiées et les mutations Kfaction par commande générique.

## Clics provider

Avant d'appeler le provider, Kgui vérifie que la session active correspond encore au holder, au slot,
au provider, à sa génération, à la révision et à l'`itemId`. Un ancien clic ne peut donc pas agir sur
un objet qui a changé de place ou disparu.

`onClick(ProviderClickContext)` renvoie un `ProviderClickResult` :

| Statut | Effet |
|---|---|
| `HANDLED` | clic accepté; les actions déclaratives de l'item peuvent suivre |
| `IGNORED` | aucune logique provider; les actions déclaratives peuvent suivre |
| `STALE` | clic arrêté et tranche ciblée invalidée |
| `DENIED` | clic arrêté, `messageKey` facultatif envoyé |
| `ERROR` | clic arrêté, erreur contrôlée affichable |

`handledAndInvalidate()` demande une invalidation du menu/joueur et de l'item concerné. Un provider
ne doit jamais faire confiance à la lore ou au NBT client : il revalide l'autorisation métier depuis
`playerId` et `itemId`.

## Viewport PAGE et ROW_SCROLL

Les slots sont ordonnés comme dans `content_slots`. Leur capacité est le nombre réel de slots.

- `PAGE` : `offset = (page - 1) × capacity`, `limit = capacity`;
- `ROW_SCROLL` : `offset = rowOffset × rowStride`, `limit = capacity`;
- `rowStride` est la largeur de la première ligne visible, donc chaque déplacement retire exactement
  la ligne du haut;
- le dernier offset vaut `ceil(max(0, total - capacity) / rowStride)`.

Cette formule rend le dernier élément atteignable même avec des lignes irrégulières, par exemple
`10-16, 19-23, 28-34`. Le moteur ne tente pas d'intercepter la molette physique du client 1.8.8 : le
protocole d'inventaire standard ne fournit pas un événement de scroll fiable. L'effet de scroll est
obtenu par des boutons avec `[kgui:navigate] direction=up|down`, ce qui est déterministe, compatible
Spigot/PandaSpigot et sans packets supplémentaires.

Les alias V1 restent acceptés : `next_page`, `prev_page`, `set_page`, `scroll_up`, `scroll_down`. Tous
modifient le même `ViewportState`; aucun second cache page/scroll n'existe.

## Invalidation ciblée

Le plugin consommateur publie une intention, même depuis un thread asynchrone :

```java
api.invalidate(InvalidationRequest.playerMenu(
    playerId,
    "faction_members",
    Collections.singleton("member:" + changedMemberId),
    "kfaction-member-role-changed"
));
```

Scopes disponibles :

| Scope | Sessions touchées | Cache provider |
|---|---|---|
| `PLAYER_MENU` | ce joueur dans ce menu | sujet correspondant |
| `PLAYER` | toutes les sessions de ce joueur | sujets du joueur |
| `MENU` | sessions affichant ce menu | sujets du menu |
| `PROVIDER` | sessions utilisant ce provider | provider complet |
| `ALL` | toutes les sessions indexées | cache complet |

Une requête peut cibler jusqu'à 256 `itemIds`; l'ordre est conservé et les doublons sont supprimés.
Chaque ID est limité à 128 caractères. Cette liste doit être exhaustive : si le total, l'ordre, des
placeholders globaux ou d'autres items peuvent changer, le producteur envoie une liste vide afin de
demander un refresh complet.
Kgui maintient des index directs joueur/menu/provider. L'invalidation ne fait donc aucun scan global
du registre des sessions. Les IDs ciblés permettent de réutiliser le frame statique et de ne refaire
que les items désignés lorsque leur `itemId` reste au même slot. Une nouvelle révision met toujours à
jour l'autorité de clic côté serveur, mais le diff visuel n'envoie au client que les slots dont
l'`ItemStack` a réellement changé.

Pour Kfaction, le mapping attendu au Lot 6 est : un événement membre cible les sessions du menu membre
et les items `member:<uuid>`; un changement banque cible le provider/menu banque; un changement de
relation cible les joueurs/factions concernés. Un événement ne doit jamais publier `ALL` par défaut.

## Scheduler, cache et budgets

Il existe une seule horloge de refresh pour Kgui. Les intervalles sont stockés dans une file des
prochaines échéances, pas obtenus en parcourant les GUI chaque seconde. La file de travail :

- est bornée et coalesce une demande par token de session;
- traite `INTERACTION`, puis `INVALIDATION`, puis `PERIODIC`;
- peut évincer un refresh périodique quand une interaction prioritaire arrive;
- borne le nombre de sessions et le temps CPU consommé par tick;
- abandonne naturellement les tokens de sessions fermées.

Le cache LRU est borné par sujet/révision/tranche. Une tranche en cache est réutilisée sans appeler le
provider; une révision identique réutilise aussi l'objet précédent. Les fermetures de session purgent
le sujet joueur et un disable provider purge toutes ses entrées.

Réglages de `config.yml` :

```yaml
performance:
  provider_cache_entries: 2048
  provider_slow_warning_ms: 2.0
  max_pending_refreshes: 2048
  max_refreshes_per_tick: 32
  refresh_budget_nanos: 2000000
```

Les valeurs par défaut visent une charge élevée sans autoriser une croissance non bornée. Elles se
mesurent avant d'être augmentées.

## Mesures et diagnostic

`/kgui debug <joueur>` expose la session, le mode/offset du viewport, la révision provider, la taille
du cache, les appels/hits/erreurs provider, le temps provider, les rendus, diffs, slots modifiés,
réouvertures d'inventaire, invalidations, taille de file et coalescences. La permission est
`kgui.debug`.

À surveiller pendant un test de charge :

- appels provider par seconde et temps moyen/maximal;
- ratio de cache hits;
- refresh coalescés et profondeur maximale de file;
- slots modifiés par diff;
- réouvertures d'inventaire, qui doivent rester à zéro à titre/taille stables;
- avertissements de lenteur ou d'erreur provider (chaque catégorie est limitée à un message par
  provider toutes les 30 secondes pour éviter un déluge de logs).

Un provider qui dépasse régulièrement 2 ms doit déplacer son calcul hors du chemin de rendu et publier
un snapshot mémoire quand son état métier change.
