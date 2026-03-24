(ns domain.adhesion-html-test
  (:require [clojure.test :refer :all]
            [domain.adhesion-html :as adhesion-html]
            [clojure.string :as str]))

(deftest render-adhesion-html-test
  (testing "renders HTML with user name and email"
    (let [html (adhesion-html/render-adhesion-html "Jean Dupont" "jean@example.com")]
      (is (string? html))
      (is (str/includes? html "Jean Dupont"))
      (is (str/includes? html "jean@example.com"))
      (is (str/includes? html "ADHÉSION ELINK-CO"))
      (is (str/includes? html "signature-field"))))

  (testing "escapes HTML special characters in name"
    (let [html (adhesion-html/render-adhesion-html "Jean <script>" "test@test.com")]
      (is (str/includes? html "&lt;script&gt;"))
      (is (not (str/includes? html "<script>")))))

  (testing "name and email are rendered as static text"
    (let [html (adhesion-html/render-adhesion-html "Alice" "alice@test.com")]
      (is (str/includes? html "<strong>Alice</strong>"))
      (is (str/includes? html "<strong>alice@test.com</strong>")))))
