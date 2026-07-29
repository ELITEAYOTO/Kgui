# Kgui Content Provider API

## Vue d'ensemble

La Content Provider API permet aux plugins externes (comme Kfaction) de fournir du contenu dynamique pour les menus paginés. Cette approche remplace l'ancien système d'injection par réflexion qui était peu fiable.

## Concepts clés

### DynamicContentProvider (Interface)
Interface que les plugins externes doivent implémenter pour fournir du contenu.

```java
public interface DynamicContentProvider {
    String getProviderId();
    List<DynamicItem> getContent(Player player, Map<String, String> args);
    void onClick(Player player, DynamicItem item, ClickType clickType);
}
```

### DynamicItem
Représente un item dynamique dans un menu paginé.

```java
DynamicItem item = new DynamicItem.Builder()
    .material("DIAMOND_SWORD")
    .name("&eMon Item")
    .lore(Arrays.asList("&7Ligne 1", "&7Ligne 2"))
    .glow(true)
    .customData("my_key", "my_value")
    .clickAction("[console] give %player% diamond 1")
    .build();
```

### ContentProviderManager
Gère l'enregistrement et l'appel des providers.

## Enregistrer un Provider (côté plugin externe)

```java
// Dans votre plugin (ex: Kfaction)
Plugin kgui = Bukkit.getPluginManager().getPlugin("Kgui");
if (kgui != null) {
    // Via reflection pour éviter les dépendances compile-time
    Method getContentProviderManager = kgui.getClass().getMethod("getContentProviderManager");
    Object manager = getContentProviderManager.invoke(kgui);
    
    // Créer et enregistrer votre provider
    Method register = manager.getClass().getMethod("register", String.class, Class.forName("me.krunsh.kgui.api.DynamicContentProvider"));
    register.invoke(manager, "myplugin_items", myProvider);
}
```

## Configuration YAML du Menu

Pour utiliser un provider dans un menu paginé:

```yaml
# menus/mes_logs.yml
menu_title: "&8Mes Logs"
size: 54
type: pagination

pagination:
  enabled: true
  content_slots: "10-16,19-25,28-34"
  prev_button_slot: 45
  next_button_slot: 53
  
  # === NOUVELLE API ===
  provider: "kfaction_logs"  # ID du provider enregistré
  provider_args:             # Arguments optionnels passés au provider
    type: "MEMBER_JOIN"
    limit: "50"
  empty_message: "&cAucun log trouvé"
  empty_item:
    material: BARRIER
    name: "&cAucun log"
    lore:
      - "&7Il n'y a aucun log à afficher"

items:
  prev_button:
    slots: [45]
    item_id: prev_page
    view_requirements:
      - type: has_previous_page
  
  next_button:
    slots: [53]
    item_id: next_page
    view_requirements:
      - type: has_next_page
```

## Providers Kfaction disponibles

| Provider ID | Description | Arguments |
|-------------|-------------|-----------|
| `kfaction_logs` | Logs de la faction du joueur | `type` (optionnel) - filtrer par type |
| `kfaction_members` | Membres de la faction | `role` (optionnel) - filtrer par rôle |
| `kfaction_warps` | Warps de la faction | - |

### Exemples d'utilisation

#### Menu des logs de faction
```yaml
pagination:
  enabled: true
  content_slots: "10-16,19-25,28-34"
  provider: "kfaction_logs"
```

#### Menu des membres avec filtre
```yaml
pagination:
  enabled: true
  content_slots: "10-16,19-25"
  provider: "kfaction_members"
  provider_args:
    role: "OFFICER"  # Afficher seulement les officiers
```

#### Menu des warps
```yaml
pagination:
  enabled: true
  content_slots: "10-16,19-25"
  provider: "kfaction_warps"
```

## Flux d'exécution

1. **Ouverture du menu**: GuiManager.openMenu() est appelé
2. **Vérification provider**: Si `menu.hasContentProvider()` est true
3. **Appel au provider**: `contentProviderManager.getContent(providerId, player, args)`
4. **Conversion**: Les `DynamicItem` sont convertis en `PaginationItem`
5. **Initialisation pagination**: `paginationManager.initializeForPlayer()` avec les items
6. **Remplissage**: fillInventory() affiche les items sur les slots configurés
7. **Clic**: Les actions de clic sont gérées via les `clickActions` stockées en NBT

## Migration depuis l'ancien système

L'ancien système utilisait `KguiHook.openLogsMenuWithContent()` qui injectait les items par réflexion. Avec la nouvelle API:

**Avant (manuel):**
```java
// Dans Kfaction
List<FactionLog> logs = logManager.getLogs(factionId);
kguiHook.openLogsMenuWithContent(player, logs);
```

**Après (automatique):**
```java
// Plus besoin de code Java!
// Configurer simplement le menu YAML avec provider: "kfaction_logs"
kguiHook.openMenu(player, "faction_logs");
```

Le provider `kfaction_logs` est automatiquement appelé lors de l'ouverture du menu.

## Debug

Activer le mode debug dans `config.yml` de Kgui:
```yaml
debug: true
```

Les logs afficheront:
- `[DEBUG] Provider 'xxx' returned N items for PlayerName`
- Les erreurs de provider non trouvé

## Avantages de la nouvelle API

1. **Déclaratif**: Configuration en YAML, pas de code
2. **Automatique**: Le contenu est chargé à l'ouverture du menu
3. **Fiable**: Pas de problèmes de timing avec la réflexion
4. **Extensible**: N'importe quel plugin peut enregistrer ses providers
5. **Filtrable**: Support des arguments pour filtrer le contenu
