package org.librewulf.fmbot.plugins.fm;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;
import org.librewulf.fmbot.plugins.Plugin;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * This plugin polls the API (or APIs) of sites we need to fetch data from. In this case, Last.fm, Libre.fm,
 * or any user defined GNU FM site. It contains a collection of users, backed by a file for persistence.
 *
 * @author William Osler
 */
public class FMPlugin extends Plugin {

    /*
     * ConcurrentLinkedDeque to hold our entries. This is used since it allows us to circle around easily making
     * iteration simple, and still lets us add elements to the front.
     */
    private final ConcurrentLinkedDeque<FMUser> users = new ConcurrentLinkedDeque<>();

    /*
     * Information needed to use the Last.fm API. If this is missing, we won't poll Last.fm
     */
    private String APIKey = "";

    /*
     * The name of the file we're saving to
     */
    private final String fileName = "data/fmusers.csv";

    /*
     * The sendificator for queueing messages
     */
    private IRCSendificator sendificator = null;

    /*
     * Private methods
     */

    /**
     * Saves the users deque to a file.
     */
    private void writeToFile() {
        try {
            PrintWriter fileOut = new PrintWriter(new FileWriter(fileName));

            synchronized (users) {
                // It should be safe to iterate
                for (FMUser user : users) {
                    fileOut.println(user.toFileString());
                }
            }

            fileOut.close();
        } catch (IOException eIO) {
            System.err.println("ERROR writing to " + fileName + ": " + eIO.getMessage());
            System.err.println("NOTE: FM user file may be corrupt, please check it before restarting the bot");
        }
    }

    /**
     * Add a user to the deque, and to the file.
     *
     * @param user The user to be added to the polling deque.
     */
    private void add(FMUser user) {
        // Put them at the front so they get processed on next poll, gives more immediate feedback to the user
        users.addFirst(user);

        // Instead of wasting time flushing the whole thing to a file, let's just append this one line
        try {
            PrintWriter fileOut = new PrintWriter(new FileOutputStream(new File(fileName), true));
            fileOut.println(user.toFileString());
            fileOut.close();
        } catch (FileNotFoundException eNotFound) {
            // We can't append to a non existent file, so let's do a full write
            this.writeToFile();
        }
    }

    /**
     * Add a user to the deque, and to the file.
     *
     * @param user The user to be added to the polling deque.
     */
    private void remove(FMUser user) {
        // Remove the user
        users.remove(user);

        // Unlike add we can't quickly just remove one line, so we re-save the file
        this.writeToFile();
    }

    /**
     * Loads user data from a comma separated file, clearing the user object and creating a new file if no file is
     * found.
     */
    private void loadFromFile() {
        Scanner fileIn = null;

        // We synchronize here because there's a case a user has been removed from the queue to be worked on,
        // but not re-added yet
        synchronized (users) {
            users.clear();
        }

        try {
            fileIn = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Warning: FM users file not found. Creating a blank one.");
            this.writeToFile();
        }

        synchronized (users) {
            while (fileIn.hasNextLine()) {
                users.add(new FMUser(fileIn.nextLine()));
            }
        }
    }

