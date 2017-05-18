(ns ^:figwheel-always churchlib.core
;  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [kioo.om :refer [content html-content append after before add-class set-style set-attr do-> substitute listen lifecycle]]
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
  (atom {::data {::books {}
                 ::users {}}
         ::app {::current-page ::books-page}}))

(def nav {::pages {::books-page {::view #'books-page ::href "/books"}
                   ::users-page {::view #'users-page ::href "/users"}}
          ::menu {::nav-left [{:name "Benutzer" :href "/users" ::page ::users-page}
                              {:name "Bücher" :href "/books" ::page ::books-page}]
                  ::nav-right [{:name "Logout" :href "/logout"}]}})

(s/def ::book (s/keys :req [:book/id :book/author :book/title]))
(s/def ::books (s/and map?
                      #(every? (fn [b] (s/conform ::book b)) %)))
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

(defmacro def-nav-route [link]
  `(secretary/defroute ~(:href link) []
     (swap! ~app-state #(assoc-in % [::app ::current-page] ~(::page link)))))

(secretary/defroute "/" []
  (swap! app-state #(assoc-in % [::app ::current-page] ::books-page)))

(secretary/defroute "/books" []
  (swap! app-state #(assoc-in % [::app ::current-page] ::books-page)))

(secretary/defroute "/users" []
  (swap! app-state #(assoc-in % [::app ::current-page] ::users-page)))

(defsnippet nav-item "main.html" [:#main-navbar-nav :> [:li first-of-type]]
  [{:keys [name href churchlib.core/page] :as inp}]
  {[:li] (if (= page (get-in @app-state [::app ::current-page]))
           (add-class "active")
           identity)
   [:a] (do-> 
         (set-attr :href href)
         (content name))})

(defsnippet nav-bar "main.html" [:#main-navbar-nav]
  [navitems]
  {[:ul] (do (println "nav-bar " navitems)
             (println (s/explain ::nav-items navitems))
             (content (map nav-item navitems)))})

(defsnippet nav-bar-right "main.html" [:.navbar-right]
  [navitems]
  {[:ul] (do (println "nav-bar right")
             (println (s/explain ::nav-items navitems))             
             (content (map nav-item navitems)))})

(defsnippet brand "main.html" [:a.navbar-brand]
  []
  {[:a.navbar-brand] (do-> (content "Biblio")
                           (set-attr :href "/"))})

(defsnippet add-info-line "booklist.html" [:table :> :tbody :> :tr.add_info]
  [data]
  {})

(defsnippet data-line "booklist.html" [:table :> :tbody :> :tr.viewing]
  [{:keys [title author status] :as data}]
  {[:td.title] (content title)
   [:td.author] (content author)
   [:td.status] (content status)
   [:input.edit_toggle] (listen :onClick #(om/transact! data (fn [st] (assoc st ::editing true))))})

(defsnippet edit-line "booklist.html" [:table :> :tbody :> :tr.editing]
  [{:keys [title author status] :as data}]
  {[:td.title :input] (do-> (set-attr :value title)
                            (listen :onChange
                                    #(om/transact! data
                                                   (fn [st]
                                                     (assoc st :title (.-value (.-target %)))))))
   [:td.author :input] (do-> (set-attr :value author)
                             (listen :onChange
                                     #(om/transact! data
                                                    (fn [st]
                                                      (assoc st :author (.-value (.-target %)))))))
   [:td.status :input] (do-> (set-attr :status title)
                             (listen :onChange
                                     #(om/transact! data
                                                    (fn [st]
                                                      (assoc st :status (.-value (.-target %)))))))
   [:input.edit_toggle] (listen :onClick #(om/transact! data (fn [st] (assoc st ::editing false))))})


(defn table-line "booklist.html" [{:keys [::editing] :as data}]
  (if editing
    (edit-line data)
    (data-line data)))

(defsnippet books-page "booklist.html" [:#app]
  [data]
  {[:table :> :tbody] (do (s/conform ::app-state data)
                          (content (for [book-id (keys (get-in data [::data ::books]))]
                                     (table-line (get-in data [::data ::books book-id])))))})

(defsnippet users-page "users.html" [:#app]
  [a]
  {}) ; [:#app] (content "Users")})

(deftemplate biblio "main.html"
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
                 (println "current page " curp)
                 (println (s/explain ::page curp))
                 (println (s/conform ::view view))
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


(accountant/configure-navigation!
 {:nav-handler
  (fn [path]
    (secretary/dispatch! path))
  :path-exists?
  (fn [path]
    (secretary/locate-route path))})

(om/root init app-state
         {:target (.-body js/document)})

(accountant/dispatch-current!)

(edn-xhr
 {:method :get
  :url "/booksel"
  :on-complete
  (fn [res]
    (swap! app-state #(assoc % ::data {::books res ::users nil}))
    (println "after xhr")
    (println (s/explain ::app-state @app-state)))})

;      (om/root app-view app-state
;               {:target (.getElementById js/document "app")
;                :shared {:tx-chan tx-pub-chan}
;                :tx-listen
;                (fn [tx-data root-cursor]
;                  (put! tx-chan [tx-data root-cursor]))}))}))
         
