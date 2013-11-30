package org.librewulf.fmbot;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * DataPoller polls the API (or APIs) of sites we need to fetch data from. In this case, Last.fm, Libre.fm,
 * or any user defined GNU FM site. It contains a collection of users, backed by a file for persistence.
 *
 * @author William Osler
 */
public class DataPoller implements Runnable {

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
    private IRCSendificator sendificator;

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

    /*
     * Overridden methods
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataPoller that = (DataPoller) o;

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
        while (true) {
            if (!users.isEmpty()) {
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

                if (proceed) {
                    String title = "";
                    String mbid = "";
                    String artist = "";
                    String album = "";

                    // Fetch data from the API
                    try {
                        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                        Document doc = builder.parse(urlString);

                        XPathFactory xpathFactory = XPathFactory.newInstance();
                        XPath xpath = xpathFactory.newXPath();

                        title = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/name", doc);
                        mbid = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/mbid", doc);
                        artist = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/artist", doc);
                        album = xpath.evaluate("/lfm/recenttracks/track[@nowplaying='true']/album", doc);
                    } catch (IOException eIO) {
                        // It's probably temporary and there's not much we can do, so just warn the user and move on.
                        System.err.println("Error: IOException while polling " + urlString + " :" + eIO.getMessage());
                    } catch (ParserConfigurationException e) {
                        // Invalid data from server
                        System.err.println("Error: Got invalid XML from server: " + e.getMessage());
                    } catch (SAXException e) {
                        // I don't have a clue what could cause this
                        System.err.println("Error: Unknown SAX error :" + e.getMessage());
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
                        //TODO: Figure out if this is a good idea
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

    /*
     * Constructor
     */

    /**
     * Default constructor.
     *
     * @param sendificator the IRCSendificator for queueing messages
     * @param APIKey the Last.fm API key
     */
    public DataPoller(IRCSendificator sendificator, String APIKey) {
        this.sendificator = sendificator;
        this.APIKey = APIKey;
    }

    /*
     * Public methods
     */

    /**
     * Add a user to the deque, and to the file.
     *
     * @param user The user to be added to the polling deque.
     */
    public void add(FMUser user) {
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
    public void remove(FMUser user) {
        // Remove the user
        users.remove(user);

        // Unlike add we can't quickly just remove one line, so we re-save the file
        this.writeToFile();
    }

    /**
     * Loads user data from a comma separated file, clearing the user object and creating a new file if no file is found.
     */
    public void loadFromFile() {
        Scanner fileIn = null;

        // No need to synchronize here, since we don't care what's in it
        users.clear();

        try {
            fileIn = new Scanner(new File(fileName));
        } catch (FileNotFoundException e) {
            System.out.println("Warning: FM users file not found. Creating a blank one.");
            this.writeToFile();
        }

        synchronized (users) {
            // Clear the users object
            while (fileIn.hasNextLine()) {
                users.add(new FMUser(fileIn.nextLine()));
            }
        }
    }

    //TODO: contains method and find by field methods
}
