package org.librewulf.fmbot;

//FIXME: This should be renamed to "Reply" or "ServerReply"
//FIXME: Rewrite this whole thing to have knowledge of specifc commands and use
//a variable list of arguments
/**
 * A class representing a reply or message coming from an IRC server to the bot.
 */
public class Message {

    /**
     * The source of the message, usually the server or a user. Technically,
     * according to the RFC, this is optional, but it is present on almost
     * every modern IRC server.
     */
    private String source = "";

    // The command (numeric or otherwise), such as 376 or PRIVMSG.
    private String command = "";

    /**
     * The destination of the message, usually a user or channel. Note that
     * this may not exist for some messages like QUITs. Technically this is
     * actually considered just a parameter for the command above according to
     * the RFC, but it is almost always a destination.
     */
    private String destination = "";

    /*
     * The content of the message. This also may not exist for some messages.
     * Like above, it is technically the second argument to the command.
     */
    private String content = "";

    /**
     * Default constructor.
     *
     * @param rawmsg
     *        A String containing a valid message received from an IRC server.
     *
     * @requires <pre>
     * {@code rawmsg != null and rawmsg is a valid irc server response}
     * </pre>
     *
     */
    public Message(String rawmsg) {
        // remove the \r\n
        String trimmed = rawmsg.trim();

        String[] tmparr = trimmed.split(" ");
        this.source = tmparr[0].substring(1); //Remove the leading ":"
        this.command = tmparr[1];

        /* Here is where we need to determine if destination or content exist.
         * If the third feild of the line begins with a ";", we can assume what
         * follows is a message which may contain whitespace. If not, then it's
         * a normal parameter (usually destination, see comment in destination
         * declaration above).
         */
        if (tmparr[2].charAt(0) == ':') {
            this.content = tmparr[2].substring(1); // Remove the leading ":"
            for (int x = 3; x < tmparr.length; x++) {
                this.content += " " + tmparr[x];
            }
        } else {
            this.destination = tmparr[2];

            // tmparr[3] may not exist, in which case content will be empty
            if (tmparr.length > 3) {
                // Remove leading ":"
                if (tmparr[3].charAt(0) == ':') {
                    this.content = tmparr[3].substring(1);
                } else {
                    this.content = tmparr[3];
                }

                for (int x = 4; x < tmparr.length; x++) {
                    this.content += " " + tmparr[x];
                }
            }
        }
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return the destination
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Tests whether the given string is an IRC message. A well formed IRC
     * message (for the purposes of this bot) begins with a ":", followed by a
     * server name, a space, a command, a space, and at least one arguments. It
     * also must not contain a \r\n anywhere besides the end of the line.
     *
     * @param line the line to test
     * @return true if the message is a valid IRC message, false otherwise.
     */
    public static boolean isIRCMessage(String line) {
        String trimmed = line.trim();

        // Must not contain a \r\n other than at the end (which was just
        // trimmed off)
        if (trimmed.contains("\r\n")) {
            return false;
        }

        // Must start with :
        if (!trimmed.startsWith(":")) {
            return false;
        }

        String[] tmparr = trimmed.split(" ");
        if (tmparr.length < 3) {
            return false;
        }

        return true;
    }
}
