package expected.keyboard2.prediction;

/**
 * Represents a predicted word candidate with its ranking score and match origin.
 */
public class WordCandidate
{
  public static final int TYPE_EXACT = 0;
  public static final int TYPE_USER_LEARNED = 1;
  public static final int TYPE_NEXT_WORD = 2;
  public static final int TYPE_PREFIX = 3;
  public static final int TYPE_AUTOCORRECT = 4;

  public String word;
  public int score;
  public int type;
  public double distance;

  public WordCandidate(String word, int score, int type)
  {
    this.word = word;
    this.score = score;
    this.type = type;
    this.distance = 0.0;
  }

  public WordCandidate(String word, int score, int type, double distance)
  {
    this.word = word;
    this.score = score;
    this.type = type;
    this.distance = distance;
  }

  @Override
  public String toString()
  {
    return word + "(" + score + ", type=" + type + ")";
  }
}
