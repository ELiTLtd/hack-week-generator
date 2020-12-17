(ns generator-api.generator
  (:require [libpython-clj.require :refer [require-python]]
            [libpython-clj.python :as py :refer [py. py.. py.-]]
            [clojure.string :as string]
            [clojure.string :as str]))

(require-python 'tensorflow
                'transformers)

(defn create-tokenizer
  []
  (py/$a transformers/GPT2Tokenizer :from_pretrained "gpt2"))

(defprotocol Generator
  (generate [this input] [this input configuration]))

(defrecord GPT2TopPSamplingGenerator [tokenizer model]
  Generator
  (generate [this input]
    (generate this input {}))
  (generate [this input options]
    (let [input-tokens (py/$a tokenizer :encode input :return_tensors "tf")
          merged-options (merge {:max_length 40
                                 :do_sample true
                                 :top_k 50
                                 :top_p 0.9
                                 :temperature 1
                                 :num_return_sequences 5}
                                options)]
      (printf "Generating text for input [%s] with options %s\n" input merged-options)
      (pmap #(py/$a tokenizer :decode % :skip_special_tokens true)
            (py/call-attr-kw model
                             :generate
                             [input-tokens]
                             merged-options)))))

(defn create-generator-buddy.v1
  [tokenizer]
  (->GPT2TopPSamplingGenerator
   tokenizer
   (py/$a transformers/TFGPT2LMHeadModel :from_pretrained
          "gpt2"
          :pad_token_id (py/$. tokenizer "eos_token_id"))))

(defn create-bert-tokenizer
  []
  (py/$a transformers/AutoTokenizer :from_pretrained "distilbert-base-cased"))

(defn create-bert-model
  [tokenizer]
  (py/$a transformers/TFAutoModelWithLMHead :from_pretrained
          "distilbert-base-cased"
          :pad_token_id (py/$. tokenizer "eos_token_id")))

(defn predict
  [model tokenizer text n]
  (let [input-tokens (py/$a tokenizer :encode text :return_tensors "tf")
        mask-token-id (py/$. tokenizer :mask_token_id)
        mask-token-index (-> (py/$a input-tokens :__eq__ mask-token-id) tensorflow/where first second)
        logits (py/$. (model input-tokens) :logits)
        mask-token-logits (-> logits first (py/$a :__getitem__ [mask-token-index]))
        top-n (-> (py/$a tensorflow/math :top_k mask-token-logits n) (py/$. :indices) vec)]
    (for [word (take 5 (shuffle top-n))]
      (println (.replace text (py/$. tokenizer :mask_token) (py/$a tokenizer :decode word))))))

(comment

  (def tokenizer (create-bert-tokenizer))
  (def model (create-bert-model tokenizer))

  (def text (str "Write a summary of your favourite "
                 (py/$. tokenizer :mask_token)
                 "."))


  (predict model tokenizer text 30)

  )
