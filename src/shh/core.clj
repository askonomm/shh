(ns shh.core
  (:require [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [jansi-clj.core :as j]
            [clojure.string :as string])
  (:import [clojure.lang PersistentList]
           [java.io File])
  (:gen-class))


(def db* (atom {}))


(defn data-store-path []
  (str (System/getProperty "user.home") File/separatorChar ".shh.edn"))


(defn update-db
  "Runs as a callback to the db* watcher whenever a change occurs,
  after which it will update the `data-store-path` with new data."
  [_ _ _ new-state]
  (spit (data-store-path) new-state))


; run `update-db` whenever db* changes.
(add-watch db* :watcher update-db)


(def complexity->characters
  "1 maps letters, numbers and special characters.
  2 maps letters and numbers
  3 maps letters."
  {1 (map char (range 33 127))
   2 (map char (concat (range 48 58) (range 65 91) (range 97 123)))
   3 (map char (concat (range 65 91) (range 97 123)))})


(def ^:private messages
  {:name-of-pass       "Shh! What is the name of the password you are looking for?"
   :pass-not-found     "No such password found. Would you like to create one? (yes/no)"
   :create             "Creating a password called:"
   :desired-length     "What is the desired password length? (number of characters)"
   :desired-complexity "What is the desired password complexity?\n
   1: hard (letters, numbers, special characters)
   2: medium (letters, numbers)
   3: easy (letters)"
   :tag                "Add a tag to the password, or leave blank for the default tag."
   :copy               "Password copied to clipboard!"
   :change             "Changing the password for:"
   :update             "Password updated."
   :delete             "Password deleted."
   :not-a-number       "Oops, given input is not a number. Please try again."
   :cannot-copy        "Cannot copy password, displaying password instead ..."
   :password-is        "Password is: "})


(defn- say!
  "Prints a message with given `message-key`, and any additional items
  with the optional `args`."
  ([message-key]
   (say! message-key nil))
  ([message-key & args]
   (let [message (j/bold (message-key messages) (apply str args))]
     (println "\n#" message "\n"))))


(defn- dblist->dbmap
  "If the edn file is a list of passwords, then creates a map
  with default as key and db as value."
  [db]
  (if (instance? clojure.lang.PersistentVector db)
    {"default" db}
    db))


(defn- init-db
  "Checks if the database exists at `data-store-path` and if it
  does, will populate the `db*` with it. Otherwise, will leave `db*`
  as-is and create the database file."
  []
  (if (.exists (io/file (data-store-path)))
    (reset! db* (-> (slurp (data-store-path))
                    (read-string)
                    dblist->dbmap))
    (spit (data-store-path) "{ \"default\" []}")))


(defn- copy-password
  "Depending on the operating system used, attempts to copy the
  given `password` into clipboard."
  [password]
  (let [os (System/getProperty "os.name")]
    (cond
      (= "Linux" os)
      (try
        (sh/sh "xclip -sel clip" "<<<" :in password)
        (say! :copy)
        (catch Exception _
          (say! :cannot-copy)
          (say! :password-is password)))

      (= "Mac OS X" os)
      (do
        (sh/sh "pbcopy" "<<<" :in password)
        (say! :copy))

      (string/includes? os "Windows") ; for win 10 and 11 (and even 7)
      (do
        (sh/sh "clip" :in password)
        (say! :copy))

      :else
      (println "Password not copied.\nCurrently" os "is not supported"))))


(defn- ask-password-info
  "Asks the user information such as desired password length
   and complexity, which is needed to be able to generate
   a password."
  []
  (try
    {:length     (do (say! :desired-length)
                     (Integer/parseInt (read-line)))
     :complexity (do (say! :desired-complexity)
                     (Integer/parseInt (read-line)))}
    (catch NumberFormatException _
      (do (say! :not-a-number)
          (System/exit 1)))))


(defn- generate-from-provided-chars
  "Produces a string of 1000 random characters
  from the provided set of `chars`"
  [chars]
  (->> #(rand-nth chars)
       (repeatedly)
       (take 1000)
       (reduce str)))


(defn- generate-password
  "Generates a password with a given `length` and `complexity`."
  [{:keys [length complexity]}]
  (-> (get complexity->characters complexity)
      (generate-from-provided-chars)
      (subs 0 length)))

(defn- ask-for-tag []
  (let [tag (do (say! :tag)
                (read-line))]
    (if (string/blank? tag)
      tag
      "default")))

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
  (let [password (-> (ask-password-info)
                     (generate-password))
        tag (ask-for-tag)]
    (swap! db* (fn [db]
                 (update db tag conj {:name     name
                                   :password password})))
    (copy-password password)
    (System/exit 0)))


(defn- delete!
  "Deletes an item from the database by a given `name`."
  [name]
  (init-db)
  (reset! db* (->> @db*
                   (filterv #(not (= (:name %) name)))))
  (say! :delete)
  (System/exit 0))


(defn- change!
  "Attempts to change the password of an existing item."
  [name]
  (init-db)
  (when (find-by-name name)
    (let [password   (-> (ask-password-info)
                         (generate-password))
          updated-db (mapv (fn [item]
                             (if (= (:name item) name)
                               (merge item {:password password})
                               item))
                           @db*)]
      (reset! db* updated-db)
      (say! :update)
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
  (say! :name-of-pass)
  (let [name (read-line)]
    (if-let [entry (find-by-name name)]
      (do (copy-password (:password entry))
          (System/exit 0))
      (do (say! :pass-not-found)
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
