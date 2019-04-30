public class User {
    private int UID;
    private String username;
    private String firstname;
    private String lastname;
    private long messageCounter;

    public User(int UID, String username, String firstname, String lastname, long messageCounter) {
        this.UID = UID;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.messageCounter = messageCounter;
    }

    public int getUID() {
        return UID;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public long getMessageCounter() {
        return messageCounter;
    }
}
