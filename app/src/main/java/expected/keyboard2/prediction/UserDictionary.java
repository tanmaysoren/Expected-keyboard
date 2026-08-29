package expected.keyboard2.prediction;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import expected.keyboard2.DirectBootAwarePreferences;

/**
 * Dynamic on-device user dictionary that learns user vocabulary, selection patterns,
 * and bigram transitions offline.
 */
public class UserDictionary
{
  private static final String PREF_NAME = "futo_user_dictionary";
  private static final String PREF_WORDS_KEY = "user_learned_words";
  private static final String PREF_BIGRAM_KEY = "user_learned_bigrams";
  private static final int MAX_USER_WORDS = 2000;

  private final SharedPreferences prefs;
  private final Map<String, Integer> wordFrequencies = new HashMap<String, Integer>();
  private final Map<String, Map<String, Integer>> bigrams = new HashMap<String, Map<String, Integer>>();

  public UserDictionary(Context context)
  {
    this.prefs = DirectBootAwarePreferences.get_protected_prefs(context, PREF_NAME);
    load();
  }

  private void load()
  {
    Set<String> wordsSet = prefs.getStringSet(PREF_WORDS_KEY, null);
    if (wordsSet != null)
    {
      for (String item : wordsSet)
      {
        int colon = item.lastIndexOf(':');
        if (colon > 0)
        {
          String word = item.substring(0, colon);
          try
          {
            int freq = Integer.parseInt(item.substring(colon + 1));
            wordFrequencies.put(word.toLowerCase(Locale.ROOT), freq);
          }
          catch (NumberFormatException ignored) {}
        }
      }
    }

    Set<String> bigramSet = prefs.getStringSet(PREF_BIGRAM_KEY, null);
    if (bigramSet != null)
    {
      for (String item : bigramSet)
      {
        String[] parts = item.split(":");
        if (parts.length == 3)
        {
          String prev = parts[0].toLowerCase(Locale.ROOT);
          String next = parts[1].toLowerCase(Locale.ROOT);
          try
          {
            int count = Integer.parseInt(parts[2]);
            Map<String, Integer> nextMap = bigrams.get(prev);
            if (nextMap == null)
            {
              nextMap = new HashMap<String, Integer>();
              bigrams.put(prev, nextMap);
            }
            nextMap.put(next, count);
          }
          catch (NumberFormatException ignored) {}
        }
      }
    }
  }

  public synchronized void save()
  {
    Set<String> wordsSet = new HashSet<String>();
    for (Map.Entry<String, Integer> e : wordFrequencies.entrySet())
    {
      wordsSet.add(e.getKey() + ":" + e.getValue());
    }

    Set<String> bigramSet = new HashSet<String>();
    for (Map.Entry<String, Map<String, Integer>> prevEntry : bigrams.entrySet())
    {
      String prev = prevEntry.getKey();
      for (Map.Entry<String, Integer> nextEntry : prevEntry.getValue().entrySet())
      {
        bigramSet.add(prev + ":" + nextEntry.getKey() + ":" + nextEntry.getValue());
      }
    }

    prefs.edit()
      .putStringSet(PREF_WORDS_KEY, wordsSet)
      .putStringSet(PREF_BIGRAM_KEY, bigramSet)
      .apply();
  }

  public synchronized void learnWord(String word)
  {
    if (word == null || word.length() < 2)
      return;
    String lower = word.toLowerCase(Locale.ROOT);
    int current = wordFrequencies.containsKey(lower) ? wordFrequencies.get(lower) : 0;
    wordFrequencies.put(lower, Math.min(current + 1, 500));

    if (wordFrequencies.size() > MAX_USER_WORDS)
    {
      prune();
    }
    save();
  }

  public synchronized void learnBigram(String prevWord, String nextWord)
  {
    if (prevWord == null || nextWord == null || prevWord.isEmpty() || nextWord.isEmpty())
      return;
    String prev = prevWord.toLowerCase(Locale.ROOT);
    String next = nextWord.toLowerCase(Locale.ROOT);

    Map<String, Integer> nextMap = bigrams.get(prev);
    if (nextMap == null)
    {
      nextMap = new HashMap<String, Integer>();
      bigrams.put(prev, nextMap);
    }
    int count = nextMap.containsKey(next) ? nextMap.get(next) : 0;
    nextMap.put(next, Math.min(count + 1, 200));
    save();
  }

  public synchronized int getWordFrequency(String word)
  {
    if (word == null) return 0;
    String lower = word.toLowerCase(Locale.ROOT);
    return wordFrequencies.containsKey(lower) ? wordFrequencies.get(lower) : 0;
  }

  public synchronized List<WordCandidate> getNextWordPredictions(String prevWord, int maxCount)
  {
    List<WordCandidate> results = new ArrayList<WordCandidate>();
    if (prevWord == null || prevWord.isEmpty())
      return results;
    String prev = prevWord.toLowerCase(Locale.ROOT);
    Map<String, Integer> nextMap = bigrams.get(prev);
    if (nextMap != null)
    {
      for (Map.Entry<String, Integer> e : nextMap.entrySet())
      {
        results.add(new WordCandidate(e.getKey(), e.getValue() * 10, WordCandidate.TYPE_NEXT_WORD));
      }
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
    }
    return results;
  }

  public synchronized List<WordCandidate> getPrefixMatches(String prefix, int maxCount)
  {
    List<WordCandidate> results = new ArrayList<WordCandidate>();
    if (prefix == null || prefix.isEmpty())
      return results;
    String lower = prefix.toLowerCase(Locale.ROOT);
    for (Map.Entry<String, Integer> e : wordFrequencies.entrySet())
    {
      if (e.getKey().startsWith(lower))
      {
        results.add(new WordCandidate(e.getKey(), e.getValue() * 5 + 50, WordCandidate.TYPE_USER_LEARNED));
      }
    }
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

  private void prune()
  {
    List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(wordFrequencies.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String, Integer>>()
    {
      @Override
      public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b)
      {
        return Integer.compare(a.getValue(), b.getValue());
      }
    });
    int toRemove = list.size() - (MAX_USER_WORDS / 2);
    for (int i = 0; i < toRemove; i++)
    {
      wordFrequencies.remove(list.get(i).getKey());
    }
  }
}
