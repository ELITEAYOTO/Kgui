# Corrections Kgui

## Phase 2: Code mort / Refactoring (Complète)

### 2.4 KfactionHook - Variables mortes ✅
- Supprimé `kfactionInstance`, `factionManager`, `getFactionPowerMethod`, `getFactionMaxPowerMethod`
- Ces champs étaient déclarés mais jamais utilisés

### 2.5 Imports non utilisés ✅
- **AnimationManager.java**: Supprimé `MenuItem`, `Bukkit` (non utilisés)
- **ConfigManager.java**: Supprimé `YamlConfiguration`, `File`, `IOException`
- **MessageManager.java**: Supprimé `Player`, `IOException`

## Phase 3: Fonctionnalités Manquantes (Complète)

### 3.2 Système de cooldowns ✅
- Ajouté `Map<UUID, Long> actionCooldowns` dans GuiManager
- Ajouté `setActionCooldown()` et `canExecuteAction()` dans GuiManager
- GuiListener vérifie le cooldown avant d'exécuter les actions

### 3.4 HeadDatabase inline support ✅
- Ajouté support dans `GuiManager.buildMenuItem()`
- Vérifie `menuItem.getInlineHdb()` et utilise `HeadDatabaseHook.getHead()`
- Copie l'ItemMeta du builder vers l'item HDB

### 3.5 Auto-refresh menus ✅
- Ajouté `startAutoRefreshTask()` et `stopAutoRefreshTask()` dans OpenGui
- Utilise BukkitRunnable pour refresh périodique
- Support attribut `auto-refresh` dans MenuData

### 3.6 JavaScript conditions ✅
- Déjà implémenté comme checker "expression" dans RequirementManager
- Alias "javascript" disponible
- Supporte opérateurs: ==, !=, >, <, >=, <=, contains

### 3.8 KclassementHook ✅
- Déjà implémenté avec support complet
- Méthodes: `getKills()`, `getDeaths()`, `getKDR()`, `getKillstreak()`, `getLevel()`, etc.

### 3.9 Consolidation isRaidable() ✅
- Supprimé le doublon dans PowerManager
- ClaimManager.isRaidable() est la seule source de vérité

## Phase 4: Warnings (Revue)

### 4.1 Logger concatenation
- Ces warnings sont des faux positifs pour Bukkit (java.util.logging)
- Bukkit n'utilise pas SLF4J, donc `{}` n'est pas supporté
- Pattern actuel (+ concatenation) est correct pour java.util.logging

### 4.2 Catch génériques dans KfactionHook
- Les `catch (Exception e)` sont acceptables pour du code reflection
- InvocationTargetException peut wrapper n'importe quelle exception
- Pattern conservé intentionnellement

### 4.3 Autres warnings
- **GuiManager.fillInventory()**: Ajouté `@SuppressWarnings("unused")` pour paramètre `page`
  - Le paramètre est réservé pour pagination future
- **ConfigManager/MenuManager**: Appels de méthodes overridables dans constructeur
  - Pattern acceptable car classes sont finales fonctionnellement

## Bugs en jeu corrigés

### NPE TerritoryManager.getActionForBlock() ✅
- Sets `forbiddenBlocks`, `allowedBlocks`, `territoryBlocks` initialisés à la déclaration

### Double enregistrement PlaceholderAPI ✅
- Ajouté vérification `isRegistered()` avant `register()` dans PlaceholderAPIHook

### Logs verbose Kgui au démarrage ✅
- Consolidé les logs multi-lignes en une seule ligne
- Format: `v1.0.0 enabled in Xms | Y items, Z menus`

## Résumé

| Phase | Statut |
|-------|--------|
| Phase 1: Bugs critiques | ✅ Complète |
| Phase 2: Code mort | ✅ Complète |
| Phase 3: Fonctionnalités | ✅ Complète |
| Phase 4: Warnings | ✅ Revue (warnings mineurs acceptés) |
| Bugs en jeu | ✅ Corrigés |

**Compilation**: ✅ Succès (mvn clean compile)

## Phase 5: Documentation et Nettoyage

### 5.1 Documentation DOCUMENTATION_COMPLETE.yml ✅
- Mise à jour complète de la documentation
- Ajout des actions Kfaction: `[f_claim]`, `[f_home]`, `[f_invite]`, `[f_deposit]`, etc.
- Ajout des requirements: `faction_role`, `faction_power`, `exp`
- Correction: CIT inline dans les menus est supporté (pas seulement dans registry)
- Ajout de la section auto-refresh (`update_interval`)
- Ajout des placeholders Kfaction

