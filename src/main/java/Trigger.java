import java.util.ArrayList;

public class Trigger {
  private ArrayList<String> keywords;
  private float chance;
  private boolean dependant;
  private String copypasta;

  public Trigger(ArrayList<String> keywords, float chance, boolean dependant, String copypasta) {
    this.keywords = keywords;
    this.chance = chance;
    this.dependant = dependant;
    this.copypasta = copypasta;
  }

  public ArrayList<String> getKeywords() {
    return keywords;
  }

  public float getChance() {
    return chance;
  }

  public boolean isDependant() {
    return dependant;
  }

  public String getCopypasta() {
    return copypasta;
  }
}
