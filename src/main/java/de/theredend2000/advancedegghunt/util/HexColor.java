package de.theredend2000.advancedegghunt.util;

import org.bukkit.ChatColor;

public class HexColor {

    public HexColor(){

    }

    public static String color(final String textToTranslate) {
        final char altColorChar = '&';
        final StringBuilder b = new StringBuilder();
        final char[] mess = textToTranslate.toCharArray();
        boolean color = false, hashtag = false, doubleTag = false;
        char tmp;

        for (int i = 0; i < mess.length; ) {

            final char c = mess[i];

            if (doubleTag) {
                doubleTag = false;

                final int max = i + 3;

                if (max <= mess.length) {
                    boolean match = true;

                    for (int n = i; n < max; n++) {
                        tmp = mess[n];
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        b.append(ChatColor.COLOR_CHAR);
                        b.append('x');

                        for (; i < max; i++) {
                            tmp = mess[i];
                            b.append(ChatColor.COLOR_CHAR);
                            b.append(tmp);
                            b.append(ChatColor.COLOR_CHAR);
                            b.append(tmp);
                        }

                        continue;
                    }
                }

                b.append(altColorChar);
                b.append("##");
            }

            if (hashtag) {
                hashtag = false;

                if (c == '#') {
                    doubleTag = true;
                    i++;
                    continue;
                }

                final int max = i + 6;

                if (max <= mess.length) {
                    boolean match = true;

                    for (int n = i; n < max; n++) {
                        tmp = mess[n];
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        b.append(ChatColor.COLOR_CHAR);
                        b.append('x');

                        for (; i < max; i++) {
                            b.append(ChatColor.COLOR_CHAR);
                            b.append(mess[i]);
                        }
                        continue;
                    }
                }

                b.append(altColorChar);
                b.append('#');
            }


            if (color) { // Color module
                color = false;

                if (c == '#') {
                    hashtag = true;
                    i++;
                    continue;
                }

                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r' || (c >= 'k' && c <= 'o') || (c >= 'A' && c <= 'F') || c == 'R' || (c >= 'K' && c <= 'O')) {
                    b.append(ChatColor.COLOR_CHAR);
                    b.append(c);
                    i++;
                    continue;
                }

                b.append(altColorChar);
            }

            if (c == altColorChar) {
                color = true;
                i++;
                continue;
            }

            b.append(c);
            i++;

        }

        if (color)
            b.append(altColorChar);
        else
            if (hashtag) {
                b.append(altColorChar);
                b.append('#');
            } else
                if (doubleTag) {
                    b.append(altColorChar);
                    b.append("##");
                }

        return b.toString();
    }

}
