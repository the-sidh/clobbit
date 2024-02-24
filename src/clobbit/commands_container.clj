(ns clobbit.commands-container)

(def command-not-found-exception-message "Command not found on container")
(defn command-from-container [container description]
  (let [command (get container description)]
    (if (nil? command)
      (throw (ex-info command-not-found-exception-message {:description description
                                                           :commands-container container}))
      command)))