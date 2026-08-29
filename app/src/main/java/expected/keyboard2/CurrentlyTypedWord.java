package expected.keyboard2;

import android.os.Build.VERSION;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;
import java.util.List;

/** Keep track of the word being typed. This also tracks whether the selection
    is empty. */
public final class CurrentlyTypedWord
{
  InputConnection _ic = null;
  Handler _handler;
  Callback _callback;

  /** The currently typed word. */
  StringBuilder _w = new StringBuilder();
  /** This can be disabled if the editor doesn't support looking at the text
      before the cursor. */
  boolean _enabled = false;
  /** The current word is empty while the selection is ongoing. */
  boolean _has_selection = false;
  /** Used to avoid concurrent refreshes in [delayed_refresh()]. */
  boolean _refresh_pending = false;

  /** The estimated cursor position in code points. Used to avoid expensive IPC
      calls when the typed word can be estimated locally with [typed]. When the
      cursor position gets out of sync, the text before the cursor is queried
      again to the editor. */
  int _cursor;
  /** The cursor position within the current word relative to the end of the
      word in chars. Equal to [0] when the cursor is at the end of the word. */
  int _w_cursor;

  public CurrentlyTypedWord(Handler h, Callback cb)
  {
    _handler = h;
    _callback = cb;
  }

  public String get()
  {
    return _w.toString();
  }

  public boolean is_selection_not_empty()
  {
    return _has_selection;
  }

  /** The cursor position relative to the end of the word. */
  public int cursor_relative()
  {
    return _w_cursor;
  }

