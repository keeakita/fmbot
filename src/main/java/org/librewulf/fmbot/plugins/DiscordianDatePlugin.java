package org.librewulf.fmbot.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.librewulf.fmbot.IRCSendificator;
import org.librewulf.fmbot.Message;

/**
 * TODO: Real Javadoc
 */
public class DiscordianDatePlugin extends Plugin {

    private static final Pattern datePattern = Pattern.compile("(\\d{4})\\-(\\d{2})\\-(\\d{2})");

    public DiscordianDatePlugin() {
    }

    @Override
    public void onPrivmsg(IRCSendificator sendificator, Message message) {
        String[] msgParts = message.getContent().split(" ");
        if (msgParts[0].equals("|ddate")) {
            try {
                String date;

                if (msgParts.length < 2) {
                    // No argument, print today
                    Process ddate = Runtime.getRuntime().exec(new String[]{
                            "ddate", "+Today is %A, the %e day of %B in the YOLD %Y. %.%N Have a Chaotic %H!"});

                    BufferedReader dIn = new BufferedReader(new InputStreamReader(ddate.getInputStream()));
                    date = dIn.readLine();

                    dIn.close();
                } else {
                    Matcher dateMatcher = datePattern.matcher(msgParts[1]);

                    if (dateMatcher.matches()) {
                        // Date given as argument

                        // Translate String -> int -> String for extra sanitization of input
                        String year = Integer.toString(Integer.parseInt(dateMatcher.group(1)));
                        String month = Integer.toString(Integer.parseInt(dateMatcher.group(2)));
                        String day = Integer.toString(Integer.parseInt(dateMatcher.group(3)));

                        Process ddate = Runtime.getRuntime().exec(new String[]{
                                "ddate", "+%A, the %e day of %B in the YOLD %Y. %.%N Holyday: %H!",
                                day, month, year});

                        BufferedReader dIn = new BufferedReader(new InputStreamReader(ddate.getInputStream()));
                        date = dIn.readLine();

                        dIn.close();
                    } else {
                        date = "Usage: |ddate [YYYY-MM-DD]";
                    }
                }

                reply(message, sendificator, date);
            } catch (IOException e) {
                reply(message, sendificator, "Something went wrong while checking the date. Hail Eris.");
                System.err.println("Unable to exec ddate: " + e.getMessage());
            }
        }
    }
}
