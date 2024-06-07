(ns clobbit.core
  (:require [clobbit.schema :as schema]
            [schema.core :as s]))

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

(defn state-with-dropped-status [state]
  "returns an update state on case of the command's execution being considered as unknown, with the status equals to dropped"
  (merge state {:status :dropped}))

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
    (nil? next-node-on-failure) (state-with-dropped-status state)
    :else (merge state {:node next-node-on-failure})))


(defn- updated-state [state]
  "Returns the state after the execution of the command. If there are no more nodes to execute, status is defined as :completed"
  (if (:node state)
    state
    (merge state {:status :completed})))

(s/defn state-after-step-execution
  "Upon receiving a state representing the Saga execution, runs the current command and, according to it's result, returns a new state with the remaining steps
  Rules:
   - given that the execution was a success returns a new state with the supplied next-node-on-success as current node.
   - given that the execution was a failure and :attempts-left is zero returns a new state with the supplied next-node-on-failure as current node.
   - given that the execution was a failure and :attempts-left is zero returns and there is no next-node-on-failure return a state that represents that the saga execution should be dropped.
   - given that the execution was a failure and :attempts-left is greater than zero returns a new state with attempts-left decreased by one keeping the current node.
   - given that the execution outcome was :unknown return a state that represents that the saga execution should be dropped
   - given that there are no more nodes to be executed, the status is complete"
  [state :- schema/State
   execution-result :- schema/ExecutionResult
   ] :- schema/State
  (let [new-state (cond
                    (execution-success? execution-result) (state-on-success state)
                    (execution-failure? execution-result) (state-on-failure state)
                    :else (state-with-dropped-status state))]
    (updated-state new-state)))