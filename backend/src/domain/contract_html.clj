(ns domain.contract-html
  (:require [clojure.string :as str])
  (:import (java.time LocalDate)
           (java.time.format DateTimeFormatter)))

(defn- escape-html [s]
  (-> s
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- today-str []
  (.format (LocalDate/now) (DateTimeFormatter/ofPattern "dd/MM/yyyy")))

(defn- html-wrapper [title body-html]
  (str
    "<!DOCTYPE html>
<html>
<head><meta charset=\"UTF-8\"><title>" title "</title></head>
<body style=\"font-family: Arial, sans-serif; max-width: 700px; margin: 0 auto; padding: 20px; font-size: 14px; line-height: 1.6;\">
"
    body-html
    "
</body>
</html>"))

(defn render-producer-contract-html
  "Render the producer contract as HTML with DocuSeal signature field.
   User name and email are static text in the document."
  [user-name user-email]
  (let [name-esc  (escape-html user-name)
        email-esc (escape-html user-email)
        today     (today-str)]
    (html-wrapper
      "Contrat Producteur - Elink-co"
      (str
        "<h2 style=\"text-align: center; color: #2d6a4f;\">CONTRAT DE PRODUCTEUR D'ÉNERGIE RENOUVELABLE</h2>

<p>Entre <strong>Elink-co</strong>, association loi 1901, ci-après « l'Organisateur »,</p>
<p>Et <strong>" name-esc "</strong> (" email-esc "), ci-après « le Consommateur »,</p>

<h3>Article 1 – Objet</h3>
<p>Le présent contrat définit les modalités de participation du Consommateur
à l'opération de production d'énergie renouvelable dans le cadre de
l'autoconsommation collective organisée par Elink-co.</p>

<h3>Article 2 – Engagements du Producteur</h3>
<p>Le Producteur s'engage à :</p>
<ul>
  <li>Maintenir les installations de production en bon état de fonctionnement</li>
  <li>Respecter les normes de sécurité en vigueur</li>
  <li>Déclarer toute modification des installations</li>
</ul>

<h3>Article 3 – Répartition de la production</h3>
<p>La production est répartie entre les consommateurs selon les clés
de répartition définies par l'organisateur, conformément à la
réglementation en vigueur.</p>

<h3>Article 4 – Durée</h3>
<p>Le présent contrat est conclu pour une durée de 12 mois,
renouvelable par tacite reconduction.</p>

<h3>Article 5 – Résiliation</h3>
<p>Chaque partie peut résilier le contrat avec un préavis de 60 jours.</p>

<p style=\"margin-top: 2rem;\">Fait le <strong>" today "</strong></p>

<p>Signature du Consommateur :</p>
<signature-field name=\"Signature\" role=\"Signataire\" style=\"width: 250px; height: 100px;\"></signature-field>"))))

(defn render-sepa-mandate-html
  "Render the SEPA mandate as HTML with DocuSeal signature field.
   User name and email are static text in the document."
  [user-name user-email]
  (let [name-esc  (escape-html user-name)
        email-esc (escape-html user-email)
        today     (today-str)]
    (html-wrapper
      "Mandat SEPA - Elink-co"
      (str
        "<h2 style=\"text-align: center; color: #2d6a4f;\">MANDAT DE PRÉLÈVEMENT SEPA</h2>

<p>Titulaire du compte : <strong>" name-esc "</strong> (" email-esc ")</p>

<p>En signant ce mandat, vous autorisez Elink-co à envoyer des
instructions à votre banque pour débiter votre compte conformément
aux instructions de Elink-co.</p>

<p>Vous bénéficiez du droit d'être remboursé par votre banque selon
les conditions décrites dans la convention que vous avez passée
avec elle. Toute demande de remboursement doit être présentée
dans les 8 semaines suivant la date de débit de votre compte.</p>

<table style=\"border-collapse: collapse; margin: 1.5rem 0; width: 100%;\">
  <tr><td style=\"padding: 0.5rem; border: 1px solid #ccc; font-weight: bold;\">Type de paiement</td>
      <td style=\"padding: 0.5rem; border: 1px solid #ccc;\">Récurrent</td></tr>
  <tr><td style=\"padding: 0.5rem; border: 1px solid #ccc; font-weight: bold;\">Créancier</td>
      <td style=\"padding: 0.5rem; border: 1px solid #ccc;\">Elink-co</td></tr>
  <tr><td style=\"padding: 0.5rem; border: 1px solid #ccc; font-weight: bold;\">Identifiant créancier SEPA</td>
      <td style=\"padding: 0.5rem; border: 1px solid #ccc;\">FR00ZZZ000000</td></tr>
</table>

<p>Vos droits concernant le présent mandat sont expliqués dans un
document que vous pouvez obtenir auprès de votre banque.</p>

<p>En cas de litige sur un prélèvement, vous pouvez contacter
votre banque ou Elink-co directement.</p>

<p style=\"margin-top: 2rem;\">Fait le <strong>" today "</strong></p>

<p>Signature :</p>
<signature-field name=\"Signature\" role=\"Signataire\" style=\"width: 250px; height: 100px;\"></signature-field>"))))
