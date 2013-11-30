package org.librewulf.fmbot;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class is responsible for reading messages from a thread safe queue, then
 * sending them out to the IRC server in a way that won't get us kicked for
 * flooding.
 *
 * @author William Osler
 */
public class IRCSendificator implements Runnable {

    /*
     * The queue that will hold our messages.
     */
    // TODO: Consider switching to Deque
    private ConcurrentLinkedQueue<String> messageQueue;

    /*
     * The PrintWriter we use to send to the server. THIS SHOULD NOT BE
     * TOUCHED OUTSIDE OF SYNCHRONIZED METHODS.
     */
    private PrintWriter printWriter;

    /*
     * The time the last message (subject to rate limiting) was sent.
     */
    private long lastSent;

    /*
     * Private internal methods
     */

    /**
     * Sends a message to the server.
     *
     * @param msg The message to be sent to the server, minus the \r\n
     */
    private synchronized void send(String msg) {
        this.printWriter.print(msg + "\r\n");
        this.printWriter.flush();
        System.out.println("Sent: " + msg.trim());
    }

    /**
     * Creates new instances of our queue.
     */
    private void createNewRep() {
        this.messageQueue = new ConcurrentLinkedQueue<>();
    }

    /*
     * Overidden methods from other classes.
     */

    @Override
    public String toString() {
        return this.messageQueue.toString();
    }

    @Override
    public int hashCode() {
        return this.messageQueue.hashCode() ^ this.printWriter.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IRCSendificator that = (IRCSendificator) o;

        if (lastSent != that.lastSent) return false;
        if (!messageQueue.equals(that.messageQueue)) return false;

        return true;
    }

    @Override
    public void run() {
        while (true) {
            // Check to see if we have a message to work with
            try {
                String message = this.messageQueue.remove();

                // Rate limit check
                long elapsed = System.currentTimeMillis() - this.lastSent;
                if (elapsed < 3000) {
                    int penalty = (message.length() - 50) * 1000;
                    if (penalty < 0) {
                        penalty = 0;
                    }

                    penalty = (penalty / 70) + 800;

                    if (elapsed < penalty) {
                        try {
                            Thread.sleep(penalty - elapsed);
                        } catch (InterruptedException ie) {
                            System.out.println("Caught an interrupt, " +
                                    "suspending message sending!");

                            /*
                             * At this point our message is no longer in the
                             * queue, but we never got a chance to send it.
                             * We can't send it now or it might cause a
                             * flood, so we just have to stick it back at the
                             * end of the queue and let it be out of order.
                             */
                            this.messageQueue.add(message);

                            return;
                        }
                    }

                }

                this.lastSent = System.currentTimeMillis();
                this.send(message);

            } catch (NoSuchElementException nsee) {
                // No element found, go to sleep
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    System.out.println("Caught an interrupt, " +
                            "suspending message sending!");

                    return;
                }
            }
        }
    }

    /*
     * Constructor and public methods
     */

    /**
     * Default Constructor
     *
     * @param out the output stream the messages should be written to
     */
    public IRCSendificator(OutputStream out) {
        this.printWriter = new PrintWriter(out);
        this.createNewRep();
        this.lastSent = 0;
    }

    /**
     * Clears the list of messages waiting to be sent.
     */
    public void clear() {
        this.createNewRep();
    }

    /**
     * Adds a raw message to the Queue to be sent.
     *
     * @param msg A message to be sent to the server. If the string contians
     *            multiple lines, each line will be queued individually.
     */
    public void queueRaw(String msg) {
        String[] strArr = msg.split("\n");

        for (String line : strArr) {
            line = line.trim();
            this.messageQueue.add(line);
        }
    }

    /**
     * Adds an IRC command with given parameters to the queue to be sent.
     *
     * @param command The IRC command to send to the server.
     * @param args Zero or more paramaters for the IRC command. Note that if
     *             the final argument contains whitespace it should begin
     *             with a ":" as per IRC spec. This function will not check
     *             for or fix this if it is missing.
     */
    public void queueCommand(String command, String... args) {
        String rawStr = command;

        for (String arg : args) {
            rawStr += " " + arg;
        }

        queueRaw(rawStr);
    }

    /**
     * Adds a PRIVMSG to a user to the queue to be sent.
     *
     * @param to The channel or user the PRIVMSG should be sent to.
     * @param content The content of the message being sent.
     */
    public void queuePrivmsg(String to, String content) {
        queueCommand("PRIVMSG", to, ":" + content);
    }

    /**
     * Immediately sends a message to the server, bypassing the message queue
     * and flood restrictions. USE THIS VERY CAREFULLY as servers will
     * disconnect bots they suspect of flooding.
     *
     * @param msg The raw IRC message to send
     */
    public void sendNow(String msg) {
        this.send(msg);
    }

    /**
     * Immediately sends a PONG to the server, bypassing the message queue
     * and any flood restrictions.
     *
     * @param arg The argument to be sent with the PONG command,
     *            usually the name of the server.
     */
    public void pong(String arg) {
        this.send("PONG " + arg);
    }
}

