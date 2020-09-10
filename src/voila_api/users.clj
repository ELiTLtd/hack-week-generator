(ns voila-api.users)

(def first-names ["Alice" "Bob" "Charlie" "Eggbert" "Yoongi" "Kim"])
(def surnames ["Park" "Min" "Smith" "Yeoung"])
(def hobbies ["singing" "dancing" "painting" "ping-pong"])

(defn generate-user []
  {:first-name (rand-nth first-names)
   :surname (rand-nth surnames)
   :age (rand-int 100)
   :hobbies (set(repeatedly (inc (rand-int (count hobbies))) #(rand-nth hobbies)))})

(defn generate-users
  [num]
  (zipmap (range num)
          (take num
                (repeatedly generate-user ))))

(def users (generate-users 10))

(defn get-user
  [user-id]
  (get users user-id))
