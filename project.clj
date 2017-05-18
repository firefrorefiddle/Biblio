(defproject churchlib "0.1.0-SNAPSHOT"
  :description "Gemeindebibliothek"
  :url "http://christenhaid.at"
  :license {:name "All rights reserved"
            :url ""}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.542"]
                 [org.clojure/core.async "0.3.442"]
                 [org.omcljs/om "0.8.8"]
                 [clojure-future-spec "1.9.0-alpha16-1"]
                 [org.clojure/test.check "0.9.0"]
                 [com.google.guava/guava "21.0"] ; fixes mysterious error            
                 [om-sync "0.1.1"]
                 [enlive "1.1.6"]
                 [kioo "0.5.0-SNAPSHOT"]
                 [ring "1.5.1"]                  ; Getting errors with 1.6
                 [compojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.5130" :exclusions [joda-time]]
                 [secretary "1.2.3"]
                 [venantius/accountant "0.2.0"
                  :exclusions [org.clojure/tools.reader]]]

  :plugins [[lein-cljsbuild "1.1.6"]
            [lein-figwheel "0.5.10"]
            [cider/cider-nrepl "0.14.0"]]

  :source-paths ["src/clj" "src/cljs"]
  :resource-paths ["resources"]
  :clean-targets ^{:protect false} ["resources/public/js/out"
                                    "resources/public/js/main.js"]

  :figwheel {:ring-handler churchlib.core/handler}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/clj" "src/cljs" "resources/templates"]
                        :figwheel true
                        :compiler {:output-to "resources/public/js/main.js"
                                   :output-dir "resources/public/js/out"
                                   :main churchlib.core
                                   :asset-path "js/out"
                                   :optimizations :none
                                   :source-map true}}]})
