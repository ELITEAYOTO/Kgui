# Diagnostic

Commandes principales :

- `kgui validate` : compile les menus et liste erreurs/avertissements ;
- `kgui reload` : recharge configuration et menus ;
- `kgui dump <menu>` : affiche le modèle compilé ;
- `kgui debug [joueur]` : session, viewport, hooks et compteurs ;
- `kgui diagnose` : sortie compacte, stable et utilisable par le harness.

Après la fermeture de tous les clients, les lignes suivantes doivent revenir à
zéro : `sessions`, `indexed`, `refresh.pending`, `refresh.periodic` et
`provider.cache` (hors snapshots neutres volontairement mis en cache).

Les signaux critiques sont `status=ERROR`, `errors>0`, `provider.errors>0`,
`refresh.rejected>0`, une file qui ne redescend pas, des sessions hors ligne ou
des `clicks.authority_rejected` inattendus avec un vrai client.

