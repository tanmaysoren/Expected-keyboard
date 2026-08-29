package expected.keyboard2.prediction;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import expected.keyboard2.Logs;

/**
 * Modern offline Word Prediction & Autocorrection Engine based on FUTO keyboard.
 * Provides fast prefix matching, N-gram next-word prediction, keyboard-distance typo correction,
 * dynamic user dictionary learning, and emoji prediction.
 */
public class PredictionEngine
{
  private static PredictionEngine sInstance = null;

  public static synchronized PredictionEngine getInstance(Context context)
  {
    if (sInstance == null && context != null)
    {
      sInstance = new PredictionEngine(context.getApplicationContext());
    }
    return sInstance;
  }

  private final Context context;
  private final TrieDictionary trie = new TrieDictionary();
  private final UserDictionary userDict;
  private final Set<String> revertedExceptions = Collections.synchronizedSet(new HashSet<String>());
  private String lastCommittedWord = null;

  public PredictionEngine(Context context)
  {
    this.context = context;
    this.userDict = new UserDictionary(context);
    initBuiltinLexicon();
  }

  public void addRevertedException(String word)
  {
    if (word != null && !word.trim().isEmpty())
    {
      revertedExceptions.add(word.trim().toLowerCase(Locale.ROOT));
    }
  }

  public boolean isRevertedException(String word)
  {
    if (word == null || word.trim().isEmpty()) return false;
    return revertedExceptions.contains(word.trim().toLowerCase(Locale.ROOT));
  }

  public void clearRevertedException(String word)
  {
    if (word != null)
    {
      revertedExceptions.remove(word.trim().toLowerCase(Locale.ROOT));
    }
  }

  private void initBuiltinLexicon()
  {
    for (Map.Entry<String, Integer> entry : BuiltinLexicon.FREQUENT_WORDS.entrySet())
    {
      trie.insert(entry.getKey(), entry.getValue());
    }
  }

  public UserDictionary getUserDictionary()
  {
    return userDict;
  }

