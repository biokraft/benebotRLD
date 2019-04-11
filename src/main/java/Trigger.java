public class Trigger {
    private int CID;
    private String command;
    private String content;
    private float probability;

    public Trigger(int CID, String command, String content, float probability) {
        this.CID = CID;
        this.content = content;
        this.command = command;
        this.probability = probability;
    }

    public int getCID() {
        return CID;
    }

    public void setCID(int CID) {
        this.CID = CID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }
}
