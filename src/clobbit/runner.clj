(ns clobbit.runner
  (:require [schema.core :as s]
            [clobbit.schema :as schema]
            [clobbit.commands-container :as container]
            [clobbit.core :as core]))

(s/defn sugar-throw-exception
  [{:keys [node :- schema/Node context]}
   message
   cause]
  (throw (ex-info message
                  {:node    node
                   :cause   cause
                   :context context})))

(defn- run-command [command context]
  "Execute the command, with context as parameter to the function"
  (require (symbol (namespace command)))
  (apply (resolve command) [context]))

(s/defn run
  [{:keys                 [context]
    {:keys [description]} :node
    :as                   state} :- schema/State
   commands-container
   ] :- schema/State
  (let [command (container/command-from-container commands-container description)
        execution-result (try (run-command command context) (catch Exception _ (core/state-with-dropped-status state)))]
    (core/state-after-step-execution state execution-result)))

