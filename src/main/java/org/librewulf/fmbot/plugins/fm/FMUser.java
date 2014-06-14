package org.librewulf.fmbot.plugins.fm;

/**
 * An object modelling a user of a *.fm service (last.fm, libre.fm etc).
 *
 * @author William Osler
 */
public class FMUser {
    /*
     * Regexes for validation outside this class
     */
    public static final String userRegex = "^[a-zA-Z0-9][a-zA-Z0-9_-]{1,14}[a-zA-z0-9]$";
    public static final String domainRegex = "^([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?.)+[a-zA-Z]{2,6}$";

    /*
     * The user's service username
     */
    private String username = "";

    /*
     * The domain name. For Last.fm, this is ws.audioscrobbler.com.
     */
    private String domain = "";

    /*
     * The IRC user's nick.
     */
    private String nick;

    /*
     * The IRC channel this user is broadcasting to, since this is a multi-channel enabled bot.
     */
    private String channel = "";

    /*
     * The last song the user was listening to.
     */
    private String lastListen = "";

    /*
     * Overrides
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FMUser fmUser = (FMUser) o;

        if (!channel.equals(fmUser.channel)) return false;
        if (!domain.equals(fmUser.domain)) return false;
        if (!username.equals(fmUser.username)) return false;
        if (!nick.equals(fmUser.nick)) return false;

        /*
         * We don't consider the last listen for equality, since we don't have that info when we need to remove
         * something from a collection which works on equality.
         */

        return true;
    }

    @Override
    public int hashCode() {
        return username.hashCode() ^ domain.hashCode() ^ nick.hashCode() ^ channel.hashCode() ^ lastListen.hashCode();
    }

    @Override
    public String toString() {
        return username + "@" + domain + " registered to " + nick + " in " + channel + " listened to " + lastListen;
    }

    /**
     * Default constructor.
     *
     * @param username The FM service username
     * @param domain The domain name where the account can be found
     * @param channel
     */
    public FMUser (String username, String domain, String nick, String channel) {
        this.username = username;

        if (domain.equals("last.fm")) {
            this.domain = "ws.audioscrobbler.com";
        } else {
            this.domain = domain;
        }

        this.nick = nick;
        this.channel = channel;
    }

    /**
     * Constructor using a comma seperated string.
     *
     * @param data A comma separated string containing the username, domain, channel, nick,
     *             and (optional) last listen (in that order).
     */
    public FMUser (String data) {
        String[] strArr = data.split(",");

        this.username = strArr[0];

        if (strArr[1].equals("last.fm")) {
            this.domain = "ws.audioscrobbler.com";
        } else {
            this.domain = strArr[1];
        }

        this.nick = strArr[2];
        this.channel = strArr[3];

        // Check for a lastListen
        if (strArr.length > 4) {
            this.lastListen = strArr[4];
        }
    }

    /**
     * The FM service username
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * The domain name where the account can be found
     *
     * @return domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * The IRC channel that this user will broadcast to
     *
     * @return channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * The IRC nick of the user
     *
     * @return nick
     */
    public String getNick() {
        return nick;
    }

    /**
     * The data of the track the user last listened to, mbid if from last.fm, or a combination of metadata otherwise.
     *
     * @return last listened info
     */
    public String getLastListen() {
        return lastListen;
    }

    /**
     * Sets the data of the track the user last listeend to.
     *
     * @param lastListen last listened info
     */
    public void setLastListen(String lastListen) {
        this.lastListen = lastListen;
    }

    /**
     * Returns a string of user data suitable to be written to a CSV. The output of this method is compatible with
     * the String constructor for this object.
     *
     * @return a comma separated string of the fields of this object
     */
    public String toFileString() {
        String result = username + "," + domain + "," + nick + "," + channel;

        if (!lastListen.equals("")) {
            result += "," + lastListen;
        }

        return result;
    }

}
