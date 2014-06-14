package org.librewulf.fmbot;

import org.librewulf.fmbot.plugins.Plugin;

import java.util.*;

/**
 * Created by william on 1/25/14.
 * TODO: Real javadoc
 */
public class BotState {
    private boolean connected;
    private String nick;
    private Set<String> channels = new HashSet<>();
    private Map<String, Plugin> plugins;
    private Properties config;

    public BotState(Properties config, Map<String, Plugin> pluginMap) {
        this.config = config;
        this.plugins = pluginMap;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Set<String> getChannels() {
        Set<String> channelsDup;

        synchronized (channels) {
            channelsDup = Collections.unmodifiableSet(channels);
        }

        return channelsDup;
    }

    public void addChannel(String channel) {
        synchronized (channels) {
            channels.add(channel);
        }
    }

    public void removeChannel(String channel) {
        synchronized (channels) {
            channels.remove(channel);
        }
    }

    public boolean inChannel(String channel) {
        boolean inChannel = false;
        synchronized (channels) {
            inChannel = channels.contains(channel);
        }

        return inChannel;
    }

    public Map<String, Plugin> getPlugins() {
        return plugins;
    }

    public Properties getConfig() {
        return config;
    }
}
