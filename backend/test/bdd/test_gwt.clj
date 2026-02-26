(ns bdd.test-gwt
  (:require [clojure.test :as t]
            [clojure.string :as str]))

;; ============================================================
;; BDD DSL for Clojure — inspired by JGiven
;; ============================================================
;;
;; Usage:
;;   (defscenario "User registration"
;;     (GIVEN "a valid user" [ctx]
;;       (assoc ctx :email "user@example.com")
;;       (assoc ctx :gender "male")
;;       (assoc ctx :age 30))
;;
;;     (WHEN "the user submits the registration form" [ctx]
;;       (assoc ctx :response (api/post "/register" ctx)))
;;
;;     (THEN "the account is created" [ctx]
;;       (assert (= 201 (-> ctx :response :status)))
;;       (assert (some? (-> ctx :response :body :id)))))
;;
;; Each step receives a context map. Multiple expressions are
;; threaded automatically: if an expression returns a map it
;; becomes the new context, otherwise the context is preserved.
;; ============================================================

;; --- Report state (atom) ------------------------------------

(def ^:dynamic *report* (atom []))

(defrecord ScenarioReport
  [name stages result duration-ms error])

(defrecord StageReport
  [type label result error])

;; --- Step execution engine ----------------------------------

(defn run-step
  "Execute a single GIVEN/WHEN/THEN step. Returns [new-ctx stage-report]."
  [stage-type label step-fn ctx]
  (let [start (System/nanoTime)]
    (try
      (let [new-ctx (step-fn ctx)
            elapsed (/ (- (System/nanoTime) start) 1e6)]
        [new-ctx (->StageReport stage-type label :pass nil)])
      (catch Throwable e
        [ctx (->StageReport stage-type label :fail (.getMessage e))]))))

