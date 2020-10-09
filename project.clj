(defproject voila "1.0.15-SNAPSHOT"
  :description "ila-project prototype project"
  :url "https://github.com/ELiTLtd/voila"
  :min-lein-version "2.0.0"
  :dependencies [[clj-http "3.10.1"]
                 [metosin/muuntaja "0.6.7"]
                 [metosin/potpuri "0.5.2"]
                 [metosin/reitit "0.5.5"]
                 [metosin/reitit-swagger "0.5.5"]
                 [metosin/reitit-swagger-ui "0.5.5"]
                 [metosin/spec-tools "0.10.4"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [org.eclipse.jetty/jetty-server "9.4.28.v20200408"]
                 [ring/ring-core "1.8.1"]
                 [ring/ring-mock "0.4.0"]
                 [ring/ring-jetty-adapter "1.8.1"]
                 [ring/ring-servlet "1.8.1"]
                 [ring-cors "0.1.13"]
                 [tick "0.4.26-alpha"]]
  :plugins [[reifyhealth/lein-git-down "0.3.6"]
            [lein-shell "0.5.0"]
            [lein-pprint "1.3.2"]]
  :profiles {:dev {:dependencies [[ring/ring-devel "1.8.1"]
                                  [thheller/shadow-cljs "2.11.2"]
                                  [ring/ring-mock "0.4.0"]]}
             :uberjar {:aot :all
                       :uberjar-name "voila-api-uberjar.jar"}}
  :middleware [lein-git-down.plugin/inject-properties]
  :repositories [["public-github" {:url "git://github.com"}]
                 ["private-github" {:url "git://github.com"
                                    :protocol :ssh}]]
  :aliases {"shadow-cljs" ["run" "-m" "shadow.cljs.devtools.cli"]}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit" "Version %s [skip ci]"]
                  ["vcs" "tag" "--no-sign"]
                  ["uberjar"]
                  ["shell" "npx" "shadow-cljs" "release" "introspect"]
                  ["shell" "npx" "webpack" "--mode" "production"
                   "target/index.js" "--output" "public/js/libs.js"]
                  ["shell" "scripts/deploy.sh"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit" "Version %s [skip ci]"]]
  :clean-targets [:target-path "public/js" ".shadow-cljs"]
  :main ^:skip-aot voila-api.core
  :target-path "target/%s")
