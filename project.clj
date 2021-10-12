(defproject shh "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {}
  :dependencies [[org.clojure/clojure "1.10.3"]]
  :main shh.core
  :aot [shh.core]
  :uberjar-name "shh.jar"
  :min-lein-version "2.0.0"
  :repl-options {:init-ns shh.core})
