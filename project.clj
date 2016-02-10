(defproject schema-fun "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/test.check "0.9.0"]
                 [clj-time "0.11.0"] ; required due to bug in `lein-ring uberwar`
                 [commons-codec "1.10"]
                 [metosin/compojure-api "1.0.0-RC1" :exclusions [commons-codec]]
                 [ring "1.4.0"]]
  :ring {:handler schema-fun.handler/app}
  :uberjar-name "server.jar"
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [cheshire "5.5.0"]
                                  [lein-ring "0.9.7"]]
                   }})
