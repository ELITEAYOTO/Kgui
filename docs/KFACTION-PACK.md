# Pack Kfaction 2.3

La référence détaillée est [KFACTION_PACK_V2.md](KFACTION_PACK_V2.md).

Le pack est optionnel et exige l'API Kfaction `2.3.x`. Son état apparaît dans
`kgui diagnose` : `ABSENT`, `INCOMPATIBLE` ou `READY_2_3`.

Les menus fournis couvrent le menu principal, membres, invitations, relations,
claims, warps, logs, quêtes, récompenses et Global Zones. Les providers
`kfaction:*` lisent des snapshots V2.3 ; les actions mutantes repassent toujours
par les services/capabilities Kfaction, jamais par les objets de rendu.

Les fichiers concernés sont `menus/faction_*.yml`. Ils peuvent être adaptés
visuellement sans recopier la logique métier dans Kgui.

