(defproject seq26 "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.reader "0.8.5"]

                 ;; CLJ
                 [ring/ring-core "1.3.0"]
                 [compojure "1.1.8"]

                 ;; CLJS
                 [org.clojure/clojurescript "0.0-2277"]
                 [org.clojure/core.async  "0.1.303.0-886421-alpha"]
                 [om "0.7.1"]]

  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-ring "0.8.11"]
            [lein-pdo "0.1.1"]]

  :aliases {"dev" ["pdo" "cljsbuild" "auto" "dev," "ring" "server-headless"]}

  :ring {:handler seq26.core/app
         :init    seq26.core/init}

  :source-paths ["src/clj"]

  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.4"]]}}

  :cljsbuild {
              :builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/seq26.js"
                                   :output-dir "resources/public/js/out"
                                   :optimizations :none
                                   :source-map true
                                   :externs ["react/externs/react.js"]}}
                       {:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/seq26.js"
                                   :source-map "resources/public/js/seq26.js.map"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :output-wrapper false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]
                                   :closure-warnings
                                   {:non-standard-jsdoc :off}}}]})
