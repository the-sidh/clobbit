(ns clobbit.fixtures
  (:require [clojure.test :refer :all]
            [schema.core :as s]))

(defn schema-validation [f]
  (s/set-fn-validation! true)
  (f)
  (s/set-fn-validation! false))
