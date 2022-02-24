(ns clobbit.core-test
  (:require [clojure.test :refer :all]
            [clobbit.core :as core]))

(deftest execute-test
  (testing "given that the execution was a success should return a new graph with the supplied next-node-on-success as current node"
    (let [second-node {:node                 :rent-car
                       :action               'clobbit.commands/rent-car
                       :next-node-on-success {:node   :buy-plane-tickets
                                              :action 'clobbit.commands/buy-plane-tickets}}
          new-state (conj second-node {:context {:this :that}})

          original-state {:node                 :hotel-room-booking
                          :action               'clobbit.commands/hotel-room-booking
                          :next-node-on-success second-node
                          :context              {:this :that}}]
      (is (= new-state (core/execute original-state)))))

  (testing "given that the execution was a failure and :attempts-left is greater than zero should return a new graph with attempts-left decreased by one"
    (let [original-state {:node                 :hotel-room-booking
                          :action               'clobbit.commands/hotel-room-booking-failure
                          :attempts-left        2
                          :next-node-on-success nil
                          :context              {:this :that}}
          new-state (merge original-state {:attempts-left 1})]
      (is (= new-state (core/execute original-state)))))

  (testing "given that the execution was a failure and :attempts-left is zero should return a new graph with the supplied next-node-on-failure as current node"
    (let [next-on-failure-graph {:node                 :revert-hotel-room-booking
                                 :action               'clobbit.commands/revert-hotel-room-booking
                                 :attempts-left        2
                                 :next-node-on-success nil}
          context {:action-result :failure
                   :this         :that}
          original-graph {:node                 :rent-car
                          :action               'clobbit.commands/rent-car
                          :attempts-left        0
                          :next-node-on-success {:node   :buy-plane-tickets
                                                 :action 'clobbit.commands/buy-plane-tickets}
                          :next-node-on-failure next-on-failure-graph
                          :context              context}
          expected-graph (conj next-on-failure-graph {:context context})]
      (is (= expected-graph (core/execute original-graph)))))

  (testing "given that the execution outcome was :abort return an empty new graph"))
