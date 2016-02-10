(ns schema-fun.util)

(defn replace-entry
  "Given a map m, key k, and value v:
Associate k with v in m, if and only if m
already contains k."
  [m k v]
  (if (contains? m k)
    (assoc m k v)
    m))

(defn max-comparable
  "Returns the maximum of some comparable values.
Nils are considered non-values and never preferred."
  ([cmp] cmp)
  ([cmp-a cmp-b]
    (cond
      (nil? cmp-a) cmp-b
      (nil? cmp-b) cmp-a
      (= 1 (compare cmp-a cmp-b)) cmp-a
      :else cmp-b))
  ([cmp-a cmp-b & more]
    (reduce max-comparable
      cmp-a (cons cmp-b more))))

(defn min-comparable
  "Returns the minimum of some comparable values
Nils are considered non-values never preferred."
  ([cmp] cmp)
  ([cmp-a cmp-b]
    (cond
      (nil? cmp-a) cmp-b
      (nil? cmp-b) cmp-a
      (= 1 (compare cmp-a cmp-b)) cmp-b
      :else cmp-a))
  ([cmp-a cmp-b & more]
    (reduce min-comparable
      cmp-a (cons cmp-b more))))


(defn compare-with
  "Returns a comparator function of a & b
which compares (f a) to (f b)"
  [f]
  (fn [a b]
    (compare (f a) (f b))))


(defn subseqs-of
  "Returns a lazy sequence of all sub-sequences
   of length n in coll"
  [n coll]
  (let [sub (take n coll)]
    (if (not= n (count sub))
      nil
      (cons sub
        (lazy-seq (subseqs-of n (rest coll)))))))

(defn ascending-pred
  "Returns a predicate function that will
test if (<= (f₀ x) (f₁ x) ... (fₙ x))"
  ([f] (constantly true))
  ([f0 f1]
    (fn [x] (not (pos? (compare (f0 x) (f1 x))))))
  ([f0 f1 & more-f's]
    (let [vec-f (apply juxt f0 f1 more-f's)]
      (fn [x]
        (every? (fn [[a b]] (not (pos? (compare a b))))
          (subseqs-of 2 (vec-f x)))))))