### 5.2 Nettoyage dossier menus/ ✅
**Fichiers conservés (11):**
- `spawners.yml` - Menu paginé des spawners
- `quest.yml` - Menu paginé des quêtes
- `grades.yml` - Menu des grades/rangs
- `faction_menu.yml` - Menu principal faction
- `faction_members.yml` - Membres de faction
- `faction_claims.yml` - Territoires/Claims
- `faction_confirm_unclaim.yml` - Confirmation unclaim
- `faction_warps.yml` - Warps faction
- `faction_permissions.yml` - Permissions faction
- `faction_upgrades.yml` - Upgrades faction
- `faction_logs.yml` - Historique faction

**Fichiers supprimés (12):**
- `confirm.yml`, `crates.yml`, `daily_rewards.yml`, `enchants.yml`
- `example_paginated.yml`, `example_template.yml`
- `kits.yml`, `main_menu.yml`, `profile.yml`
- `settings.yml`, `shop.yml`, `warps.yml`

### 5.3 Mise à jour MenuManager.saveDefaultMenus() ✅
- Ne sauvegarde plus que les menus utiles au démarrage
- Ajouté: `spawners.yml`, `quest.yml`, `grades.yml`
- Conservé: tous les `faction_*.yml`

## Phase 6: Intégration Kgui/Kfaction (2025-06-30)

### 6.1 Rendu PaginationItems dans fillInventory() ✅
**Problème**: Les menus paginés avec contenu dynamique (logs faction) montraient 0 items.
- `KguiHook.openLogsMenuWithContent()` injectait correctement les `PaginationItems` via `initializeForPlayer()`
- Mais `fillInventory()` ne les rendait jamais - il parcourait uniquement `menuData.getItems()` (items YAML statiques)

**Solution**: Modifié `GuiManager.fillInventory()` pour:
1. Après avoir placé les items YAML statiques
2. Vérifier si le menu a des `content_slots` (est paginé)
3. Appeler `paginationManager.mapItemsToSlots()` pour récupérer les items dynamiques
4. Construire et placer les `ItemStack` via nouvelle méthode `buildPaginationItem()`

**Fichier modifié**: `GuiManager.java` lignes 188-305

### 6.2 Click handler pour PaginationItems ✅
**Problème**: Cliquer sur un item de pagination ne faisait rien.
- Les `MenuItem` statiques ont leurs actions définies dans le YAML
- Les `PaginationItems` dynamiques stockent leurs actions dans NBT

**Solution**: Modifié `GuiListener.onInventoryClick()` pour:
1. D'abord vérifier si l'item cliqué a la clé NBT `kgui_pagination_actions`
2. Si oui, parser et exécuter les actions (séparées par `||`)
3. Sinon, continuer avec la logique MenuItem normale

**Fichier modifié**: `GuiListener.java` lignes 68-88

### Impact
- Les menus `faction_logs.yml` affichent maintenant correctement l'historique
- Les items de pagination dynamiques sont cliquables et exécutent leurs actions

### 6.3 Fix: Slot conflict faction_logs.yml ✅
**Problème**: Le bouton retour (slot 45) ne fonctionnait pas car il était écrasé par `border_bottom`.

**Cause**: `border_bottom` utilisait `slots: "45,46,47,51,52,53"` incluant le slot 45, et puisque les deux items avaient la même priorité (0), l'ordre de placement était imprévisible.

**Solution**: Retiré slot 45 de `border_bottom` → `slots: "46,47,51,52,53"`

**Fichier modifié**: `faction_logs.yml` ligne 61

## Phase 7: Debug et Diagnostic (2025-02-20)

### 7.1 Debug logging Kfaction ✅
Ajout de logs de diagnostic pour tracer les problèmes de logs faction:
- **LogsCommand.java**: Affiche le nombre de logs récupérés avant ouverture du menu
- **KguiHook.java**: Affiche le nombre de logs passés à la pagination

Ces logs s'affichent uniquement en mode debug (config.yml: `debug: true`)

### Note: Permissions Relations
Pour les permissions ally/truce/neutral/enemy:
- **NON**, tu n'as pas besoin d'avoir un allié pour configurer les permissions
- Les permissions définissent ce que les factions de ce type POURRONT faire
- Si les boutons ne répondent pas, vérifier:
  1. Supprimer `plugins/Kgui/menus/` et redémarrer → regénère les fichiers menu par défaut
  2. Vérifier que tu es Leader ou CoLeader de la faction
  3. Activer `debug: true` dans config.yml de Kfaction pour voir les logs