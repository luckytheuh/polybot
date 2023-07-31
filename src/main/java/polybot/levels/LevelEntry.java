package polybot.levels;

public class LevelEntry {

    private final boolean error;
    private final long userId;
    private int rank, level, xp, messages;

    public LevelEntry(long userId, int level, int xp, int rank, int messages, boolean error) {
        this.messages = messages;
        this.userId = userId;
        this.error = error;
        this.level = level;
        this.rank = rank;
        this.xp = xp;
    }

    public long getUserId() {
        return userId;
    }

    public boolean isError() {
        return error;
    }

    /*
        public User getUser() {
            User user = PolyBot.getJDA().getUserById(userId);

            // Fetch from API if it failed
            if (user == null) user = PolyBot.getJDA().retrieveUserById(userId).complete();

            return user;
        }
    */

    public boolean calculateLevel() {
        boolean didGoUp = getLevelProgress() >= 1;

        while (getLevelProgress() >= 1) {
            level++;
        }

        return didGoUp;
    }

    public int getXpForNextLevel() {
        return getXpForLevel(level+1);
    }

    public int getXpForLevel(int level) {
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

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messages) {
        this.messages = messages;
    }
}
