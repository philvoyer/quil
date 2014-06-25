(ns quil.middlewares.pause-on-error
  (:require [quil.core :as q]
            [quil.util :as u]
            [clojure.string :refer [join]]))

(defn- wrap-fn [pause name function do-on-pause]
  (fn [& args]
    (if @pause
      (do-on-pause)
      (try
        (apply function args)
        (catch Exception e
          (reset! pause {:name name
                         :exception e
                         :time (java.util.Date.)}))))))

(defn- draw-error-message [pause]
  (q/push-style)
  (q/background 255)
  (q/fill 0)
  (q/text-size 15)
  (let [{:keys [name exception time]} @pause
        str [(format "Sketch was paused due to an exception thrown in %s" name)
             (str "Check REPL or console for stacktrace")
             (str "Time: " time)
             (str "Fix the the error and then press any key to unpause sketch.")]]
    (q/text (join \newline str) 10 20))
  (q/pop-style))

(defn- wrap-draw [options pause]
  (let [draw (:draw options (fn []))]
    (assoc options
      :draw (wrap-fn pause :draw draw #(draw-error-message pause)))))

(defn- unpause [pause]
  (reset! pause nil))

(defn- wrap-key-pressed [options pause]
  (let [key-pressed (:key-pressed options (fn []))]
    (assoc options
      :key-pressed (wrap-fn pause :key-pressed key-pressed #(unpause pause)))))

(defn pause-on-error
  [options]
  (let [pause (atom nil)]
    (-> (into {}
              (for [[name value] options]
                [name (if (and (u/callable? value)
                               (not= name :draw)
                               (not= name :key-pressed))
                        (wrap-fn pause name value (fn []))
                        value)]))
        (wrap-draw pause)
        (wrap-key-pressed pause))))
