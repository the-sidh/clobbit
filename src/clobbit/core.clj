(ns clobbit.core)

(defn- success?
  [execution-result]
  (= {:outcome :success} execution-result))

(defn- failure?
  [execution-result]
  (= {:outcome :failure} execution-result))

(defn- update-graph [node context]
  (conj node {:context context}))

(defn- success
  [{:keys [ next-node-on-success context]}]
  (update-graph next-node-on-success context))

(defn- failure
  [{:keys [attempts-left next-node-on-failure context] :as graph}]
  (if (> attempts-left 0)
    (merge graph {:attempts-left (- attempts-left 1)})
    (update-graph next-node-on-failure context)))

(defn- run-command [action context]
  (require (symbol (namespace action)))
  (apply (resolve action) [context]))

(defn execute
  "Upon receiving a graph representing the Saga execution, runs the current command and, according to it's result, returns a new graph with the remaining steps
  Rules:
   - given that the execution was a success returns a new graph with the supplied next-node-on-success as current node
   - given that the execution was a failure and :attempts-left is zero returns a new graph with the supplied next-node-on-failure as current node
   - given that the execution was a failure and :attempts-left is greater than zero should return a new graph with attempts-left decreased by one keeping the current node"
  [{:keys [action context] :as graph}]
  (let [execution-result (run-command action context)]
    (cond
      (success? execution-result) (success graph)
      (failure? execution-result) (failure graph))))