  /**
   * Load custom words/dictionary from raw text stream (word per line or word:frequency).
   */
  public void loadWordList(InputStream inputStream)
  {
    if (inputStream == null) return;
    try
    {
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null)
      {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) continue;
        int colon = line.indexOf(':');
        if (colon > 0)
        {
          String word = line.substring(0, colon).trim();
          try
          {
            int freq = Integer.parseInt(line.substring(colon + 1).trim());
            trie.insert(word, freq);
          }
          catch (NumberFormatException e)
          {
            trie.insert(word, 5);
          }
        }
        else
        {
          trie.insert(line, 5);
        }
      }
      reader.close();
    }
    catch (Throwable t)
    {
      Logs.warn("Error loading wordlist into PredictionEngine", t);
    }
  }

  /**
   * Predict word suggestions for the currently typed word.
   * If word is empty, provides next-word predictions based on the previous word context.
   */
  public List<WordCandidate> predict(String currentWord, String prevWord, int maxCount)
  {
    Map<String, WordCandidate> candidates = new LinkedHashMap<String, WordCandidate>();
    if (maxCount <= 0) maxCount = 5;

    // 1. Next-word prediction if no word typed or just starting
    if (currentWord == null || currentWord.isEmpty())
    {
      if (prevWord != null && !prevWord.isEmpty())
      {
        String prevLower = prevWord.toLowerCase(Locale.ROOT);
        // Check user bigrams first (highest priority)
        List<WordCandidate> userNext = userDict.getNextWordPredictions(prevLower, maxCount * 2);
        for (WordCandidate wc : userNext)
        {
          addCandidate(candidates, wc.word, wc.score + 150, WordCandidate.TYPE_NEXT_WORD, 0.0);
        }

        // Check builtin bigrams (second priority)
        List<String> builtinNext = BuiltinLexicon.COMMON_BIGRAMS.get(prevLower);
        if (builtinNext != null)
        {
          int score = 120;
          for (String next : builtinNext)
          {
            addCandidate(candidates, next, score, WordCandidate.TYPE_NEXT_WORD, 0.0);
            score -= 3;
          }
        }

        // Also suggest common words that frequently follow any word
        addCommonFollowers(candidates, maxCount);
      }
      else
      {
        // No previous context, suggest most common starting words
        addCommonStarters(candidates, maxCount);
      }
      return sortAndTrim(candidates, currentWord, maxCount);
    }

    String lowerWord = currentWord.toLowerCase(Locale.ROOT);
    boolean isReverted = isRevertedException(lowerWord);

    // 2. Direct exact known typo mapping (e.g. "teh" -> "the", "woukd" -> "would")
    if (!isReverted && BuiltinLexicon.COMMON_TYPOS.containsKey(lowerWord))
    {
      String fix = BuiltinLexicon.COMMON_TYPOS.get(lowerWord);
      addCandidate(candidates, fix, 200, WordCandidate.TYPE_AUTOCORRECT, 0.1);
    }

    // 3. User learned words prefix matching (highest priority after typos)
    List<WordCandidate> userMatches = userDict.getPrefixMatches(lowerWord, maxCount * 2);
    for (WordCandidate uc : userMatches)
    {
      addCandidate(candidates, uc.word, uc.score + 100, WordCandidate.TYPE_USER_LEARNED, 0.0);
    }

    // 4. Exact dictionary match or verbatim word
    if (trie.contains(lowerWord) || isReverted)
    {
      int freq = trie.getFrequency(lowerWord);
      addCandidate(candidates, lowerWord, freq + 200, WordCandidate.TYPE_EXACT, 0.0);
    }

    // 5. Trie prefix matches (aggressive completion)
    List<WordCandidate> prefixMatches = trie.getPrefixMatches(lowerWord, maxCount * 3);
    for (WordCandidate pc : prefixMatches)
    {
      int bonus = 0;
      if (pc.word.length() <= lowerWord.length() + 3) bonus = 30;
      addCandidate(candidates, pc.word, pc.score + bonus, WordCandidate.TYPE_PREFIX, 0.0);
    }

    // 6. Typo / spatial distance autocorrection (more aggressive)
    if (!isReverted && lowerWord.length() >= 2 && candidates.size() < maxCount * 3)
    {
      findFuzzyMatches(lowerWord, candidates, maxCount);
    }

    // 7. Add common words if we have few candidates
    if (candidates.size() < maxCount && lowerWord.length() >= 2)
    {
      addCommonWordMatches(lowerWord, candidates, maxCount);
    }

    return sortAndTrim(candidates, currentWord, maxCount);
  }

  private void addCommonFollowers(Map<String, WordCandidate> candidates, int maxCount)
  {
    String[] commonFollowers = {
      "the", "a", "I", "you", "it", "is", "are", "was", "have", "has",
      "will", "would", "could", "should", "can", "do", "does", "did",
      "and", "but", "or", "so", "because", "that", "this", "with",
      "for", "to", "in", "on", "at", "by", "from", "of"
    };
    int score = 40;
    for (String word : commonFollowers)
    {
      if (!candidates.containsKey(word.toLowerCase(Locale.ROOT)))
      {
        addCandidate(candidates, word, score, WordCandidate.TYPE_NEXT_WORD, 0.0);
        score -= 1;
        if (candidates.size() >= maxCount * 2) break;
      }
    }
  }

  private void addCommonStarters(Map<String, WordCandidate> candidates, int maxCount)
  {
    String[] starters = {
      "I", "The", "A", "Hello", "Hey", "Hi", "Thank", "Please",
      "So", "Well", "Oh", "Yeah", "No", "Yes", "OK", "Sure"
    };
    int score = 60;
    for (String word : starters)
    {
      addCandidate(candidates, word, score, WordCandidate.TYPE_NEXT_WORD, 0.0);
      score -= 3;
      if (candidates.size() >= maxCount) break;
    }
  }

  private void addCommonWordMatches(String prefix, Map<String, WordCandidate> candidates, int maxCount)
  {
    String[] commonWords = {
      "the", "that", "this", "there", "their", "they", "them", "then",
      "than", "what", "when", "where", "which", "while", "who", "whom",
      "why", "how", "have", "has", "had", "having", "being", "been",
      "are", "was", "were", "will", "would", "could", "should", "can",
      "may", "might", "must", "shall", "need", "dare", "ought", "used",
      "do", "does", "did", "doing", "done", "make", "made", "making",
      "get", "got", "getting", "go", "going", "went", "gone", "come",
      "came", "coming", "take", "took", "taken", "taking", "give",
      "gave", "given", "giving", "say", "said", "saying", "tell",
      "told", "telling", "ask", "asked", "asking", "try", "tried",
      "trying", "use", "used", "using", "work", "worked", "working",
      "think", "thought", "thinking", "know", "known", "knowing",
      "see", "seen", "seeing", "want", "wanted", "wanting",
      "like", "liked", "liking", "look", "looked", "looking",
      "find", "found", "finding", "feel", "felt", "feeling",
      "leave", "left", "leaving", "put", "putting", "keep",
      "kept", "keeping", "let", "letting", "start", "started",
      "starting", "seem", "seemed", "seeming", "help", "helped",
      "helping", "show", "showed", "showing", "hear", "heard",
      "hearing", "play", "played", "playing", "run", "ran", "running",
      "move", "moved", "moving", "live", "lived", "living",
      "believe", "believed", "believing", "bring", "brought",
      "bringing", "happen", "happened", "happening", "write",
      "wrote", "written", "writing", "sit", "sat", "sitting",
      "stand", "stood", "standing", "lose", "lost", "losing",
      "pay", "paid", "paying", "meet", "met", "meeting",
      "include", "included", "including", "continue", "continued",
      "continuing", "set", "setting", "learn", "learned", "learning",
      "change", "changed", "changing", "lead", "led", "leading",
      "understand", "understood", "understanding", "watch",
      "watched", "watching", "follow", "followed", "following",
      "stop", "stopped", "stopping", "create", "created", "creating",
      "speak", "spoke", "spoken", "speaking", "read", "reading",
      "allow", "allowed", "allowing", "add", "added", "adding",
      "spend", "spent", "spending", "grow", "grew", "grown",
      "growing", "open", "opened", "opening", "walk", "walked",
      "walking", "win", "won", "winning", "offer", "offered",
      "offering", "remember", "remembered", "remembering",
      "love", "loved", "loving", "consider", "considered",
      "considering", "appear", "appeared", "appearing", "buy",
      "bought", "buying", "wait", "waited", "waiting",
      "serve", "served", "serving", "die", "died", "dying",
      "send", "sent", "sending", "expect", "expected", "expecting",
      "build", "built", "building", "stay", "stayed", "staying",
      "fall", "fell", "fallen", "falling", "cut", "cutting",
      "reach", "reached", "reaching", "kill", "killed", "killing",
      "remain", "remained", "remaining", "suggest", "suggested",
      "suggesting", "raise", "raised", "raising", "pass", "passed",
      "passing", "sell", "sold", "selling", "require", "required",
      "requiring", "report", "reported", "reporting", "decide",
      "decided", "deciding", "pull", "pulled", "pulling",
      "thank", "thanked", "thanking", "hello", "world",
      "today", "tomorrow", "yesterday", "morning", "night",
      "friend", "family", "happy", "birthday", "ready",
      "always", "awesome", "beautiful", "better", "best",
      "busy", "clean", "cool", "different", "difficult",
      "early", "easy", "enough", "everything", "everyone",
      "excited", "famous", "fast", "fine", "free", "funny",
      "glad", "important", "interesting", "kind", "late",
      "little", "lucky", "maybe", "nice", "normal", "perfect",
      "possible", "pretty", "quick", "quiet", "real", "safe",
      "simple", "sorry", "special", "strong", "sure", "sweet",
      "terrible", "together", "true", "useful", "wonderful",
      "welcome", "already", "anyway", "around", "before",
      "behind", "between", "during", "inside", "outside",
      "without", "within", "under", "through", "towards",
      "across", "against", "along", "among", "beyond"
    };

    for (String word : commonWords)
    {
      if (word.toLowerCase(Locale.ROOT).startsWith(prefix) && !candidates.containsKey(word.toLowerCase(Locale.ROOT)))
      {
        int score = 20;
        addCandidate(candidates, word, score, WordCandidate.TYPE_PREFIX, 0.0);
        if (candidates.size() >= maxCount * 2) break;
      }
    }
  }

  private void findFuzzyMatches(String word, Map<String, WordCandidate> candidates, int maxCount)
  {
    int wordLen = word.length();
    for (Map.Entry<String, Integer> entry : BuiltinLexicon.FREQUENT_WORDS.entrySet())
    {
      String candidate = entry.getKey();
      if (Math.abs(candidate.length() - wordLen) > 2)
        continue;
      double dist = SpatialDistance.weightedEditDistance(word, candidate);
      if (dist <= 1.5)
      {
        int score = (int) (entry.getValue() * 2 + (1.7 - dist) * 50);
        addCandidate(candidates, candidate, score, WordCandidate.TYPE_AUTOCORRECT, dist);
      }
    }
  }

  private void addCandidate(Map<String, WordCandidate> map, String word, int score, int type, double dist)
  {
    if (word == null || word.isEmpty()) return;
    String key = word.toLowerCase(Locale.ROOT);
    WordCandidate existing = map.get(key);
    if (existing == null)
    {
      map.put(key, new WordCandidate(word, score, type, dist));
    }
    else
    {
      if (score > existing.score)
      {
        existing.word = word;
        existing.score = score;
        existing.type = type;
        existing.distance = dist;
      }
    }
  }

  private List<WordCandidate> sortAndTrim(Map<String, WordCandidate> map, final String typedWord, int maxCount)
  {
    List<WordCandidate> list = new ArrayList<WordCandidate>(map.values());
    Collections.sort(list, new Comparator<WordCandidate>()
    {
      @Override
      public int compare(WordCandidate a, WordCandidate b)
      {
        // Prioritize exact matches
        if (a.type == WordCandidate.TYPE_EXACT && b.type != WordCandidate.TYPE_EXACT) return -1;
        if (b.type == WordCandidate.TYPE_EXACT && a.type != WordCandidate.TYPE_EXACT) return 1;

        // Then user learned words
        if (a.type == WordCandidate.TYPE_USER_LEARNED && b.type != WordCandidate.TYPE_USER_LEARNED) return -1;
        if (b.type == WordCandidate.TYPE_USER_LEARNED && a.type != WordCandidate.TYPE_USER_LEARNED) return 1;

        // Then by score
        if (a.score != b.score)
        {
          return Integer.compare(b.score, a.score);
        }

        // Prefer words closer in length to typed word
        if (typedWord != null && !typedWord.isEmpty())
        {
          int lenDiffA = Math.abs(a.word.length() - typedWord.length());
          int lenDiffB = Math.abs(b.word.length() - typedWord.length());
          if (lenDiffA != lenDiffB)
          {
            return Integer.compare(lenDiffA, lenDiffB);
          }
        }

        // Finally alphabetically
        return a.word.compareToIgnoreCase(b.word);
      }
    });

    if (list.size() > maxCount)
    {
      return new ArrayList<WordCandidate>(list.subList(0, maxCount));
    }
    return list;
  }

  /**
   * Called when a word is completed or accepted by the user.
   */
  public void onWordCommitted(String word)
  {
    if (word == null || word.length() < 2) return;
    userDict.learnWord(word);
    if (lastCommittedWord != null && !lastCommittedWord.isEmpty())
    {
      userDict.learnBigram(lastCommittedWord, word);
    }
    lastCommittedWord = word;
  }

  public void resetContext()
  {
    lastCommittedWord = null;
  }

  public String getLastCommittedWord()
  {
    return lastCommittedWord;
  }

  public String predictEmoji(String word)
  {
    return EmojiPredictor.getEmoji(word);
  }
}