  public void started(Config conf, InputConnection ic)
  {
    try
    {
      _ic = ic;
      _enabled = true;
      _refresh_pending = false;
      EditorConfig e = conf.editor_config;
      _has_selection = e.initial_sel_start != e.initial_sel_end;
      _cursor = e.initial_sel_start;
      _w_cursor = 0;
      if (!_has_selection)
      {
        set_current_word(e.initial_text_before_cursor);
        _w_cursor = (e.initial_text_after_cursor == null) ? 0 :
          -append_chars(e.initial_text_after_cursor); 
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error starting CurrentlyTypedWord", t);
    }
  }

  public void typed(String s)
  {
    if (!_enabled || s == null)
      return;
    try
    {
      _has_selection = false;
      type_chars(s);
      callback();
    }
    catch (Throwable t)
    {
      Logs.warn("Error in CurrentlyTypedWord.typed", t);
    }
  }

  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    // Avoid the expensive [refresh_current_word] call when [typed] was called
    // before.
    if (!_enabled)
      return;
    try
    {
      boolean new_has_sel = newSelStart != newSelEnd;
      if (new_has_sel || _has_selection) // Selection was on or is now on.
      {
        _cursor = newSelStart;
        _has_selection = new_has_sel;
        refresh_current_word();
      }
      else if (newSelStart != _cursor)
      {
        _cursor = newSelStart;
        _w_cursor += newSelStart - oldSelStart;
        if (_w_cursor < -_w.length() || _w_cursor > 0)
          refresh_current_word();
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error in CurrentlyTypedWord.selection_updated", t);
    }
  }

  public void event_sent(int code, int meta)
  {
    if (!_enabled)
      return;
    try
    {
      switch (code)
      {
        case KeyEvent.KEYCODE_DEL:
          if (meta == 0)
            remove_surrounding_text(1, 0);
          else
            delayed_refresh();
          break;
        default:
          delayed_refresh();
          break;
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error in CurrentlyTypedWord.event_sent", t);
    }
  }

  public void remove_surrounding_text(int remove_before, int remove_after)
  {
    if (!_enabled)
      return;
    try
    {
      int len = _w.length();
      int c = len + _w_cursor;
      int del_start = Math.max(0, Math.min(c - remove_before, len));
      int del_end = Math.max(del_start, Math.min(c + remove_after, len));
      if (del_start <= del_end && del_end <= len)
        _w.delete(del_start, del_end);
      _cursor = Math.max(0, _cursor - remove_before);
      _w_cursor -= Math.min(remove_after, 0);
      callback();
    }
    catch (Throwable t)
    {
      Logs.warn("Error in CurrentlyTypedWord.remove_surrounding_text", t);
    }
  }

  void callback()
  {
    try
    {
      String w = _w.toString();
      if (_callback != null)
        _callback.currently_typed_word(w);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in CurrentlyTypedWord.callback", t);
    }
  }

  /** Estimate the currently typed word after [chars] has been typed. */
  void type_chars(CharSequence s, int start, int end)
  {
    if (s == null)
      return;
    int s_len = s.length();
    start = Math.max(0, Math.min(start, s_len));
    end = Math.max(start, Math.min(end, s_len));
    if (start >= end)
      return;

    int insert_start = start;
    // Iterate over code points as that's the unit of [_cursor].
    for (int i = start; i < end;)
    {
      int c = Character.codePointAt(s, i);
      i += Character.charCount(c);
      _cursor++;
      // [i >= end] might happen when the cursor is in the middle of a
      // surrogate pair
      if (!is_word_char(c) && i <= end)
        insert_start = i;
    }
    if (insert_start > start)
      _w.setLength(0);
    int insert_offset = Math.max(0, Math.min(_w.length(), _w.length() + _w_cursor));
    int safe_start = Math.max(0, Math.min(insert_start, s_len));
    int safe_end = Math.max(safe_start, Math.min(end, s_len));
    if (safe_start < safe_end)
      _w.insert(insert_offset, s, safe_start, safe_end);
  }

  void type_chars(CharSequence s)
  {
    if (s != null)
      type_chars(s, 0, s.length());
  }

  /** Append chars to the current word without moving the cursor. Return the
      number of characters that were added in the current word. */
  int append_chars(CharSequence s, int start, int end)
  {
    if (s == null)
      return 0;
    int s_len = s.length();
    start = Math.max(0, Math.min(start, s_len));
    end = Math.max(start, Math.min(end, s_len));
    int i = start;
    while (i < end)
    {
      int c = Character.codePointAt(s, i);
      if (!is_word_char(c))
        break;
      _w.appendCodePoint(c);
      i += Character.charCount(c);
    }
    return i - start;
  }

  int append_chars(CharSequence s)
  {
    if (s == null)
      return 0;
    return append_chars(s, 0, s.length());
  }

  /** Refresh the current word by immediately querying the editor. */
  void refresh_current_word()
  {
    _refresh_pending = false;
    _w_cursor = 0;
    if (_ic == null)
      return;
    try
    {
      if (_has_selection)
        set_current_word("");
      else if (VERSION.SDK_INT >= 31)
      {
        SurroundingText st = _ic.getSurroundingText(20, 20, 0);
        if (st != null)
          set_current_word(st);
        else
          set_current_word(_ic.getTextBeforeCursor(20, 0));
      }
      else
        set_current_word(_ic.getTextBeforeCursor(20, 0));
    }
    catch (Throwable t)
    {
      Logs.warn("Error in refresh_current_word", t);
    }
  }

  /** Refresh the current word by immediately querying the editor. */
  void set_current_word(CharSequence text_before_cursor)
  {
    if (text_before_cursor == null)
      return;
    _w.setLength(0);
    int saved_cursor = _cursor;
    type_chars(text_before_cursor.toString());
    _cursor = saved_cursor;
    callback();
  }

  /** Like above but take the text after the cursor into account. */
  void set_current_word(SurroundingText st)
  {
    if (st == null)
      return;
    CharSequence st_text = st.getText();
    if (st_text == null)
      return;
    _w.setLength(0);
    int saved_cursor = _cursor;
    int st_sel = st.getSelectionStart();
    st_sel = Math.max(0, Math.min(st_sel, st_text.length()));
    type_chars(st_text, 0, st_sel);
    _w_cursor = -append_chars(st_text, st_sel, st_text.length());
    _cursor = saved_cursor;
    callback();
  }

  /** Wait some time to let the editor finishes reacting to changes and call
      [refresh_current_word]. */
  void delayed_refresh()
  {
    _refresh_pending = true;
    _handler.removeCallbacks(delayed_refresh_run);
    _handler.postDelayed(delayed_refresh_run, 50);
  }

  Runnable delayed_refresh_run = new Runnable()
  {
    public void run()
    {
      if (_refresh_pending)
      {
        try
        {
          refresh_current_word();
        }
        catch (Throwable t)
        {
          Logs.warn("Error running delayed_refresh", t);
        }
      }
    }
  };

  /** A word is the longest consecutive sequence for which [is_word_char]
      returns [true]. */
  public static boolean is_word_char(int c)
  {
    return Character.isLetterOrDigit(c) || (c == '\'');
  }

  public static interface Callback
  {
    public void currently_typed_word(String word);
  }
}
