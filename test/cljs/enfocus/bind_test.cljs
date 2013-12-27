(ns enfocus.bind-test
  (:require
   [enfocus.core :as ef :refer [at from content get-text html]]
   [enfocus.bind :as bind :refer [bind-view mget-in mset-in key-or-props
                                  save-form-to-atm]]
   [cemerick.cljs.test :as t])
  (:require-macros
   [enfocus.macros :as em]
   [cemerick.cljs.test :refer (is are deftest testing use-fixtures)]))

(defn each-fixture [f]
  ;; initialize the environment
  (let [div (.createElement js/document "div")]
    (.setAttribute div "id" "test-id")
    (.setAttribute div "class" "test-class")
    (.appendChild (.-body js/document) div)
    ;; execute the unit test
    (f)
    ;; clear the environment 
    (.removeChild (.-body js/document) div)))

(use-fixtures :each each-fixture)

(defn by-id [id]  (.getElementById js/document id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; HELPER FUNC TESTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest mget-in-test
  (testing "standard js-obj object"
    (let [obj (js-obj "a" (js-obj "c" 3) "b" 4)]
      (testing "simple field"
        (are [expected actual] (= expected actual)
             4 (mget-in obj "b")
             4 (mget-in obj :b)))
      (testing "multi leve acess"
        (are [expected actual] (= expected actual)
             3 (mget-in obj ["a" "c"])
             3 (mget-in obj [:a :c])
             3 (mget-in obj [:a "c"])
             3 (mget-in obj ["a" :c])))))
  (testing "standard clojurescript map"
    (let [obj {:a {:b 3 :c 4 :d {:a 5}} :b 2}]
      (testing "simple field"
        (is (= 2 (mget-in obj :b))))
      (testing "multi leve acess"
        (are [expected actual] (= expected actual)
             3 (mget-in obj [:a :b])
             5 (mget-in obj [:a :d :a]))))))


(deftest mset-in-test
  (testing "standard js-obj object"
    (let [obj (js-obj "a" (js-obj "c" "_") "b" "_")]
      (testing "simple field"
        (are [expected actual] (= expected actual)
             1 (.-b (mset-in obj "b" 1))
             2 (.-b (mset-in obj :b 2))))
      (testing "multi leve acess"
        (are [expected actual] (= expected actual)
             1 (.-c (.-a (mset-in obj ["a" "c"] 1)))
             2 (.-c (.-a (mset-in obj [:a "c"] 2)))
             3 (.-c (.-a (mset-in obj ["a" :c] 3)))
             4 (.-c (.-a (mset-in obj [:a :c] 4)))))))
  (testing "standard clojurescript map"
    (let [obj {:a {:b "bb" :c "cc" :d {:a "aa"}} :b "b"}]
      (testing "simple field"
        (is (= 2 (:b (mset-in obj :b 2)))))
      (testing "multi leve acess"
        (are [expected actual] (= expected actual)
             1 (:b (:a (mset-in obj [:a :b] 1)))
             2 (:a (:d (:a (mset-in obj [:a :d :a] 2)))))))))


(deftest key-or-prop-test
  (testing "getting the keys from an obj or map"
    (is (= [:a :b] (key-or-props {:a 2 :b 3})))
    (is (= ["a" "b"] (key-or-props (js-obj "a" 2 "b" 3))))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;   MAIN TESTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest bind-view-test
  (testing "binding a view to an atom"
    (let [atm (atom "initial")]
      (at "#test-id" (bind-view atm #(at %1 (content %2))))
      (testing "initial value set"
        (is (= "initial" (from "#test-id" (get-text)))))
      (testing "updated value set"
        (reset! atm "updated")
        (is (= "updated" (from "#test-id" (get-text))))))))


(deftest save-form-to-atm-test
  (let [form-frag (html [:form {:name "my-form"
                                    :id "my-form"}
                             [:input {:name "a" :value "a"}]
                             [:input {:name "b" :value "b"}]])]
    (at "#test-id" (content form-frag))
    (testing "atoms with maps"
      (testing "straight form to map mapping"
        (let [atm (atom {:a "_" :b "_" :c "c"})]
          (save-form-to-atm atm (by-id "my-form"))
          (is (= {:a "a" :b "b" :c "c"} @atm))))
      (testing "field mapping for simple map"
        (let [atm (atom {:a "_" :b "_" :c "c"})]
          (save-form-to-atm atm (by-id "my-form") {:a :b :b :a})
          (is (= {:a "b" :b "a" :c "c"} @atm))))
      (testing "field mapping for complex map"
        (let [atm (atom {:a "a" :b {:aa "aa" :bb "bb"} :c "c"})]
          (save-form-to-atm atm (by-id "my-form") {[:b :aa] :a
                                                   [:b :bb] :b})
          (is (= {:a "a" :b {:aa "a" :bb "b"} :c "c"} @atm)))))
    (testing "atoms as js-objs"
      (testing "straight form to obj mapping"
        (let [atm (atom (js-obj "a" "_" "b" "_" "c" "c"))]
          (save-form-to-atm atm (by-id "my-form"))
          (is (= "a" (.-a @atm)))
          (is (= "b" (.-b @atm)))
          (is (= "c" (.-c @atm)))))
      (testing "field mapping form to simple obj"
        (let [atm (atom (js-obj "a" "_" "b" "_" "c" "c"))]
          (save-form-to-atm atm (by-id "my-form") {:a :b :b :a})
          (is (= "b" (.-a @atm)))
          (is (= "a" (.-b @atm)))
          (is (= "c" (.-c @atm)))))
      (testing "field mapping form to complex obj"
        (let [atm (atom (js-obj "a" "a"
                                "b" (js-obj "aa" "aa"  "bb" "bb")
                                "c" "c"))]
          (save-form-to-atm atm (by-id "my-form") {[:b :aa] :a
                                                   [:b :bb] :b})
          (is (= "a" (.-a @atm)))
          (is (= "a" (.-aa (.-b @atm))))
          (is (= "b" (.-bb (.-b @atm))))
          (is (= "c" (.-c @atm))))))))


