(ns churchlib.core
  (:require [ring.util.response :refer [file-response]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer [defroutes GET PUT POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.edn :as edn]
            [datomic.api :as d]))

(def uri "datomic:free://localhost:4334/churchlib")
(def conn (d/connect uri))

(defn index []
  (file-response "public/html/index.html" {:root "resources"}))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defn get-books []
  (let [db (d/db conn)]
    (->> (d/q '[:find ?title ?author
                :where
                [?b :book/title ?title]
                [?b :book/author ?author]                
                ]
              db)
         vec)))
;         (map #(d/touch (d/entity db (first %))))
;         vec)))

(defn add-book [id author title]
  (d/transact conn [{:db/id id
                     :book/title title
                     :book/author author}]))

(defn books []
  (let [book-line (fn [[book author]] {:title book :author author :status "OK"})]
    (generate-response (map book-line (get-books)))))

(defn put-book [params]
  (let [title (:book/title params)
        author (:book/author params)
        id (:db/id params)]
    (println title author id)
    (add-book id title author)))

; [{:db/id #db/id[:db.part/db]
;   :db/ident :book/title
;   :db/index true
;   :db/valueType :db.type/string
;   :db/cardinality :db.cardinality/one
;   :db.install/_attribute :db.part/db}
;  {:db/id #db/id[:db.part/db]
;   :db/ident :book/author
;   :db/index true
;   :db/valueType :db.type/string
;   :db/cardinality :db.cardinality/one
;   :db.install/_attribute :db.part/db}]

;(defn get-classes [db]
;  (->> (d/q '[:find ?class
;              :where [?class :class/id]]
;           db)
;      (map #(d/touch (d/entity db (first %))))
;       vec))

;(defn init []
;  (generate-response
;   {:classes {:url "/classes" :coll (get-classes (d/db conn))}}))

;(defn create-class [params]
;  {:status 500})
;
;(defn update-class [params]
;  (let [id    (:class/id params)
;        db    (d/db conn)
;        title (:class/title params)
;        eid   (ffirst
;                (d/q '[:find ?class
;                       :in $ ?id
;                       :where 
;                       [?class :class/id ?id]]
;                     db id))]
;    (d/transact conn [[:db/add eid :class/title title]])
;    (generate-response {:status :ok})))
;
;(defn classes []
;  (generate-response (get-classes (d/db conn))))

(defroutes routes
  (GET "/" [] (index))
  (GET "/users" [] (index))
  (GET "/books" [] (index))    
;  (GET "/init" [] (init))
  (GET "/booksel" [] (books))
;  (POST "/books" {params :edn-body} (create-class params))
  (PUT "/booksel" {params :edn-body} (put-book params))
  (route/files "/" {:root "resources/public"}))

(defn read-inputstream-edn [input]
  (edn/read
   {:eof nil}
   (java.io.PushbackReader.
    (java.io.InputStreamReader. input "UTF-8"))))

(defn parse-edn-body [handler]
  (fn [request]
    (handler (if-let [body (:body request)]
               (assoc request
                 :edn-body (read-inputstream-edn body))
               request))))

(def handler 
  (-> routes
      parse-edn-body))
