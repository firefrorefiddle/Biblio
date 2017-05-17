(ns ^:figwheel-always churchlib.core
;  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [kioo.om :refer [content html-content add-class set-style set-attr do-> substitute listen lifecycle]]
            [kioo.core :as kioo]
;            [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
;            [om.dom :as dom :include-macros true]
;            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            )
  (:require-macros [kioo.om :refer [defsnippet deftemplate]]))  

(enable-console-print!)

(declare books-page users-page)

(def app-state
  (atom {::data {::books []
                 ::users []}
         ::app {::current-page ::books-page}}))

(def nav {::pages {::books-page {::view books-page ::href "/books"}
                   ::users-page {::view users-page ::href "/users"}}
          ::menu {::nav-left [{:name "Benutzer" :href "/users" ::page ::users-page}
                              {:name "Bücher" :href "/books" ::page ::books-page}]
                  ::nav-right [{:name "Logout" :href "/logout"}]}})

(s/def ::books (s/and seq?
                      #(every? map? %)))
(s/def ::data (s/keys :req [::books ::users]))
(s/def ::page #{::books-page ::users-page})
(s/def ::current-page ::page)
(s/def ::app (s/keys :req [::current-page]))
(s/def ::app-state (s/keys :req [::data ::app]))

(s/def ::view fn?)
(s/def ::pagedef (s/keys :req [::view ::href]))
(s/def ::books-page ::pagedef)
(s/def ::users-page ::pagedef)
(s/def ::pages (s/keys :req [::books-page ::users-page]))
(s/def ::link (s/keys :req-un [::name ::href] :opt [::page]))
(s/def ::nav-items (s/and vector? #(every? ::link %)))
(s/def ::nav-left ::nav-items)
(s/def ::nav-right ::nav-items)
(s/def ::menu (s/keys :req [::nav-left ::nav-right]))
(s/def ::nav (s/keys :req [::pages ::menu]))

(println (s/explain @app-state ::app-state))
(println (s/conform ::nav nav))
                       

;(defmacro setup-routes [

(secretary/defroute "/" []
  (swap! app-state #(assoc-in % [::app ::current-page] ::books-page)))

(secretary/defroute "/books" []
  (swap! app-state #(assoc-in % [::app ::current-page] ::books-page)))

(secretary/defroute "/users" []
  (swap! app-state #(assoc-in % [::app ::current-page] ::users-page)))


(defsnippet nav-item "churchlib/main.html" [:#main-navbar-nav :> [:li first-of-type]]
  [{:keys [name href churchlib.core/page] :as inp}]
  {[:li] (if (= page (get-in @app-state [::app ::current-page]))
           (add-class "active")
           identity)
   [:a] (do-> 
         (set-attr :href href)
         (content name))})

(defsnippet nav-bar "churchlib/main.html" [:#main-navbar-nav]
  [navitems]
  {[:ul] (do (println "nav-bar " navitems)
             (println (s/explain ::nav-items navitems))
             (content (map nav-item navitems)))})

(defsnippet nav-bar-right "churchlib/main.html" [:.navbar-right]
  [navitems]
  {[:ul] (do (println "nav-bar right")
             (println (s/explain ::nav-items navitems))             
             (content (map nav-item navitems)))})

(defsnippet brand "churchlib/main.html" [:a.navbar-brand]
  []
  {[:a.navbar-brand] (do-> (content "Biblio")
                           (set-attr :href "/"))})

(defsnippet data-line "churchlib/booklist.html" [:table :> :tbody :> [:tr first-of-type]]
  [{:keys [title author status]}]
  {[:td.title] (content title)
   [:td.author] (content author)
   [:td.status] (content status)})

(defsnippet books-page "churchlib/booklist.html" [:#app]
  [data]
  {[:table :> :tbody] (do (println "bookspage")
                          (s/conform ::app-state data)
                          (content (map data-line (get-in data [::data ::books]))))})
;  {[:#app] (content "Buecher")})

(defsnippet users-page "churchlib/users.html" [:#app]
  [a]
  {}) ; [:#app] (content "Users")})

(deftemplate biblio "churchlib/main.html"
  [data]
  {[:#main-navbar-nav] (do (println "biblio")
                           (println (s/conform ::app-state data))
                           
                           (substitute (nav-bar (get-in nav [::menu ::nav-left]))))
   [:.navbar-right] (do (println "biblio2")
                        (substitute (nav-bar-right (get-in data [::menu ::nav-right]))))
   [:a.navbar-brand] (do (println "biblio3") (substitute (brand)))
   [:#app] (do (println "biblio4")
               (let [curp (get-in data [::app ::current-page])
                     view (get-in nav [::pages curp ::view])]
                 (s/conform ::view view)
                 (substitute (view data))))
                                        ;(do
;                         (println (get-in @app-state [:app :current-page]))
;                         (books-page nil))
   ; ( nil))
   })

(defn init [data]
  (println "init")
  (println (s/conform ::app-state data))
  (om/component (biblio data)))


(println "Before xhr")
(println (s/conform ::app-state @app-state))
(println (s/explain ::app-state @app-state))

(edn-xhr
 {:method :get
  :url "/booksel"
  :on-complete
  (fn [res]
    (swap! app-state #(assoc % ::data {::books res ::users nil}))
    (println "after xhr")
    (println (s/explain ::app-state @app-state))
    (om/root init app-state
             {:target (.-body js/document)}))})
;      (om/root app-view app-state
;               {:target (.getElementById js/document "app")
;                :shared {:tx-chan tx-pub-chan}
;                :tx-listen
;                (fn [tx-data root-cursor]
;                  (put! tx-chan [tx-data root-cursor]))}))}))
         

(accountant/configure-navigation!
 {:nav-handler
  (fn [path]
    (secretary/dispatch! path))
  :path-exists?
  (fn [path]
    (secretary/locate-route path))})

(accountant/dispatch-current!)
