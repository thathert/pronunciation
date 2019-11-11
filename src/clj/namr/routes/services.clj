(ns namr.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [namr.db.core :as db]
    [namr.middleware.formats :as formats]
    [namr.middleware.exception :as exception]
    [namr.voice.synthesis :as synth]
    [ring.util.http-response :refer :all]
    [struct.core :as st]
    [clojure.java.io :as io]))

(defn db-insert [func args]
  (println args)
  (try
    (if (pos? (func args))
      {:status 201 :body args}
      {:status 500 :body {:message "No rows were updated."}})
    (catch Exception ex
      {:status 400 :body {:message (.getMessage (.getCause ex))}})))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "pronunciat.io/n"
                         :description "/prəˌnʌnsiˈeɪʃən/"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/status"
    {:get (constantly (ok {:message "OK"}))}]

   ["/voices"
    {:get (constantly (ok {:voices synth/available-voices}))}]

   ["/words"
    {:swagger {:tags ["words"]}}

    ["" {:post {:summary "add a new word"
                :parameters {:body {:spelling string?}}
                :responses {201 {:body {:spelling string?}}}
                :handler
                (fn [request]
                  (let [[errors body] (st/validate (request :body-params) [[:spelling st/required st/string]])]
                    (if errors
                      {:status 400 :body errors}
                      (db-insert db/create-word! body))))}}]

    ["/:spelling/pronunciations"
     {:post {:summary "create pronunciation for a spelling"
            :parameters {:path {:spelling string?}
                         :body {:ipa string? :soundsLike string? :voiceId string?}}
            :responses {200 {:body {:spelling string?}}}
            :handler (fn [request]
                       (let [[errors body] (st/validate (request :body-params)
                                                        [[:ipa st/required st/string]
                                                         [:soundsLike st/string]
                                                         [:voiceId st/required st/string]])
                             word-spelling (:spelling (:path-params request))
                             word (db/get-word {:spelling word-spelling})]
                         (if (or errors (not word))
                           {:status 400 :body (or errors {:message (str "No record for " word-spelling)})}
                           (let [synth-task (synth/save-speech (:voiceId body) (:ipa body) word-spelling)]
                             (db-insert db/create-pronunciation!
                                        {:word_id (:id word)
                                         :ipa (:ipa body)
                                         :sounds_like (:soundsLike body)
                                         :audio_uri (.getOutputUri synth-task)})))))}}]]

   ["/math"
    {:swagger {:tags ["math"]}}

    ["/plus"
     {:get {:summary "plus with spec query parameters"
            :parameters {:query {:x int?, :y int?}}
            :responses {200 {:body {:total pos-int?}}}
            :handler (fn [{{{:keys [x y]} :query} :parameters}]
                       {:status 200
                        :body {:total (+ x y)}})}
      :post {:summary "plus with spec body parameters"
             :parameters {:body {:x int?, :y int?}}
             :responses {200 {:body {:total pos-int?}}}
             :handler (fn [{{{:keys [x y]} :body} :parameters}]
                        {:status 200
                         :body {:total (+ x y)}})}}]]

   ["/files"
    {:swagger {:tags ["files"]}}

    ["/upload"
     {:post {:summary "upload a file"
             :parameters {:multipart {:file multipart/temp-file-part}}
             :responses {200 {:body {:name string?, :size int?}}}
             :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                        {:status 200
                         :body {:name (:filename file)
                                :size (:size file)}})}}]

    ["/download"
     {:get {:summary "downloads a file"
            :swagger {:produces ["image/png"]}
            :handler (fn [_]
                       {:status 200
                        :headers {"Content-Type" "image/png"}
                        :body (-> "public/img/warning_clojure.png"
                                  (io/resource)
                                  (io/input-stream))})}}]]])
