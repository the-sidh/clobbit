(ns clobbit.core-test
  (:require [clojure.test :refer :all]
            [clobbit.core :as core]
            [clobbit.fixtures :as f]))

(use-fixtures :once f/schema-validation)

(deftest success-test
  (testing "given that the execution was a success should return a new graph with the supplied next-node-on-success as current node"
    (let [original-state {:status  :running
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        2
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure {:description :notify-customer}}
                          :context {:command-result :success
                                    :this           :that}}
          new-state {:status  :running
                     :node    {:description          :rent-car
                               :next-node-on-success {:description :buy-plane-tickets}}
                     :context {:command-result :success
                               :this           :that}}]
      (is (= new-state (core/state-after-step-execution original-state {:outcome :success}))))))

(deftest failure-attempt-left-test
  (testing "given that the execution was a failure and :attempts-left is greater than zero should return a new graph with attempts-left decreased by one"
    (let [original-state {:status  :running
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        2
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure {:description :notify-customer}}
                          :context {:command-result :failure
                                    :this           :that}}

          new-state {:status  :running
                     :node    {:description          :hotel-room-booking
                               :attempts-left        1
                               :next-node-on-success {:description          :rent-car
                                                      :next-node-on-success {:description :buy-plane-tickets}}
                               :next-node-on-failure {:description :notify-customer}}
                     :context {:command-result :failure
                               :this           :that}}]
      (is (= new-state (core/state-after-step-execution original-state {:outcome :failure}))))))

(deftest failure-no-attempt-left-test
  (testing "given that the execution was a failure, :attempts-left is zero and there are no :next-node-on-failure defined, should drop the saga by returning status equals to dropped"
    (let [original-state {:status  :running
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        0
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure nil}
                          :context {:command-result :failure
                                    :this           :that}}
          expected-state {:status  :dropped
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        0
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure nil}
                          :context {:command-result :failure
                                    :this           :that}}]
(is (= expected-state (core/state-after-step-execution original-state {:outcome :failure})))))

  (testing "given that the execution was a failure and :attempts-left is zero should return a new graph with the supplied next-node-on-failure as current node"
    (let [original-state {:status  :running
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        0
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure {:description :notify-customer}}
                          :context {:command-result :failure
                                    :this           :that}}
          expected-state-after-execution {:status  :running
                                          :node    {:description :notify-customer}
                                          :context {:command-result :failure
                                                    :this           :that}}]
      (is (= expected-state-after-execution (core/state-after-step-execution original-state {:outcome :failure}))))))

(deftest unknown-outcome-test
  (testing "given that the execution outcome was :unknown should drop the saga by returning status equals to dropped"
    (let [original-state {:status  :running
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        2
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure {:description :notify-customer}}
                          :context {:command-result :unknown
                                    :this           :that}}
          expected-state {:status  :dropped
                          :node    {:description          :hotel-room-booking
                                    :attempts-left        2
                                    :next-node-on-success {:description          :rent-car
                                                           :next-node-on-success {:description :buy-plane-tickets}}
                                    :next-node-on-failure {:description :notify-customer}}
                          :context {:command-result :unknown
                                    :this           :that}}]
      (is (= expected-state (core/state-after-step-execution  original-state {:outcome :unknown}))))))

(deftest saga-completed-test
  (testing "given that there are no more nodes, should return a state with status equals to :completed")
  (let [expected-state-after-execution {:status  :completed
                                        :node    nil
                                        :context {:command-result :success
                                                  :this           :that}}
        original-state  {:status  :running
                           :node    {:description          :hotel-room-booking
                                     :attempts-left        2
                                     :next-node-on-success nil
                                     :next-node-on-failure {:description :notify-customer}}
                           :context {:command-result :success
                                     :this           :that}
                           }]
    (is (= expected-state-after-execution (core/state-after-step-execution  original-state {:outcome :success})))))
