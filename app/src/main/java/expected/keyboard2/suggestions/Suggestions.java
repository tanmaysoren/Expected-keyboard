package expected.keyboard2.suggestions;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import expected.keyboard2.Config;
import expected.keyboard2.Logs;
import expected.keyboard2.prediction.PredictionEngine;
import expected.keyboard2.prediction.WordCandidate;

/**
 * Keep track of the word being typed and provide suggestions for [CandidatesView]
 * using the modern FUTO offline PredictionEngine.
 */
public final class Suggestions
{
  Callback _callback;
  Config _config;
  Context _context;
  PredictionEngine _engine;
  boolean _enabled;

  /** Current suggestions. The best suggestion is at index [0]. */
  public String[] suggestions = new String[MAX_COUNT];
  /** Number of suggestions at the beginning of the [suggestions] array that are not [null]. */
  public int count = 0;
  public String emoji_suggestion = null;
  public String best_autocorrect = null;
  /** Number of suggestions in [suggestions]. */
  public static final int MAX_COUNT = 5;

  public Suggestions(Callback c, Config conf)
  {
    _callback = c;
    _config = conf;
  }

  public void setContext(Context ctx)
  {
    _context = ctx;
    if (_context != null)
    {
      _engine = PredictionEngine.getInstance(_context);
    }
  }

  public PredictionEngine getEngine()
  {
    if (_engine == null && _context != null)
    {
      _engine = PredictionEngine.getInstance(_context);
    }
    return _engine;
  }

  public void started()
  {
    _enabled = (_config != null && _config.editor_config != null && _config.editor_config.should_show_candidates_view);
    clear();
  }

  public void currently_typed_word(String word)
  {
    if (!_enabled)
      return;

    try
    {
      query_suggestions(word);
    }
    catch (Throwable t)
    {
      Logs.warn("Error querying suggestions", t);
      clear();
    }

    if (_callback != null)
      _callback.set_suggestions(this);
  }

  public void onWordCommitted(String word)
  {
    if (_engine != null && word != null)
    {
      _engine.onWordCommitted(word);
    }
  }

  public void resetContext()
  {
    if (_engine != null)
    {
      _engine.resetContext();
    }
  }

  void clear()
  {
    count = 0;
    for (int i = 0; i < MAX_COUNT; i++)
      suggestions[i] = null;
    emoji_suggestion = null;
    best_autocorrect = null;
  }

  int query_suggestions(String word)
  {
    clear();
    if (_engine == null && _context != null)
    {
      _engine = PredictionEngine.getInstance(_context);
    }
    if (_engine == null)
      return 0;

    String prevWord = _engine.getLastCommittedWord();
    List<WordCandidate> candidates = _engine.predict(word, prevWord, MAX_COUNT);

    if (candidates != null && !candidates.isEmpty())
    {
      boolean firstUpper = (word != null && !word.isEmpty() && Character.isUpperCase(word.charAt(0)));
      int added = 0;
      for (WordCandidate wc : candidates)
      {
        if (added >= MAX_COUNT)
          break;
        String w = wc.word;
        if (firstUpper && w.length() > 0)
        {
          w = w.substring(0, 1).toUpperCase(Locale.ROOT) + (w.length() > 1 ? w.substring(1) : "");
        }
        suggestions[added++] = w;
        if (wc.type == WordCandidate.TYPE_AUTOCORRECT && best_autocorrect == null
            && (_config == null || _config.autocorrect_enabled))
        {
          best_autocorrect = w;
        }
      }
      count = added;
    }

    if (word != null && word.length() >= 2)
    {
      emoji_suggestion = _engine.predictEmoji(word);
    }

    return count;
  }

  public static interface Callback
  {
    public void set_suggestions(Suggestions suggestions);
  }
}
