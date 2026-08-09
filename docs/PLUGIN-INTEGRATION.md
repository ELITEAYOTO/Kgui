# Intégrer un plugin à Kgui

1. Déclarer Kgui en `softdepend`.
2. Détecter son activation sans conserver de classe Kgui quand il est absent.
3. Enregistrer requirements, actions et providers sous un namespace unique.
4. Conserver les handles retournés.
5. Fermer tous les handles à `onDisable` et lorsque Kgui est désactivé.
6. Publier une invalidation après chaque mutation métier pertinente.

L'API Kgui transporte des DTO immuables. Kgui ne doit jamais mémoriser une
entité mutable interne de Kfaction/Kjobs et le plugin métier ne doit jamais
conserver `Player`, `Inventory` ou une session Kgui après leur fermeture.

Les intégrations doivent fonctionner dans les quatre ordres : propriétaire
absent, présent avant Kgui, activé après Kgui et désactivé avant Kgui. Les tests
de cycle de vie vérifient l'absence d'enregistrements orphelins.

