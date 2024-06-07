(ns clobbit.test-commands
  (:require [clojure.test :refer :all]))

(defn hotel-room-booking [context]
  {:outcome (get context :command-result)})

(defn rent-car [context]
  {:outcome (get context :command-result)})

(defn irrelevant [context])

(defn broken [context]
  (throw (ex-info "broken" {})))