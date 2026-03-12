(ns infrastructure.init-seed.network
  (:require [domain.network :as network]
            [integrant.core :as ig]))

(def ^:private seed-networks
  [(network/build-network
     {:network/id         #uuid "11111111-0000-0000-0000-000000000001"
      :network/name       "Réseau EnR Bordeaux Métropole"
      :network/center-lat 44.8378
      :network/center-lng -0.5792
      :network/radius-km  10.0
      :network/lifecycle :public})
   (network/build-network
     {:network/id         #uuid "22222222-0000-0000-0000-000000000002"
      :network/name       "Réseau Solaire Lyon Sud"
      :network/center-lat 45.7597
      :network/center-lng 4.8422
      :network/radius-km  10.0
      :network/lifecycle :public})
   (network/build-network
     {:network/id         #uuid "33333333-0000-0000-0000-000000000003"
      :network/name       "Réseau élec de L'Erdre"
      :network/center-lat 47.3466
      :network/center-lng -1.5213
      :network/radius-km  10.0
      :network/lifecycle :public})
   (network/build-network
     {:network/id         #uuid "44444444-0000-0000-0000-000000000004"
      :network/name       "Réseau des Génices de la Nièvre"
      :network/center-lat 47.3054
      :network/center-lng 3.2537
      :network/radius-km  10.0
      :network/lifecycle :public})])

(defmethod ig/init-key :networks/seed [_ {:keys [repo]}]
  (when (empty? (network/find-all repo))
    (doseq [n seed-networks]
      (network/save! repo n))
    (count seed-networks)))

(defmethod ig/halt-key! :networks/seed [_ _] nil)
