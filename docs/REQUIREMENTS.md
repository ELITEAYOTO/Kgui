# Conditions (requirements)

Les conditions existent à l'ouverture du menu, à l'affichage d'un item et au
clic. Elles sont nommées afin de produire un diagnostic et un message de refus
précis.

```yaml
open_requirements:
  permission:
    type: permission
    permission: example.open
    deny_message: "&cAccès refusé."
  money:
    type: money
    amount: 1000
  expression:
    type: expression
    expression: "%player_level% >= 10"
```


Les extensions enregistrent leurs propres types (`kfaction:available`,
`kfaction:has_faction`, `kfaction:capability`,
`kjobsultimate:data_loaded`, etc.). Une dépendance absente doit refuser la
condition proprement, jamais provoquer une erreur de classe.

Les expressions ne sont pas un langage de script arbitraire. Utiliser des
valeurs résolues, des opérateurs simples et des bornes explicites.

