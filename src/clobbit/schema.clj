(ns clobbit.schema
  (:require [clojure.test :refer :all]
            [schema.core :as s]))


(s/defschema Node
  "A given step of the saga"
  {(s/required-key :description)          s/Keyword
   (s/optional-key :command)        s/Any
   (s/required-key :attempts-left)        s/Int
   (s/optional-key :next-node-on-success) s/Any
   (s/optional-key :next-node-on-failure) s/Any
   })


(s/defschema State
  "Represents the current state of a saga"
  {(s/required-key :status) (s/enum :completed :running :dropped)
   (s/required-key :node)             Node
   (s/required-key :context)             s/Any
   } )
