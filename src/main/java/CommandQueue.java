public class CommandQueue {
    private int UserID;
    private long ChatID;
    private String Command;
    private int State;

    public CommandQueue(int userID, long chatID, String command, int state) {
        UserID = userID;
        ChatID = chatID;
        Command = command;
        State = state;
    }

    public int getUserID() {
        return UserID;
    }

    public long getChatID() {
        return ChatID;
    }

    public String getCommand() {
        return Command;
    }

    public int getState() {
        return State;
    }
}
