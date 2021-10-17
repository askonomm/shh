(defproject shh "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]
                 [jansi-clj "0.1.1"]]
  :plugins [[lein-shell "0.5.0"]
            [lein-ancient "0.7.0"]]
  :aliases
  {"native"
   ["shell"
    "native-image" "--report-unsupported-elements-at-runtime" "--no-fallback"
    "-jar" "./target/shh.jar"]}
  :main shh.core
  :aot [shh.core]
  :uberjar-name "shh.jar"
  :min-lein-version "2.0.0"
  :repl-options {:init-ns shh.core})
