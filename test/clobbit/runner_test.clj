(ns clobbit.runner-test
  (:require [clojure.test :refer :all]
            [clobbit.runner :as runner]))

(deftest exception-on-command-test
  (testing "given that the command execution threw an exception should return a new state with execution status equals to dropped"
    (let [commands-container {:this-will-break 'clobbit.test-commands/broken}
          original-state {:status  :running
                          :node    {:description          :this-will-break
                                    :attempts-left        2
                                    :next-node-on-success nil
                                    :next-node-on-failure nil}
                          :context nil}]

      (is (= :dropped (:status (runner/run original-state commands-container)))))))

(deftest saga-completed-test
  (testing "given that the execution was a success and there are no more steps, should return a state with status equals to :completed")
  (let [commands-container {:hotel-room-booking 'clobbit.test-commands/hotel-room-booking
                             :notify-customer    'clobbit.test-commands/irrelevant}
         original-state {:status  :running
                         :node    {:description          :hotel-room-booking
                                   :attempts-left        2
                                   :next-node-on-success nil
                                   :next-node-on-failure {:description :notify-customer}}
                         :context {:command-result :success
                                   :this           :that}
                         }]
        (is (= :completed (:status (runner/run original-state commands-container))))))


(deftest saga-running-test
  (testing "given that the execution was a success and there are still more steps , should return a state with status equals to :running")
  (let [commands-container {:hotel-room-booking 'clobbit.test-commands/hotel-room-booking
                            :rent-car    'clobbit.test-commands/rent-car
                            :notify-customer    'clobbit.test-commands/irrelevant}
        original-state {:status  :running
                        :node    {:description          :hotel-room-booking
                                  :attempts-left        2
                                  :next-node-on-success {:description :rent-car}
                                  :next-node-on-failure {:description :notify-customer}}
                        :context {:command-result :success
                                  :this           :that}
                        }]
    (is (= :running (:status (runner/run original-state commands-container))))))

;(deftest exception-on-command-test
;  (testing "given that the execution was a success should return a new graph with the supplied next-node-on-success as current node"
;    (let [commands-container {:hotel-room-booking 'clobbit.test-commands/hotel-room-booking
;                              :rent-car           'clobbit.test-commands/rent-car
;                              :buy-plane-tickets  'clobbit.test-commands/irrelevant
;                              :notify-customer    'clobbit.test-commands/irrelevant}
;
;          original-state {:status  :running
;                          :node    {:description          :hotel-room-booking
;                                    :attempts-left        2
;                                    :next-node-on-success {:description          :rent-car
;                                                           :next-node-on-success {:description :buy-plane-tickets}}
;                                    :next-node-on-failure {:description :notify-customer}}
;                          :context {:command-result :success
;                                    :this           :that}}
;          new-state {:status  :running
;                     :node    {:description          :rent-car
;                               :next-node-on-success {:description :buy-plane-tickets}}
;                     :context {:command-result :success
;                               :this           :that}}]
;      (is (= new-state (core/state-after-step-execution original-state {:outcome :success}))))))
