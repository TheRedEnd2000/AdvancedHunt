package de.theredend2000.advancedegghunt.util;

import org.bukkit.ChatColor;

public class HexColor {

    public HexColor(){

    }

    public static String color(final String textToTranslate) {
        final char altColorChar = '&';
        final StringBuilder stringBuilder = new StringBuilder();
        final char[] textToTranslateCharArray = textToTranslate.toCharArray();
        boolean color = false, hashtag = false, doubleTag = false;
        char tmp;

        for (int i = 0; i < textToTranslateCharArray.length; ) {
            final char c = textToTranslateCharArray[i];

            if (doubleTag) {
                doubleTag = false;

                final int max = i + 3;

                if (max <= textToTranslateCharArray.length) {
                    boolean match = true;

                    for (int n = i; n < max; n++) {
                        tmp = textToTranslateCharArray[n];
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        stringBuilder.append(ChatColor.COLOR_CHAR);
                        stringBuilder.append('x');

                        for (; i < max; i++) {
                            tmp = textToTranslateCharArray[i];
                            stringBuilder.append(ChatColor.COLOR_CHAR);
                            stringBuilder.append(tmp);
                            stringBuilder.append(ChatColor.COLOR_CHAR);
                            stringBuilder.append(tmp);
                        }

                        continue;
                    }
                }

                stringBuilder.append(altColorChar);
                stringBuilder.append("##");
            }

            if (hashtag) {
                hashtag = false;

                if (c == '#') {
                    doubleTag = true;
                    i++;
                    continue;
                }

                final int max = i + 6;

                if (max <= textToTranslateCharArray.length) {
                    boolean match = true;

                    for (int n = i; n < max; n++) {
                        tmp = textToTranslateCharArray[n];
                        if (!((tmp >= '0' && tmp <= '9') || (tmp >= 'a' && tmp <= 'f') || (tmp >= 'A' && tmp <= 'F'))) {
                            match = false;
                            break;
                        }
                    }

                    if (match) {
                        stringBuilder.append(ChatColor.COLOR_CHAR);
                        stringBuilder.append('x');

                        for (; i < max; i++) {
                            stringBuilder.append(ChatColor.COLOR_CHAR);
                            stringBuilder.append(textToTranslateCharArray[i]);
                        }
                        continue;
                    }
                }

                stringBuilder.append(altColorChar);
                stringBuilder.append('#');
            }

            if (color) { // Color module
                color = false;

                if (c == '#') {
                    hashtag = true;
                    i++;
                    continue;
                }

                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || c == 'r' || (c >= 'k' && c <= 'o') || (c >= 'A' && c <= 'F') || c == 'R' || (c >= 'K' && c <= 'O')) {
                    stringBuilder.append(ChatColor.COLOR_CHAR);
                    stringBuilder.append(c);
                    i++;
                    continue;
                }

                stringBuilder.append(altColorChar);
            }

            if (c == altColorChar) {
                color = true;
                i++;
                continue;
            }

            stringBuilder.append(c);
            i++;

        }

        if (color)
            stringBuilder.append(altColorChar);
        else
            if (hashtag) {
                stringBuilder.append(altColorChar);
                stringBuilder.append('#');
            } else
                if (doubleTag) {
                    stringBuilder.append(altColorChar);
                    stringBuilder.append("##");
                }

        return stringBuilder.toString();
    }
}
