(defproject voila "1.0.9-SNAPSHOT"
  :description "ila-project prototype project"
  :url "https://github.com/ELiTLtd/voila"
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "3.10.1"]
                 [metosin/muuntaja "0.6.7"]
                 [metosin/potpuri "0.5.2"]
                 [metosin/reitit "0.5.2"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [org.eclipse.jetty/jetty-server "9.4.28.v20200408"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-jetty-adapter "1.8.1"]
                 [ring/ring-servlet "1.8.1"]
                 [tick "0.4.26-alpha"]]
  :plugins [[reifyhealth/lein-git-down "0.3.6"]
            [lein-shell "0.5.0"]
            [lein-pprint "1.3.2"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.8.1"]]}
             :testing {:dependencies [[ring/ring-mock "0.4.0"]]}
             :uberjar {:aot :all
                       :uberjar-name "voila-uberjar.jar"}}
  :middleware [lein-git-down.plugin/inject-properties]
  :repositories [["public-github" {:url "git://github.com"}]
                 ["private-github" {:url "git://github.com"
                                    :protocol :ssh}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit" "Version %s [skip ci]"]
                  ["vcs" "tag" "--no-sign"]
                  ["clean"]
                  ["uberjar"]
                  ["shell" "scripts/deploy.sh"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit" "Version %s [skip ci]"]]
  :main ^:skip-aot voila.core
  :target-path "target/%s")
