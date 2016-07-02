(ns kosha.app.util
  (:import [java.text Normalizer]))

;; public String unaccent(String s) {
;;     String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
;;     return normalized.replaceAll("[^\\p{ASCII}]", "");
;; }

(defn unaccent [s]
  (let [normalized (Normalizer/normalize s java.text.Normalizer$Form/NFD)]
    (.replaceAll normalized "[^\\p{ASCII}]" "")))

(defn ->printable [swarams & {:keys [bold?]}]
  (s/join ", " (map (comp s/capitalize name) swarams)))

(defn display-ragam-name [ragam-name]
  (s/capitalize (name ragam-name)))
