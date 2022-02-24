(ns clobbit.commands
  (:require [clojure.test :refer :all]))

(defn hotel-room-booking [context]
  {:outcome :success})

(defn hotel-room-booking-failure [context]
  {:outcome :failure})

(defn hotel-room-booking-failure [context]
  {:outcome :failure})

(defn revert-hotel-room-booking [context]
  {:outcome (:action-result context)})

(defn rent-car [context]
  {:outcome (get context :action-result)})

