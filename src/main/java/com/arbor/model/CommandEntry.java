package com.arbor.model;

public record CommandEntry(String name, String category, String shortcut, Runnable action) {
}
