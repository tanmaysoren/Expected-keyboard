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
  /** Terminal mode flag and full list for scrollable bar */
  public boolean is_terminal = false;
  public java.util.List<String> terminal_list = new java.util.ArrayList<>();
  /** Email mode flag and list */
  public boolean is_email = false;
  public java.util.List<String> email_list = new java.util.ArrayList<>();

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
    // Fix: respect persistent bar and suggestion texts toggle
    // Bar persistence ensures suggestions are re-enabled after terminal apps
    boolean textsEnabled = (_config != null && _config.suggestions_enabled);
    boolean editorAllows = (_config != null && _config.editor_config != null && _config.editor_config.should_show_candidates_view);
    boolean barPersistent = (_config != null && _config.show_suggestion_bar);
    // If bar is persistent, keep suggestions enabled even after terminal transitions
    _enabled = textsEnabled && (editorAllows || barPersistent);
    // Also ensure engine context is refreshed after app switch (fixes post-terminal empty)
    if (_engine == null && _context != null) {
      _engine = PredictionEngine.getInstance(_context);
    }
    if (_engine != null) {
      // Do not reset context completely, but ensure engine is ready
    }
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
    is_terminal = false;
    terminal_list.clear();
    is_email = false;
    email_list.clear();
  }

  private int query_terminal_suggestions(String word) {
    is_terminal = true;
    terminal_list.clear();
    if (_config == null || _config.terminal_commands == null || _config.terminal_commands.isEmpty()) {
      count = 0;
      return 0;
    }
    String lower = (word == null) ? "" : word.toLowerCase(Locale.ROOT).trim();
    java.util.List<String> cmds = new java.util.ArrayList<>(_config.terminal_commands);
    java.util.Collections.sort(cmds, String.CASE_INSENSITIVE_ORDER);
    java.util.List<String> matched = new java.util.ArrayList<>();
    for (String cmd : cmds) {
      if (cmd == null || cmd.isEmpty()) continue;
      if (lower.isEmpty() || cmd.toLowerCase(Locale.ROOT).startsWith(lower) || cmd.toLowerCase(Locale.ROOT).contains(lower)) {
        matched.add(cmd);
      }
    }
    // If no prefix match, show all (up to limit) so user can scroll
    if (matched.isEmpty() && lower.isEmpty()) {
      matched.addAll(cmds);
    } else if (matched.isEmpty() && !lower.isEmpty()) {
      // Show no matches -> fallback to show all for scrolling discovery
      matched.addAll(cmds);
      if (matched.size() > 20) matched = matched.subList(0, 20);
    }
    // For scrollable bar, keep up to 20 for full list
    terminal_list.addAll(matched.size() > 20 ? matched.subList(0, 20) : matched);
    // Also fill legacy array with first 5 for backward compat
    int n = Math.min(terminal_list.size(), MAX_COUNT);
    for (int i = 0; i < n; i++) suggestions[i] = terminal_list.get(i);
    count = n;
    // No emoji for terminal
    emoji_suggestion = null;
    return count;
  }

  private int query_email_suggestions(String word) {
    is_email = true;
    email_list.clear();
    if (_config == null || _config.custom_emails == null || _config.custom_emails.isEmpty()) {
      count = 0;
      return 0;
    }
    String lower = (word == null) ? "" : word.toLowerCase(Locale.ROOT).trim();
    java.util.List<String> emails = new java.util.ArrayList<>(_config.custom_emails);
    java.util.Collections.sort(emails, String.CASE_INSENSITIVE_ORDER);
    java.util.List<String> matched = new java.util.ArrayList<>();
    for (String e : emails) {
      if (e == null || e.isEmpty()) continue;
      String el = e.toLowerCase(Locale.ROOT);
      if (lower.isEmpty()) {
        matched.add(e);
      } else if (el.startsWith(lower)) {
        matched.add(e);
      } else if (lower.length() >= 3 && el.contains(lower)) {
        matched.add(e);
      }
    }
    // Show all if no prefix match but in email field, show all for scroll
    if (matched.isEmpty() && isEmailMode()) {
      matched.addAll(emails);
      if (matched.size() > 20) matched = matched.subList(0, 20);
    }
    email_list.addAll(matched.size() > 20 ? matched.subList(0, 20) : matched);
    int n = Math.min(email_list.size(), MAX_COUNT);
    for (int i = 0; i < n; i++) suggestions[i] = email_list.get(i);
    count = n;
    emoji_suggestion = null;
    return count;
  }

  // Check if we are in a terminal app (Termux etc.) - selection_mode false
  private boolean isTerminalMode() {
    return _config != null && _config.editor_config != null && !_config.editor_config.selection_mode_enabled;
  }

  private boolean isEmailMode() {
    return _config != null && _config.editor_config != null && (_config.editor_config.is_email_field || _config.editor_config.is_phone_field);
  }

  int query_suggestions(String word)
  {
    clear();
    // Email/phone login fields: show custom emails (only there, plus 3-letter trigger elsewhere)
    if (isEmailMode()) {
      return query_email_suggestions(word);
    }
    // For non-email fields: if typed 3+ chars matches an email prefix, also show emails
    if (word != null && word.length() >= 3 && _config != null && _config.custom_emails != null && !_config.custom_emails.isEmpty()) {
      String lower = word.toLowerCase(Locale.ROOT);
      boolean matchesEmail = false;
      for (String e : _config.custom_emails) {
        if (e.toLowerCase(Locale.ROOT).startsWith(lower)) { matchesEmail = true; break; }
      }
      if (matchesEmail) {
        return query_email_suggestions(word);
      }
    }
    // Terminal mode: show custom commands instead of dictionary predictions
    if (isTerminalMode()) {
      return query_terminal_suggestions(word);
    }
    // If suggestion texts are disabled, show empty (bar stays visible via persistent flag)
    if (_config != null && !_config.suggestions_enabled) {
      return 0;
    }
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
