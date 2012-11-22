package org.pojava.datetime;

/*
 Copyright 2008-09 John Pile

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/**
 * A set of methods for performing string manipulation, mostly to support internal needs without
 * requiring external libraries.
 */
public class ParseTool {

    /**
     * Returns a zero-based offset if the left-most characters of str match all of the
     * characters of any member of list.
     * 
     * @param list
     * @param str
     * @return -1 if no match, else indexed offset
     */
    public static int indexedStartMatch(String[] list, String str) {
        for (int i = 0; i < list.length; i++) {
            int len = list[i].length();
            if (str.length() >= len && list[i].equals(str.substring(0, len))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * True if a string matches /^[-]?[0-9]+$/
     * 
     * @param s
     * @return true if string is numeric
     */
    public static boolean isInteger(String s) {
        if (s == null || s.length() == 0)
            return false;
        char c = s.charAt(0);
        if (s.length() == 1)
            return c >= '0' && c <= '9';
        return (c == '-' || c >= '0' && c <= '9') && onlyDigits(s.substring(1));
    }

    /**
     * True if a string has only digits in it.
     * 
     * @param s
     * @return true if string is composed of only digits.
     */
    public static boolean onlyDigits(String s) {
        if (s == null || s.length() == 0)
            return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
    }

    /**
     * True if a string starts with a digit.
     * 
     * @param s
     * @return true if string starts with a digit.
     */
    public static boolean startsWithDigit(String s) {
        if (s == null || s.length() == 0)
            return false;
        char c = s.charAt(0);
        return (c >= '0' && c <= '9');
    }

    /**
     * Parse an integer from left-to-right until non-digit reached
     * 
     * @param str
     * @return first integer greedily matched from a string
     */
    public static int parseIntFragment(String str) {
        if (str == null) {
            return 0;
        }
        int parsed = 0;
        boolean isNeg = false;
        char[] strip = str.toCharArray();
        char c = strip[0];
        if (c == '-') {
            isNeg = true;
        } else if (c >= '0' && c <= '9') {
            parsed = c - '0';
        } else {
            return 0;
        }
        for (int i = 1; i < strip.length; i++) {
            c = strip[i];
            if (c >= '0' && c <= '9') {
                parsed = 10 * parsed + c - '0';
            } else {
                break;
            }
        }
        return isNeg ? -parsed : parsed;
    }

}

