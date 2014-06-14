package org.librewulf.fmbot.plugins;

import org.librewulf.fmbot.BotState;
import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;

/**
 * TODO: javadoc comment
 */
public abstract class Plugin implements Runnable {

    // Time to wait for a thread to die after being interrupted
    public static int THREAD_TIMEOUT = 500;

    // Whether our plugin is enabled or not
    private boolean enabled = false;
    private Thread threadInstance;
    protected BotState state;

    public final void initState(BotState state) {
        this.state = state;
    }

    /*
     * Static methods
     */

    public static void reply(Message source, IRCSendificator sendificator, String reply) {
        String nick =  source.getSource().split("!")[0];
        if (source.getDestination().startsWith("#")) {
            // It's a channel
            sendificator.queuePrivmsg(source.getDestination(), nick + ": " + reply);
        } else {
            // It's a user
            String user = source.getSource().split("!")[0];
            sendificator.queuePrivmsg(user, nick + ": " + reply);
        }
    }

    /*
     * Overrides
     */

    /**
     * A thread that is started once after plugin initialization. You should not manually be using this method or
     * passing this Runnable object to newThread(), use startThread() instead as it makes sure there is a way to
     * manage the thread after creation.
     */
    @Override
    public void run() {
        // Do nothing by default
        return;
    }

    /*
     * Public instance methods
     */

    /**
     * Returns the plugin's enabled state.
     *
     * @return true if the plugin is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the plugin's enabled state. If disabling a plugin, stop the thread associated with it.
     *
     * @param enabled true to enable the plugin, false to disable it.
     */
    public void setEnabled(boolean enabled) {
        // Only disable if enabled and vice versa
        if (!enabled && this.enabled) {
            this.enabled = false;
            this.onDisable();
            // Stop the thread
            this.stopThread();
        } else if(!this.enabled) {
            this.enabled = true;
            this.onEnable();
            // Start the thread
            this.startThread();
        }
    }

    /**
     * TODO: Real javadoc
     *
     * @return true if thread is known to be stopped, false otherwise
     */
    private boolean stopThread() {
        if (this.threadInstance.isAlive()) {
            this.threadInstance.interrupt();

            // Figure out if our interrupt worked by trying to join()
            try {
                this.threadInstance.join(500);
            } catch (InterruptedException e) {
                // See if maybe the thread was successfully ended when we caught our own interrupt
                if (!this.threadInstance.isAlive()) {
                    return true;
                } else {
                    System.err.format("WARNING: Thread for %s was asked to stop, but may not have.",
                            this.getClass().getName());
                    return false;
                }
            }

            if (!this.threadInstance.isAlive()) {
                return true;
            }

            /*
             * If we're here, the thread hasn't stopped for some reason. This is likely the fault of the author of
             * the plugin.
             */
            System.err.format("WARNING: Thread for %s was asked to stop, but has not after %dms.",
                    this.getClass().getName(), this.THREAD_TIMEOUT);

            return false;

        } else {
            // Thread wasn't running to begin with
            return true;
        }
    }

    /**
     * Instantiates and starts the thread associated with the plugin. No guarantees are made about being connected or
     * being able to send messages when the thread is started, so make sure you track state using the provided events.
     *
     * @return a reference to the started Thread
     */
    private Thread startThread() {
        this.threadInstance = new Thread(this);
        this.threadInstance.start();
        return this.threadInstance;
    }

    /*
     * The methods we want to be overridden in implementing classes. We aren't declaring them as abstract because
     * that would force plugin writers to implement all methods, leading to lots of useless duplication.
     */

    /**
     * A function to be run once, during bot initialization, before connection to the server and before the run()
     * method is started as a thread, if applicable. This is the place to check for dependencies your plugin may
     * require, as at this point all the plugins are loaded into the state.plugins HashMap.
     */
    public void onInitialize() {
        // Do nothing by default
        return;
    }

    /**
     * Run as soon as the connection is established, before the NICK and USER commands.
     *
     * @param sendificator the sendificator to the server
     */
    public void onConnect(IRCSendificator sendificator) {
        // Do nothing by default
        return;
    }

    /**
     * Run on any message received from the server. Unlike other methods that deal with messages, the message parameter
     * of this method is the raw string, line endings and all, for things that can't be put into message objects
     * cleanly (ex: SASL involves sending the string "AUTHENTICATE +", which doesn't start with :, and is not an IRC
     * command.
     *
     * @param sendificator the sendificator to the server
     * @param message the message
     */
    public void onRawMessage(IRCSendificator sendificator, String message) {
        // Do nothing by default
        return;
    }

    /**
     * Run at the end of the MOTD command, ensuring that the connection was successful and the server is ready for
     * commands.
     *
     * @param sendificator the sendificator to the server
     * @param motdEnd the End of MOTD message from the server
     */
    public void onEndOfMOTD(IRCSendificator sendificator, Message motdEnd) {
        // Do nothing by default
        return;
    }

    /**
     * Run on a PRIVMSG to either a channel the bot is listening to or the bot itself.
     *
     * @param sendificator the sendificator to the server
     * @param message the message
     */
    public void onPrivmsg(IRCSendificator sendificator, Message message) {
        // Do nothing by default
        return;
    }

    /**
     * Run when a user (including the bot itself) joins a channel.
     *
     * @param sendificator the sendificator to the server
     * @param message the JOIN message
     */
    public void onUserJoin(IRCSendificator sendificator, Message message) {
        // Do nothing by default
        return;
    }

    /**
     * Run when a user (including the bot itself) leaves a channel (part, quit or kick).
     *
     * @param sendificator the sendificator to the server
     * @param message the leave message
     */
    public void onUserLeave(IRCSendificator sendificator, Message message) {
        // Do nothing by default
        return;
    }

    /**
     * Run when the plugin is enabled.
     */
    public void onEnable() {
        // Do nothing by default
        return;
    }

    /**
     * Run when the plugin is disabled.
     */
    public void onDisable() {
        // Do nothing by default
        return;
    }

    // TODO: Implement secondary methods
}
