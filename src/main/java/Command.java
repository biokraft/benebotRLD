public class Command {
  private String name;
  private String response;

  public Command(String name, String response) {
    this.name = name;
    this.response = response;
  }

  public String getName() {
    return name;
  }

  public String getResponse() {
    return response;
  }
}
