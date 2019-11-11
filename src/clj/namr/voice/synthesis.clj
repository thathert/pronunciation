(ns namr.voice.synthesis
  (:require [clojure.xml :as xml]
            [sc.api]) ; (sc.api/spy or /brk), then (sc.nrepl.repl/in-ep 7) or /in-ep [num -num]
  (:import (com.amazonaws ClientConfiguration)
           (com.amazonaws.regions Region
                                  Regions)
           (com.amazonaws.services.polly.model DescribeVoicesRequest
                                               StartSpeechSynthesisTaskRequest
                                               GetSpeechSynthesisTaskRequest
                                               TaskStatus)
           (com.amazonaws.services.polly AmazonPollyClient)
           (com.amazonaws.auth DefaultAWSCredentialsProviderChain)))

(defn wait-for
  "Invoke predicate every interval (default 10) seconds until it returns true,
   or timeout (default 150) seconds have elapsed. E.g.:
       (wait-for #(< (rand) 0.2) :interval 1 :timeout 10)
   Returns nil if the timeout elapses before the predicate becomes true, otherwise
   the value of the predicate on its last evaluation."
  [predicate & {:keys [interval timeout]
                :or {interval 10
                     timeout 150}}]
  (let [end-time (+ (System/currentTimeMillis) (* timeout 1000))]
    (loop []
      (if-let [result (predicate)]
        result
        (do
          (Thread/sleep (* interval 1000))
          (if (< (System/currentTimeMillis) end-time)
            (recur)))))))

(def polly (AmazonPollyClient. (DefaultAWSCredentialsProviderChain.) (ClientConfiguration.)))
(.setRegion polly (Region/getRegion (Regions/US_EAST_1)))

(def available-voices (.getVoices (.describeVoices polly (DescribeVoicesRequest.))))

(defn get-task [task-id]
  (let [task-req (GetSpeechSynthesisTaskRequest.)]
    (.setTaskId task-req task-id)
    (.getSpeechSynthesisTask polly task-req)))

(defn poll-task [task-id]
  (wait-for #(= (.getTaskStatus (.getSynthesisTask (get-task task-id)))
                (str TaskStatus/Completed))
            :interval 1
            :timeout 45))

(defn generate-ssml [ipa spelling]
  (with-out-str (xml/emit-element
    (struct xml/element :speak {}
            [(struct xml/element :phoneme {:alphabet "ipa" :ph ipa}
                     [spelling])]))))

(defn save-speech [voice-id pronunciation spelling]
  (let [ssml (generate-ssml pronunciation spelling)
        req (StartSpeechSynthesisTaskRequest.)]
    (println ssml)
    (.setOutputS3BucketName req "namr")
    (.setOutputFormat req "mp3")
    (.setText req ssml) ; check valid ipa or x-sampa
    (.setVoiceId req voice-id)
    (.setTextType req "ssml")
    (let [response (.startSpeechSynthesisTask polly req)
          task (.getSynthesisTask response)
          task-id (.getTaskId task)]
      (if (poll-task task-id)
       task
       (throw (ex-info "It looks like the task didn't complete successfully", {:task-id task-id}))))))
