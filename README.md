fmbot
=====

An IRC bot for announcing what people are playing via Last.fm, Libre.fm, or any other GNUfm server.

Status
------
Last.fm/GNU FM functionality is present, and users can add and remove themselves from the poll list. More functionality
for user management is planned, with more complex things pending a rewrite and organization of the commands into a
module based architecture.

A note about versions
---------------------
This project uses a few Java 7 features. You should be able to import it and have the right settings (I hope), but make
sure you have some kind of JDK 7. Tested on Linux against OpenJDK, Oracle's should work fine too.

License
-------
See the LICENSE file for details. It's MIT.

TODO
----
- Create a modular framework and break commands into modules
- Add more methods to the DataPoller for enhanced search/removal commands
- Implement lots of unimplemented stuff in the config file
  - Joining multiple channels
  - Reading admins list into an array
  - Considering ignore list for commands
- Add some functionality to stop/restart/reload non-critical threads via admin commands
