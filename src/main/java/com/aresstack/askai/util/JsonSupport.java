package com.aresstack.askai.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small JSON helper for the spike so the app stays dependency-light.
 */
public final class JsonSupport {

    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    private JsonSupport() {
    }

    public static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    public static List<String> extractModelNames(String json) {
        ArrayList<String> names = new ArrayList<String>();
        if (json == null || json.isEmpty()) {
            return names;
        }
        Matcher matcher = MODEL_NAME_PATTERN.matcher(json);
        while (matcher.find()) {
            names.add(unescape(matcher.group(1)));
        }
        return names;
    }

    public static String extractFirstStringValue(String json, String key) {
        if (json == null || key == null) {
            return "";
        }
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return "";
        }
        return readJsonString(json, valueStart);
    }

    public static String extractFirstNumberValue(String json, String key) {
        if (json == null || key == null) {
            return "";
        }
        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }
        int valueStart = skipWhitespace(json, colonIndex + 1);
        int valueEnd = valueStart;
        while (valueEnd < json.length()) {
            char ch = json.charAt(valueEnd);
            if (!(ch == '-' || ch == '+' || ch == '.' || Character.isDigit(ch))) {
                break;
            }
            valueEnd++;
        }
        if (valueEnd <= valueStart) {
            return "";
        }
        return json.substring(valueStart, valueEnd);
    }

    public static String extractChatMessageContent(String json) {
        int messageIndex = json == null ? -1 : json.indexOf("\"message\"");
        if (messageIndex < 0) {
            return extractFirstStringValue(json, "response");
        }
        int contentIndex = json.indexOf("\"content\"", messageIndex);
        if (contentIndex < 0) {
            return extractFirstStringValue(json, "response");
        }
        int colonIndex = json.indexOf(':', contentIndex);
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return "";
        }
        return readJsonString(json, valueStart);
    }

    public static List<String> extractArrayObjects(String json, String key) {
        ArrayList<String> objects = new ArrayList<String>();
        int arrayStart = findValueStart(json, key, '[');
        if (arrayStart < 0) {
            return objects;
        }
        int index = arrayStart + 1;
        while (index < json.length()) {
            char ch = json.charAt(index);
            if (ch == ']') {
                return objects;
            }
            if (ch == '{') {
                int objectEnd = findMatchingEnd(json, index, '{', '}');
                if (objectEnd < 0) {
                    return objects;
                }
                objects.add(json.substring(index, objectEnd + 1));
                index = objectEnd + 1;
            } else {
                index++;
            }
        }
        return objects;
    }

    public static String extractObjectValue(String json, String key) {
        int objectStart = findValueStart(json, key, '{');
        if (objectStart < 0) {
            return ;
        }
        int objectEnd = findMatchingEnd(json, objectStart, '{', '}');
        if (objectEnd < 0) {
            return ;
        }
        return json.substring(objectStart, objectEnd + 1);
    }

    public static long extractFirstLongValue(String json, String key) {
        String number = extractFirstNumberValue(json, key);
        if (number.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static int findValueStart(String json, String key, char expectedStart) {
        if (json == null || key == null) {
            return -1;
        }
        String marker = " + key + ";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return -1;
        }
        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return -1;
        }
        int valueStart = skipWhitespace(json, colonIndex + 1);
        if (valueStart >= json.length() || json.charAt(valueStart) != expectedStart) {
            return -1;
        }
        return valueStart;
    }

    private static int findMatchingEnd(String json, int start, char open, char close) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '') { inString = false; } continue; } if (ch == '') {
                inString = true;
            } else if (ch == open) {
                depth++;
            } else if (ch == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    private static int skipWhitespace(String text, int start) {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static String readJsonString(String json, int quoteIndex) {
        StringBuilder builder = new StringBuilder();
        boolean escaped = false;
        for (int i = quoteIndex + 1; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (escaped) {
                appendEscaped(builder, ch, json, i);
                if (ch == 'u') {
                    i += 4;
                }
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else if (ch == '"') {
                return builder.toString();
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static void appendEscaped(StringBuilder builder, char escapedChar, String json, int index) {
        switch (escapedChar) {
            case '"':
                builder.append('"');
                break;
            case '\\':
                builder.append('\\');
                break;
            case '/':
                builder.append('/');
                break;
            case 'b':
                builder.append('\b');
                break;
            case 'f':
                builder.append('\f');
                break;
            case 'n':
                builder.append('\n');
                break;
            case 'r':
                builder.append('\r');
                break;
            case 't':
                builder.append('\t');
                break;
            case 'u':
                if (index + 4 < json.length()) {
                    String hex = json.substring(index + 1, index + 5);
                    try {
                        builder.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException ignored) {
                        builder.append("\\u").append(hex);
                    }
                }
                break;
            default:
                builder.append(escapedChar);
                break;
        }
    }

    private static String unescape(String value) {
        return readJsonString("\"" + value + "\"", 0);
    }
}

