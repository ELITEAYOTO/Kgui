# Compatibilité

| Plateforme | Statut | Notes |
|---|---|---|
| KHope/PandaSpigot 1.8.8 | support principal | profil réellement compilé et testé |
| Spigot/Paper 1.8.8 | compatible attendu | API Bukkit utilisée, validation complète non exécutée |
| Paper moderne | non garanti | noms de matériaux, sons, NBT et limites de titre diffèrent |
| Java 8 | supporté | cible de compilation et runtime de test |
| Java 17+ | non qualifié | possible selon le serveur, mais hors matrice actuelle |

Kgui ne lie jamais directement Kfaction au démarrage. L'absence, l'activation
tardive ou la désactivation de Kfaction doit seulement faire passer le pack
dans un état `ABSENT`, `INCOMPATIBLE` ou `READY_2_3`. Les menus génériques
continuent de fonctionner.

Les menus utilisent les matériaux et sons de Minecraft 1.8.8. Une prise en
charge multi-version sérieuse devra introduire une couche de traduction ; elle
n'est volontairement pas incluse dans Kgui 2.0.

