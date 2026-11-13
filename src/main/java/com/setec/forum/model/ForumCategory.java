package com.setec.forum.model;

import java.util.Arrays;

public enum ForumCategory {
    GENERAL("general", "General", "Open questions and community conversations"),
    FRONTEND("frontend", "Frontend", "Web interfaces, browsers, and frontend tools"),
    BACKEND("backend", "Backend", "APIs, databases, and server-side systems"),
    AI_ML("ai-ml", "AI & ML", "Machine learning, AI tools, and applications"),
    DEVOPS("devops", "DevOps", "Infrastructure, delivery, and operations"),
    CAREER("career", "Career", "Growth, hiring, and working in technology"),
    OPEN_SOURCE("open-source", "Open source", "Projects, contributions, and maintainers"),
    DESIGN("design", "Design", "Product design, UX, and creative tooling");

    private final String key;
    private final String label;
    private final String description;

    ForumCategory(String key, String label, String description) {
        this.key = key;
        this.label = label;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static ForumCategory fromKey(String key) {
        return Arrays.stream(values())
                .filter(category -> category.key.equalsIgnoreCase(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Choose a valid forum category"));
    }
}
