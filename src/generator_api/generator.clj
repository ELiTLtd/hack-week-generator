(ns generator-api.generator
  (:require [libpython-clj.require :refer [require-python]]
            [libpython-clj.python :as py :refer [py. py.. py.-]]
            [clojure.string :as string]))

(comment
  (nltk/download "book")
  (require-python 'math)
  (math/sin 1.0)
  (require-python 'sys)
  sys/version
  (require-python '[nltk.book :as book])
  (book/texts))

(require-python '[nltk :as nltk]
                'torch
                'tensorflow
                'keras
                'transformers
                'mxnet
                '(mxnet ndarray module io model))

(def torch-model
  (py/$a transformers/GPT2LMHeadModel from_pretrained "gpt2"))

;;; Set the model in evaluation mode to deactivate the DropOut modules
;;; This is IMPORTANT to have reproducible results during evaluation!

;; (comment (py/$a model eval))

;; (def generated (into [] (py/$a tokenizer encode "The Manhattan bridge")))

;; (def context (torch/tensor [generated]))

;; (defn decode-sequence [{:keys [generated-tokens]}]
;;   (py/$a tokenizer decode generated-tokens))

;; (defn generate-sequence-step [k {:keys [generated-tokens context past]}]
;;   (let [model-output (if past (torch-model context :past_key_values past) (torch-model context))
;;         output (py/$. model-output "logits")
;;         past (py/$. model-output "past_key_values")
;;         ;; top-k (torch/topk (first output) k)
;;         ;; top-k-indices (py/$. top-k "indices")
;;         token (torch/argmax (first output))
;;         new-generated  (conj generated-tokens (py/$a token tolist))]
;;     ;; (prn top-k)
;;     ;; (prn top-k-indices)
;;     ;; (prn (decode-sequence {:generated-tokens (first top-k-indices)}))
;;     ;; (prn (decode-sequence {:generated-tokens (second top-k-indices)}))
;;     {:generated-tokens new-generated
;;      :context (py/$a token unsqueeze 0)
;;      :past past
;;      :token token}))

;; (defn generate-text [starting-text k num-of-words-to-predict]
;;   (let [tokens (into [] (py/$a tokenizer encode starting-text))
;;         context (torch/tensor [tokens])
;;         result (reduce
;;                 (fn [r i]
;;                   #_(println i)
;;                   (generate-sequence-step k r))
;;                 {:generated-tokens tokens
;;                  :context context
;;                  :past nil}
;;                 (range num-of-words-to-predict))]
;;     (decode-sequence result)))

(def tokenizer (py/$a transformers/GPT2Tokenizer from_pretrained "gpt2"))

(comment
  (time
   (py/$a transformers/GPT2Tokenizer from_pretrained "gpt2")))


(defn create-tokenizer
  []
  (py/$a transformers/GPT2Tokenizer from_pretrained "gpt2"))

(defonce tensorflow-head-model (py/$a transformers/TFGPT2LMHeadModel from_pretrained "gpt2"
                                  :pad_token_id (py/$. tokenizer "eos_token_id")))

(defonce tensorflow-double-head-model (py/$a transformers/TFGPT2DoubleHeadsModel from_pretrained "gpt2"
                                         :pad_token_id (py/$. tokenizer "eos_token_id")))

(def input-ids (py/$a tokenizer encode
                      "Tell us about"
                      :return_tensors "tf"))
(def max-length 15)

(defn decode-sequence
  [tokens-seq]
  (doall
   (for [tokens tokens-seq]
     (py/$a tokenizer decode tokens
            :skip_special_tokens true))))

(defn greedy-output
  [model max-length input]
  (py/$a model generate
         input
         :max_length max-length))

(defn beam-output
  [model max-length input]
  (py/$a model generate
         input
         :max_length max-length
         :num_beams 5
         :no_repeat_ngram_size 2
         :num_return_sequences 1
         :early_stopping true))

(defn top-k-output
  [model max-length input]
  (py/$a model generate
         input
         :max_length max-length
         :do_sample true
         :top_k 50
         :num_return_sequences 5))

(defn top-p-output
  [model max-length input]
  (py/$a model generate
         input
         :max_length max-length
         :do_sample true
         :top_k 50
         :top_p 0.99999
         :num_return_sequences 5))

(defprotocol Generator
  (generate [this input] [this input configuration]))

(defrecord GPT2TopPSamplingGenerator [tokenizer model]
  Generator
  (generate [this input]
    (generate this input {}))
  (generate [this input options]
    (let [input-tokens (py/$a tokenizer :encode input :return_tensors "tf")
          merged-options (merge {:max_length 20
                                 :do_sample true
                                 :top_k 10
                                 :top_p 0.9
                                 :num_return_sequences 5}
                                options)]
      (pmap #(py/$a tokenizer :decode % :skip_special_tokens true)
            (py/call-attr-kw tensorflow-head-model
                             :generate
                             [input-tokens]
                             merged-options)))))

(defn create-generator-buddy.v1
  [tokenizer]
  (->GPT2TopPSamplingGenerator
   tokenizer
   (py/$a transformers/TFGPT2LMHeadModel :from_pretrained "gpt2"
          :pad_token_id (py/$. tokenizer "eos_token_id"))))

(comment
  (let [input-s "Hello World, I want to "
        input-tokens (py/$a tokenizer encode input-s :return_tensors "tf")]
    (py/call-attr-kw tensorflow-head-model
                     :generate
                     [input-tokens]
                     {:max_length max-length
                      :do_sample true
                      :top_k 10
                      :top_p 0.99
                      :num_return_sequences 5}))

  (generate (->GPT2TopPSamplingGenerator tokenizer tensorflow-head-model) "Do do do"))

(defn generate-text
  [input max-length]
  (let [tokenized-input (py/$a tokenizer encode input :return_tensors "tf")]
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
