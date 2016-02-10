(ns schema-fun.calendar
  (:require [clj-time.core :as time]
            [ring.swagger.schema :refer [describe]]
            [schema.core :as schema]
            [schema-fun.util :refer :all])
  (:import  java.util.UUID
            org.joda.time.DateTime))




(defn fresh-calendar-db
  "Returns a new, empty calendar database."
  []
  (atom {}))







(schema/defschema EventRequest
  (-> {:start (describe DateTime "The date and time the event starts")
       :stop  (describe DateTime "The data and time the event completes")
       :description (describe schema/Str "A description of the event")
       (schema/optional-key :location)  (describe schema/Str "Where the event takes place")
       (schema/optional-key :attendees) (describe [schema/Str] "A list of attendees")}

    (describe "User supplied event information")))






(schema/defschema EventId
  (-> schema/Uuid
      (schema/named "EventId")
      (describe "The unique identifier of an event")))


(schema/defschema Event
  (-> EventRequest
    (assoc :event EventId)
    (describe "A calendar event")))



(schema/defschema Schedule
  (-> {:from   (-> DateTime (describe "The starting time"))
       :to     (-> DateTime (describe "The cutoff time"))
       :events (-> [Event]  (describe "The list of events"))
       :count  (-> schema/Int (describe "The number of Events returned"))}
    (describe "A collection of events within a span of time")))



(defn new-event-id
  "Generate a new EventId"
  []
  (UUID/randomUUID))

(defn get-event
  "Retrieve an event from a calendar database."
  [calendar-db event-id]
  (get @calendar-db event-id))

(declare sanitize-event)

(defn broken-sanitize-event
  "Transform an EventRequest into a valid Event.
Ignores :start, :stop chronology."
  ([event-id {:keys [start stop] :as orig-event}]
    (-> orig-event
      (select-keys [:start :stop :location :description :attendees])
      (assoc :event event-id)))
  ([orig-event]
    (sanitize-event (new-event-id) orig-event)))

(defn correct-sanitize-event
  "Transform an EventRequest into a valid Event.
Ensures correct :start, :stop chronology."
  ([event-id {:keys [start stop] :as orig-event}]
    (-> orig-event
      (select-keys [:location :description :attendees])
      (assoc
        :event event-id
        :start (min-comparable start stop)
        :stop  (max-comparable start stop))))
  ([orig-event]
    (sanitize-event (new-event-id) orig-event)))

(def sanitize-event
  "Transform an EventRequest into a valid Event."
  broken-sanitize-event)


(defn- add-event [calendar-db event]
  (swap! calendar-db assoc (:event event) event)
  event)

(defn create-event
  "Add a new event to a calendar-db after sanitizing it.
Returns the new, sanitized event."
  [calendar-db new-event]
  (add-event calendar-db
    (sanitize-event new-event)))

(defn update-event
  "Replace an event in a calendar-db with an updated
event after sanitizing it.
Returns the sanitized event."
  [calendar-db event-id upd-event]
  (-> calendar-db
    (swap! replace-entry event-id
      (sanitize-event event-id upd-event))
    (get event-id)))

(defn broken-delete-event
  "Appears to delete an event correctly, but
fails during concurrent deletes."
  [calendar-db event-id]
  (when-some [event (get-event calendar-db event-id)]
    (swap! calendar-db dissoc event-id)
    event))

(defn correct-delete-event
  "Deletes events correctly, even when
run concurrently."
  [calendar-db event-id]
  (let [event (atom nil)]
    (swap! calendar-db
      (fn [cdb]
        (reset! event (get cdb event-id))
        (dissoc cdb event-id)))
    @event))


(def delete-event
  "Remove an event from a calender-db.
Returns the event that was removed."
  broken-delete-event)


(defn broken-between
  "Returns a predicate function which will test
if a date-time is _exclusively_ between the two
provided date-times."
  [dt-a dt-b]
  (let [min-dt (min-comparable dt-a dt-b)
        max-dt (max-comparable dt-a dt-b)]
    (fn [x]
      (and (time/after? min-dt x)
           (time/before? max-dt x)))))

(defn correct-between
  "Returns a predicate function which will test
if a date-time is _inclusively_ between the two
provided two date-times."
  [dt-a dt-b]
  (let [min-dt (min-comparable dt-a dt-b)
        max-dt (max-comparable dt-a dt-b)]
    (fn [x]
      (and (not (time/before? x min-dt))
           (not (time/after? x max-dt))))))

(def between
  "Returns a predicate function which will test
if a date-time is between the provided two date-times."
  broken-between)




(defn schedule-for
  [calendar-db dt-a dt-b]
  (let [events  (sort-by :start
                  (filter (comp (between dt-a dt-b) :start)
                    (vals @calendar-db)))]
    {:from   (min-comparable dt-a dt-b)
     :to     (max-comparable dt-a dt-b)
     :events events
     :count  (count events)}))





(schema/defschema ConstrainedEvent
  (schema/constrained Event
    (ascending-pred :start :stop)
    'chronological-start-stop?))







(schema/defschema non-negative-Int
  (schema/constrained schema/Int
    (complement neg?)
    'not-negative?))

(defn all-events-start-between-from-&-to?
  "Tests if every event in a schedule starts at or after
the :from field, and before or at the :to field."
  [{:keys [from to events] :as schedule}]
  (every? (comp (between from to) :start) events))


(schema/defschema ConstrainedSchedule
  (-> Schedule
      (assoc :count non-negative-Int)

      (schema/constrained
        (ascending-pred :from :to)
        'sequential-from-to?)

      (schema/constrained
        (fn [{sched-count  :count
              sched-events :events}]
          (= sched-count (count sched-events)))
        'correct-event-count?)

      (schema/constrained
        all-events-start-between-from-&-to?
        'all-events-start-between-from-&-to?)))