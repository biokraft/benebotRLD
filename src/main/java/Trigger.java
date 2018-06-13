import java.util.ArrayList;

public class Trigger {
  private ArrayList<String> keywords;
  private float chance;
  private boolean dependant;

  public Trigger(ArrayList<String> keywords, float chance, boolean dependant) {
    this.keywords = keywords;
    this.chance = chance;
    this.dependant = dependant;
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
}
