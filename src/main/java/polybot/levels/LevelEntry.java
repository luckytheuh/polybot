package polybot.levels;

import net.dv8tion.jda.api.entities.User;
import polybot.PolyBot;

public class LevelEntry {

    private final long userId;
    private int level, xp;

    public LevelEntry(long userId, int level, int xp) {
        this.userId = userId;
        this.level = level;
        this.xp = xp;
    }

    public User getUser() {
        User user = PolyBot.getJDA().getUserById(userId);

        // Fetch from API if it failed
        if (user == null) user = PolyBot.getJDA().retrieveUserById(userId).complete();

        return user;
    }

    public int getXpForNextLevel() {
        return getXpForLevel(level+1);
    }

    private int getXpForLevel(int level) {
        return Math.round(5f / 6 * level * (2 * level * level + 27 * level + 91));
    }

    //xp remaining for next level
    public int getXpToNextLevel() {
        return getXpForNextLevel() - xp;
    }

    //percentage
    public float getLevelProgress() {
        return (float) (xp - getXpForLevel(level)) / (getXpForNextLevel() - getXpForLevel(level));
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }
}
