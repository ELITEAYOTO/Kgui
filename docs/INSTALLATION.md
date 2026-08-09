# Installation

## Prérequis validés

- Java 8 ;
- KHope/PandaSpigot 1.8.8 (`v1_8_R3`) ;
- aucun plugin obligatoire pour le cœur Kgui ;
- NBTAPI est embarqué/utilisé par Kgui pour marquer les items ;
- ProtocolLib, PlaceholderAPI, Vault, PlayerPoints, WorldGuard,
  CombatTagPlus et HeadDatabase sont des hooks optionnels.

## Installation propre

1. Arrêter complètement le serveur de développement.
2. Supprimer l'ancien `Kgui-1.0.0.jar` s'il existe. Ne jamais conserver deux
   versions du même plugin.
3. Copier `Kgui-2.0.0.jar` dans `plugins/`.
4. Copier le dossier `Kgui/` fourni, notamment `config.yml`, `messages.yml`,
   `menus/`, `items/` et `templates/`.
5. Si les packs sont utilisés, installer aussi `Kfaction-2.3.0.jar`,
   `KjobsUltimate-1.0.0-SNAPSHOT.jar` et `KSpawner-1.0.0.jar` avec leurs
   configurations à jour.
6. Démarrer le serveur et exécuter `kgui diagnose`, puis `kf doctor` si
   Kfaction est présent.

Le résultat attendu est `status=OK`, `menus=22`, `warnings=0`, `errors=0` et
`kfaction=READY_2_3`. Un redémarrage complet est requis après un changement de
JAR ; `/reload` et les gestionnaires de hot-reload ne sont pas supportés.

## Retour arrière

Conserver ensemble l'ancien JAR et son ancien dossier de configuration. Un
retour arrière doit restaurer les deux, serveur arrêté. Les configurations V2
ne sont pas promises compatibles avec la V1.

