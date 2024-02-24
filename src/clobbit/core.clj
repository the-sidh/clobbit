(ns clobbit.core
  (:require [clobbit.schema :as schema]
            [clobbit.commands-container :as container]
            [schema.core :as s]))

(def unknown-outcome-exception-message "Unknown outcome for command execution. Aborting saga execution")
(def no-defined-handler-for-failure-exception-message "The command failed and there isn't an action defined to handle the failure. Aborting saga execution")

(s/defn sugar-throw-exception
  [{:keys [node
           context]}
   message
   cause]
  (throw (ex-info message
                  {:node    node
                   :cause   cause
                   :context context})))

(defn- execution-success?
  "asserts if the execution result was success"
  [execution-result]
  (= {:outcome :success} execution-result))

(defn- execution-failure?
  "asserts if the execution result was failure"
  [execution-result]
  (= {:outcome :failure} execution-result))

(defn- state-on-success
  "returns an update state on case of the command's execution being considered as success, with the former :next-node-on-success as root node"
  [{{:keys [next-node-on-success]} :node
    :as                            state}]
  (merge state {:node next-node-on-success}))

(defn- state-on-failure
  "returns an update state on case of the command's execution being considered as failure
    Rules:
    - Given that there are retry attempts left, decrease this value by one
    - If there are no retry attempts left and there is no next-node-on-failure defined, raises an exception
      - If there are no retry attempts left, change the state's node, with the previous next-node-on-failure as the new node\n"
  [{:keys                                        [node]
    {:keys [attempts-left next-node-on-failure]} :node
    :as                                          state}]
  (cond
    (> attempts-left 0) (as-> node v
                              (merge v {:attempts-left (- attempts-left 1)})
                              (merge state {:node v}))
    (nil? next-node-on-failure) (sugar-throw-exception
                                  state
                                  no-defined-handler-for-failure-exception-message
                                  :failure-handler-not-defined)
    :else (merge state {:node next-node-on-failure})))

(defn- run-command [command context]
  "Execute the command, with context as parameter to the function"
  (require (symbol (namespace command)))
  (apply (resolve command) [context]))

(defn- post-execution-state [state]
  "Returns the state after the execution of the command. If there are no more nodes to execute, status is defined as :completed"
  (if (:node state)
    state
    (merge state {:status :completed})))

(s/defn execute
  "Upon receiving a state representing the Saga execution, runs the current command and, according to it's result, returns a new state with the remaining steps
  Rules:
   - given that the execution was a success returns a new state with the supplied next-node-on-success as current node.
   - given that the execution was a failure and :attempts-left is zero returns a new state with the supplied next-node-on-failure as current node.
   - given that the execution was a failure and :attempts-left is greater than zero returns a new state with attempts-left decreased by one keeping the current node.
   - given that the execution outcome was :unknown throws an ExceptionInfo with node and context information.
   - given that there are no more nodes to be executed, the status is complete"
  [{:keys             [context]
    {:keys [description]} :node
    :as               state} :- schema/State
   commands-container
   ]
  (let [execution-result (run-command (container/command-from-container commands-container description) context)
        new-state (cond
                    (execution-success? execution-result) (state-on-success state)
                    (execution-failure? execution-result) (state-on-failure state)
                    :else (sugar-throw-exception
                            state
                            unknown-outcome-exception-message
                            :unknown-outcome))]
    (post-execution-state new-state)))