(defn run-steps
  "Execute a sequence of steps threading the context through.
   Stops at first failure. Returns [final-ctx [stage-reports]]."
  [steps initial-ctx]
  (loop [ctx       initial-ctx
         remaining steps
         reports   []]
    (if (empty? remaining)
      [ctx reports]
      (let [{:keys [stage-type label step-fn]} (first remaining)
            [new-ctx report] (run-step stage-type label step-fn ctx)
            reports' (conj reports report)]
        (if (= :fail (:result report))
          [new-ctx reports']
          (recur new-ctx (rest remaining) reports'))))))

;; --- DSL macros ---------------------------------------------

(defn- make-step
  "Build a step map. Each body expression is evaluated in sequence.
   If an expression returns a map, it becomes the new context.
   If it returns nil (like assert), the context is preserved.

   (GIVEN \"something\" [ctx]
     (assoc ctx :a 1)
     (assoc ctx :b 2)
     (assert (= 1 (:a ctx))))  ;; returns nil, ctx unchanged

   This means you never need a trailing `ctx` after assertions.
  "
  [stage-type label bindings body]
  (let [ctx-sym (first bindings)]
    `{:stage-type ~stage-type
      :label      ~label
      :step-fn    (fn [~ctx-sym]
                    ~(reduce
                       (fn [acc expr]
                         `(let [~ctx-sym ~acc
                                result# ~expr]
                            (if (map? result#) result# ~ctx-sym)))
                       ctx-sym
                       body))}))

(defmacro GIVEN
  "Define a Given step. Multiple expressions are threaded.
   Each expression receives the context returned by the previous one."
  [label bindings & body]
  (make-step :given label bindings body))

(defmacro WHEN
  "Define a When step. Multiple expressions are threaded."
  [label bindings & body]
  (make-step :when label bindings body))

(defmacro THEN
  "Define a Then step. Multiple expressions are threaded.
   Assertions (assert) return nil and are automatically ignored
   for threading — no need to add a trailing `ctx`."
  [label bindings & body]
  (make-step :then label bindings body))

;; --- Scenario macro -----------------------------------------

(defmacro defscenario
  "Define a BDD scenario as a clojure.test test.
   Steps are executed in order, threading a context map.

   Example:
     (defscenario \"User logs in successfully\"
       (GIVEN \"valid credentials\" [ctx]
         (assoc ctx :username \"alice\")
         (assoc ctx :password \"secret\"))

       (WHEN \"the user submits the login form\" [ctx]
         (assoc ctx :response (my-api/login (:username ctx) (:password ctx))))

       (THEN \"a token is returned\" [ctx]
         (assert (some? (-> ctx :response :token)))))
  "
  [scenario-name & steps]
  (let [test-sym (symbol (-> scenario-name
                              str/lower-case
                              (str/replace #"[^a-z0-9]+" "-")
                              (str/replace #"^-|-$" "")
                              (str "-scenario")))]
    `(t/deftest ~test-sym
       (let [steps#   [~@steps]
             start#   (System/nanoTime)
             [_ctx# reports#] (run-steps steps# {})
             elapsed# (/ (- (System/nanoTime) start#) 1e6)
             failed?# (some #(= :fail (:result %)) reports#)
             scenario-report# (->ScenarioReport
                                 ~scenario-name
                                 reports#
                                 (if failed?# :fail :pass)
                                 elapsed#
                                 (when failed?#
                                   (->> reports#
                                        (filter #(= :fail (:result %)))
                                        first
                                        :error)))]
         (swap! *report* conj scenario-report#)
         (print-scenario-report scenario-report#)
         (t/is (not failed?#)
               (str "Scenario failed: " ~scenario-name
                    " — " (:error scenario-report#)))))))

;; --- Scenario outline (parameterized) -----------------------

(defmacro defscenario-outline
  "Define a parameterized scenario that runs for each example.

   Example:
     (defscenario-outline \"Division\"
       (examples
         {:a 10 :b 2 :expected 5}
         {:a 9  :b 3 :expected 3})

       (GIVEN \"numbers a and b\" [ctx params]
         (merge ctx (select-keys params [:a :b])))

       (WHEN \"I divide a by b\" [ctx _]
         (assoc ctx :result (/ (:a ctx) (:b ctx))))

       (THEN \"the result is expected\" [ctx params]
         (assert (= (:expected params) (:result ctx)))))
  "
  [scenario-name examples-form & steps]
  (let [test-sym (symbol (-> scenario-name
                              str/lower-case
                              (str/replace #"[^a-z0-9]+" "-")
                              (str/replace #"^-|-$" "")
                              (str "-scenario-outline")))]
    `(t/deftest ~test-sym
       (let [example-rows# ~examples-form]
         (doseq [params# example-rows#]
           (let [steps# (mapv
                          (fn [step#]
                            (update step# :step-fn
                                    (fn [f#]
                                      (fn [ctx#] (f# ctx# params#)))))
                          [~@steps])
                 start#   (System/nanoTime)
                 [_ctx# reports#] (run-steps steps# {})
                 elapsed# (/ (- (System/nanoTime) start#) 1e6)
                 failed?# (some #(= :fail (:result %)) reports#)
                 label#   (str ~scenario-name " " (pr-str params#))
                 scenario-report# (->ScenarioReport
                                    label#
                                    reports#
                                    (if failed?# :fail :pass)
                                    elapsed#
                                    (when failed?#
                                      (->> reports#
                                           (filter #(= :fail (:result %)))
                                           first
                                           :error)))]
             (swap! *report* conj scenario-report#)
             (print-scenario-report scenario-report#)
             (t/is (not failed?#)
                   (str "Scenario failed: " label#
                        " — " (:error scenario-report#)))))))))

(defmacro examples
  "Sugar for defining example rows in a scenario outline."
  [& rows]
  `[~@rows])

;; --- Console report -----------------------------------------

(def ^:private stage-icons
  {:given "📋"
   :when  "⚡"
   :then  "✅"})

(def ^:private result-icons
  {:pass "✓"
   :fail "✗"})

(defn print-scenario-report
  "Pretty-print a single scenario report to the console."
  [{:keys [name stages result duration-ms]}]
  (let [color (if (= :pass result) "\u001b[32m" "\u001b[31m")
        reset "\u001b[0m"]
    (println)
    (println (str color "━━━ Scenario: " name
                  " [" (result-icons result) " "
                  (format "%.1fms" (double duration-ms)) "]" reset))
    (doseq [{:keys [type label result error]} stages]
      (let [icon   (stage-icons type "•")
            prefix (case type
                     :given "  Given "
                     :when  "  When  "
                     :then  "  Then  ")]
        (println (str color "  " icon prefix label
                      " " (result-icons result)
                      (when error (str " → " error))
                      reset))))
    (println)))

;; --- Full report --------------------------------------------

(defn print-full-report
  "Print a summary report of all scenarios."
  []
  (let [reports @*report*
        total   (count reports)
        passed  (count (filter #(= :pass (:result %)) reports))
        failed  (count (filter #(= :fail (:result %)) reports))
        total-ms (reduce + 0 (map :duration-ms reports))]
    (println)
    (println "══════════════════════════════════════════")
    (println (str "  BDD Report: " passed "/" total " passed"
                  (when (pos? failed)
                    (str ", " failed " failed"))
                  (str " (" (format "%.1fms" (double total-ms)) ")")))
    (println "══════════════════════════════════════════")
    (when (pos? failed)
      (println)
      (println "  Failed scenarios:")
      (doseq [{:keys [name error]} (filter #(= :fail (:result %)) reports)]
        (println (str "    ✗ " name ": " error))))
    (println)))

(defn reset-report!
  "Reset the report atom. Call before a test run."
  []
  (reset! *report* []))
