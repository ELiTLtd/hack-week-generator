(ns hack-week-generator-api.generator
  (:require [libpython-clj.require :refer [require-python]]
            [libpython-clj.python :as py :refer [py. py.. py.-]]))

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

(def torch-model (py/$a transformers/GPT2LMHeadModel from_pretrained "gpt2"))

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

(def tokenizer (py/$a transformers/GPT2Tokenizer from_pretrained "gpt2"
                      :additional_special_tokens ["\n"]))


(def tensorflow-head-model (py/$a transformers/TFGPT2LMHeadModel from_pretrained "gpt2"
                                  :pad_token_id (py/$. tokenizer "eos_token_id")))

(def tensorflow-double-head-model (py/$a transformers/TFGPT2DoubleHeadsModel from_pretrained "gpt2"
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
  [input]
  (py/$a tensorflow-model generate
         input
         :max_length max-length))

(defn beam-output
  [input]
  (py/$a tensorflow-model generate
         input
         :max_length max-length
         :num_beams 5
         :no_repeat_ngram_size 2
         :num_return_sequences 1
         :early_stopping true))

(defn top-k-output
  [input]
  (py/$a tensorflow-model generate
         input
         :max_length max-length
         :do_sample true
         :top_k 50
         :num_return_sequences 5))

(defn top-p-output
  [model input]
  (py/$a model generate
         input
         :max_length max-length
         :do_sample true
         :top_k 50
         :top_p 0.99999
         :num_return_sequences 5))

(defn generate-text
  [input]
  (println "Generating text\n")
  (println (str "Original text: "(->> input decode-sequence (apply str)) "\n"))
  ;; (println (str "Greedy output: "(-> input greedy-output decode-sequence) "\n"))
  ;; (println (str "Beam output:   "(-> input beam-output decode-sequence) "\n"))
  ;; (println (str "Top_K outputs: \n" (->> input top-k-output decode-sequence (string/join "\n")) "\n"))
  (println (str "[HEAD MODEL] Top_P outputs: \n" (->> input (top-p-output tensorflow-head-model) decode-sequence (string/join "\n")) "\n"))
  (println (str "[DOUBLE HEAD MODEL] Top_P outputs: \n" (->> input (top-p-output tensorflow-double-head-model) decode-sequence (string/join "\n")) "\n")))
