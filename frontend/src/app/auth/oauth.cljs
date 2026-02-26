(ns app.auth.oauth
  (:require [app.config :as config]
            [re-frame.core :as rf]))

;; ── Script loading helper ────────────────────────────────────────────────────

(defn- load-script!
  "Injects a <script> tag once, calls on-ready when loaded."
  [src data-attr on-ready]
  (if (.querySelector js/document (str "script[" data-attr "]"))
    (on-ready)
    (let [script (.createElement js/document "script")]
      (set! (.-src script) src)
      (.setAttribute script data-attr "true")
      (set! (.-async script) true)
      (set! (.-onload script) on-ready)
      (.appendChild (.-head js/document) script))))

;; ── Google Sign-In ───────────────────────────────────────────────────────────

(defn google-sign-in!
  "Loads Google Identity Services SDK and triggers sign-in prompt."
  []
  (let [client-id config/GOOGLE_CLIENT_ID]
    (when (empty? client-id)
      (js/console.warn "GOOGLE_CLIENT_ID is not set"))
    (let [init-and-prompt
          (fn []
            (let [gis (.-accounts js/google)]
              (.initialize (.-id gis)
                           #js {:client_id client-id
                                :callback  (fn [response]
                                             (let [id-token (.-credential response)]
                                               (rf/dispatch [:auth/login-with-provider :google id-token])))})
              (.prompt (.-id gis))))]
      (if (and (exists? js/google) (exists? js/google.accounts))
        (init-and-prompt)
        (load-script!
          "https://accounts.google.com/gsi/client"
          "data-google-gsi"
          init-and-prompt)))))

;; ── Facebook Sign-In ─────────────────────────────────────────────────────────

(defn facebook-sign-in!
  "Loads Facebook JS SDK and triggers login."
  []
  (let [app-id config/FACEBOOK_APP_ID]
    (when (empty? app-id)
      (js/console.warn "FACEBOOK_APP_ID is not set"))
    (let [do-login
          (fn []
            (.login js/FB
                    (fn [^js response]
                      (when (= "connected" (.. response -status))
                        (let [token (.. response -authResponse -accessToken)]
                          (rf/dispatch [:auth/login-with-provider :facebook token]))))
                    #js {:scope "email,public_profile"}))]
      (if (exists? js/FB)
        (do-login)
        (do
          (set! js/fbAsyncInit
                (fn []
                  (.init js/FB #js {:appId  app-id
                                    :cookie true
                                    :xfbml  false
                                    :version "v18.0"})
                  (do-login)))
          (load-script!
            "https://connect.facebook.net/en_US/sdk.js"
            "data-facebook-sdk"
            (fn [])))))))

;; ── Apple Sign-In ────────────────────────────────────────────────────────────

(defn apple-sign-in!
  "Loads Apple Sign-In JS and triggers authorization."
  []
  (let [client-id config/APPLE_CLIENT_ID]
    (when (empty? client-id)
      (js/console.warn "APPLE_CLIENT_ID is not set"))
    (let [do-sign-in
          (fn []
            (-> (.init js/AppleID.auth
                       #js {:clientId    client-id
                            :scope       "name email"
                            :redirectURI (.-origin js/location)
                            :usePopup    true})
                (.then (fn [^js response]
                         (let [id-token (.. response -authorization -id_token)]
                           (rf/dispatch [:auth/login-with-provider :apple id-token]))))
                (.catch (fn [err]
                          (js/console.error "Apple Sign-In error:" err)))))]
      (if (exists? js/AppleID)
        (do-sign-in)
        (load-script!
          "https://appleid.cdn-apple.com/appleauth/static/jsapi/appleid/1/en_US/appleid.auth.js"
          "data-apple-auth"
          do-sign-in)))))
