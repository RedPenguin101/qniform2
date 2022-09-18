(ns qniform.time
  (:require [clojure.string :as str]))

;; specs and conversion - domain rep of string datetime is YYYY:MM:DD HH:MM:SS.SSS 
(def date #"\d{4}-\d{2}-\d{2}")
(defn- is-date-str? [str] (re-matches date str))
(def datetime-space #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}")
(defn- is-datetime-str? [str] (re-matches datetime-space str))

(def datetime-t #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}")
(defn- date-time-space->t [s] (str/replace s #" " "T"))

(defn- str-to-date [date-str] (java.time.LocalDate/parse date-str))
(defn- date-to-str [date] (.toString date))

(defn- str-to-datetime [datetime-str]
  (java.time.LocalDateTime/parse (date-time-space->t datetime-str)))

(defn- str-to-date-or-datetime [str]
  (if (is-date-str? str) (str-to-date str) (str-to-datetime str)))

(comment
  (str-to-datetime "2022-09-18 10:14:46.123")
  (str-to-date-or-datetime "2022-09-18 10:14:46.123") ;; returns localdatetime 
  (str-to-date-or-datetime "2022-09-18") ;; returns java localdate
  )

;; comparisons are all on dates 
(defn date-comp [a b]
  (.compareTo (str-to-date-or-datetime a) (str-to-date-or-datetime b)))

(defn date< [a b] (neg? (date-comp a b)))
(defn date= [a b] (zero? (date-comp a b)))
(defn date> [a b] (pos? (date-comp a b)))
(defn date<= [a b] (or (date< a b) (date= a b)))
(defn date>= [a b] (or (date> a b) (date= a b)))

(comment
  (date< "2022-09-18 10:14:46.123" "2022-09-12 10:14:46.123")
  (date< "2022-09-01 10:14:46.123" "2022-09-12 10:14:46.123")
  (date< "2022-09-01" "2022-09-12 10:14:46.123"))