(ns generator-api.generator-notebook
  (:require [libpython-clj.require :refer [require-python]]
            [libpython-clj.python :as py :refer [py. py.. py.-]]
            [clojure.string :as string]))


(defonce tensorflow-head-model
  (py/$a transformers/TFGPT2LMHeadModel from_pretrained "gpt2"
         :pad_token_id (py/$. tokenizer "eos_token_id")))

(defonce tensorflow-double-head-model
  (py/$a transformers/TFGPT2DoubleHeadsModel from_pretrained "gpt2"
         :pad_token_id (py/$. tokenizer "eos_token_id")))

(def input-ids
  (py/$a tokenizer :encode
         "Tell us about"
         :return_tensors "tf"))
(def max-length 15)

(defn decode-sequence
  [tokens-seq]
  (doall
   (for [tokens tokens-seq]
     (py/$a tokenizer :decode tokens
            :skip_special_tokens true))))

(defn greedy-output
  [model max-length input]
  (py/$a model :generate
         input
         :max_length max-length))

(defn beam-output
  [model max-length input]
  (py/$a model :generate
         input
         :max_length max-length
         :num_beams 5
         :no_repeat_ngram_size 2
         :num_return_sequences 1
         :early_stopping true))

(defn top-k-output
  [model max-length input]
  (py/$a model :generate
         input
         :max_length max-length
         :do_sample true
         :top_k 50
         :num_return_sequences 5))

(defn top-p-output
  [model max-length input]
  (py/$a model :generate
         input
         :max_length max-length
         :do_sample true
         :top_k 50
         :top_p 0.99999
         :num_return_sequences 5))

(defn generate-text
  [input max-length]
  (let [tokenized-input (py/$a tokenizer :encode input :return_tensors "tf")]
    (println "Generating text")
    (println (str "Original text: " input))
    (println (str "Max Length: " max-length "\n"))
    ;; (println (str "Greedy output: "(-> input greedy-output decode-sequence) "\n"))
    ;; (println (str "Beam output:   "(-> input beam-output decode-sequence) "\n"))
    ;; (println (str "Top_K outputs: \n" (->> input top-k-output decode-sequence (string/join "\n")) "\n"))
    (println (str "[HEAD MODEL] Top_P outputs: \n"
                  (->> tokenized-input
                       (top-p-output tensorflow-head-model max-length)
                       decode-sequence
                       (string/join "\n"))
                  "\n"))
    (println (str "[DOUBLE HEAD MODEL] Top_P outputs: \n"
                  (->> tokenized-input
                       (top-p-output tensorflow-double-head-model max-length)
                       decode-sequence
                       (string/join "\n"))
                  "\n"))))
