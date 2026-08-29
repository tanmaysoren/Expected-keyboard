package expected.keyboard2.prediction;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculates spatial key proximity on standard QWERTY layout and
 * Damerau-Levenshtein distance with keyboard-aware typo penalties.
 */
public class SpatialDistance
{
  private static final Map<Character, float[]> KEY_POSITIONS = new HashMap<Character, float[]>();

  static
  {
    String[] rows = new String[]{
      "qwertyuiop",
      "asdfghjkl",
      "zxcvbnm"
    };
    float[] rowOffsets = new float[]{ 0.0f, 0.5f, 1.0f };

    for (int r = 0; r < rows.length; r++)
    {
      String row = rows[r];
      float y = (float) r;
      for (int c = 0; c < row.length(); c++)
      {
        char ch = row.charAt(c);
        float x = (float) c + rowOffsets[r];
        KEY_POSITIONS.put(ch, new float[]{ x, y });
      }
    }
  }

  public static double keyDistance(char c1, char c2)
  {
    c1 = Character.toLowerCase(c1);
    c2 = Character.toLowerCase(c2);
    if (c1 == c2)
      return 0.0;
    float[] p1 = KEY_POSITIONS.get(c1);
    float[] p2 = KEY_POSITIONS.get(c2);
    if (p1 == null || p2 == null)
      return 1.5;
    double dx = p1[0] - p2[0];
    double dy = p1[1] - p2[1];
    return Math.sqrt(dx * dx + dy * dy);
  }

  /**
   * Weighted Damerau-Levenshtein distance where keyboard-adjacent substitutions
   * incur much smaller penalty than random edits.
   */
  public static double weightedEditDistance(String s1, String s2)
  {
    if (s1 == null || s2 == null)
      return 100.0;
    int len1 = s1.length();
    int len2 = s2.length();
    if (len1 == 0) return len2 * 1.0;
    if (len2 == 0) return len1 * 1.0;

    double[][] d = new double[len1 + 1][len2 + 1];

    for (int i = 0; i <= len1; i++)
      d[i][0] = i * 1.0;
    for (int j = 0; j <= len2; j++)
      d[0][j] = j * 1.0;

    for (int i = 1; i <= len1; i++)
    {
      char c1 = s1.charAt(i - 1);
      for (int j = 1; j <= len2; j++)
      {
        char c2 = s2.charAt(j - 1);
        double cost;
        if (Character.toLowerCase(c1) == Character.toLowerCase(c2))
        {
          cost = 0.0;
        }
        else
        {
          double kDist = keyDistance(c1, c2);
          cost = Math.min(1.0, 0.4 + 0.3 * Math.min(kDist, 2.0));
        }

        double deletion = d[i - 1][j] + 1.0;
        double insertion = d[i][j - 1] + 1.0;
        double substitution = d[i - 1][j - 1] + cost;

        double min = Math.min(deletion, Math.min(insertion, substitution));

        // Transposition check (e.g. "teh" -> "the")
        if (i > 1 && j > 1 && s1.charAt(i - 1) == s2.charAt(j - 2) && s1.charAt(i - 2) == s2.charAt(j - 1))
        {
          min = Math.min(min, d[i - 2][j - 2] + 0.4);
        }

        d[i][j] = min;
      }
    }
    return d[len1][len2];
  }
}