    /*
     * Overridden methods
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        if (!super.equals(o)) return false;

        FMPlugin that = (FMPlugin) o;

        if (!APIKey.equals(that.APIKey)) return false;
        if (!users.equals(that.users)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return users.hashCode() ^ APIKey.hashCode();
    }

    @Override
    public void run() {
        while (this.isEnabled()) {
            // Wait until we have a user and a valid way to send it
            if (!users.isEmpty() && sendificator != null) {
                // Take one user off the list and check the API
                FMUser user;

                // Synchronize this so an attempt to save the file doesn't catch the deque missing an element
                synchronized(users) {
                    user = users.removeFirst();
                    users.addLast(user);
                }

                // Construct the url
                String urlString = "http://" + user.getDomain() + "/2.0/?method=user.getrecenttracks&user=" + user
                        .getUsername() + "&nowplaying=true&limit=1";

                // We have to skip if the endpoint is last.fm and there's no API Key
                boolean proceed = true;

                if (user.getDomain().equals("ws.audioscrobbler.com")) {
                    if (!APIKey.equals("")) {
                        urlString += "&api_key=" + APIKey;
                    } else {
                        System.err.println("Error: Cannot poll Last.fm because no APIKey was specified. Skipping " +
                                user.toString());
                        proceed = false;
                    }
                }

                if (!state.inChannel(user.getChannel())) {
                    System.err.println("Error: User " + user + " will not be checked for updates, " +
                            "since we aren't in that channel.");
                    proceed = false;
                }

                if (proceed) {
                    String title = "";
                    String mbid = "";
                    String artist = "";
                    String album = "";

                    // Fetch data from the API
                    try {
                        Document doc = fetchDocument(urlString);

                        if (doc != null) {
                            XPathFactory xpathFactory = XPathFactory.newInstance();
                            XPath xpath = xpathFactory.newXPath();

                            title = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/name", doc);
                            mbid = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/mbid", doc);
                            artist = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/artist", doc);
                            album = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/album", doc);
                        }
                    } catch (XPathExpressionException e) {
                        // XPath expression was malformed. This should never occur, since our XPath is hardcoded in
                        System.err.println("Error: XPath expression threw an error: " + e.getMessage());
                    }

                    // See if the user is currently listening to anything
                    boolean sendUpdate = false;
                    String newLastListened = "";

                    // We might have not gotten anything. Don't send a blank update
                    if (!(title.equals("") && artist.equals("") && album.equals(""))) {
                        if (!mbid.equals("")) {
                            // Use the mbid since it should stay constant
                            newLastListened = mbid;

                            if (!mbid.equals(user.getLastListen())) {
                                sendUpdate = true;
                            }
                        } else {
                            // mbid not available, combine available info
                            newLastListened = title + "|" + artist + "|" + album;

                            if (!newLastListened.equals(user.getLastListen())) {
                                sendUpdate = true;
                            }
                        }

                        user.setLastListen(newLastListened);
                    }

                    // Determine what to send and send it
                    if (sendUpdate) {
                        String message = user.getNick() + " is now listening to ";

                        if (!title.equals("")) {
                            message += "\"\u0002\u000308" + title + "\u000F\"";
                        } else {
                            message += "a song";
                        }

                        if (!artist.equals("")) {
                            message += " by \u0002" + artist + "\u000F";
                        }

                        if (!album.equals("")) {
                            message += " from \u0002" + album + "\u000F";
                        }

                        message += ".";

                        sendificator.queuePrivmsg(user.getChannel(), message);

                        /*
                         * We modified the user object, so we should save it to the file. This way, we can interrupt the
                         * thread and restart it later, using the cached listening data to prevent from repeating a
                         * listen announcement.
                         */
                        this.writeToFile();
                    }
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Everything should be in a sane state and saved to a file, so we can just return
                return;
            }
        }
    }

    @Override
    public void onInitialize() {
        this.APIKey = state.getConfig().getProperty("apikey");
    }

    @Override
    public void onEndOfMOTD(IRCSendificator sendificator, Message motdEnd) {
        this.sendificator = sendificator;
    }

    @Override
    public void onPrivmsg(IRCSendificator sendificator, Message message) {
        // Add an fm user
        if (message.getContent().startsWith("|fmadd ")) {
            String[] cmdArr = message.getContent().split(" ");
            // |fmadd user domain
            if (cmdArr.length == 3 && cmdArr[1].matches(FMUser.userRegex) && cmdArr[2].matches(
                    FMUser.domainRegex) && message.getDestination().startsWith("#")) {

                FMUser user = new FMUser(cmdArr[1], cmdArr[2], message.getSource().split("!")[0],
                        message.getDestination());

                if (!users.contains(user)) {
                    this.add(user);
                    reply(message, sendificator, String.format("Now tracking %s@%s in this channel as %s",
                            user.getUsername(), user.getDomain(), user.getNick()));
                    reply(message, sendificator, String.format("To stop monitoring, type \"|fmdel %s %s\"",
                            user.getUsername(), user.getDomain()));
                } else {
                    reply(message, sendificator, "User already exists.");
                }

            // |fmadd user domain channel
            } else if (cmdArr.length == 4 && cmdArr[1].matches(FMUser.userRegex) && cmdArr[2].matches(
                    FMUser.domainRegex) && cmdArr[3].startsWith("#")) {

                // Make sure the bot is in a channel before letting somebody register with it
                if (state.inChannel(cmdArr[3])) {
                    FMUser user = new FMUser(cmdArr[1], cmdArr[2], message.getSource().split("!")[0], cmdArr[3]);

                    if (!users.contains(user)) {
                        this.add(user);
                        reply(message, sendificator, String.format("Now tracking %s@%s in %s as %s", user.getUsername(),
                                user.getDomain(), user.getChannel(), user.getNick()));
                        reply(message, sendificator, String.format("To stop monitoring, type \"|fmdel %s %s %s\"",
                                user.getUsername(), user.getDomain(), user.getChannel()));
                    } else {
                        reply(message, sendificator, "User already exists.");
                    }
                } else {
                    reply(message, sendificator, "You can't register to " + cmdArr[3] +  ", I'm not in that channel!");
                }
            } else {
                reply(message, sendificator, "Usage: |fmadd user domain (channel)");
            }
        // Remove an fm user
        } else if (message.getContent().startsWith("|fmdel ")) {
            String[] cmdArr = message.getContent().split(" ");
            // |fmdel user domain
            if (cmdArr.length == 3 && cmdArr[1].matches(FMUser.userRegex) && cmdArr[2].matches(
                    FMUser.domainRegex) && message.getDestination().startsWith("#")) {

                FMUser user = new FMUser(cmdArr[1], cmdArr[2], message.getSource().split("!")[0],
                        message.getDestination());

                if (users.contains(user)) {
                    this.remove(user);
                    reply(message, sendificator, String.format("No longer monitoring %s@%s in this channel.",
                            user.getUsername(), user.getDomain()));
                } else {
                    reply(message, sendificator, "No such user found.");
                }

            // |fmdel user domain channel
            } else if (cmdArr.length == 4 && cmdArr[1].matches(FMUser.userRegex) && cmdArr[2].matches(
                    FMUser.domainRegex) && cmdArr[3].startsWith("#")) {

                FMUser user = new FMUser(cmdArr[1], cmdArr[2], message.getSource().split("!")[0], cmdArr[3]);

                if (this.users.contains(user)) {
                    this.remove(user);
                    reply(message, sendificator, String.format("No longer monitoring %s@%s in %s.", user.getUsername(),
                            user.getDomain(), user.getChannel()));
                } else {
                    reply(message, sendificator, "No such user found.");
                }
            } else {
                reply(message, sendificator, "Usage: |fmdel user domain (channel)");
            }
        }
    }

    @Override
    public void onEnable() {
        loadFromFile();
    }

    /**
     * Fetches the document, taking care of redirects.
     * @param uri
     */
    private Document fetchDocument(String uri) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(uri);
        CloseableHttpResponse response = null;

        try {
            response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = builder.parse(response.getEntity().getContent());
                return doc;
            } else {
                System.err.println("Error: Not OK trying to fetch " + uri + ": " + response.getStatusLine());
            }
        } catch (Exception e) {
            System.err.println("Error: Got Exception trying to fetch " + uri + ": " + e.getMessage());
        } finally {
            // Close it if it needs to be
            try {
                response.close();
            } catch (Exception e) {}
        }

        return null;
    }
}
