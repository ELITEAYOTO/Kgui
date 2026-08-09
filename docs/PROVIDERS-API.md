# API des providers

La référence complète est
[KGUI_PROVIDERS_VIEWPORT_INVALIDATION_V2.md](KGUI_PROVIDERS_VIEWPORT_INVALIDATION_V2.md).

Un provider est propriétaire d'un namespace, reçoit un sujet immuable
(joueur/menu/arguments/offset/limit) et renvoie un snapshot borné avec :

- un identifiant/révision ;
- le nombre total d'éléments ;
- uniquement la tranche demandée ;
- des `ContentItem` dont l'identifiant est stable ;
- éventuellement un traitement de clic serveur.

Le plugin propriétaire s'enregistre avec un owner token et ferme ce handle à
sa désactivation. Il publie ensuite des invalidations ciblées par joueur,
menu/provider et, si possible, identifiants d'items. Kgui coalesce les demandes
et applique un budget global ; un provider ne doit jamais effectuer d'I/O
bloquante sur le thread principal.

