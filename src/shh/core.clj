(ns shh.core
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import (java.util UUID))
  (:gen-class))


(def db* (atom []))


(def data-store-path
  (str (System/getProperty "user.home") "/.shh.edn"))


(defn update-db
  "Runs as a callback to the db* watcher whenever a change occurs,
  after which it will update the `data-store-path` with new data."
  [_ _ _ new-state]
  (spit data-store-path new-state))


(add-watch db* :watcher update-db)


(def messages
  ["Shh! What is the name of the password you are looking for?"
   "No such password found. Would you like to create one? (yes/no)"
   "Creating a password called:"
   "What is the desired password length?"
   "Password copied!"
   "Changing a password for:"
   "Password updated and copied!"])


(defn- init-db
  ""
  []
  (if (.exists (io/file data-store-path))
    (reset! db* (-> (slurp data-store-path)
                    (read-string)))
    (spit data-store-path "[]")))


(defn- copy-password
  ""
  [password]
  (sh/sh "pbcopy" "<<<" :in (str password))
  (println (nth messages 4))
  (System/exit 0))


(defn- generate-password
  ""
  [length]
  (UUID/randomUUID))


(defn find-by-name
  ""
  [name]
  (->> @db*
       (filter #(= (:name %) name))
       first))


(defn- create!
  ""
  [name]
  (println (nth messages 2) name "...")
  (let [password-length (Integer/parseInt (read-line))
        password        (generate-password password-length)]
    (swap! db* conj {:name     name
                     :password password})
    (copy-password password)
    (println (nth messages 3))))


(defn- delete!
  ""
  [name]
  (init-db))


(defn- change!
  ""
  [name]
  (init-db)
  (println (nth messages 5) name "...")
  (let [password-length (Integer/parseInt (read-line))
        password        (generate-password password-length)
        updated-db      (mapv (fn [item]
                                (if (= (:name item) name)
                                  (merge item {:password password})
                                  item))
                              @db*)]
    (reset! db* updated-db)
    (copy-password password)
    (println (nth messages 6))))



(defn- list-items!
  ""
  []
  (init-db)
  (doseq [entry @db*]
    (println (:name entry))))


(defn init
  ""
  []
  (init-db)
  (println (nth messages 0))
  (let [name (read-line)]
    (if-let [entry (find-by-name name)]
      (copy-password (:password entry))
      (do (println (nth messages 1))
          (when (= (read-line) "yes")
            (create! name))))))


(defn argcmd
  "Parses a given list of `args` for a `command` and returns
  `true` if the command was found. If the command has a
  subcommand provided, then it will return that instead."
  [command args]
  (when (seq? args)
    (let [index (.indexOf args command)]
      (if-not (= -1 index)
        (if-let [subcommand (nth args (+ index 1) nil)]
          subcommand
          true)
        nil))))


(defn -main [& opts]
  (let [maybe-delete-item (argcmd "delete" opts)
        maybe-change-item (argcmd "change" opts)
        list-items?       (argcmd "list" opts)]
    (cond
      ; delete an item from db.
      (string? maybe-delete-item)
      (delete! maybe-delete-item)
      ; change an item in the db.
      (string? maybe-change-item)
      (change! maybe-change-item)
      ; display a list of all items in the db.
      list-items?
      (list-items!)
      ; if none of the above, let's get busy with the
      ; real work that is either creating or getting
      ; passwords.
      :else
      (init))))