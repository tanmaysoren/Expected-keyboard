package expected.keyboard2.prediction;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Modern offline Dictionary instance.
 */
public class Dictionary
{
  public final String name;
  public final TrieDictionary trie;

  public Dictionary(String name)
  {
    this.name = name;
    this.trie = new TrieDictionary();
  }

  public static Dictionary loadFromFile(String name, File file)
  {
    Dictionary dict = new Dictionary(name);
    if (file != null && file.exists())
    {
      try
      {
        FileInputStream fis = new FileInputStream(file);
        dict.load(fis);
        fis.close();
      }
      catch (Throwable ignored) {}
    }
    return dict;
  }

  public void load(InputStream is)
  {
    try
    {
      java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null)
      {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        int colon = line.indexOf(':');
        if (colon > 0)
        {
          String w = line.substring(0, colon).trim();
          try
          {
            int f = Integer.parseInt(line.substring(colon + 1).trim());
            trie.insert(w, f);
          }
          catch (NumberFormatException e)
          {
            trie.insert(w, 5);
          }
        }
        else
        {
          trie.insert(line, 5);
        }
      }
    }
    catch (Throwable ignored) {}
  }

  public void addWord(String word, int freq)
  {
    trie.insert(word, freq);
  }

  public boolean contains(String word)
  {
    return trie.contains(word);
  }

  public int getFrequency(String word)
  {
    return trie.getFrequency(word);
  }

  public List<WordCandidate> getPrefixMatches(String prefix, int maxCount)
  {
    return trie.getPrefixMatches(prefix, maxCount);
  }
}
