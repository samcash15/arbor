package com.arbor.service;

import com.arbor.model.OutlineItem;
import org.commonmark.node.*;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutlineService {

    private static final Parser MD_PARSER = Parser.builder()
            .includeSourceSpans(IncludeSourceSpans.BLOCKS)
            .build();

    // Java patterns
    private static final Pattern JAVA_CLASS = Pattern.compile(
            "^\\s*(?:public\\s+|private\\s+|protected\\s+)?(?:abstract\\s+|final\\s+|static\\s+|sealed\\s+|non-sealed\\s+)*(?:class|interface|enum|record)\\s+(\\w+)",
            Pattern.MULTILINE);
    private static final Pattern JAVA_METHOD = Pattern.compile(
            "^\\s*(?:public|private|protected)\\s+(?:static\\s+)?(?:final\\s+)?(?:synchronized\\s+)?(?:<[^>]+>\\s+)?\\w[\\w<>\\[\\],\\s]*\\s+(\\w+)\\s*\\(",
            Pattern.MULTILINE);

    // JS/TS patterns
    private static final Pattern JS_FUNCTION = Pattern.compile(
            "^\\s*(?:export\\s+)?(?:async\\s+)?function\\s+(\\w+)",
            Pattern.MULTILINE);
    private static final Pattern JS_CLASS = Pattern.compile(
            "^\\s*(?:export\\s+)?(?:abstract\\s+)?class\\s+(\\w+)",
            Pattern.MULTILINE);
    private static final Pattern JS_ARROW = Pattern.compile(
            "^\\s*(?:export\\s+)?(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:async\\s+)?(?:\\([^)]*\\)|\\w+)\\s*=>",
            Pattern.MULTILINE);

    // Python patterns
    private static final Pattern PY_CLASS = Pattern.compile(
            "^class\\s+(\\w+)", Pattern.MULTILINE);
    private static final Pattern PY_DEF = Pattern.compile(
            "^\\s+def\\s+(\\w+)|^def\\s+(\\w+)", Pattern.MULTILINE);

    public List<OutlineItem> buildOutline(String text, String language, boolean isMarkdown) {
        if (isMarkdown) {
            return buildMarkdownOutline(text);
        }
        if (language != null) {
            return buildCodeOutline(text, language);
        }
        return List.of();
    }

    private List<OutlineItem> buildMarkdownOutline(String text) {
        List<OutlineItem> items = new ArrayList<>();
        Node document = MD_PARSER.parse(text);

        document.accept(new AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                int line = 0;
                if (!heading.getSourceSpans().isEmpty()) {
                    line = heading.getSourceSpans().getFirst().getLineIndex();
                }
                String label = extractText(heading);
                items.add(new OutlineItem(label, heading.getLevel(), line));
            }
        });

        return items;
    }

    private String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Text text) {
                sb.append(text.getLiteral());
            } else if (child instanceof Code code) {
                sb.append(code.getLiteral());
            } else {
                sb.append(extractText(child));
            }
            child = child.getNext();
        }
        return sb.toString();
    }

    private List<OutlineItem> buildCodeOutline(String text, String language) {
        List<OutlineItem> items = new ArrayList<>();
        String[] lines = text.split("\n");

        switch (language) {
            case "java" -> {
                addMatches(items, text, lines, JAVA_CLASS, 1, 1);
                addMatches(items, text, lines, JAVA_METHOD, 1, 2);
            }
            case "js" -> {
                addMatches(items, text, lines, JS_CLASS, 1, 1);
                addMatches(items, text, lines, JS_FUNCTION, 1, 2);
                addMatches(items, text, lines, JS_ARROW, 1, 2);
            }
            case "python" -> {
                addMatches(items, text, lines, PY_CLASS, 1, 1);
                addPythonDefs(items, text, lines);
            }
        }

        items.sort((a, b) -> Integer.compare(a.lineNumber(), b.lineNumber()));
        return items;
    }

    private void addMatches(List<OutlineItem> items, String text, String[] lines,
                            Pattern pattern, int group, int level) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(group);
            int lineNum = lineNumberAt(text, matcher.start());
            items.add(new OutlineItem(name, level, lineNum));
        }
    }

    private void addPythonDefs(List<OutlineItem> items, String text, String[] lines) {
        Matcher matcher = PY_DEF.matcher(text);
        while (matcher.find()) {
            // Group 1 = indented def (method), Group 2 = top-level def (function)
            String name = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int level = matcher.group(1) != null ? 2 : 1;
            int lineNum = lineNumberAt(text, matcher.start());
            items.add(new OutlineItem(name, level, lineNum));
        }
    }

    private int lineNumberAt(String text, int charIndex) {
        int line = 0;
        for (int i = 0; i < charIndex && i < text.length(); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return line;
    }
}
