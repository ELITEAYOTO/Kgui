# Kgui — Guide des Menus Système de Niveaux Faction

> Documentation des 4 menus GUI du système de niveaux créés pour Kfaction.
> Ces menus utilisent les **placeholders PlaceholderAPI** et les **Content Providers** dynamiques.

---

## Table des Matières

1. [Vue d'ensemble](#1-vue-densemble)
2. [Architecture des Menus](#2-architecture-des-menus)
3. [Placeholders disponibles](#3-placeholders-disponibles)
4. [Content Providers dynamiques](#4-content-providers-dynamiques)
5. [Personnalisation des menus](#5-personnalisation-des-menus)
6. [Définir les quêtes dans les GUIs](#6-définir-les-quêtes-dans-les-guis)
7. [Créer un nouveau menu lié au niveau](#7-créer-un-nouveau-menu-lié-au-niveau)
8. [Dépannage](#8-dépannage)

---

## 1. Vue d'ensemble

Le système de niveaux de faction utilise **4 menus** dans Kgui, accessibles depuis le menu `faction_upgrades.yml` :

```
faction_upgrades.yml (menu existant)
 ├── Slot 12 → faction_quests.yml     (Quêtes actives)
 ├── Slot 13 → faction_quest_category.yml (Choix de catégorie)
 ├── Slot 21 → faction_rewards.yml    (Arbre de récompenses)
 └── Slot 22 → faction_level.yml      (Vue d'ensemble du niveau)
```

### Accès joueur
- `/f upgrade` ou clic dans le menu principal → `faction_upgrades.yml`
- Les slots 12, 13, 21, 22 du menu upgrades redirigent vers les 4 nouveaux menus
- Le slot 13 (catégorie) nécessite le rôle **MODERATOR+** pour changer la catégorie

---

## 2. Architecture des Menus

### 2.1 `faction_level.yml` — Vue d'ensemble du niveau

| Propriété | Valeur |
|-----------|--------|
| Type | `normal` |
| Taille | 54 slots |
| Accès | Tous les membres de la faction |

**Layout :**
```
Ligne 1: [bg] [bg] [bg] [bg] [NIVEAU] [bg] [bg] [bg] [bg]
Ligne 2: [bg] [bg] [bg] [QUÊTES] [CATEG] [bg] [bg] [bg] [bg]
Ligne 3: [bg] [bg] [RECOMP] [CHEST] [FLY] [ANTISETHOME] [bg] [bg] [bg]
Ligne 4: [bg] [LEVEL1] [LEVEL2] [LEVEL3] [bg] [bg] [bg] [bg] [bg]
Ligne 5: [LEVEL4] [LEVEL5] [bg] [bg] [bg] [bg] [bg] [bg] [bg]
Ligne 6: [BACK] [bg] [bg] [bg] [INFO] [bg] [bg] [bg] [bg]
```

**Slots clés :**
- **Slot 4** : Icône titre avec XP/progressbar (`%kfaction_faction_level%`, `%kfaction_faction_progressbar%`)
- **Slot 12** : Bouton quêtes → ouvre `faction_quests`
- **Slot 13** : Bouton catégorie (MODERATOR+) → ouvre `faction_quest_category`
- **Slot 20** : Bouton récompenses → ouvre `faction_rewards`
- **Slots 21-23** : Statuts F-Chest / F-Fly / Anti-Sethome (utilise `*_display`)
- **Slots 30-32, 40-41** : Aperçu niveaux 1-5 (verre coloré)
- **Slot 45** : Retour au menu upgrades

### 2.2 `faction_quests.yml` — Quêtes actives

| Propriété | Valeur |
|-----------|--------|
| Type | `pagination` |
| Provider | `kfaction_quests` |
| Content slots | `20, 21, 22, 23, 24` |
| Update interval | 10 ticks |

**Fonctionnement :**
- Le menu appelle le content provider `kfaction_quests` automatiquement
- Le provider retourne les 3 quêtes actives de la faction du joueur
- Chaque quête affiche : nom, progression, barre de progression, XP
- Si aucune quête : affiche un item BARRIER avec suggestion d'ouvrir les catégories
- Slot 49 : Bouton "Changer catégorie" (MODERATOR+ requis)

### 2.3 `faction_quest_category.yml` — Choix de catégorie

| Propriété | Valeur |
|-----------|--------|
| Type | `normal` |
| Restriction | MODERATOR+ |
| Taille | 54 slots |

**Les 3 catégories :**

| Slot | Catégorie | Matériau | Commande exécutée |
|------|-----------|----------|-------------------|
| 13 | Mineur | `DIAMOND_PICKAXE` | `/f quest mineur` |
| 22 | Farmer | `DIAMOND_HOE` | `/f quest farmer` |
| 15 | Chasseur | `DIAMOND_SWORD` | `/f quest chasseur` |

Le clic exécute la commande **en tant que joueur**, ce qui déclenche le changement de catégorie + le reroll des quêtes actives. Le menu se ferme automatiquement après le clic.

### 2.4 `faction_rewards.yml` — Arbre de récompenses

| Propriété | Valeur |
|-----------|--------|
| Type | `pagination` |
| Provider | `kfaction_rewards` |
| Content slots | `10-16, 19-25` (14 slots) |

**Fonctionnement :**
- Le provider `kfaction_rewards` génère 10 items (un par niveau)
- Chaque item est un `STAINED_GLASS_PANE` coloré :
  - **Vert (data:5)** : Niveau débloqué (+ glow)
  - **Orange (data:1)** : Niveau en cours
  - **Rouge (data:14)** : Niveau verrouillé
- Le lore affiche les récompenses de chaque niveau depuis `levels.yml`
- Le slot 4 affiche le niveau actuel + légende des couleurs

---

## 3. Placeholders disponibles

Tous les placeholders sont fournis par **PlaceholderAPI** via l'expansion `kfaction`.
Format : `%kfaction_<placeholder>%`

### 3.1 Placeholders de base (texte brut)

| Placeholder | Description | Exemple |
|-------------|-------------|---------|
| `faction_level` | Niveau actuel de la faction | `5` |
| `faction_xp` | XP actuelle | `3200` |
| `faction_required_xp` | XP requise pour le prochain niveau | `8000` |
| `faction_progressbar` | Barre de progression formatée | `▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌` |
| `faction_category` | Catégorie de quête active | `Mineur` |
| `faction_quests_remaining` | Nombre de quêtes restantes | `2` |
| `faction_has_chest` | A le F-Chest | `true` / `false` |
| `faction_has_fly` | A le F-Fly | `true` / `false` |
| `faction_has_antisethome` | A l'anti-sethome | `true` / `false` |

### 3.2 Placeholders d'affichage (colorés, pour GUIs)

| Placeholder | Description | Résultat |
|-------------|-------------|----------|
| `faction_has_chest_display` | Statut F-Chest coloré | `§a✔ Débloqué` ou `§c✖ Verrouillé` |
| `faction_has_fly_display` | Statut F-Fly coloré | `§a✔ Débloqué` ou `§c✖ Verrouillé` |
| `faction_has_antisethome_display` | Statut Anti-Sethome coloré | `§a✔ Actif` ou `§c✖ Verrouillé` |
| `faction_role` | Rôle du joueur dans sa faction | `Leader` |

### 3.3 Utilisation dans les YML

```yaml
items:
  level_icon:
    slots: [4]
    material: NETHER_STAR
    name: "&6⭐ Niveau %kfaction_faction_level%"
    lore:
      - "&7XP: &e%kfaction_faction_xp% &7/ &e%kfaction_faction_required_xp%"
      - ""
      - "%kfaction_faction_progressbar%"
      - ""
      - "&7Catégorie: &a%kfaction_faction_category%"
```

> **Note :** Ces placeholders fonctionnent dans **tous les menus Kgui** car PlaceholderAPI est parsé automatiquement lors du rendu des items.

---

## 4. Content Providers dynamiques

### 4.1 Provider `kfaction_quests`

**ID :** `kfaction_quests`
**Enregistré par :** `KguiContentProviders.java` (dans Kfaction)

Ce provider retourne la liste des quêtes actives de la faction du joueur.

**Données retournées par quête :**

| Champ | Valeur |
|-------|--------|
| Material | Selon la catégorie : `DIAMOND_PICKAXE` (mineur), `DIAMOND_HOE` (farmer), `DIAMOND_SWORD` (chasseur) |
| Nom | `&e<display_quest>` |
| Lore | Progression `X / Y`, barre `[████████░░]`, XP reward, statut |
| Glow | Oui si la quête est complétée |

**Utilisation dans un menu YAML :**
```yaml
type: pagination
pagination:
  enabled: true
  provider: "kfaction_quests"
  content_slots: "20,21,22,23,24"
  update_interval: 10
  empty_item:
    material: BARRIER
    name: "&cAucune quête active"
    lore:
      - "&7Votre faction n'a aucune quête."
```

### 4.2 Provider `kfaction_rewards`

**ID :** `kfaction_rewards`
**Enregistré par :** `KguiContentProviders.java` (dans Kfaction)

Ce provider retourne les 10 niveaux de récompense avec leur statut de déblocage.

**Données retournées par niveau :**

| Champ | Valeur |
|-------|--------|
| Material | `STAINED_GLASS_PANE` |
| Data | `5` (vert/débloqué), `1` (orange/en cours), `14` (rouge/verrouillé) |
| Nom | `&aNiveau X ✔`, `&6Niveau X ⚡`, ou `&cNiveau X ✖` |
| Lore | XP requise + liste des récompenses depuis `levels.yml` |
| Glow | Oui si le niveau est débloqué |

**Utilisation dans un menu YAML :**
```yaml
type: pagination
pagination:
  enabled: true
  provider: "kfaction_rewards"
  content_slots: "10-16,19-25"
  empty_item:
    material: BARRIER
    name: "&cErreur"
```

---

## 5. Personnalisation des menus

### 5.1 Modifier l'apparence des items

Toutes les propriétés visuelles sont dans les fichiers YML :

```yaml
items:
  quest_button:
    slots: [12]
    material: BOOK_AND_QUILL    # Changer le matériau
    name: "&eMes Quêtes"        # Changer le nom
    lore:                        # Changer le lore
      - "&7Voir les quêtes en cours"
      - "&7Catégorie: &a%kfaction_faction_category%"
    click_actions:
      - "[open] faction_quests"  # Menu à ouvrir
```

### 5.2 Modifier les slots

Changez simplement la valeur `slots:` dans le YAML :
```yaml
items:
  quest_button:
    slots: [20]  # Déplacer le bouton quêtes au slot 20
```

### 5.3 Ajouter des conditions d'affichage

Utilisez `view_requirement` pour restreindre la visibilité d'un item :

```yaml
items:
  category_button:
    slots: [13]
    material: COMPASS
    view_requirement:
      type: placeholder
      placeholder: "%kfaction_faction_role%"
      values:
        - "Leader"
        - "CoLeader"
        - "Moderator"
```

### 5.4 Modifier les content_slots de pagination

Pour afficher plus/moins de quêtes par page :
```yaml
pagination:
  content_slots: "10-16,19-25,28-34"  # 21 slots = 3 lignes de quêtes
```

### 5.5 Changer le provider d'un menu

Pour utiliser un autre provider (par exemple un provider custom d'un autre plugin) :
```yaml
pagination:
  provider: "mon_plugin_items"  # Remplacer le provider
```

---

## 6. Définir les quêtes dans les GUIs

### 6.1 Comment les quêtes apparaissent dans les menus

Les quêtes NE sont PAS définies dans les fichiers YML de Kgui. Elles sont :

1. **Définies** dans `Kfaction/src/main/resources/quests.yml` (pool de quêtes)
2. **Gérées** par le `QuestManager` de Kfaction (sélection, tracking, progression)
3. **Affichées** via le content provider `kfaction_quests` dans Kgui

Le provider lit les données en temps réel depuis Kfaction et les transforme en `DynamicItem` :

```
quests.yml (config) → QuestManager (runtime) → KguiContentProviders (DynamicItem) → Kgui (affichage)
```

### 6.2 Modifier l'affichage d'une quête dans le GUI

Pour modifier comment les quêtes sont affichées dans le GUI, il faut éditer `KguiContentProviders.java` dans Kfaction. Les éléments configurables :

```java
// Matériau par catégorie
if (category.name().equals("MINEUR")) material = "DIAMOND_PICKAXE";
else if (category.name().equals("FARMER")) material = "DIAMOND_HOE";
else material = "DIAMOND_SWORD";

// Barre de progression
int filled = (int) ((double) progress / target * 20);
StringBuilder bar = new StringBuilder("&8[");
for (int b = 0; b < 20; b++) {
    bar.append(b < filled ? "&a█" : "&7░");
}
bar.append("&8]");
```

### 6.3 Ajouter des informations via placeholder

Pour afficher des informations supplémentaires dans le lore des quêtes, ajoutez un placeholder dans `KfactionExpansion.java` puis utilisez-le dans le YAML :

```yaml
lore:
  - "&7Bonus actif: %kfaction_faction_xp_multiplier%"
```

---

## 7. Créer un nouveau menu lié au niveau

### Étape 1 : Créer le fichier YML

Créez un fichier dans `Kgui/src/main/resources/menus/` :

```yaml
menu_title: "&8Mon Menu Custom"
size: 54
type: normal
open_sound: NOTE_PLING:1:1.5

items:
  background:
    slots: [0-8, 9, 17, 18, 26, 27, 35, 36, 44, 45-53]
    material: CLAY_BALL
    cit: background_2  # Bordure orange standard
    name: " "
    
  background_inner:
    slots: [10-16, 19-25, 28-34, 37-43]
    material: CLAY_BALL
    cit: background_1  # Fond jaune standard
    name: " "

  mon_item:
    slots: [22]
    material: NETHER_STAR
    name: "&6Niveau: %kfaction_faction_level%"
    lore:
      - "&7XP: %kfaction_faction_xp%/%kfaction_faction_required_xp%"
      
  back:
    slots: [45]
    material: ARROW
    name: "&cRetour"
    click_actions:
      - "[open] faction_level"
```

### Étape 2 : Lier depuis un menu existant

Ajoutez un bouton dans le menu qui doit ouvrir le nouveau menu :

```yaml
  mon_bouton:
    slots: [14]
    material: DIAMOND
    name: "&eMon Nouveau Menu"
    click_actions:
      - "[open] mon_menu_custom"
```

### Étape 3 : Avec un content provider (optionnel)

Si le menu nécessite du contenu dynamique :

```yaml
type: pagination
pagination:
  enabled: true
  provider: "mon_provider_id"
  content_slots: "10-16,19-25"
```

Et enregistrez le provider dans Java :
```java
// Via reflection depuis votre plugin
Plugin kgui = Bukkit.getPluginManager().getPlugin("Kgui");
Method getManager = kgui.getClass().getMethod("getContentProviderManager");
Object manager = getManager.invoke(kgui);
// ... voir KguiContentProviders.java pour l'exemple complet
```

---

## 8. Dépannage

### Les placeholders ne s'affichent pas
- Vérifier que **PlaceholderAPI** est installé
- Vérifier que Kfaction est chargé correctement
- Tester avec `/papi parse me %kfaction_faction_level%`

### Le content provider ne charge pas
- Activer le debug dans `Kgui/config.yml` : `debug: true`
- Chercher dans la console : `[DEBUG] Provider 'kfaction_quests' returned N items`
- Vérifier que Kfaction est chargé **avant** via `softdepend: [Kfaction]`

### Le menu catégorie ne s'ouvre pas
- Vérifier le rôle du joueur : seuls **MODERATOR, COLEADER, LEADER** peuvent y accéder
- Tester avec un admin : `/f promote <joueur>`

### Les barres de progression ne bougent pas
- Le `update_interval: 10` dans `faction_quests.yml` rafraîchit toutes les 0.5s
- Si la progression est bloquée, vérifier les logs de `QuestListener` dans Kfaction

### Les items du provider sont vides
- S'assurer que la faction a une catégorie définie : `/f quest <catégorie>`
- Vérifier que `quests.yml` contient des quêtes pour la catégorie choisie

---

*Dernière mise à jour: Session 4 Part 3 — Création des menus niveau faction*
