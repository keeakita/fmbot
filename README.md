fmbot
=====

An modular IRC bot for announcing what people are playing via Last.fm, Libre.fm, or any other GNUfm server.

Status
------
This is very much an alpha. It's not yet "stable", but should be working enough for others to play around with.
Please keep in mind if you write plugins that the plugin architecture could change at any time,
and probably will this early in development.

A note about versions
---------------------
This project uses a few Java 7 features. You should be able to import it and have the right settings (I hope), but make
sure you have some kind of JDK 7. Tested on Linux against OpenJDK, Oracle's should work fine too.

License
-------
See the LICENSE file for details. It's MIT.

TODO
----
- Migrate to a maven project
- Catch exceptions thrown by plugins, so that no single plugin can crash the main thread
- Documentation is badly needed
- Refactor stuff out of Main so that it can tested by giving dummy inputs and outputs
  - Write said tests
- More plugins! A Java REPL plugin is planned, Software I and II students should appreciate this
- Investigate letting users write plugins in other languages capable of running on the JVM (JRuby, Jython, Clojure, ...)
