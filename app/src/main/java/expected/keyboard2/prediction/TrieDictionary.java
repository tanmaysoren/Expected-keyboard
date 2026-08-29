package expected.keyboard2.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Fast Trie-based prefix and frequency dictionary for word prediction and completion.
 */
public class TrieDictionary
{
  public static class Node
  {
    public final char character;
    public boolean isWord = false;
    public int frequency = 0;
    public String word = null;
    public Map<Character, Node> children = new HashMap<Character, Node>();

    public Node(char c)
    {
      this.character = c;
    }
  }

  private final Node root = new Node('\0');
  private int wordCount = 0;

  public void insert(String word, int frequency)
  {
    if (word == null || word.isEmpty())
      return;
    String lower = word.toLowerCase(Locale.ROOT);
    Node current = root;
    for (int i = 0; i < lower.length(); i++)
    {
      char c = lower.charAt(i);
      Node child = current.children.get(c);
      if (child == null)
      {
        child = new Node(c);
        current.children.put(c, child);
      }
      current = child;
    }
    if (!current.isWord)
    {
      wordCount++;
    }
    current.isWord = true;
    current.word = word;
    if (frequency > current.frequency)
    {
      current.frequency = frequency;
    }
  }

  public boolean contains(String word)
  {
    if (word == null || word.isEmpty())
      return false;
    String lower = word.toLowerCase(Locale.ROOT);
    Node current = root;
    for (int i = 0; i < lower.length(); i++)
    {
      Node child = current.children.get(lower.charAt(i));
      if (child == null)
        return false;
      current = child;
    }
    return current.isWord;
  }

  public int getFrequency(String word)
  {
    if (word == null || word.isEmpty())
      return 0;
    String lower = word.toLowerCase(Locale.ROOT);
    Node current = root;
    for (int i = 0; i < lower.length(); i++)
    {
      Node child = current.children.get(lower.charAt(i));
      if (child == null)
        return 0;
      current = child;
    }
    return current.isWord ? current.frequency : 0;
  }

  public List<WordCandidate> getPrefixMatches(String prefix, int maxCount)
  {
    List<WordCandidate> results = new ArrayList<WordCandidate>();
    if (prefix == null || prefix.isEmpty())
      return results;

    String lower = prefix.toLowerCase(Locale.ROOT);
    Node current = root;
    for (int i = 0; i < lower.length(); i++)
    {
      Node child = current.children.get(lower.charAt(i));
      if (child == null)
        return results;
      current = child;
    }

    collectAllWords(current, results);
    Collections.sort(results, new Comparator<WordCandidate>()
    {
      @Override
      public int compare(WordCandidate a, WordCandidate b)
      {
        return Integer.compare(b.score, a.score);
      }
    });

    if (results.size() > maxCount)
    {
      return new ArrayList<WordCandidate>(results.subList(0, maxCount));
    }
    return results;
  }

  private void collectAllWords(Node node, List<WordCandidate> results)
  {
    if (node.isWord && node.word != null)
    {
      results.add(new WordCandidate(node.word, node.frequency, WordCandidate.TYPE_PREFIX));
    }
    for (Node child : node.children.values())
    {
      collectAllWords(child, results);
    }
  }

  public int size()
  {
    return wordCount;
  }

  public Node getRoot()
  {
    return root;
  }
}
