(ns clobbit.producer
  (:require [clobbit.schema :as schema]))
(defprotocol Producer
  (produce [this
            state :- schema/State
            queue]))