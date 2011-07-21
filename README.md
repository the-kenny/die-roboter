# Die Roboter

> Wir laden unsere Batterie.  
> Jetzt sind wir voller Energie.
>
> Wir sind die Roboter.
>
> Wir funktionieren automatik.  
> Jetzt wollen wir tanzen mechanik.
>
> Wir sind die Roboter.
>
> Wir sind auf Alles programmiert.  
> Und was du willst wird ausgefuehrt.
> 
> - Die Roboter by Kraftwerk

<img src="http://technomancy.us/i/die_roboter.jpg" align="right" />

The Robots get your work done in an straightforward way.

## Usage

    (ns die.roboter.example
      (:require [die.roboter :as roboter]))
    
    ;; starting up
    (roboter/work) ; on the worker nodes (this will block)
    
    ;; ordering around
    (roboter/send-off '(println "what are the haps")) ; returns immediately

    (roboter/broadcast '(println "greetings all workers")) ; runs on all nodes

    (let [f (roboter/future
              (slurp "/etc/hosts"))]
      (println @f)) ; when you need a return value back

    ;; By default only clojure/core is available for robot
    ;; workers, but you can tag defns to make them available.
    (defn ^{:roboter true} my-job [a b c]
      (prn :my-job {:a a :b b :c c})

    ;; These defns should be loaded before you run roboter/work,
    ;; but you can pick up stragglers explicitly:
    
    (roboter/register #'my-job)
    (roboter/auto-register #"my.project") ; searches all matching namespaces

Jobs will not ack to the server until they've completed successfully,
so workers that throw exceptions or disappear entirely will have their
jobs automatically retried. By default failed jobs will simply log
using `clojure.tools.logging/warn`, but you can rebind
`*exception-handler*` to respond in your own way, including acking the
message back to the server:

    (defn handle-tachyon [e msg]
      (if (re-find #"tachyon") ; tachyon failures don't need to be retried
        (com.mefesto.wabbitmq/ack (-> msg :envelope :delivery-tag))
        (println "Oh, we got trouble!" (.getMessage e))))

    (binding [roboter/*exception-handler* handle-tachyon]
      (roboter/work))

AMQP is used as the transport. [RabbitMQ](http://rabbitmq.com) is a
popular choice. Most functions take an optional `config` argument that
can be used to specify the AMQP connection settings, but you can also
use the `with-roboter` macro to bind it dynamically.

    (roboter/broadcast '(println "Greetings, programs") {:host "10.1.12.99"})

    (roboter/with-roboter {:username "flynn" :password "reindeerflotilla"}
      (roboter/broadcast `(println "Started working on" hostname))
      (roboter/work))

## Todo

* Timeouts
* Thread pooling
* Switch to tools.namespace once it's fixed
* Fix broadcast (see failing test)

## License

Copyright (C) 2011 Phil Hagelberg

Distributed under the Eclipse Public License, the same as Clojure.
