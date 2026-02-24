package com.arbor.service;

import java.util.Map;

public class BracketMatchService {

    public record BracketPair(int openPos, int closePos) {}

    private static final Map<Character, Character> OPEN_TO_CLOSE = Map.of(
            '(', ')',
            '{', '}',
            '[', ']'
    );

    private static final Map<Character, Character> CLOSE_TO_OPEN = Map.of(
            ')', '(',
            '}', '{',
            ']', '['
    );

    public BracketPair findMatchingBracket(String text, int caretPos) {
        if (text == null || text.isEmpty()) {
            return null;
        }

        // Check character before caret
        if (caretPos > 0) {
            char before = text.charAt(caretPos - 1);
            BracketPair result = tryMatch(text, caretPos - 1, before);
            if (result != null) return result;
        }

        // Check character after caret
        if (caretPos < text.length()) {
            char after = text.charAt(caretPos);
            BracketPair result = tryMatch(text, caretPos, after);
            if (result != null) return result;
        }

        return null;
    }

    private BracketPair tryMatch(String text, int pos, char ch) {
        if (OPEN_TO_CLOSE.containsKey(ch)) {
            char target = OPEN_TO_CLOSE.get(ch);
            int match = searchForward(text, pos, ch, target);
            if (match >= 0) {
                return new BracketPair(pos, match);
            }
        } else if (CLOSE_TO_OPEN.containsKey(ch)) {
            char target = CLOSE_TO_OPEN.get(ch);
            int match = searchBackward(text, pos, ch, target);
            if (match >= 0) {
                return new BracketPair(match, pos);
            }
        }
        return null;
    }

    private int searchForward(String text, int pos, char open, char close) {
        int depth = 0;
        for (int i = pos; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private int searchBackward(String text, int pos, char close, char open) {
        int depth = 0;
        for (int i = pos; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == close) depth++;
            else if (c == open) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
