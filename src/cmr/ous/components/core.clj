(ns cmr.ous.components.core
  (:require
    [cmr.exchange.common.components.config :as config]
    [cmr.exchange.common.components.logging :as logging]
    [cmr.http.kit.components.server :as httpd]
    [cmr.metadata.proxy.components.core :as metadata]
    [cmr.ous.config :as config-lib]
    [com.stuartsierra.component :as component]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Common Configuration Components   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cfg
  []
  {:config (config/create-component (config-lib/data))})

(def log
  {:logging (component/using
             (logging/create-component)
             [:config])})

(def httpd
  {:httpd (component/using
           (httpd/create-component)
           [:config :logging :plugin
            :pubsub :auth-caching :auth
            :concept-caching :concepts])})

;;; Additional components for systems that want to supress logging (e.g.,
;;; systems created for testing).

(def httpd-without-logging
  {:httpd (component/using
           (httpd/create-component)
           [:config :plugin :pubsub
            :auth-caching :auth
            :concept-caching :concepts])})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;   Component Initializations   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn initialize-config-only
  []
  (component/map->SystemMap (cfg)))

(defn initialize-bare-bones
  []
  (component/map->SystemMap
    (merge (cfg)
           log)))

(defn initialize
  []
  (component/map->SystemMap
    (merge (initialize-bare-bones)
           metadata/pubsub
           metadata/auth-cache
           metadata/authz
           metadata/concept-cache
           metadata/concepts
           httpd)))

(defn initialize-without-logging
  []
  (component/map->SystemMap
    (merge (cfg)
           metadata/pubsub-without-logging
           metadata/auth-cache-without-logging
           metadata/authz
           metadata/concept-cache-without-logging
           metadata/concepts
           httpd-without-logging)))

(def init-lookup
  {:basic #'initialize-bare-bones
   :testing-config-only #'initialize-config-only
   :testing #'initialize-without-logging
   :main #'initialize})

(defn init
  ([]
    (init :main))
  ([mode]
    ((mode init-lookup))))

(def testing #(init :testing))
(def testing-config-only #(init :testing-config-only))
