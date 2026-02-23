package com.arbor.service;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlightService {

    private static final Map<String, Pattern> LANGUAGE_PATTERNS = new HashMap<>();
    private static final Map<String, Set<String>> EXTENSION_MAP = new HashMap<>();

    static {
        // Java
        String javaKeywords = String.join("|",
                "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
                "class", "const", "continue", "default", "do", "double", "else", "enum",
                "extends", "final", "finally", "float", "for", "goto", "if", "implements",
                "import", "instanceof", "int", "interface", "long", "native", "new",
                "package", "private", "protected", "public", "return", "short", "static",
                "strictfp", "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "var", "void", "volatile", "while", "yield", "record",
                "sealed", "permits", "non-sealed");
        LANGUAGE_PATTERNS.put("java", buildPattern(javaKeywords));
        EXTENSION_MAP.put("java", Set.of("java"));

        // JavaScript / TypeScript
        String jsKeywords = String.join("|",
                "async", "await", "break", "case", "catch", "class", "const", "continue",
                "debugger", "default", "delete", "do", "else", "export", "extends", "false",
                "finally", "for", "from", "function", "if", "import", "in", "instanceof",
                "let", "new", "null", "of", "return", "static", "super", "switch", "this",
                "throw", "true", "try", "typeof", "undefined", "var", "void", "while",
                "with", "yield", "interface", "type", "enum", "implements", "public",
                "private", "protected", "readonly", "abstract", "as", "any", "boolean",
                "number", "string", "symbol", "never", "unknown");
        LANGUAGE_PATTERNS.put("js", buildPattern(jsKeywords));
        EXTENSION_MAP.put("js", Set.of("js", "jsx", "ts", "tsx", "mjs"));

        // Python
        String pyKeywords = String.join("|",
                "False", "None", "True", "and", "as", "assert", "async", "await",
                "break", "class", "continue", "def", "del", "elif", "else", "except",
                "finally", "for", "from", "global", "if", "import", "in", "is",
                "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try",
                "while", "with", "yield", "self");
        LANGUAGE_PATTERNS.put("python", buildPattern(pyKeywords));
        EXTENSION_MAP.put("python", Set.of("py", "pyw"));

        // JSON
        String jsonKeywords = "true|false|null";
        LANGUAGE_PATTERNS.put("json", Pattern.compile(
                "(?<STRING>\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\")"
                + "|(?<NUMBER>-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)"
                + "|(?<KEYWORD>\\b(" + jsonKeywords + ")\\b)"
        ));
        EXTENSION_MAP.put("json", Set.of("json"));

        // XML / HTML
        LANGUAGE_PATTERNS.put("xml", Pattern.compile(
                "(?<XMLTAG></?[a-zA-Z][a-zA-Z0-9_-]*)"
                + "|(?<XMLCLOSE>/?>)"
                + "|(?<XMLATTR>\\b[a-zA-Z_][a-zA-Z0-9_-]*(?=\\s*=))"
                + "|(?<STRING>\"[^\"]*\"|'[^']*')"
                + "|(?<COMMENT><!--[\\s\\S]*?-->)"
        ));
        EXTENSION_MAP.put("xml", Set.of("xml", "html", "htm", "xhtml", "fxml", "svg"));

        // CSS
        String cssKeywords = String.join("|",
                "inherit", "initial", "unset", "none", "auto", "important");
        LANGUAGE_PATTERNS.put("css", Pattern.compile(
                "(?<SELECTOR>[.#]?[a-zA-Z_][a-zA-Z0-9_-]*(?=\\s*[{,]))"
                + "|(?<PROPERTY>[a-zA-Z-]+(?=\\s*:))"
                + "|(?<STRING>\"[^\"]*\"|'[^']*')"
                + "|(?<NUMBER>-?\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|s|ms|deg)?)"
                + "|(?<COMMENT>/\\*[\\s\\S]*?\\*/)"
                + "|(?<KEYWORD>\\b(" + cssKeywords + ")\\b)"
        ));
        EXTENSION_MAP.put("css", Set.of("css", "scss", "less"));
    }

    private static Pattern buildPattern(String keywords) {
        return Pattern.compile(
                "(?<KEYWORD>\\b(" + keywords + ")\\b)"
                + "|(?<STRING>\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(\\\\.[^'\\\\]*)*')"
                + "|(?<COMMENT>//[^\n]*|/\\*[\\s\\S]*?\\*/)"
                + "|(?<NUMBER>\\b\\d+(\\.\\d+)?[fFdDlL]?\\b)"
                + "|(?<ANNOTATION>@[a-zA-Z_][a-zA-Z0-9_]*)"
        );
    }

    public String detectLanguage(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = fileName.substring(dot + 1).toLowerCase();

        for (Map.Entry<String, Set<String>> entry : EXTENSION_MAP.entrySet()) {
            if (entry.getValue().contains(ext)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public StyleSpans<Collection<String>> computeHighlighting(String text, String language) {
        if (language == null || !LANGUAGE_PATTERNS.containsKey(language)) {
            StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
            builder.add(Collections.emptySet(), text.length());
            return builder.create();
        }

        Pattern pattern = LANGUAGE_PATTERNS.get(language);
        Matcher matcher = pattern.matcher(text);
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        while (matcher.find()) {
            String styleClass = getStyleClass(matcher);
            if (styleClass == null) continue;

            builder.add(Collections.emptySet(), matcher.start() - lastEnd);
            builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastEnd = matcher.end();
        }

        builder.add(Collections.emptySet(), text.length() - lastEnd);
        return builder.create();
    }

    private String getStyleClass(Matcher matcher) {
        try { if (matcher.group("KEYWORD") != null) return "syntax-keyword"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("STRING") != null) return "syntax-string"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("COMMENT") != null) return "syntax-comment"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("NUMBER") != null) return "syntax-number"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("ANNOTATION") != null) return "syntax-annotation"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("XMLTAG") != null) return "syntax-keyword"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("XMLCLOSE") != null) return "syntax-keyword"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("XMLATTR") != null) return "syntax-annotation"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("SELECTOR") != null) return "syntax-keyword"; } catch (IllegalArgumentException ignored) {}
        try { if (matcher.group("PROPERTY") != null) return "syntax-annotation"; } catch (IllegalArgumentException ignored) {}
        return null;
    }
}
