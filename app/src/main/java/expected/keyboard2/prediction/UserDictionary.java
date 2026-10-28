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
  private static final String PREF_BLOCKLIST_KEY = "user_blocklist";
  private static final String PREF_LEARN_ENABLED_KEY = "learn_words_enabled";
  private static final int MAX_USER_WORDS = 2000;

  private static UserDictionary sInstance = null;

  /** Shared instance so the IME and the settings screen always operate on the
      same in-memory state. Without this, a stale instance would overwrite the
      saved dictionary on its next [save()], resurrecting deleted words and
      wiping the blocklist. */
  public static synchronized UserDictionary getInstance(Context context)
  {
    if (sInstance == null && context != null)
      sInstance = new UserDictionary(context.getApplicationContext());
    return sInstance;
  }

  private final SharedPreferences prefs;
  private final Map<String, Integer> wordFrequencies = new HashMap<String, Integer>();
  private final Map<String, Map<String, Integer>> bigrams = new HashMap<String, Map<String, Integer>>();
  private final Set<String> blocklist = new HashSet<String>();

  public static String normalizeWord(String word)
  {
    if (word == null)
      return null;
    String s = word.trim().replace("\r", "").replace("\n", "").replace("\t", "");
    if (s.isEmpty())
      return null;

    int start = 0;
    int end = s.length();
    while (start < end)
    {
      char c = s.charAt(start);
      if (Character.isLetterOrDigit(c))
        break;
      start++;
    }
    while (end > start)
    {
      char c = s.charAt(end - 1);
      if (Character.isLetterOrDigit(c))
        break;
      end--;
    }
    if (start >= end)
      return null;

    s = s.substring(start, end).toLowerCase(Locale.ROOT);

    boolean hasLetter = false;
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      if (Character.isLetter(c))
      {
        hasLetter = true;
      }
      else if (!Character.isDigit(c) && c != '\'' && c != '-')
      {
        return null;
      }
    }
    if (!hasLetter || s.length() < 2)
      return null;

    // Keys are stored lowercased so that lookups, deletions and the blocklist
    // are all case-insensitive. The suggestion bar re-capitalizes as needed.
    return s.toLowerCase(Locale.ROOT);
  }

  public UserDictionary(Context context)
  {
    this.prefs = DirectBootAwarePreferences.get_protected_prefs(context, PREF_NAME);
    load();
  }

  private void load()
  {
    // Load the blocklist first so blocked words are never re-added to the
    // in-memory dictionary below.
    Set<String> blockSet = prefs.getStringSet(PREF_BLOCKLIST_KEY, null);
    if (blockSet != null)
    {
      for (String w : blockSet) {
        String clean = normalizeWord(w);
        if (clean != null && !clean.isEmpty()) blocklist.add(clean);
      }
    }

    Set<String> wordsSet = prefs.getStringSet(PREF_WORDS_KEY, null);
    boolean needsResave = false;
    if (wordsSet != null)
    {
      for (String item : wordsSet)
      {
        if (item == null) continue;
        int colon = item.lastIndexOf(':');
        String rawWord;
        int freq = 1;
        if (colon > 0)
        {
          rawWord = item.substring(0, colon);
          try
          {
            freq = Integer.parseInt(item.substring(colon + 1));
          }
          catch (NumberFormatException ignored) {}
        }
        else
        {
          rawWord = item;
        }

        String clean = normalizeWord(rawWord);
        if (clean != null && !isBlocked(clean))
        {
          int existing = wordFrequencies.containsKey(clean) ? wordFrequencies.get(clean) : 0;
          wordFrequencies.put(clean, Math.min(500, existing + freq));
          if (!clean.equals(rawWord) || existing > 0)
          {
            needsResave = true;
          }
        }
        else
        {
          needsResave = true;
        }
      }
    }

    Set<String> bigramSet = prefs.getStringSet(PREF_BIGRAM_KEY, null);
    if (bigramSet != null)
    {
      for (String item : bigramSet)
      {
        if (item == null) continue;
        String[] parts = item.split(":");
        if (parts.length == 3)
        {
          String prev = normalizeWord(parts[0]);
          String next = normalizeWord(parts[1]);
          if (prev != null && next != null && !isBlocked(prev) && !isBlocked(next))
          {
            try
            {
              int count = Integer.parseInt(parts[2]);
              Map<String, Integer> nextMap = bigrams.get(prev);
              if (nextMap == null)
              {
                nextMap = new HashMap<String, Integer>();
                bigrams.put(prev, nextMap);
              }
              int existingCount = nextMap.containsKey(next) ? nextMap.get(next) : 0;
              nextMap.put(next, Math.min(200, existingCount + count));
            }
            catch (NumberFormatException ignored) {}
          }
          else
          {
            needsResave = true;
          }
        }
      }
    }

    if (needsResave)
    {
      save();
    }
  }

  public synchronized void save()
  {
    Set<String> wordsSet = new HashSet<String>();
    for (Map.Entry<String, Integer> e : wordFrequencies.entrySet())
    {
      String clean = normalizeWord(e.getKey());
      if (clean != null && !isBlocked(clean))
      {
        wordsSet.add(clean + ":" + e.getValue());
      }
    }

    Set<String> bigramSet = new HashSet<String>();
    for (Map.Entry<String, Map<String, Integer>> prevEntry : bigrams.entrySet())
    {
      String prev = normalizeWord(prevEntry.getKey());
      if (prev == null || isBlocked(prev)) continue;
      for (Map.Entry<String, Integer> nextEntry : prevEntry.getValue().entrySet())
      {
        String next = normalizeWord(nextEntry.getKey());
        if (next == null || isBlocked(next)) continue;
        bigramSet.add(prev + ":" + next + ":" + nextEntry.getValue());
      }
    }

    prefs.edit()
      .putStringSet(PREF_WORDS_KEY, wordsSet)
      .putStringSet(PREF_BIGRAM_KEY, bigramSet)
      .putStringSet(PREF_BLOCKLIST_KEY, new HashSet<String>(blocklist))
      .apply();
  }

  /** Removes all duplicate or repeated words, consolidating frequencies and saving immediately. */
  public synchronized int removeDuplicatesAndSave()
  {
    Map<String, Integer> cleaned = new HashMap<String, Integer>();
    int removedCount = 0;
    for (Map.Entry<String, Integer> e : wordFrequencies.entrySet())
    {
      String clean = normalizeWord(e.getKey());
      if (clean != null && !isBlocked(clean))
      {
        if (cleaned.containsKey(clean))
        {
          removedCount++;
          cleaned.put(clean, Math.min(500, cleaned.get(clean) + e.getValue()));
        }
        else
        {
          cleaned.put(clean, e.getValue());
        }
      }
      else
      {
        removedCount++;
      }
    }
    wordFrequencies.clear();
    wordFrequencies.putAll(cleaned);
    save();
    return removedCount;
  }

  public synchronized boolean isBlocked(String word) {
    if (word == null) return false;
    return blocklist.contains(word.toLowerCase(Locale.ROOT));
  }

  public synchronized void addToBlocklist(String word) {
    if (word == null || word.isEmpty()) return;
    String lower = word.toLowerCase(Locale.ROOT);
    blocklist.add(lower);
    // Also remove from learned words and bigrams
    wordFrequencies.remove(lower);
    bigrams.remove(lower);
    for (Map<String,Integer> m : bigrams.values()) m.remove(lower);
    save();
  }

  public synchronized boolean deleteWord(String word) {
    if (word == null) return false;
    String lower = word.toLowerCase(Locale.ROOT);
    boolean removed = wordFrequencies.remove(lower) != null;
    bigrams.remove(lower);
    for (Map<String,Integer> m : bigrams.values()) m.remove(lower);
    if (removed) save();
    return removed;
  }

  public synchronized java.util.List<Map.Entry<String,Integer>> getAllWordsSorted() {
    java.util.List<Map.Entry<String,Integer>> list = new ArrayList<>(wordFrequencies.entrySet());
    Collections.sort(list, new Comparator<Map.Entry<String,Integer>>() {
      @Override public int compare(Map.Entry<String,Integer> a, Map.Entry<String,Integer> b) { return Integer.compare(b.getValue(), a.getValue()); }
    });
    return list;
  }

  public synchronized void clearAllWords() {
    wordFrequencies.clear(); bigrams.clear(); save();
  }

  public synchronized boolean isLearnEnabled() {
    return prefs.getBoolean(PREF_LEARN_ENABLED_KEY, true);
  }

  public synchronized void setLearnEnabled(boolean enabled) {
    prefs.edit().putBoolean(PREF_LEARN_ENABLED_KEY, enabled).apply();
  }

  public synchronized java.util.Set<String> getBlocklist() { return new HashSet<>(blocklist); }

  public synchronized void removeFromBlocklist(String word) {
    if (word == null) return;
    blocklist.remove(word.toLowerCase(Locale.ROOT));
    save();
  }

  public synchronized void clearBlocklist() {
    blocklist.clear();
    save();
  }

  public synchronized void learnWord(String word)
  {
    if (!isLearnEnabled()) return;
    String clean = normalizeWord(word);
    if (clean == null || clean.length() < 2 || isBlocked(clean)) return;

    int current = wordFrequencies.containsKey(clean) ? wordFrequencies.get(clean) : 0;
    wordFrequencies.put(clean, Math.min(current + 1, 500));

    if (wordFrequencies.size() > MAX_USER_WORDS)
    {
      prune();
    }
    save();
  }

  public synchronized void learnBigram(String prevWord, String nextWord)
  {
    if (!isLearnEnabled()) return;
    String prev = normalizeWord(prevWord);
    String next = normalizeWord(nextWord);
    if (prev == null || next == null || isBlocked(prev) || isBlocked(next)) return;

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
    if (isBlocked(prev)) return results;
    Map<String, Integer> nextMap = bigrams.get(prev);
    if (nextMap != null)
    {
      for (Map.Entry<String, Integer> e : nextMap.entrySet())
      {
        if (isBlocked(e.getKey())) continue;
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
      if (isBlocked(e.getKey())) continue;
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
