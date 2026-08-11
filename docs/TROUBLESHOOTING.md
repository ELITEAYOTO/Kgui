# Dépannage

## Un menu n'est pas chargé

Exécuter `kgui validate`. Vérifier `schema_version: 2`, l'unicité de `id`, la
taille, les slots et le nom du provider. Un template doit lui aussi annoncer le
schéma V2.

## Le clic ou la pagination ne répond pas

Vérifier `kgui debug <joueur>` puis `kgui diagnose`. La session et le holder
doivent partager le même owner/id/révision. Ne jamais tester immédiatement au
milieu des tâches de connexion automatisées : attendre la fin de l'initialisation
du joueur. Kgui 2.0 accepte les wrappers d'inventaire PandaSpigot en validant
l'inventaire stable détenu par le holder.

## Le provider est absent

Vérifier le plugin propriétaire et sa version, puis l'état des hooks. Kfaction
doit être `READY_2_3`. Redémarrer complètement après un changement de JAR.

## Le CPU monte

Rechercher un provider lent, trop de placeholders, des invalidations globales
ou un intervalle périodique. Ne pas augmenter le budget avant d'avoir identifié
la source. Comparer `rendered slots` et `sent slots` pour vérifier le diff.

## La mémoire monte

Fermer les clients, forcer un Full GC sur le serveur d'audit et vérifier les
sessions/index/files. Une hausse avant GC mesure des allocations, pas une fuite.

