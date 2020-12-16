(ns generator-api.generator
  (:require [libpython-clj.require :refer [require-python]]
            [libpython-clj.python :as py :refer [py. py.. py.-]]
            [clojure.string :as string]))

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
            (py/call-attr-kw tensorflow-head-model
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
