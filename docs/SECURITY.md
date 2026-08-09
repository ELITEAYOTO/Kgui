# Sécurité des menus et clics

L'autorité est exclusivement serveur : joueur, owner du holder, identifiant de
session, révision de rendu, inventaire détenu par le holder, slot rendu et
binding de clic. Le contenu d'item renvoyé par le client n'accorde aucun droit.

Kgui annule les clics/drag/move concernant ses inventaires, refuse les slots du
bas pour les actions, limite les types de clic, borne la navigation et vérifie
à nouveau requirements/capabilities avant toute mutation.

Les clics de fenêtre falsifiée, les anciennes fenêtres après fermeture et les
callbacks d'une ancienne session sont ignorés. Les tags NBT servent au
nettoyage anti-fuite d'items, pas à authentifier une action.

Les actions console et économie doivent rester explicitement configurées et
ne jamais incorporer un argument utilisateur non validé. Les providers doivent
traiter leurs clics à partir de l'identifiant serveur du `ContentItem`.

