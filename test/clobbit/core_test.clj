(ns clobbit.core-test
  (:require [clojure.test :refer :all]
            [clobbit.core :as core])
  (:import (clojure.lang ExceptionInfo)))

(def second-node-on-success {:description          :rent-car
                             :command               'clobbit.commands/rent-car
                             :next-node-on-success {:description :buy-plane-tickets
                                                    :command      'clobbit.commands/irrelevant}})

(def second-node-on-failure {:description :notify-customer
                             :command      'clobbit.commands/irrelevant})

(def first-node {:description          :hotel-room-booking
                 :command               'clobbit.commands/hotel-room-booking
                 :attempts-left        2
                 :next-node-on-success second-node-on-success
                 :next-node-on-failure second-node-on-failure})

(def saga-starting-state {:status :running
                          :node   first-node})


(deftest success-test
  (testing "given that the execution was a success should return a new graph with the supplied next-node-on-success as current node"
    (let [context {:command-result :success
                   :this          :that}
          original-state (conj saga-starting-state {:context context})
          new-state (merge original-state {:node second-node-on-success})]
      (is (= new-state (core/execute original-state))))))

(deftest failure-attempt-left-test
  (testing "given that the execution was a failure and :attempts-left is greater than zero should return a new graph with attempts-left decreased by one"
    (let [context {:command-result :failure
                   :this          :that}
          original-state (conj saga-starting-state {:context context})
          expected-node-after-execution (merge (:node saga-starting-state) {:attempts-left 1})
          new-state (merge original-state {:node expected-node-after-execution})]
      (is (= new-state (core/execute original-state))))))

(deftest failure-no-attempt-left-test
  (testing "given that the execution was a failure and :attempts-left is zero should return a new graph with the supplied next-node-on-failure as current node"
    (let [context {:command-result :failure
                   :this          :that}
          first-node-with-no-way-to-revert (merge first-node {:attempts-left        0
                                                              :next-node-on-failure nil})
          original-state (merge saga-starting-state {:node    first-node-with-no-way-to-revert
                                                     :context context})]
      (is (thrown-with-msg? ExceptionInfo #"The command failed and there isn't an action defined to handle the failure. Aborting saga execution"
                            (core/execute original-state)))))

  (testing "given that the execution was a failure, :attempts-left is zero and there are no :next-node-on-failure defined, should throw an exception"
    (let [context {:command-result :failure
                   :this          :that}
          first-node-with-no-attempt-left (merge first-node {:attempts-left 0})
          original-state (merge saga-starting-state {:node    first-node-with-no-attempt-left
                                                     :context context})
          expected-state-after-execution (merge original-state {:node second-node-on-failure})]
      (is (= expected-state-after-execution (core/execute original-state))))))

(deftest unknown-outcome-test
  (testing "given that the execution outcome was :unknown should throw an exception"
    (let [context {:command-result :unknown
                   :this          :that}
          original-state (conj saga-starting-state {:context context})]
      (is (thrown-with-msg? ExceptionInfo #"Unknown outcome for command execution. Aborting saga execution"
                            (core/execute original-state))))))

(deftest saga-completed-test
  (testing "given that there is no more nodes, should return a state with status equals to :completed")
  (let [state-soon-to-be-completed (merge saga-starting-state {:node (merge first-node {:next-node-on-success nil})})
        context {:command-result :success
                 :this          :that}
        expected-state-after-execution {:status  :completed
                                        :node    nil
                                        :context context}
        original-state (conj state-soon-to-be-completed {:context context})]
    (is (= expected-state-after-execution (core/execute original-state)))))
