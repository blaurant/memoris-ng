(ns domain.adhesion-html
  (:require [clojure.string :as str])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(defn render-adhesion-html
  "Render the Elink-co adhesion contract as HTML with DocuSeal field tags.
   The user's name and email are pre-filled as readonly fields."
  [user-name user-email]
  (let [escape-html (fn [s] (-> s
                                (str/replace "&" "&amp;")
                                (str/replace "<" "&lt;")
                                (str/replace ">" "&gt;")
                                (str/replace "\"" "&quot;")))
        name-esc    (escape-html user-name)
        email-esc   (escape-html user-email)
        today       (.format (LocalDate/now) (DateTimeFormatter/ofPattern "dd/MM/yyyy"))]
    (str
      "<!DOCTYPE html>
<html>
<head><meta charset=\"UTF-8\"><title>Adhésion Elink-co</title></head>
<body style=\"font-family: Arial, sans-serif; max-width: 700px; margin: 0 auto; padding: 20px; font-size: 14px; line-height: 1.6;\">

<h2 style=\"text-align: center; color: #2d6a4f;\">ADHÉSION ELINK-CO</h2>

<p>Je soussigné(e) <strong>" name-esc "</strong></p>
<p>Email : <strong>" email-esc "</strong></p>

<h3>Article 1 – Objet</h3>
<p>Le présent contrat a pour objet de définir les conditions dans lesquelles
le Consommateur participe à une opération d'autoconsommation collective
organisée par Elink-co, conformément aux articles L315-2 et suivants
du Code de l'énergie.</p>

<h3>Article 2 – Engagements du Consommateur</h3>
<p>Le Consommateur s'engage à :</p>
<ul>
  <li>Fournir des informations exactes sur son point de livraison (PRM Linky)</li>
  <li>Maintenir son contrat de fourniture d'électricité actif</li>
  <li>Informer Elink-co de tout changement de situation</li>
</ul>

<h3>Article 3 – Engagements de Elink-co</h3>
<p>Elink-co s'engage à :</p>
<ul>
  <li>Assurer la répartition équitable de l'énergie produite</li>
  <li>Fournir un suivi mensuel de la consommation</li>
  <li>Garantir la transparence des prix appliqués</li>
</ul>

<h3>Article 4 – Durée</h3>
<p>Le présent contrat est conclu pour une durée initiale de 12 mois,
renouvelable par tacite reconduction.</p>

<h3>Article 5 – Résiliation</h3>
<p>Chaque partie peut résilier le contrat avec un préavis de 30 jours.</p>

<h3>Article 6 – Protection des données</h3>
<p>Les données personnelles sont traitées conformément au RGPD.
Le Consommateur dispose d'un droit d'accès, de rectification
et de suppression de ses données.</p>

<p style=\"margin-top: 2rem;\">Fait le <strong>" today "</strong></p>

<p>Signature :</p>
<signature-field name=\"Signature\" role=\"Adherent\" style=\"width: 250px; height: 100px;\"></signature-field>

</body>
</html>")))
