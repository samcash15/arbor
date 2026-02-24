package com.arbor.service;

import com.arbor.model.CommandEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CommandRegistry {
    private final List<CommandEntry> commands = new ArrayList<>();

    public void register(CommandEntry command) {
        commands.add(command);
    }

    public void registerAll(List<CommandEntry> entries) {
        commands.addAll(entries);
    }

    public void clear() {
        commands.clear();
    }

    public List<CommandEntry> getAll() {
        return commands.stream()
                .sorted(Comparator.comparing(CommandEntry::category)
                        .thenComparing(CommandEntry::name))
                .toList();
    }

    public List<CommandEntry> search(String query) {
        if (query == null || query.isBlank()) {
            return getAll();
        }

        String[] tokens = query.toLowerCase().strip().split("\\s+");

        List<CommandEntry> matched = commands.stream()
                .filter(cmd -> {
                    String text = (cmd.category() + " " + cmd.name()).toLowerCase();
                    for (String token : tokens) {
                        if (!text.contains(token)) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();

        String lowerQuery = query.toLowerCase().strip();

        return matched.stream()
                .sorted(Comparator.comparingInt((CommandEntry cmd) -> {
                    String name = cmd.name().toLowerCase();
                    if (name.startsWith(lowerQuery)) {
                        return 0; // exact prefix match
                    } else if (name.contains(lowerQuery)) {
                        return 1; // contains full query
                    } else {
                        return 2; // token match
                    }
                }).thenComparing(CommandEntry::category)
                  .thenComparing(CommandEntry::name))
                .toList();
    }
}
