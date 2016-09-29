package org.librewulf.fmbot;

//FIXME: This should be renamed to "Reply" or "ServerReply"
//a variable list of arguments

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class representing a reply or message coming from an IRC server to the bot.
 */
public class Message {

    /**
     * The prefix of the message, usually the server or a user. Technically,
     * according to the RFC, this is optional, but it is present on almost
     * every modern IRC server.
     */
    private String prefix = "";

    // The command (numeric or otherwise), such as 376 or PRIVMSG.
    private String command = "";

    /**
     * A list of parameters associated with the command. Per the RFC,
     * parameters can't contain spaces except for the very last one.
     */
    private List<String> parameters = new ArrayList<>();

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
        int iterator = 0;

        if (tmparr[iterator].startsWith(":")) {
            this.prefix = tmparr[iterator].substring(1); // Remove the leading ":"
            iterator++;
        }

        this.command = tmparr[iterator];
        iterator++;

        // Collect parameters
        StringBuilder lastParam = new StringBuilder();
        boolean isLastParam = false;
        while (iterator < tmparr.length) {
            // Concatenate the final parameter into one string
            if (! isLastParam && tmparr[iterator].startsWith(":")) {
                isLastParam = true;

                // Strip leading ":"
                lastParam.append(tmparr[iterator].substring(1));
            } else if (isLastParam) {
                lastParam.append(" " + tmparr[iterator]);
            } else {
                this.parameters.add(tmparr[iterator]);
            }

            iterator++;
        }

        // Add the special final parameter, if there was one
        String lastParamStr = lastParam.toString();
        if (lastParamStr.length() > 0) {
            this.parameters.add(lastParamStr);
        }
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * The "destination" of the message, usually a user or channel. Note that
     * this may not exist for some messages like QUITs.
     *
     * Specifically, this returns the first parameter, unless that parameter
     * contains whitespace. See deprecation notice.
     *
     * @return the destination
     * @deprecated This uses a flawed and inaccurate model of IRC messages.
     * Instead, reference parameters by position.
     */
    public String getDestination() {
        String dest = parameters.get(0);

        if (dest.contains(" ")) {
            return "";
        }

        return dest;
    }

    /**
     * Get the "content" of the message. This is a space separated concatenation
     * of all parameters except the first.
     *
     * @return the last parameter of the message.
     * @deprecated This uses a flawed and inaccurate model of IRC messages.
     * Instead, reference parameters by position.
     */
    public String getContent() {
        if (parameters.size() > 1) {
            Iterator<String> iterator = parameters.iterator();
            iterator.next();
            return StringUtils.join(iterator, " ");
        }

        return "";
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
