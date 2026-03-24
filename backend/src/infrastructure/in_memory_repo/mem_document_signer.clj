(ns infrastructure.in-memory-repo.mem-document-signer
  (:require [domain.document-signer :as document-signer]
            [integrant.core :as ig]))

(defrecord InMemoryDocumentSigner [store counter auto-complete?]
  document-signer/DocumentSigner

  (create-html-signature-request [_ _html _email]
    (let [id (swap! counter inc)]
      (swap! store assoc id {:status (if auto-complete? :completed :pending)})
      {:submission-id id
       :signing-url   (str "https://docuseal.test/s/" id)}))

  (submission-completed? [_ submission-id]
    (= :completed (:status (get @store submission-id))))

  (get-signed-document-url [_ submission-id]
    (when-let [sub (get @store submission-id)]
      (when (= :completed (:status sub))
        (str "https://docuseal.test/d/" submission-id ".pdf")))))

(defn make-test-signer
  "Create a test signer that auto-completes submissions."
  []
  (->InMemoryDocumentSigner (atom {}) (atom 0) true))

(defmethod ig/init-key :docuseal/in-memory-gateway [_ _]
  (->InMemoryDocumentSigner (atom {}) (atom 0) false))

(defmethod ig/halt-key! :docuseal/in-memory-gateway [_ _] nil)
