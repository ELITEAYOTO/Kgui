# Performance

Architecture V2 : une horloge globale, une file bornée/coalescée, un index
d'invalidation par session, des snapshots providers bornés et un diff de slots.
Il n'existe aucun timer de refresh par joueur.

Budgets par défaut : 32 refresh/tick, 2 ms/tick et 2048 demandes en attente.
Objectifs de qualification : ouverture simple p95 ≤ 5 ms, dynamique p95 ≤
10 ms, scheduler moyen ≤ 1 ms/tick à 400 menus et p99 planifié ≤ 3 ms/tick.

`kgui diagnose` expose les p50/p95/p99/max des ouvertures, providers, rendus et
ticks du scheduler, ainsi que les slots rendus/envoyés, erreurs providers,
coalescences, rejets et pics de file. Les histogrammes ont une taille fixe et
ne conservent aucun échantillon individuel.

Les résultats du lot 8 sont consignés dans le rapport de livraison. Une montée
du nombre de threads `Craft Scheduler` pendant des centaines de connexions est
une caractéristique du scheduler Bukkit/fork et doit être suivie séparément des
compteurs Kgui.

