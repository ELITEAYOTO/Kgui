# Kgui API 2.0

`kgui-api` est le seul artifact Java que les plugins externes doivent compiler.
Il est declare en `provided` chez le consommateur et n'est jamais recopie dans
son JAR.

## Resolution

```java
KguiApi api = KguiApis.get();
KguiApiCompatibility compatibility = KguiApiCompatibility.evaluate(api);
if (!compatibility.isReady()) {
    // Desactiver uniquement l'integration GUI.
    return;
}
```

Kgui publie `KguiApi` dans le `ServicesManager` Bukkit. Un consommateur ne doit
ni caster le plugin principal, ni appeler `Kgui.getInstance()`, ni importer un
manager interne.

## Enregistrements possedes

Les providers, actions, requirements et packs sont enregistres avec le plugin
proprietaire. L'identifiant est namespace (`plugin:extension`) et le handle
retourne est idempotent :

```java
ActionRegistration registration = api.registerAction(
        plugin,
        "example:accept",
        context -> ActionResult.handledAndInvalidate()
);

// onDisable, facultatif mais recommande :
registration.close();
// ou api.unregisterAll(plugin);
```

Kgui supprime aussi automatiquement les extensions lors du
`PluginDisableEvent`, ce qui evite de retenir le classloader d'un plugin
desactive.

## Regles de donnees

- aucun `ItemStack`, inventaire ou modele interne dans le contrat ;
- `ContentRequest` est borne a 100 items ;
- `ContentSnapshot` est immutable et revisionne ;
- un clic provider transporte `providerId`, `itemId` et la revision rendue ;
- les listes et maps publiques sont des copies non modifiables ;
- les actions Kfaction passent par des handlers `kfaction:*`, jamais par `/f`.

Le branchement complet du renderer V2 sur `ContentProvider` et la validation
des clics revisionnes sont traites dans les lots renderer/securite suivants.
