(ns clobbit.commands
  (:require [clojure.test :refer :all]))

(defn hotel-room-booking [context]
  {:outcome (get context :command-result)})

(defn rent-car [context]
  {:outcome (get context :command-result)})

(defn irrelevant [context])

