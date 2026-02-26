(ns infrastructure.auth.token-verifier
  (:require [clojure.data.json :as json]
            [domain.token-verifier :as tv]
            [integrant.core :as ig])
  (:import [com.auth0.jwk JwkProviderBuilder]
           [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [java.net URL]
           [java.util.concurrent TimeUnit]))

;; ── JWKS providers ──────────────────────────────────────────────────────────

(def ^:private google-jwks-url
  "https://www.googleapis.com/oauth2/v3/certs")

(def ^:private apple-jwks-url
  "https://appleid.apple.com/auth/keys")

(defn- build-jwk-provider [url]
  (-> (JwkProviderBuilder. (URL. url))
      (.cached 10 24 TimeUnit/HOURS)
      (.build)))

;; ── Provider-specific verification ──────────────────────────────────────────

(defn- verify-google-token [jwk-provider client-id id-token]
  (let [decoded   (JWT/decode id-token)
        kid       (.getKeyId decoded)
        jwk       (.get jwk-provider kid)
        algorithm (Algorithm/RSA256 (.getPublicKey jwk) nil)
        verifier  (-> (JWT/require algorithm)
                      (.withIssuer (into-array String
                                    ["https://accounts.google.com"
                                     "accounts.google.com"]))
                      (.withAudience (into-array String [client-id]))
                      (.build))
        verified  (.verify verifier id-token)]
    {:sub   (.getSubject verified)
     :email (.asString (.getClaim verified "email"))
     :name  (.asString (.getClaim verified "name"))}))

(defn- verify-apple-token [jwk-provider client-id id-token]
  (let [decoded   (JWT/decode id-token)
        kid       (.getKeyId decoded)
        jwk       (.get jwk-provider kid)
        algorithm (Algorithm/RSA256 (.getPublicKey jwk) nil)
        verifier  (-> (JWT/require algorithm)
                      (.withIssuer (into-array String
                                    ["https://appleid.apple.com"]))
                      (.withAudience (into-array String [client-id]))
                      (.build))
        verified  (.verify verifier id-token)]
    {:sub   (.getSubject verified)
     :email (.asString (.getClaim verified "email"))
     :name  (or (.asString (.getClaim verified "name"))
                (.asString (.getClaim verified "email")))}))

(defn- verify-facebook-token [app-id access-token]
  (let [debug-url  (str "https://graph.facebook.com/debug_token"
                         "?input_token=" access-token
                         "&access_token=" app-id)
        debug-resp (slurp (URL. debug-url))
        me-url     (str "https://graph.facebook.com/me"
                         "?fields=id,name,email"
                         "&access_token=" access-token)
        me-resp    (slurp (URL. me-url))
        parse      (fn [s] (json/read-str s :key-fn keyword))]
    (let [_        (parse debug-resp)
          me-data (parse me-resp)]
      {:sub   (:id me-data)
       :email (:email me-data)
       :name  (:name me-data)})))

;; ── Record ──────────────────────────────────────────────────────────────────

(defrecord OAuthTokenVerifier [client-ids google-jwk-provider apple-jwk-provider]
  tv/TokenVerifier
  (verify-provider-token [_ provider id-token]
    (case provider
      :google   (verify-google-token google-jwk-provider
                                      (:google client-ids)
                                      id-token)
      :apple    (verify-apple-token  apple-jwk-provider
                                      (:apple client-ids)
                                      id-token)
      :facebook (verify-facebook-token (:facebook client-ids)
                                        id-token)
      (throw (ex-info "Unknown provider" {:provider provider})))))

(defmethod ig/init-key :auth/token-verifier
  [_ {:keys [client-ids]}]
  (->OAuthTokenVerifier
    client-ids
    (build-jwk-provider google-jwks-url)
    (build-jwk-provider apple-jwks-url)))

(defmethod ig/halt-key! :auth/token-verifier [_ _] nil)
