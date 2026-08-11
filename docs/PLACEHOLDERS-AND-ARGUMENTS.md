# Placeholders et arguments

Kgui résout d'abord ses variables de session (`%page%`, `%max_page%`, joueur,
menu et arguments), puis PlaceholderAPI si elle est active. Les résolutions
sont mises en cache par session selon `placeholder_cache_ticks`.

Les arguments runtime sont attachés au couple session/menu :

```text
[open] kjobs_detail job=mining
```

Le menu ou son provider peut ensuite lire l'argument `job`. Ils ne doivent pas
être stockés dans une map globale par UUID : leur durée de vie est celle de la
session et ils sont supprimés à la fermeture/quitte/désactivation.

Éviter les placeholders qui font des accès disque ou réseau. Lorsqu'une donnée
change, le plugin propriétaire doit publier une invalidation ciblée au lieu de
forcer un polling périodique.

