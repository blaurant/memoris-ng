(ns app.consumptions.contract)

(def contract-text
  "CONTRAT D'AUTOCONSOMMATION COLLECTIVE ProxyWatt

Article 1 - Objet
Le présent contrat a pour objet de définir les conditions dans lesquelles
le Consommateur participe à une opération d'autoconsommation collective
organisée par ProxyWatt, conformément aux articles L315-2 et suivants
du Code de l'énergie.

Article 2 - Engagements du Consommateur
Le Consommateur s'engage à :
- Fournir des informations exactes sur son point de livraison (PRM Linky)
- Maintenir son contrat de fourniture d'électricité actif
- Informer ProxyWatt de tout changement de situation

Article 3 - Engagements de ProxyWatt
ProxyWatt s'engage à :
- Assurer la répartition équitable de l'énergie produite
- Fournir un suivi mensuel de la consommation
- Garantir la transparence des prix appliqués

Article 4 - Durée
Le présent contrat est conclu pour une durée initiale de 12 mois,
renouvelable par tacite reconduction.

Article 5 - Résiliation
Chaque partie peut résilier le contrat avec un préavis de 30 jours.

Article 6 - Protection des données
Les données personnelles sont traitées conformément au RGPD.
Le Consommateur dispose d'un droit d'accès, de rectification
et de suppression de ses données.")

(def producer-contract-text
  "CONTRAT DE PRODUCTEUR D'ÉNERGIE RENOUVELABLE

Article 1 - Objet
Le présent contrat définit les modalités de participation du Consommateur
à l'opération de production d'énergie renouvelable dans le cadre de
l'autoconsommation collective organisée par ProxyWatt.

Article 2 - Engagements du Producteur
Le Producteur s'engage à :
- Maintenir les installations de production en bon état de fonctionnement
- Respecter les normes de sécurité en vigueur
- Déclarer toute modification des installations

Article 3 - Répartition de la production
La production est répartie entre les consommateurs selon les clés
de répartition définies par l'organisateur, conformément à la
réglementation en vigueur.

Article 4 - Durée
Le présent contrat est conclu pour une durée de 12 mois,
renouvelable par tacite reconduction.

Article 5 - Résiliation
Chaque partie peut résilier le contrat avec un préavis de 60 jours.")

(def sepa-mandate-text
  "MANDAT DE PRÉLÈVEMENT SEPA

En signant ce mandat, vous autorisez ProxyWatt à envoyer des
instructions à votre banque pour débiter votre compte conformément
aux instructions de ProxyWatt.

Vous bénéficiez du droit d'être remboursé par votre banque selon
les conditions décrites dans la convention que vous avez passée
avec elle. Toute demande de remboursement doit être présentée
dans les 8 semaines suivant la date de débit de votre compte.

Type de paiement : Récurrent
Créancier : ProxyWatt SAS
Identifiant créancier SEPA : FR00ZZZ000000

Vos droits concernant le présent mandat sont expliqués dans un
document que vous pouvez obtenir auprès de votre banque.

En cas de litige sur un prélèvement, vous pouvez contacter
votre banque ou ProxyWatt directement.")
