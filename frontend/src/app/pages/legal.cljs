(ns app.pages.legal)

(defn legal-page []
  [:div.landing
   [:section.section
    [:div.container
     [:h1.section__title {:style {:margin-bottom "1.5rem"}} "Mentions l\u00e9gales"]

     [:h2 {:style {:color "var(--color-green)" :margin-top "2rem" :margin-bottom "0.75rem"}}
      "Note d\u2019information relative au traitement des donn\u00e9es \u00e0 caract\u00e8re personnel des membres de l\u2019Association \u00ab\u00a0Elinkco Association\u00a0\u00bb"]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Contexte"]
     [:p "Dans le cadre de l\u2019organisation et de la mise en \u0153uvre d\u2019une op\u00e9ration d\u2019autoconsommation collective, l\u2019association \u00ab\u00a0Elinkco Association\u00a0\u00bb assure la coordination technique, administrative et financi\u00e8re entre le Gestionnaire du R\u00e9seau Public de Distribution (le \u00ab\u00a0GRD\u00a0\u00bb), les \u00e9tablissements bancaires partenaires et les participants \u00e0 l\u2019op\u00e9ration d\u2019autoconsommation collective."]
     [:p "\u00c0 ce titre, et conform\u00e9ment au R\u00e8glement g\u00e9n\u00e9ral sur la protection des donn\u00e9es personnelles, la PMO collecte et traite certaines donn\u00e9es personnelles relatives aux personnes physiques ou aux repr\u00e9sentants de personnes morales participant \u00e0 l\u2019Op\u00e9ration, qu\u2019il s\u2019agisse de producteurs d\u2019\u00e9lectricit\u00e9 (les \u00ab\u00a0Producteurs\u00a0\u00bb) ou de consommateurs finaux (les \u00ab\u00a0Consommateurs\u00a0\u00bb)."]
     [:p "Le fonctionnement de l\u2019Op\u00e9ration est subordonn\u00e9 \u00e0 la collecte et au traitement des donn\u00e9es personnelles des personnes vis\u00e9es. De ce fait, leur consentement \u00e0 la collecte et au traitement de leurs donn\u00e9es personnelles est rendue n\u00e9cessaire au moment de la conclusion aux conditions g\u00e9n\u00e9rales du contrat de vente d\u2019\u00e9lectricit\u00e9."]

     [:p {:style {:margin-top "0.75rem"}} "Ces traitements ont pour finalit\u00e9s exclusives\u00a0:"]
     [:ul
      [:li "la gestion op\u00e9rationnelle de l\u2019Op\u00e9ration, notamment les \u00e9changes avec le GRD (tel qu\u2019Enedis) pour la mise en \u0153uvre et le suivi de l\u2019autoconsommation collective\u00a0;"]
      [:li "la gestion des flux financiers li\u00e9s \u00e0 l\u2019Op\u00e9ration, incluant les relations avec les \u00e9tablissements bancaires (facturation, encaissement, reversement)\u00a0;"]
      [:li "la r\u00e9alisation d\u2019analyses et de statistiques internes, \u00e0 des fins d\u2019am\u00e9lioration et d\u2019optimisation du fonctionnement de l\u2019Op\u00e9ration, notamment en mati\u00e8re de consommation et de production d\u2019\u00e9lectricit\u00e9."]]

     [:p "Dans ce contexte, la pr\u00e9sente note a vocation \u00e0 informer les Producteurs et les Consommateurs\u00a0:"]
     [:ul
      [:li "des conditions dans lesquelles leurs donn\u00e9es personnelles sont collect\u00e9es, trait\u00e9es, conserv\u00e9es et, le cas \u00e9ch\u00e9ant, transmises par la PMO\u00a0;"]
      [:li "des droits dont ils disposent au regard de la r\u00e9glementation applicable en mati\u00e8re de protection des donn\u00e9es personnelles\u00a0;"]
      [:li "des cons\u00e9quences attach\u00e9es aux traitements mis en \u0153uvre dans le cadre de l\u2019Op\u00e9ration."]]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Notions importantes"]

     [:p [:strong "Donn\u00e9es \u00e0 caract\u00e8re personnel\u00a0:"] " toute information se rapportant \u00e0 une personne physique identifi\u00e9e ou identifiable. Est r\u00e9put\u00e9e \u00eatre une \u00ab\u00a0personne physique identifiable\u00a0\u00bb une personne physique qui peut \u00eatre identifi\u00e9e, directement ou indirectement, notamment par r\u00e9f\u00e9rence \u00e0 un identifiant, tel qu\u2019un nom, un num\u00e9ro d\u2019identification, des donn\u00e9es de localisation, un identifiant en ligne, ou \u00e0 un ou plusieurs \u00e9l\u00e9ments sp\u00e9cifiques propres \u00e0 son identit\u00e9 physique, physiologique, g\u00e9n\u00e9tique, psychique, \u00e9conomique, culturelle ou sociale."]

     [:p [:strong "Traitement des donn\u00e9es\u00a0:"] " op\u00e9ration, ou ensemble d\u2019op\u00e9rations, portant sur des donn\u00e9es personnelles, quel que soit le proc\u00e9d\u00e9 utilis\u00e9 (collecte, enregistrement, organisation, conservation, adaptation, modification, extraction, consultation, utilisation, communication par transmission ou diffusion ou toute autre forme de mise \u00e0 disposition, rapprochement)."]

     [:p [:strong "Consentement de la personne concern\u00e9e\u00a0:"] " repr\u00e9sente l\u2019accord de la personne concern\u00e9e \u00e0 ce que ses donn\u00e9es soient collect\u00e9es et utilis\u00e9es. C\u2019est une des six bases l\u00e9gales pr\u00e9vues par le RGPD. Il doit \u00eatre libre, sp\u00e9cifique, \u00e9clair\u00e9 et univoque."]

     [:p [:strong "Responsable du traitement\u00a0:"] " personne morale (entreprise, commune, etc.) ou physique qui d\u00e9termine les finalit\u00e9s et les moyens d\u2019un traitement, c\u2019est-\u00e0-dire l\u2019objectif et la fa\u00e7on de le r\u00e9aliser. En pratique et en g\u00e9n\u00e9ral, il s\u2019agit de la personne morale incarn\u00e9e par son repr\u00e9sentant l\u00e9gal."]

     [:p [:strong "Sous-traitant\u00a0:"] " personne morale (entreprise, commune, etc.) ou physique qui traite des donn\u00e9es pour le compte d\u2019un autre organisme (\u00ab\u00a0le responsable de traitement\u00a0\u00bb), dans le cadre d\u2019un service ou d\u2019une prestation."]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Cat\u00e9gorie de donn\u00e9es concern\u00e9es"]
     [:p "Dans le cadre de l\u2019Op\u00e9ration, la collecte et le traitement concernent, en particulier mais sans limitation, les donn\u00e9es suivantes. La transmission de ces informations rev\u00eat un caract\u00e8re obligatoire, sauf mention contraire\u00a0:"]
     [:ul
      [:li "Informations relatives \u00e0 l\u2019\u00e9tat civil complet des personnes concern\u00e9es (telles que le nom, le cas \u00e9ch\u00e9ant le nom de jeune fille, pr\u00e9noms, date et lieu de naissance, nationalit\u00e9, domicile)\u00a0;"]
      [:li "Coordonn\u00e9es personnelles et professionnelles (telles que l\u2019adresse, l\u2019adresse email, le num\u00e9ro de t\u00e9l\u00e9phone du domicile ou du portable)\u00a0;"]
      [:li "Donn\u00e9es bancaires (telles que les coordonn\u00e9es bancaires, les mandats de pr\u00e9l\u00e8vement, les informations relatives aux paiements)\u00a0;"]
      [:li "Donn\u00e9es de connexion (telles que les identifiants, les journaux de connexion, les adresses IP)\u00a0;"]
      [:li "Donn\u00e9es de localisation (notamment li\u00e9es aux points de livraison ou aux installations de production)\u00a0;"]
      [:li "Donn\u00e9es de consommation ou de production (notamment les donn\u00e9es issues des dispositifs de comptage, les historiques de consommation et de production)."]]
     [:p "Le traitement de ces donn\u00e9es est op\u00e9r\u00e9 sur la base de l\u2019ex\u00e9cution du contrat de vente d\u2019\u00e9lectricit\u00e9."]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Principes et objectifs du RGPD"]
     [:p "Dans un souci de renforcer la protection de la vie priv\u00e9e, le r\u00e8glement g\u00e9n\u00e9ral sur la protection des donn\u00e9es 2016/679 (le \u00ab\u00a0RGPD\u00a0\u00bb), compl\u00e9t\u00e9 par la loi du 20 juin 2018 adaptant la loi du 6 janvier 1978 relative \u00e0 l\u2019informatique, aux fichiers et aux libert\u00e9s, r\u00e9pond \u00e0 trois objectifs principaux."]
     [:p "Les trois principes fondamentaux du RGPD sont la transparence, la loyaut\u00e9 et la lic\u00e9it\u00e9."]
     [:p "Au sens du RGPD, la transparence \u00ab\u00a0exige que toute information et communication relatives au traitement de ces donn\u00e9es \u00e0 caract\u00e8re personnel soient ais\u00e9ment accessibles, faciles \u00e0 comprendre, et formul\u00e9es en des termes clairs et simples\u00a0\u00bb."]
     [:p "Ceci implique\u00a0:"]
     [:ul
      [:li "Que le responsable du traitement soit identifi\u00e9\u00a0;"]
      [:li "Que la finalit\u00e9 du traitement soit clairement \u00e9tablie\u00a0;"]
      [:li "Que les personnes concern\u00e9es disposent du droit d\u2019obtenir la confirmation sur le traitement r\u00e9serv\u00e9 \u00e0 leurs donn\u00e9es personnelles et la communication de leurs donn\u00e9es faisant l\u2019objet d\u2019un traitement\u00a0;"]
      [:li "La collecte et le traitement des donn\u00e9es doivent \u00eatre ad\u00e9quats, pertinents et limit\u00e9s \u00e0 ce qui est n\u00e9cessaire pour les finalit\u00e9s auxquelles les donn\u00e9es sont limit\u00e9es\u00a0;"]
      [:li "La dur\u00e9e au cours de laquelle les donn\u00e9es sont conserv\u00e9es doit \u00eatre limit\u00e9e au minimum n\u00e9cessaire\u00a0;"]
      [:li "La s\u00e9curit\u00e9 et la confidentialit\u00e9 des donn\u00e9es personnelles doit \u00eatre garantie par le responsable de traitement."]]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Les finalit\u00e9s du traitement des donn\u00e9es"]
     [:p "Dans le cadre de l\u2019op\u00e9ration d\u2019autoconsommation collective, le traitement des donn\u00e9es personnelles collect\u00e9es a pour finalit\u00e9\u00a0:"]
     [:ul
      [:li "D\u2019assurer la gestion op\u00e9rationnelle de l\u2019op\u00e9ration d\u2019autoconsommation (r\u00e9partition de l\u2019\u00e9lectricit\u00e9 produite entre les participants, calcul de la quantit\u00e9 d\u2019\u00e9nergie autoconsom\u00e9e, gestion des flux de production et de consommation)\u00a0;"]
      [:li "De permettre la gestion administrative et financi\u00e8re de l\u2019op\u00e9ration par la PMO (identification des participants, information des participants, gestion des contrats, facturation, relation avec les \u00e9tablissements bancaires)\u00a0;"]
      [:li "D\u2019assurer la maintenance et l\u2019optimisation du syst\u00e8me de fourniture et de production d\u2019\u00e9nergie (suivi de la production et de consommation, d\u00e9tection des anomalies)."]]
     [:p "Le traitement des donn\u00e9es peut \u00e9galement \u00eatre pr\u00e9vu \u00e0 des fins de recherche et de statistiques, afin que la PMO puisse optimiser la quantit\u00e9 d\u2019\u00e9nergie autoconsom\u00e9e."]
     [:p "Vos donn\u00e9es \u00e0 caract\u00e8re personnel sont conserv\u00e9es par la PMO pendant toute la dur\u00e9e de votre adh\u00e9sion \u00e0 l\u2019association Elinkco et de votre participation effective \u00e0 l\u2019op\u00e9ration d\u2019autoconsommation collective."]
     [:p "Elles peuvent \u00eatre archiv\u00e9es pendant la dur\u00e9e n\u00e9cessaire au respect des obligations l\u00e9gales et r\u00e9glementaires applicables, notamment en mati\u00e8re comptable et fiscale."]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Les droits des personnes concern\u00e9es"]
     [:p "Le traitement des donn\u00e9es personnelles mis en \u0153uvre dans le cadre de l\u2019Op\u00e9ration repose principalement sur l\u2019ex\u00e9cution du contrat liant les Participants \u00e0 la PMO, ainsi que, le cas \u00e9ch\u00e9ant, sur le respect d\u2019obligations l\u00e9gales et sur l\u2019int\u00e9r\u00eat l\u00e9gitime de la PMO."]
     [:p "Il est pr\u00e9cis\u00e9 que, conform\u00e9ment \u00e0 la r\u00e9glementation applicable, le droit d\u2019opposition ne s\u2019applique pas aux traitements fond\u00e9s sur l\u2019ex\u00e9cution du contrat, dans la mesure o\u00f9 ces traitements sont n\u00e9cessaires \u00e0 la fourniture des services."]
     [:p "En revanche, ce droit peut s\u2019exercer lorsque le traitement est fond\u00e9 sur l\u2019int\u00e9r\u00eat l\u00e9gitime de la PMO, sous r\u00e9serve de justifier de raisons tenant \u00e0 la situation particuli\u00e8re de la personne concern\u00e9e."]
     [:p "En cas de demande, les Producteurs et Consommateurs pourront s\u2019adresser \u00e0 J\u00fcrgen Klein via l\u2019adresse mail suivante\u00a0: "
      [:a {:href "mailto:j.klein@elinkco.fr" :style {:color "var(--color-green)"}} "j.klein@elinkco.fr"] "."]

     [:h3 {:style {:margin-top "1.5rem" :margin-bottom "0.5rem"}} "Identification des responsables de traitement et destinataires"]
     [:p "La PMO agit en tant que responsable de traitement, puisqu\u2019elle assure la mise en relation et la liaison technique entre tous les participants et les entit\u00e9s qui contribuent \u00e0 la mise en \u0153uvre et au fonctionnement de l\u2019Op\u00e9ration."]
     [:p "\u00c0 cet effet, elle collecte et traite non seulement les donn\u00e9es relatives \u00e0 la consommation d\u2019\u00e9nergie transmises par ENEDIS (avec l\u2019autorisation des personnes concern\u00e9es), mais \u00e9galement les donn\u00e9es auxquelles elle a acc\u00e8s au moment de l\u2019adh\u00e9sion des participants \u00e0 ladite PMO."]
     [:p "Le GRD et la PMO agissent en qualit\u00e9 de responsables de traitement distincts pour les traitements qu\u2019ils mettent respectivement en \u0153uvre."]
     [:p "FEP SOLUTION, en sa qualit\u00e9 de prestataire de service pour le compte de la PMO, est \u00e9galement impliqu\u00e9 dans le traitement des donn\u00e9es des personnes concern\u00e9es. La soci\u00e9t\u00e9 rev\u00eat ici la qualit\u00e9 de sous-traitant en mati\u00e8re de traitement des donn\u00e9es."]
     [:p "La PMO demeure toutefois responsable du traitement des donn\u00e9es transmises \u00e0 son sous-traitant."]
     [:p "Les autres membres de la PMO peuvent \u00eatre destinataires des donn\u00e9es, notamment les membres producteurs dans le cadre de la facturation."]
     [:p "ENEDIS agit de mani\u00e8re autonome, dans la mesure o\u00f9 elle collecte et traite d\u00e9j\u00e0 les donn\u00e9es relatives \u00e0 la consommation d\u2019\u00e9lectricit\u00e9 de ces derniers."]
     [:p "La communication de certaines donn\u00e9es par le GRD \u00e0 la PMO s\u2019effectue conform\u00e9ment \u00e0 la r\u00e9glementation applicable et peut n\u00e9cessiter, le cas \u00e9ch\u00e9ant, une autorisation pr\u00e9alable des personnes concern\u00e9es."]]]])
