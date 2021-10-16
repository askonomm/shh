(ns shh.core
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io])
  (:import [clojure.lang PersistentList])
  (:gen-class))


(def db* (atom []))


(defn data-store-path []
  (str (System/getProperty "user.home") "/.shh.edn"))


(defn update-db
  "Runs as a callback to the db* watcher whenever a change occurs,
  after which it will update the `data-store-path` with new data."
  [_ _ _ new-state]
  (spit (data-store-path) new-state))


; run `update-db` whenever db* changes.
(add-watch db* :watcher update-db)


(def ^:private messages
  {:name-of-pass   "Shh! What is the name of the password you are looking for?"
   :pass-not-found "No such password found. Would you like to create one? (yes/no)"
   :create         "Creating a password called:"
   :desired-length "What is the desired password length?"
   :copy           "Password copied!"
   :change         "Changing the password for:"
   :update         "Password updated."
   :delete         "Password deleted."})
(defn- say!
  "Prints a message with given `message-key`, and any additional items
  with the optional `args`."
  ([message-key]
   (say! message-key nil))
  ([message-key & args]
   (println "\n" (message-key messages) (apply str args) "\n")))

(defn- init-db
  "Checks of the database exists at `data-store-path` and if it
  does, will populate the `db*` with it. Otherwise will leave `db*`
  as-is and create the database file."
  []
  (if (.exists (io/file (data-store-path)))
    (reset! db* (-> (slurp (data-store-path))
                    (read-string)))
    (spit (data-store-path) "[]")))


(defn- copy-password
  "Depending on the operating system used, attempts to copy the
  given `password` into clipboard."
  [password]
  (let [os (System/getProperty "os.name")]
    (cond
      (= "Linux" os)
      (do
        (sh/sh "xclip -sel clip" "<<<" :in password)
        (println (:copy messages)))
      (= "Mac OS X" os)
      (do
        (sh/sh "pbcopy" "<<<" :in password)
        (println (:copy messages)))
      :else
      (println "Password not copied.\nCurrently" os "is not supported"))))


(defn- generate-password
  "Generates a password with a given `length`."
  [length]
  (let [chars    (map char (range 33 127))
        password (take length (repeatedly #(rand-nth chars)))]
    (reduce str password)))


(defn find-by-name
  "Attempts to find an entry in the database by a given `name`.
  Will return `nil` if not found."
  [name]
  (->> @db*
       (filter #(= (:name %) name))
       first))


(defn- create!
  "Creates a new item in the database with a given `name`."
  [name]
  (println (:create messages ) name "...")
  (println (:desired-length messages))
  (let [password-length (Integer/parseInt (read-line))
        password        (generate-password password-length)]
    (swap! db* conj {:name     name
                     :password password})
    (copy-password password)
    (System/exit 0)))


(defn- delete!
  "Deletes an item from the database by a given `name`."
  [name]
  (init-db)
  (println (:update messages) name "...")
  (reset! db* (->> @db*
                   (filterv #(not (= (:name %) name)))))
  (println (:delete messages))
  (System/exit 0))


(defn- change!
  "Attempts to change the password of an existing item."
  [name]
  (init-db)
  (when (find-by-name name)
    (println (:change messages) name "...")
    (println (:desired-length messages))
    (let [password-length (Integer/parseInt (read-line))
          password        (generate-password password-length)
          updated-db      (mapv (fn [item]
                                  (if (= (:name item) name)
                                    (merge item {:password password})
                                    item))
                                @db*)]
      (reset! db* updated-db)
      (println (:update messages))
      (copy-password password)
      (System/exit 0))))


(defn- list-items!
  "Lists all the names of the items in db."
  []
  (init-db)
  (doseq [entry @db*]
    (println (:name entry)))
  (System/exit 0))


(defn find-or-create!
  "Attempts to find a password from user given input, and
  offers to create one instead upon failure."
  []
  (init-db)
  (println (:name-of-pass messages))
  (let [name (read-line)]
    (if-let [entry (find-by-name name)]
      (do (copy-password (:password entry))
          (System/exit 0))
      (do (println (:pass-not-found messages))
          (if (= (read-line) "yes")
            (create! name)
            (System/exit 0))))))


(defn argcmd
  "Parses a given list of `args` for a `command` and returns
  `true` if the command was found. If the command has a
  subcommand provided, then it will return that instead."
  [command ^PersistentList args]
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
      (find-or-create!))))