(ns infrastructure.docuseal.docuseal-gateway
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [com.brunobonacci.mulog :as mu]
            [domain.document-signer :as document-signer]
            [integrant.core :as ig]))

(defn- parse-submission-response
  "Extract submission-id and signing-url from DocuSeal API response.
   The response can be a single object or an array."
  [body]
  (let [raw        (json/read-str body :key-fn keyword)
        data       (if (sequential? raw) (first raw) raw)
        submitters (or (:submitters data) [])
        first-sub  (first submitters)]
    {:submission-id (or (:submission_id first-sub) (:id data))
     :signing-url   (:embed_src first-sub)}))

(defn- parse-document-url
  "Extract the first document URL from DocuSeal documents response."
  [body]
  (let [data (json/read-str body :key-fn keyword)
        docs (or (:documents data) (when (sequential? data) data))]
    (when (seq docs)
      (:url (first docs)))))

(defrecord DocuSealGateway [api-key api-base]
  document-signer/DocumentSigner

  (create-html-signature-request [_ html email]
    (let [url  (str api-base "/submissions/html")
          body (json/write-str
                 {:documents  [{:name "Adhésion Elink-co"
                                :html html}]
                  :send_email false
                  :submitters [{:role  "Adherent"
                                :email email}]})]
      (mu/log ::create-signature-request :email email)
      (let [resp (http/post url
                            {:headers      {"X-Auth-Token" api-key
                                            "Content-Type" "application/json"}
                             :body         body
                             :as           :string
                             :throw-exceptions false})]
        (if (<= 200 (:status resp) 299)
          (let [result (parse-submission-response (:body resp))]
            (mu/log ::signature-request-created
                    :submission-id (:submission-id result)
                    :signing-url   (:signing-url result)
                    :raw-body      (:body resp))
            result)
          (do
            (mu/log ::signature-request-failed
                    :status (:status resp)
                    :body   (:body resp))
            (throw (ex-info "DocuSeal API error"
                            {:status (:status resp)
                             :body   (:body resp)})))))))

  (submission-completed? [_ submission-id]
    (let [url  (str api-base "/submissions/" submission-id)
          resp (http/get url
                         {:headers          {"X-Auth-Token" api-key}
                          :as               :string
                          :throw-exceptions false})]
      (if (<= 200 (:status resp) 299)
        (let [data (json/read-str (:body resp) :key-fn keyword)]
          (= "completed" (:status data)))
        false)))

  (get-signed-document-url [_ submission-id]
    (let [url  (str api-base "/submissions/" submission-id "/documents")
          resp (http/get url
                         {:headers          {"X-Auth-Token" api-key}
                          :as               :string
                          :throw-exceptions false})]
      (if (<= 200 (:status resp) 299)
        (parse-document-url (:body resp))
        (do
          (mu/log ::get-document-failed
                  :status (:status resp)
                  :submission-id submission-id)
          nil)))))

(defmethod ig/init-key :docuseal/gateway [_ {:keys [api-key api-base]}]
  (mu/log ::docuseal-gateway-init :api-base api-base)
  (->DocuSealGateway api-key api-base))

(defmethod ig/halt-key! :docuseal/gateway [_ _] nil)
