(ns ^:figwheel-always churchlib.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [kioo.om :refer [content html-content set-style set-attr do-> substitute listen lifecycle]]
            [kioo.core :as kioo]
            [cljs.core.async :as async :refer [put! chan alts!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [om-sync.core :refer [om-sync]]
            [om-sync.util :refer [tx-tag edn-xhr]]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant])
  (:require-macros [kioo.om :refer [defsnippet deftemplate]]))  

(enable-console-print!)

(declare books-page)
(declare users-page)

(def app-state
  (atom {:data {:books []
                :users []}
         :app {:nav-left [["Benutzer" "/users"]
                          ["Bücher" "/books"]]
               :nav-right [["Logout" "/logout"]]
               :current-page books-page}}))

(defsnippet nav-item "churchlib/main.html" [:#main-navbar-nav :> [:li first-of-type]]
  [[name target]]
  {[:a] (do-> (content name)
              (set-attr :href target))})

(defsnippet nav-bar "churchlib/main.html" [:#main-navbar-nav]
  [navitems]
  {[:ul] (content (map nav-item navitems))})

(defsnippet nav-bar-right "churchlib/main.html" [:.navbar-right]
  [navitems]
  {[:ul] (content (map nav-item navitems))})

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
  {[:table :> :tbody] (do (println (:books (:data data)))
                          (content (map data-line (get-in data [:data :books]))))
   })
;  {[:#app] (content "Buecher")})

(defsnippet users-page "churchlib/users.html" [:#app]
  [a]
  {}) ; [:#app] (content "Users")})

(deftemplate biblio "churchlib/main.html"
  [data]
  {[:#main-navbar-nav] (substitute (nav-bar (get-in data [:app :nav-left])))
   [:.navbar-right] (substitute (nav-bar-right (get-in data [:app :nav-right])))
   [:a.navbar-brand] (substitute (brand))
   [:#app] (substitute ((get-in @app-state [:app :current-page]) @app-state))
                                        ;(do
;                         (println (get-in @app-state [:app :current-page]))
;                         (books-page nil))
   ; ( nil))
   })

(defn init [data]
  (om/component (biblio data)))

;(defn display [show]
;  (if show
;    #js {}
;    #js {:display "none"}))
;
;(defn handle-change [e data edit-key owner]
;  (om/transact! data edit-key (fn [_] (.. e -target -value))))
;
;(defn end-edit [data edit-key text owner cb]
;  (om/set-state! owner :editing false)
;  (om/transact! data edit-key (fn [_] text) :update)
;  (when cb (cb text)))
;
;(defn editable [data owner {:keys [edit-key on-edit] :as opts}]
;  (reify
;    om/IInitState
;    (init-state [_]
;      {:editing false})
;    om/IRenderState
;    (render-state [_ {:keys [editing]}]
;      (let [text (get data edit-key)]
;        (dom/li nil
;                (dom/span #js {:style (display (not editing))} text)
;                (dom/input
;                 #js {:style (display editing)
;                      :value text
;                      :onChange #(handle-change % data edit-key owner)
;                      :onKeyDown #(when (= (.-key %) "Enter")
;                                    (end-edit data edit-key text owner o;n-edit))
;                      :onBlur (fn [e]
;                                (when (om/get-state owner :editing)
;                                  (end-edit data edit-key text owner on-;edit)))})
;                (dom/button
;                 #js {:style (display (not editing))
;                      :onClick #(om/set-state! owner :editing true)}
;                 "Edit"))))))
;
;(defn app-view [app owner]
;  (reify
;    om/IRender
;    (render[{:keys [books]}]
;      (js/console.log books)
;      (dom/div nil
;               (dom/h1 nil "Bücher")
;               (for [book books]
;                 (dom/div nil
;                          (dom/p nil "ein buch")                        ;  
;                          (dom/p nil (str book))
;                          (dom/p nil (str (:book/title book) " by " (:book/author book)))))))))



;(defn create-class [classes owner]
;  (let [class-id-el   (om/get-node owner "class-id")
;        class-id      (.-value class-id-el)
;        class-name-el (om/get-node owner "class-name")
;        class-name    (.-value class-name-el)
;        new-class     {:class/id class-id :class/title class-name}]
;    (om/transact! classes [] #(conj % new-class)
;                  [:create new-class])
;    (set! (.-value class-id-el) "")
;    (set! (.-value class-name-el) "")))


;(defn books-view [books owner]
;  (reify
;    om/IRender
;    (render [_]
;      (dom/div #js {:id "app"}
;               (dom/h2 nil "Classes")
;               (apply dom/ul nil
;                      (map #(om/build editable % {:opts {:edit-key :class/title}})
;                           classes))
;               (dom/div nil
;                        (dom/label nil "ID:")
;                        (dom/input #js {:ref "class-id"})
;                        (dom/label nil "Name:")
;                        (dom/input #js {:ref "class-name"})
;                        (dom/button
;                         #js {:onClick (fn [e] (create-class classes owner))}
;                         "Add"))))))
;
;
;(defn app-view [app owner]
;  (reify
;    om/IWillUpdate
;    (will-update [_ next-props next-state]
;      (when (:err-msg next-state)
;        (js/setTimeout #(om/set-state! owner :err-msg nil) 5000)))
;    om/IRenderState
;    (render-state [_ {:keys [err-msg]}]
;      (dom/div nil
;               (om/build om-sync (:classes app)
;                         {:opts {:view classes-view
;                                 :filter (comp #{:create :update :delete} tx-tag)
;                                 :id-key :class/id
;                                 :on-success (fn [res tx-data] (println res))
;                                 :on-error (fn [err tx-data]
;                                             (reset! app-state (:old-state tx-data))
;                                             (om/set-state! owner :err-msg
;                                                            "Oops!"))}})
;               (when err-msg
;                 (dom/div nil err-msg))))))
;

;(let [tx-chan (chan)
;      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
(edn-xhr
 {:method :get
  :url "/booksel"
  :on-complete
  (fn [res]
    (swap! app-state #(assoc % :data {:books res :users nil}))
    (om/root init app-state
             {:target (.-body js/document)}))})
;      (om/root app-view app-state
;               {:target (.getElementById js/document "app")
;                :shared {:tx-chan tx-pub-chan}
;                :tx-listen
;                (fn [tx-data root-cursor]
;                  (put! tx-chan [tx-data root-cursor]))}))}))
         

(secretary/defroute "/" []
  (swap! app-state #(assoc-in % [:app ::current-page] books-page)))

(secretary/defroute "/books" []
  (swap! app-state #(assoc-in % [:app :current-page] books-page)))

(secretary/defroute "/users" []
  (swap! app-state #(assoc-in % [:app :current-page] users-page)))

(accountant/configure-navigation!
 {:nav-handler
  (fn [path]
    (secretary/dispatch! path))
  :path-exists?
  (fn [path]
    (secretary/locate-route path))})

(accountant/dispatch-current!)
