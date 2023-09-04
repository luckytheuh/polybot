package polybot.commands;

public enum Category {
    FUN("Fun and novelty commands"),
    GENERAL("Commands that don't really fit into a category."),
    LEVELING("Leveling related commands"),
    MEDIA("Media related commands, whether it be editing, applying overlays, or displaying images."),
    MODERATOR("Moderation related commands."),
    UTILITY("Commands that provide some form of utility.");

    final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Category searchFromName(String s) {
        if (s == null) return null;

        for (Category category : Category.values()) {
            if (category.name().equalsIgnoreCase(s)) return category;
        }

        return null;
    }
}
