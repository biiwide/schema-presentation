(ns schema-fun.calendar-test
  (:require [clj-time.core :as time]
            [clj-time.coerce :refer [from-long]]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :refer [for-all]]
            [schema.core :as schema]
            [schema.experimental.generators :as schema-gen]
            [schema-fun.calendar :as cal]
            [schema-fun.util :refer :all])
  (:import  org.joda.time.DateTime))

(defn =with [f a b]
  (= (f a) (f b)))



(def check-event-schema
  (schema/checker cal/ConstrainedEvent))



(defn checked-create-event [calendar-db orig-event]
  "Add an event to the provided calendar-db and check
the result"

  (let [saved-event (cal/create-event calendar-db orig-event)]

    (if (and (is (some? (:event saved-event))
               "Saved Event must have an EventId")

             (is (nil? (check-event-schema saved-event))
               "Saved Event must be valid")

             (is (= (:start saved-event)
                    (min-comparable (:start orig-event) (:stop orig-event)))
               "Start date must be the earliest date provided")

             (is (= (:stop saved-event)
                    (max-comparable (:start orig-event) (:stop orig-event)))
               "Stop date must be the latest date provided")

             (is (=with :description orig-event saved-event)
               "Descriptions must match")

             (is (=with :location orig-event saved-event)
               "Locations must match")

             (is (=with :attendees orig-event saved-event)
               "Attendees must match")
          )
      saved-event
      )))


(def gen-date-time
  "A generator for Joda DateTime values.
Shrinks towards the present instant."
  (gen/fmap (comp time/ago time/millis)
    (gen/scale (fn [x] (* x x x x))
      gen/int)))



(def gen-event-request
  "A generator for the EventRequest schema."
  (schema-gen/generator cal/EventRequest
    {DateTime gen-date-time}))



(defspec create-and-get-event 20
  (let [db (cal/fresh-calendar-db)]

    (for-all [requested-event gen-event-request]

      (when-some [saved-event (checked-create-event db requested-event)]
        (let [saved-event-id (:event saved-event)
              retrieved-event (cal/get-event db saved-event-id)]

          (is (= saved-event retrieved-event)
            "Event returned after save must match retrieved event"))
        ))))




(defspec cannot-fetch-deleted-events 20
  (let [db (cal/fresh-calendar-db)]

    (for-all [requested-event gen-event-request]

      (when-some [saved-event (checked-create-event db requested-event)]
        (let [saved-event-id (:event saved-event)]

          (and (is (= saved-event (cal/delete-event db saved-event-id))
                 "Event returned on delete must match original saved event")

               (is (nil? (cal/get-event db saved-event-id))
                 "Must not be able to retrieve event after deletion")
            ))))))




(defspec delete-event-only-once 20
  (let [db (cal/fresh-calendar-db)]

    (for-all [requested-event gen-event-request
              delete-count    (gen/choose 3 50)]

      (when-some [saved-event (checked-create-event db requested-event)]
        (let [saved-event-id (:event saved-event)
              deletion-attempts (pmap
                                  (fn [id] (cal/delete-event db id))
                                  (repeat delete-count saved-event-id))]

          (is (= 1 (count (remove nil? deletion-attempts)))
            "Only one deletion attempt should succeed"))))))




(defn save-all-events [calendar-db requested-events]
  (let [saved-events (remove nil?
                       (pmap (fn [e] (checked-create-event calendar-db e))
                         requested-events))]
    (when (is (= (count requested-events)
                 (count saved-events))
            "Must save all requested events")
      saved-events)))
      
(defn earliest-start-time [events]
  (reduce min-comparable
    (map :start events)))

(defn latest-stop-time [events]
  (reduce max-comparable
    (map :stop events)))

(defn validate-scheduled-events [sched expected-events]
  (and
    (is (nil? (schema/check cal/ConstrainedSchedule sched))
      "Schedule must validate against schema")

    (is (empty? (reduce disj
                  (set expected-events)
                  (:events sched)))
      "Scheduled events most contain all expected events")))



(defspec list-events 20
  (let [db (cal/fresh-calendar-db)]

    (for-all [requested-events (gen/vector gen-event-request 1 60)]

      (let [saved-events (save-all-events db requested-events)
            from (earliest-start-time saved-events)
            to   (latest-stop-time saved-events)]

        (when-some [sched (cal/schedule-for db from to)]
          (validate-scheduled-events sched saved-events))))))
