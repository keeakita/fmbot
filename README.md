fmbot
=====

A modular IRC bot for announcing what people are playing via Last.fm, Libre.fm,
or any other GNUfm server.

Status
------
This is very much an alpha. It's not yet "stable", but should be working enough
for others to play around with.  Please keep in mind if you write plugins that
the plugin architecture could change at any time, and probably will this early
in development.

Requirements
------------
- JDK 7 (tested against OpenJDK)
- Maven 3.x

Installation & Running
----------------------
Clone the project, then:

    mvn package
    cd target/appassembler
    mkdir data
    cp conf/sample_config.properties conf/config.properties
    # Edit config/sample.properties with your favorite editor (vim)
    chmod +x ./bin/fmbot.sh
    ./bin/fmbot.sh

This will change to a much easier system later when I get around to
implementing it.

License
-------
See the LICENSE file for details. It's MIT.

TODO
----
- Rewrite the Message class to handle things in a more RFC-conforming way
- Catch exceptions thrown by plugins, so that no single plugin can crash the
  main thread
- Documentation and unit testing is badly needed
- Refactor stuff out of Main so that it can tested by giving dummy inputs and
  outputs
  - Write said tests
- More plugins! A Java REPL plugin is planned, Software I and II students
  should appreciate this
- Investigate letting users write plugins in other languages capable of running
  on the JVM (JRuby, Jython, Clojure, ...)
- Somewhere along the way I decided a 120 char width was a good idea and now I
  regret it. Everything needs to be changed back to 80.
