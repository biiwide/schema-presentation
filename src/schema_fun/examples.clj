(ns schema-fun.examples
  (:require [schema.core :as schema]))







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for a number, any number
;;
(def any-number schema/Num)

;; Valid
(schema/check any-number -3)
(schema/check any-number 5.2)
(schema/check any-number 2/3)

;; Invalid
(schema/check any-number "foo")
(schema/check any-number nil)







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for a string, any string
;;
(def any-string schema/Str)

;; Valid
(schema/check any-string "hello world")
(schema/check any-string "")

;; Invalid
(schema/check any-string 5)
(schema/check any-string nil)
(schema/check any-string :abcd)







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for a keyword, any keyword
;;
(def any-keyword schema/Keyword)

;; Valid
(schema/check any-keyword :abcd)
(schema/check any-keyword :foo/bar)

;; Invalid
(schema/check any-keyword "a string")
(schema/check any-keyword 'some-symbol)








;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for an integer
;;
(def an-integer schema/Int)

;; Valid
(schema/check an-integer 0)
(schema/check an-integer -8)
(schema/check an-integer Long/MAX_VALUE)
(schema/check an-integer Integer/MIN_VALUE)
(schema/check an-integer Short/MAX_VALUE)
(schema/check an-integer Byte/MIN_VALUE)

;; Invalid
(schema/check an-integer 1.1)
(schema/check an-integer "123")
(schema/check an-integer false)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for only positive integers
;;
(def a-positive-integer
  (schema/constrained schema/Int pos?))

;; Valid
(schema/check a-positive-integer 5)
(schema/check a-positive-integer 321)
(schema/check a-positive-integer Short/MAX_VALUE)

;; Invalid
(schema/check a-positive-integer -3)
(schema/check a-positive-integer 0)
(schema/check a-positive-integer 1.1)
(schema/check a-positive-integer "123")
(schema/check a-positive-integer Short/MIN_VALUE)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A regular expression schema for
;; a string of more than one word
;;
(def words
  (schema/named #"\s" "More than one word"))

;; Valid
(schema/check words "one two")
(schema/check words "a b c")

;; Invalid
(schema/check words "one-big-hyphenated-word")
(schema/check words (keyword "foo bar"))






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema for a "greeting", a
;; string beginning with "Hello"
(def greeting
  (schema/named #"^Hello" "Must begin with 'Hello'"))

;; Valid
(schema/check greeting "Hello Alice")
(schema/check greeting "Hello")

;; Invalid
(schema/check greeting "Güten tag")








;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for either integers or nil
;;
(def maybe-an-integer
  "Might be an integer, might be nil"
  (schema/maybe schema/Int))

;; Valid
(schema/check maybe-an-integer 456)
(schema/check maybe-an-integer nil)










;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A predicate function schema for
;; positive numbers of any kind
;;
(def positive-number-predicate
  (schema/pred
    (fn [x] (and (number? x) (pos? x)))
    'positive-number?))

;; Valid
(schema/check positive-number-predicate 2.3)
(schema/check positive-number-predicate 1/2)

;; Invalid
(schema/check positive-number-predicate 0)
(schema/check positive-number-predicate "abc")
(schema/check positive-number-predicate -1.23)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for either a number or a string
;; that could be read as a number
;;
(def number-or-numeric-string
  (schema/if string?
    (schema/named
      #"^(0|-?[1-9][0-9]*)(\.[0-9]*|/[1-9][0-9]*)?$"
      "numeric string")
    schema/Num))

;; Valid
(schema/check number-or-numeric-string 123)
(schema/check number-or-numeric-string 3.33)
(schema/check number-or-numeric-string 4/3)
(schema/check number-or-numeric-string "0")
(schema/check number-or-numeric-string "2")
(schema/check number-or-numeric-string "201")
(schema/check number-or-numeric-string "-22")
(schema/check number-or-numeric-string "5.43")
(schema/check number-or-numeric-string "-1.1")
(schema/check number-or-numeric-string "5/6")

;; Invalid
(schema/check number-or-numeric-string "abc")
(schema/check number-or-numeric-string "00")
(schema/check number-or-numeric-string "5-4")
(schema/check number-or-numeric-string "4.3.2.1")
(schema/check number-or-numeric-string "3.5/2")
(schema/check number-or-numeric-string 'a-symbol)










;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for a possibly mixed sequence
;; of numbers and number-like strings
;;
(def sequence-of-numeric-things
  [number-or-numeric-string])

;; Valid
(schema/check sequence-of-numeric-things [3 "0" 4.3 "-3.33"])
(schema/check sequence-of-numeric-things '("1.1" 2 3/5))
(schema/check sequence-of-numeric-things [])

;; Invalid
(schema/check sequence-of-numeric-things ["abc" 5])
(schema/check sequence-of-numeric-things [4 3.3 "abc" "3.0"])





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for a sequence of at least
;; one number or numeric string
;;
(def non-empty-sequence-of-numeric-things
  [(schema/one number-or-numeric-string "First numeric thing")
   number-or-numeric-string])

;; Valid
(schema/check non-empty-sequence-of-numeric-things [500])
(schema/check non-empty-sequence-of-numeric-things '("1.23"))

;; Invalid
(schema/check non-empty-sequence-of-numeric-things [])






;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; A schema for a map value that
;; sorta looks like a bill-of-sale
;;
(def bill
  {:name words
   :amount number-or-numeric-string
   (schema/optional-key :message) greeting})

;; Valid
(schema/check bill
  {:name "Trillian McMillan"
   :amount "500"})

(schema/check bill
  {:name "Arthur Dent"
   :amount 500
   :message "Hello Mr. Dent, here is your monthly bill"})

;; Invalid
(schema/check bill
  {:name "Jesse"
   :amount 34})

(schema/check bill
  {:name "Saul Goodman"})

(schema/check bill
  {:name "Walter White"
   :amount "abc"
   :message "Pay Up!"})









;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This schema contains several
;; random sub-schema elements
;;
(def grab-bag
  {:type (schema/conditional
           keyword? (schema/enum :animal :vegetable :mineral)
           symbol?  (schema/enum 'animal 'vegetable 'mineral)
           string?  (schema/enum "animal" "vegetable" "mineral"))
   (schema/optional-key :must-be-one)  (schema/eq "one")
   (schema/optional-key :one-ish)      (schema/enum "one" "1" 1 1.0)
   (schema/optional-key :boolean)      schema/Bool
   schema/Str    schema/Str
   })






(def odd-evens
  "A sequence containing an odd number of even integers"
  (schema/constrained
    [(schema/constrained schema/Int even?)]
    (comp odd? count)
    'odd-count?))

;; Valid
(schema/check odd-evens [2])
(schema/check odd-evens [2 4 6])
(schema/check odd-evens [0 0 0 0 0])

;; Invalid
(schema/check odd-evens [1])
(schema/check odd-evens [2 4])
(schema/check odd-evens [4 5 6])