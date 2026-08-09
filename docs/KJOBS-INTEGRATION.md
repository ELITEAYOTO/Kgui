# Intégration KjobsUltimate

La référence détaillée est [KJOBS_PACK_V2.md](KJOBS_PACK_V2.md).

KjobsUltimate DEV enregistre cinq providers Kgui et les actions sous le
namespace `kjobsultimate:*`. Les menus fournis sont `kjobs_main`,
`kjobs_detail`, `kjobs_quests`, `kjobs_top`, `kjobs_settings` et
`kjobs_confirm_leave`.

Les données joueur sont chargées par Kjobs avant l'ouverture. La condition
`kjobsultimate:data_loaded` refuse proprement un menu pendant le chargement.
Les progressions publient des invalidations ciblées ; aucun menu Kjobs ne doit
être rafraîchi toutes les secondes sans changement métier.

