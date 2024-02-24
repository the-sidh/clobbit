(ns clobbit.commands-container-test
  (:require [clojure.test :refer :all]
            [clobbit.test-commands :as commands]
            [clobbit.commands-container :as container])
  (:import (clojure.lang ExceptionInfo)))




(deftest retrieve-command
  (testing "given an existing command in the context should retrieve the function associated with the description"
    (let [command commands/hotel-room-booking
          commands-container {:hotel-room-booking command}]
      (is (= command (container/command-from-container commands-container :hotel-room-booking)))))
  (testing "given a non-existing command in the context should receive an exception"
    (is (thrown-with-msg? ExceptionInfo #"Command not found on container"
                          (container/command-from-container {} :hotel-room-booking)))))