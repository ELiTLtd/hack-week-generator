(ns generator-api.generator
  (:require [libpython-clj.python :as py]
            [libpython-clj.require :refer [require-python]])
  (:import java.util.regex.Pattern))

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

(defn create-generator-buddy-v1
  []
  (let [tokenizer (py/$a transformers/GPT2Tokenizer :from_pretrained "gpt2")]
    (->GPT2TopPSamplingGenerator tokenizer
                                 (py/$a transformers/TFGPT2LMHeadModel :from_pretrained
                                        "gpt2"
                                        :pad_token_id (py/$. tokenizer "eos_token_id")))))

(defn create-bert-tokenizer
  []
  (py/$a transformers/AutoTokenizer :from_pretrained "distilbert-base-cased"))

(defn create-bert-model
  [tokenizer]
  (py/$a transformers/TFAutoModelWithLMHead :from_pretrained
          "distilbert-base-cased"
          :pad_token_id (py/$. tokenizer "eos_token_id")))

(defn indicies-of
  [xs v]
  (keep-indexed
   (fn [index value]
     (when (= v value)
       index))
   xs))

(defrecord DistilBertMaskPredictionGenerator [tokenizer model]
  Generator
  (generate [this input]
    (generate this input {}))
  (generate [this input {:keys [top_k sample_size] :as options}]
    (let [input-tokens (py/$a tokenizer :encode input :return_tensors "tf")
          logits (py/$. (model input-tokens) :logits)
          mask-token-indicies (indicies-of (py/$a (first input-tokens) :numpy)
                                           (py/$. tokenizer :mask_token_id))
          mask-token-replacements (map #(let [mask-token-logits (py/$a (first logits)
                                                                       :__getitem__
                                                                       [(tensorflow/constant %)])]
                                          (-> (py/$a tensorflow/math
                                                     :top_k
                                                     mask-token-logits
                                                     top_k)
                                              (py/$. :indices)
                                              vec))
                                       mask-token-indicies)
          top-n (map (fn [token-col]
                       (map #(py/$a tokenizer :decode %) token-col))
                     (apply map vector mask-token-replacements))]
      (printf "Generating text for input [%s] with options %s\n" input options)
      (for [words (take sample_size (shuffle top-n))]
        (reduce (fn [s replacement]
                  (.replaceFirst s (Pattern/quote (py/$. tokenizer :mask_token)) replacement))
                input
                words)))))

(comment

  (Pattern/quote "[MASK]")
  (tensorflow/constant 8)

  (def tokenizer (create-bert-tokenizer))
  (def model (create-bert-model tokenizer))

  (predict model
           tokenizer
           #_
           "Write a summary of your favourite [MASK] author, and why they wrote [MASK]. Later in life they turned to [MASK], [MASK] [MASK] [MASK]"
           "(reduce (fn [x] ([MASK] x 2)))"
           50
           10)

  (py/$a
   (first
    (py/$a tokenizer
           :encode
           "Write a summary of your favourite [MASK] author, and why they wrote [MASK]."
           :return_tensors
           "tf"))
   :numpy)

  (some #(= % (py/$. tokenizer :mask_token_id) (py/$. tokenizer :mask_token_id))
        (first
         (py/$a tokenizer
                :encode
                "Write a summary of your favourite [MASK] author, and why they wrote [MASK]."
                :return_tensors
                "tf"))))

#_
(defn create-generator-buddy-v1
  ([tokenizer]
   (create-generator-buddy-v1
    tokenizer
    {:max_length 40
     :do_sample true
     :top_k 50
     :top_p 0.9
     :temperature 1
     :num_return_sequences 5}))
  ([tokenizer default-options]
   (->GPT2TopPSamplingGenerator
    tokenizer
    (py/$a transformers/TFGPT2LMHeadModel :from_pretrained
           "gpt2"
           :pad_token_id (py/$. tokenizer "eos_token_id"))
    default-options)))

(defn create-generator-buddy-v1
  []
  (let [tokenizer (py/$a transformers/GPT2Tokenizer :from_pretrained "gpt2")]
    (->GPT2TopPSamplingGenerator tokenizer
                                 (py/$a transformers/TFGPT2LMHeadModel :from_pretrained
                                        "gpt2"
                                        :pad_token_id (py/$. tokenizer "eos_token_id")))))


(defn create-generator-macaulay-v1
  []
  (let [tokenizer (create-bert-tokenizer)]
    (->DistilBertMaskPredictionGenerator tokenizer
                                         (create-bert-model tokenizer))))

(comment
  (def tmp-tokenizer (create-tokenizer))
  (def tmp-generator (create-generator-buddy-v1 tmp-tokenizer {}))

  (def bad-word-ids
    (py/$a tmp-tokenizer :encode "\n" :return_tensors "tf"))

  (generate tmp-generator
            "Why don't you stop"
            {:max_length 50
             :min_length 10
             :early_stopping true
             :num_beams 5
             :do_sample true
             :top_k 50
             :top_p 0.9
             :temperature 2
             :num_return_sequences 10
             :bad_words_ids [[198]]}))
