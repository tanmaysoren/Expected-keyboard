package expected.keyboard2;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import java.util.Iterator;
import expected.keyboard2.suggestions.Suggestions;

public final class KeyEventHandler
  implements Config.IKeyEventHandler,
             ClipboardHistoryService.ClipboardPasteCallback,
             CurrentlyTypedWord.Callback
{
  IReceiver _recv;
  Autocapitalisation _autocap;
  Suggestions _suggestions;
  CurrentlyTypedWord _typedword;
  /** State of the system modifiers. It is updated whether a modifier is down
      or up and a corresponding key event is sent. */
  Pointers.Modifiers _mods;
  /** Consistent with [_mods]. This is a mutable state rather than computed
      from [_mods] to ensure that the meta state is correct while up and down
      events are sent for the modifier keys. */
  int _meta_state = 0;
  /** Whether to force sending arrow keys to move the cursor when
      [setSelection] could be used instead. */
  boolean _move_cursor_force_fallback = false;
  /** Whether the space bar automatically enters the best suggestion. */
  boolean _space_bar_auto_complete = false;
  /** Remember the action that was handled. This is used by autocorrect. */
  LastAction _last_action = null;
  LastAction _next_last_action = null;

  public KeyEventHandler(IReceiver recv, Suggestions sg)
  {
    _recv = recv;
    Handler handler = recv.getHandler();
    _autocap = new Autocapitalisation(handler,
        this.new Autocapitalisation_callback());
    _mods = Pointers.Modifiers.EMPTY;
    _suggestions = sg;
    _typedword = new CurrentlyTypedWord(handler, this);
  }

  /** Editing just started. */
  public void started(Config conf)
  {
    InputConnection ic = _recv.getCurrentInputConnection();
    _autocap.started(conf, ic);
    _typedword.started(conf, ic);
    _suggestions.started();
    _move_cursor_force_fallback =
      conf.editor_config.should_move_cursor_force_fallback;
    _space_bar_auto_complete = conf.space_bar_auto_complete;
    _autocorrect_enabled = conf.autocorrect_enabled;
    _last_action = null;
  }

  /** Selection has been updated. */
  public void selection_updated(int oldSelStart, int newSelStart, int newSelEnd)
  {
    try
    {
      _autocap.selection_updated(oldSelStart, newSelStart);
      _typedword.selection_updated(oldSelStart, newSelStart, newSelEnd);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in selection_updated", t);
    }
  }

  /** A key is being pressed. There will not necessarily be a corresponding
      [key_up] event. */
  @Override
  public void key_down(KeyValue key, boolean isSwipe)
  {
    if (key == null)
      return;
    try
    {
      // Stop auto capitalisation when pressing some keys
      switch (key.getKind())
      {
        case Modifier:
          switch (key.getModifier())
          {
            case CTRL:
            case ALT:
            case META:
              _autocap.stop();
              break;
          }
          break;
        case Compose_pending:
          _autocap.stop();
          break;
        case Slider:
          // Don't wait for the next key_up and move the cursor right away. This
          // is called after the trigger distance have been travelled.
          handle_slider(key.getSlider(), key.getSliderRepeat(), true);
          break;
        default: break;
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error in key_down", t);
    }
  }

  /** A key has been released. */
  @Override
  public void key_up(KeyValue key, Pointers.Modifiers mods)
  {
    if (key == null)
      return;
    try
    {
      _next_last_action = LastAction.OTHER;
      Pointers.Modifiers old_mods = _mods;
      update_meta_state(mods);
      switch (key.getKind())
      {
        case Char: send_text(String.valueOf(key.getChar())); break;
        case String: send_text(key.getString()); break;
        case Event: _recv.handle_event_key(key.getEvent()); break;
        case Keyevent:
          if (key.getKeyevent() == android.view.KeyEvent.KEYCODE_TAB && _meta_state == 0)
            send_text("\t");
          else
            send_key_down_up(key.getKeyevent());
          break;
        case Modifier: break;
        case Editing: handle_editing_key(key.getEditing()); break;
        case Compose_pending:
          break;
        case Slider: handle_slider(key.getSlider(), key.getSliderRepeat(), false); break;
        case Macro: evaluate_macro(key.getMacro()); break;
        case Stateful: handle_stateful(key.getStateful()); break;
      }
      update_meta_state(old_mods);
      _last_action = _next_last_action;
    }
    catch (Throwable t)
    {
      Logs.warn("Error in key_up", t);
    }
  }

  @Override
  public void mods_changed(Pointers.Modifiers mods)
  {
    try
    {
      update_meta_state(mods);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in mods_changed", t);
    }
  }

  @Override
  public void suggestion_entered(String text)
  {
    if (text == null)
      return;
    try
    {
      String old = _typedword.get();
      int cur_rel = _typedword.cursor_relative();
      replace_surrounding_text(old.length() + cur_rel, -cur_rel, text);
      last_replaced_word = old;
      last_replacement_word_len = text.length();
      _next_last_action = LastAction.SUGGESTION_ENTERED;
      if (_suggestions != null)
        _suggestions.onWordCommitted(text.trim());
    }
    catch (Throwable t)
    {
      Logs.warn("Error entering suggestion", t);
    }
  }

  @Override
  public void paste_from_clipboard_pane(String content)
  {
    if (content != null)
      send_text(content);
  }

  @Override
  public void currently_typed_word(String word)
  {
    if (_suggestions != null)
      _suggestions.currently_typed_word(word);
  }

  public void dictionary_changed()
  {
    try
    {
      // Refresh the suggestions immediately after dictionary changed.
      if (_suggestions != null && _typedword != null)
        _suggestions.currently_typed_word(_typedword.get());
    }
    catch (Throwable t)
    {
      Logs.warn("Error in dictionary_changed", t);
    }
  }

  /** Update [_mods] to be consistent with the [mods], sending key events if
      needed. */
  void update_meta_state(Pointers.Modifiers mods)
  {
    if (mods == null)
      return;
    // Released modifiers
    Iterator<KeyValue> it = _mods.diff(mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), false);
    // Activated modifiers
    it = mods.diff(_mods);
    while (it.hasNext())
      sendMetaKeyForModifier(it.next(), true);
    _mods = mods;
  }

  void sendMetaKey(int eventCode, int meta_flags, boolean down)
  {
    if (down)
    {
      _meta_state = _meta_state | meta_flags;
      send_keyevent(KeyEvent.ACTION_DOWN, eventCode, _meta_state);
    }
    else
    {
      send_keyevent(KeyEvent.ACTION_UP, eventCode, _meta_state);
      _meta_state = _meta_state & ~meta_flags;
    }
  }

  void sendMetaKeyForModifier(KeyValue kv, boolean down)
  {
    if (kv == null)
      return;
    switch (kv.getKind())
    {
      case Modifier:
        switch (kv.getModifier())
        {
          case CTRL:
            sendMetaKey(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.META_CTRL_LEFT_ON | KeyEvent.META_CTRL_ON, down);
            break;
          case ALT:
            sendMetaKey(KeyEvent.KEYCODE_ALT_LEFT, KeyEvent.META_ALT_LEFT_ON | KeyEvent.META_ALT_ON, down);
            break;
          case SHIFT:
            sendMetaKey(KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_ON, down);
            break;
          case META:
            sendMetaKey(KeyEvent.KEYCODE_META_LEFT, KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_ON, down);
            break;
          default:
            break;
        }
        break;
    }
  }

  void send_key_down_up(int keyCode)
  {
    send_key_down_up(keyCode, _meta_state);
  }

  public void send_key_down_up(int keyCode, int metaState)
  {
    send_keyevent(KeyEvent.ACTION_DOWN, keyCode, metaState);
    send_keyevent(KeyEvent.ACTION_UP, keyCode, metaState);
  }

  void send_keyevent(int eventAction, int eventCode, int metaState)
  {
    try
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn == null)
        return;
      long now = android.os.SystemClock.uptimeMillis();
      conn.sendKeyEvent(new KeyEvent(now, now, eventAction, eventCode, 0,
            metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
      if (eventAction == KeyEvent.ACTION_UP)
      {
        _autocap.event_sent(eventCode, metaState);
        _typedword.event_sent(eventCode, metaState);
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error sending keyevent", t);
    }
  }

  void send_text(String text)
  {
    if (text == null)
      return;
    try
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn == null)
        return;
      _autocap.typed(text);
      _typedword.typed(text);
      conn.commitText(text, 1);
    }
    catch (Throwable t)
    {
      Logs.warn("Error sending text", t);
    }
  }

  void replace_surrounding_text(int remove_before, int remove_after,
      String new_text)
  {
    if (new_text == null)
      return;
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    try
    {
      conn.beginBatchEdit();
      boolean deleted = false;
      try
      {
        deleted = conn.deleteSurroundingText(remove_before, remove_after);
      }
      catch (Throwable t) {}

      // Fallback for terminal apps (such as Termux) where deleteSurroundingText returns false or is not handled
      if (!deleted && remove_before > 0)
      {
        for (int i = 0; i < remove_before; i++)
        {
          conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
          conn.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
        }
      }

      conn.commitText(new_text, 1);
      _typedword.remove_surrounding_text(remove_before, remove_after);
      _typedword.typed(new_text);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in replace_surrounding_text", t);
    }
    finally
    {
      try
      {
        conn.endBatchEdit();
      }
      catch (Throwable t) {}
    }
  }

  /** See {!InputConnection.performContextMenuAction}. */
  void send_context_menu_action(int id)
  {
    try
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn == null)
        return;
      conn.performContextMenuAction(id);
    }
    catch (Throwable t)
    {
      Logs.warn("Error performing context menu action", t);
    }
  }

  void handle_paste(boolean plainText)
  {
    try
    {
      Context ctx = _recv.getContext();
      if (ctx != null)
      {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip())
        {
          ClipData clip = clipboard.getPrimaryClip();
          if (clip != null && clip.getItemCount() > 0)
          {
            CharSequence text = clip.getItemAt(0).coerceToText(ctx);
            if (text != null && text.length() > 0)
            {
              send_text(text.toString());
              return;
            }
          }
        }
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error pasting clipboard text", t);
    }
    send_context_menu_action(plainText ? android.R.id.pasteAsPlainText : android.R.id.paste);
  }

  @SuppressLint("InlinedApi")
  public void handle_editing_key(KeyValue.Editing ev)
  {
    if (ev == null)
      return;
    switch (ev)
    {
      case COPY: if(_typedword.is_selection_not_empty()) send_context_menu_action(android.R.id.copy); break;
      case PASTE: handle_paste(false); break;
      case CUT: if(_typedword.is_selection_not_empty()) send_context_menu_action(android.R.id.cut); break;
      case SELECT_ALL: send_context_menu_action(android.R.id.selectAll); break;
      case SHARE: send_context_menu_action(android.R.id.shareText); break;
      case PASTE_PLAIN: handle_paste(true); break;
      case UNDO: send_context_menu_action(android.R.id.undo); break;
      case REDO: send_context_menu_action(android.R.id.redo); break;
      case REPLACE: send_context_menu_action(android.R.id.replaceText); break;
      case ASSIST: send_context_menu_action(android.R.id.textAssist); break;
      case AUTOFILL: send_context_menu_action(android.R.id.autofill); break;
      case DELETE_WORD: send_key_down_up(KeyEvent.KEYCODE_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON); break;
      case FORWARD_DELETE_WORD: send_key_down_up(KeyEvent.KEYCODE_FORWARD_DEL, KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON); break;
      case SELECTION_CANCEL: cancel_selection(); break;
      case SPACE_BAR: handle_space_bar(); break;
      case BACKSPACE: handle_backspace(); break;
      case WORD_LEFT: move_cursor(-1); break;
      case WORD_RIGHT: move_cursor(1); break;
      case SELECTION_CURSOR_LEFT: move_cursor_sel(-1, true, false); break;
      case SELECTION_CURSOR_RIGHT: move_cursor_sel(1, false, false); break;
    }
  }

  static ExtractedTextRequest _move_cursor_req = null;

  /** Query selection range [start, end] accurately from the InputConnection. */
  int[] get_selection_range(InputConnection conn)
  {
    if (conn == null)
      return null;
    try
    {
      if (_move_cursor_req == null)
      {
        _move_cursor_req = new ExtractedTextRequest();
        _move_cursor_req.hintMaxChars = 0;
      }
      ExtractedText et = conn.getExtractedText(_move_cursor_req, 0);
      if (et != null && et.selectionStart >= 0 && et.selectionEnd >= 0)
      {
        int offset = et.startOffset;
        return new int[]{offset + et.selectionStart, offset + et.selectionEnd};
      }
    }
    catch (Throwable ignored) {}

    if (android.os.Build.VERSION.SDK_INT >= 31)
    {
      try
      {
        android.view.inputmethod.SurroundingText st = conn.getSurroundingText(2048, 2048, 0);
        if (st != null && st.getSelectionStart() >= 0 && st.getSelectionEnd() >= 0)
        {
          int offset = st.getOffset();
          return new int[]{offset + st.getSelectionStart(), offset + st.getSelectionEnd()};
        }
      }
      catch (Throwable ignored) {}
    }

    try
    {
      CharSequence before = conn.getTextBeforeCursor(10000, 0);
      if (before != null)
      {
        int pos = before.length();
        return new int[]{pos, pos};
      }
    }
    catch (Throwable ignored) {}

    return null;
  }

  /** [r] might be negative, in which case the direction is reversed. */
  void handle_slider(KeyValue.Slider s, int r, boolean key_down)
  {
    if (s == null || r == 0)
      return;
    switch (s)
    {
      case Cursor_left: move_cursor(-r); break;
      case Cursor_right: move_cursor(r); break;
      case Cursor_up: move_cursor_vertical(-r); break;
      case Cursor_down: move_cursor_vertical(r); break;
      case Selection_cursor_left: move_cursor_sel(r, true, key_down); break;
      case Selection_cursor_right: move_cursor_sel(r, false, key_down); break;
    }
  }

  void handle_stateful(KeyValue.Stateful st)
  {
    if (st == null)
      return;
    switch (st)
    {
      case Complete_first:
      case Complete_second:
      case Complete_third:
      case Complete_fourth:
      case Complete_fifth:
      case Complete_emoji:
        suggestion_entered(st.toString());
        break;
    }
  }

  private static boolean isWordChar(char c)
  {
    return Character.isLetterOrDigit(c) || c == '_' || c == '\'';
  }

  /**
   * Find the character offset to move the cursor by [stepCount] words to the left.
   * Returns a negative offset, or 0 if cannot move further left.
   */
  public static int getWordOffsetLeft(CharSequence textBefore, int stepCount)
  {
    if (textBefore == null || textBefore.length() == 0 || stepCount <= 0)
      return 0;
    int len = textBefore.length();
    int curr = len;
    for (int s = 0; s < stepCount && curr > 0; s++)
    {
      int i = curr - 1;
      // Skip any whitespace before the cursor
      while (i >= 0 && Character.isWhitespace(textBefore.charAt(i)))
      {
        i--;
      }
      if (i < 0)
      {
        curr = 0;
        break;
      }
      if (isWordChar(textBefore.charAt(i)))
      {
        while (i >= 0 && isWordChar(textBefore.charAt(i)))
        {
          i--;
        }
      }
      else
      {
        while (i >= 0 && !isWordChar(textBefore.charAt(i)) && !Character.isWhitespace(textBefore.charAt(i)))
        {
          i--;
        }
      }
      curr = i + 1;
    }
    return curr - len;
  }

  /**
   * Find the character offset to move the cursor by [stepCount] words to the right.
   * Returns a positive offset, or 0 if cannot move further right.
   */
  public static int getWordOffsetRight(CharSequence textAfter, int stepCount)
  {
    if (textAfter == null || textAfter.length() == 0 || stepCount <= 0)
      return 0;
    int len = textAfter.length();
    int curr = 0;
    for (int s = 0; s < stepCount && curr < len; s++)
    {
      int i = curr;
      // Skip any whitespace after the cursor
      while (i < len && Character.isWhitespace(textAfter.charAt(i)))
      {
        i++;
      }
      if (i >= len)
      {
        curr = len;
        break;
      }
      if (isWordChar(textAfter.charAt(i)))
      {
        while (i < len && isWordChar(textAfter.charAt(i)))
        {
          i++;
        }
      }
      else
      {
        while (i < len && !isWordChar(textAfter.charAt(i)) && !Character.isWhitespace(textAfter.charAt(i)))
        {
          i++;
        }
      }
      curr = i;
    }
    return curr;
  }

  /** Move the cursor right or left by word(s).
      Unlike arrow keys, the selection is not removed even if shift is not on.
      Falls back to sending arrow keys events with CTRL if the editor does not support
      moving the cursor or a modifier other than shift is pressed. */
  void move_cursor(int d)
  {
    if (d == 0)
      return;
    try
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn == null)
        return;
      if (can_set_selection(conn))
      {
        int[] sel = get_selection_range(conn);
        if (sel != null)
        {
          int sel_start = sel[0];
          int sel_end = sel[1];
          int delta = 0;
          if (d < 0)
          {
            CharSequence before = conn.getTextBeforeCursor(2048, 0);
            delta = getWordOffsetLeft(before, -d);
          }
          else
          {
            CharSequence after = conn.getTextAfterCursor(2048, 0);
            delta = getWordOffsetRight(after, d);
          }
          if (delta != 0)
          {
            // Continue expanding the selection even if shift is not pressed
            if (sel_end != sel_start)
            {
              sel_end += delta;
            }
            else
            {
              sel_end += delta;
              // Leave 'sel_start' where it is if shift is pressed
              if ((_meta_state & KeyEvent.META_SHIFT_ON) == 0)
                sel_start = sel_end;
            }
            if (sel_start < 0) sel_start = 0;
            if (sel_end < 0) sel_end = 0;
            if (conn.setSelection(sel_start, sel_end))
              return;
          }
        }
      }
      move_cursor_fallback(d);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in move_cursor", t);
    }
  }

  /** Move one of the two sides of a selection by word(s). If [sel_left] is true, the left
      position is moved, otherwise the right position is moved. */
  void move_cursor_sel(int d, boolean sel_left, boolean key_down)
  {
    if (d == 0)
      return;
    try
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn == null)
        return;
      if (can_set_selection(conn))
      {
        int[] sel = get_selection_range(conn);
        if (sel != null)
        {
          int sel_start = sel[0];
          int sel_end = sel[1];
          // Reorder the selection when the slider has just been pressed.
          if (key_down && sel_start > sel_end)
          {
            sel_start = sel[1];
            sel_end = sel[0];
          }
          int delta = 0;
          if (d < 0)
          {
            CharSequence before = conn.getTextBeforeCursor(2048, 0);
            delta = getWordOffsetLeft(before, -d);
          }
          else
          {
            CharSequence after = conn.getTextAfterCursor(2048, 0);
            delta = getWordOffsetRight(after, d);
          }
          if (delta != 0)
          {
            if (sel_left)
              sel_start += delta;
            else
              sel_end += delta;
            if (sel_start < 0) sel_start = 0;
            if (sel_end < 0) sel_end = 0;
            if (conn.setSelection(sel_start, sel_end))
              return;
          }
        }
      }
      move_cursor_fallback(d);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in move_cursor_sel", t);
    }
  }

  /** Returns whether the selection can be set using [conn.setSelection()].
      This can happen on Termux or when system modifiers are activated for
      example. */
  boolean can_set_selection(InputConnection conn)
  {
    final int system_mods =
      KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON;
    return !_move_cursor_force_fallback && (_meta_state & system_mods) == 0;
  }

  void send_ctrl_dpad(int dpadKeyCode, int repeat)
  {
    InputConnection conn = _recv.getCurrentInputConnection();
    if (conn == null)
      return;
    int mods = _meta_state | KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
    long now = android.os.SystemClock.uptimeMillis();
    for (int i = 0; i < repeat; i++)
    {
      conn.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CTRL_LEFT, 0,
            mods, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
      conn.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, dpadKeyCode, 0,
            mods, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
      conn.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, dpadKeyCode, 0,
            mods, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
      conn.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_CTRL_LEFT, 0,
            _meta_state, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD | KeyEvent.FLAG_KEEP_TOUCH_MODE));
    }
  }

  void move_cursor_fallback(int d)
  {
    if (d < 0)
    {
      send_ctrl_dpad(KeyEvent.KEYCODE_DPAD_LEFT, -d);
    }
    else
    {
      send_ctrl_dpad(KeyEvent.KEYCODE_DPAD_RIGHT, d);
    }
  }

  /** Move the cursor up and down. This sends UP and DOWN key events that might
      make the focus exit the text box. */
  void move_cursor_vertical(int d)
  {
    if (d < 0)
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_UP, -d);
    else
      send_key_down_up_repeat(KeyEvent.KEYCODE_DPAD_DOWN, d);
  }

  void evaluate_macro(KeyValue[] keys)
  {
    if (keys == null || keys.length == 0)
      return;
    // Ignore modifiers that are activated at the time the macro is evaluated
    mods_changed(Pointers.Modifiers.EMPTY);
    evaluate_macro_loop(keys, 0, Pointers.Modifiers.EMPTY, _autocap.pause());
  }

  /** Evaluate the macro asynchronously to make sure event are processed in the
      right order. */
  void evaluate_macro_loop(final KeyValue[] keys, int i, Pointers.Modifiers mods, final boolean autocap_paused)
  {
    if (keys == null || i < 0)
      return;
    boolean should_delay = false;
    if (i < keys.length)
    {
      KeyValue kv = KeyModifier.modify_no_modmap(keys[i], mods);
      if (kv != null)
      {
        if (kv.hasFlagsAny(KeyValue.FLAG_LATCH))
        {
          // Non-special latchable keys clear latched modifiers
          if (!kv.hasFlagsAny(KeyValue.FLAG_SPECIAL))
            mods = Pointers.Modifiers.EMPTY;
          mods = mods.with_extra_mod(kv);
        }
        else
        {
          key_down(kv, false);
          key_up(kv, mods);
          mods = Pointers.Modifiers.EMPTY;
        }
        should_delay = wait_after_macro_key(kv);
      }
    }
    i++;
    if (i >= keys.length) // Stop looping
    {
      _autocap.unpause(autocap_paused);
    }
    else if (should_delay)
    {
      // Add a delay before sending the next key to avoid race conditions
      // causing keys to be handled in the wrong order. Notably, KeyEvent keys
      // handling is scheduled differently than the other edit functions.
      final int i_ = i;
      final Pointers.Modifiers mods_ = mods;
      _recv.getHandler().postDelayed(new Runnable() {
        public void run()
        {
          evaluate_macro_loop(keys, i_, mods_, autocap_paused);
        }
      }, 1000/30);
    }
    else
      evaluate_macro_loop(keys, i, mods, autocap_paused);
  }

  boolean wait_after_macro_key(KeyValue kv)
  {
    if (kv == null)
      return false;
    switch (kv.getKind())
    {
      case Keyevent:
      case Editing:
      case Event:
        return true;
      case Slider:
        return _move_cursor_force_fallback;
      default:
        return false;
    }
  }

  /** Repeat calls to [send_key_down_up]. */
  void send_key_down_up_repeat(int event_code, int repeat)
  {
    while (repeat-- > 0)
      send_key_down_up(event_code);
  }

  void send_key_down_up_repeat(int event_code, int repeat, int metaState)
  {
    while (repeat-- > 0)
      send_key_down_up(event_code, metaState);
  }

  void cancel_selection()
  {
    try
    {
      InputConnection conn = _recv.getCurrentInputConnection();
      if (conn == null)
        return;
      int[] sel = get_selection_range(conn);
      if (sel == null) return;
      final int curs = sel[0];
      // Notify the receiver as Android's [onUpdateSelection] is not triggered.
      if (conn.setSelection(curs, curs))
        _recv.selection_state_changed(false);
    }
    catch (Throwable t)
    {
      Logs.warn("Error in cancel_selection", t);
    }
  }

  /** The word that was replaced by a suggestion when the last action was to
      enter a suggestion (with the space bar or the candidates view) or [null]
      otherwise. */
  String last_replaced_word = null;
  String last_autocorrect_original = null;
  int last_autocorrect_replacement_len = 0;
  private final java.util.Set<String> _reverted_autocorrect_words = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());
  boolean _autocorrect_enabled = true;
  /** Length of the text before the cursor that should be replaced by
      backspace. */
  int last_replacement_word_len = 0;

  /** Implement autocorrect when enabled in the settings. */
  void handle_space_bar()
  {
    try
    {
      String typed = _typedword.get();
      if (typed != null && !typed.trim().isEmpty())
      {
        String lower = typed.toLowerCase(java.util.Locale.ROOT);
        if (_reverted_autocorrect_words.contains(lower))
        {
          // User previously reverted autocorrect for this word: keep verbatim!
          if (_suggestions != null)
          {
            _suggestions.onWordCommitted(typed);
            if (_suggestions.getEngine() != null)
            {
              _suggestions.getEngine().clearRevertedException(lower);
            }
          }
          _reverted_autocorrect_words.remove(lower);
          send_text(" ");
          last_autocorrect_original = null;
          last_replaced_word = null;
          _next_last_action = LastAction.OTHER;
        }
        else if (_autocorrect_enabled && _suggestions != null && _suggestions.best_autocorrect != null
            && !_suggestions.best_autocorrect.equalsIgnoreCase(typed)
            && !_typedword.is_selection_not_empty()
            && _typedword.cursor_relative() == 0)
        {
          // Perform FUTO-style Autocorrect
          String best = _suggestions.best_autocorrect;
          int cur_rel = _typedword.cursor_relative();
          String replacement = best + " ";
          replace_surrounding_text(typed.length() + cur_rel, -cur_rel, replacement);
          last_autocorrect_original = typed;
          last_autocorrect_replacement_len = replacement.length();
          _last_action = LastAction.AUTOCORRECTED;
          _next_last_action = LastAction.AUTOCORRECTED;
          if (_suggestions != null)
          {
            _suggestions.onWordCommitted(best);
          }
        }
        else if (_space_bar_auto_complete && _suggestions != null && _suggestions.count > 0
            && _suggestions.suggestions[0] != null
            && !_typedword.is_selection_not_empty()
            && _typedword.cursor_relative() == 0)
        {
          suggestion_entered(_suggestions.suggestions[0] + " ");
        }
        else
        {
          if (_suggestions != null)
          {
            _suggestions.onWordCommitted(typed);
          }
          send_text(" ");
          last_autocorrect_original = null;
          _next_last_action = LastAction.OTHER;
        }
      }
      else
      {
        send_text(" ");
        last_autocorrect_original = null;
        _next_last_action = LastAction.OTHER;
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error in handle_space_bar", t);
      send_text(" ");
    }
  }

  /** Undo the last autocorrect. */
  void handle_backspace()
  {
    try
    {
      if (_last_action == LastAction.AUTOCORRECTED && last_autocorrect_original != null)
      {
        // Revert autocorrect back to the verbatim typed word!
        replace_surrounding_text(last_autocorrect_replacement_len, 0, last_autocorrect_original);
        String rev = last_autocorrect_original.toLowerCase(java.util.Locale.ROOT);
        _reverted_autocorrect_words.add(rev);
        if (_suggestions != null && _suggestions.getEngine() != null)
        {
          _suggestions.getEngine().addRevertedException(rev);
        }
        String oldWord = last_autocorrect_original;
        last_autocorrect_original = null;
        _last_action = LastAction.REVERTED_AUTOCORRECT;
        _next_last_action = LastAction.REVERTED_AUTOCORRECT;
        if (_suggestions != null)
        {
          _suggestions.currently_typed_word(oldWord);
        }
      }
      else if (_last_action == LastAction.SUGGESTION_ENTERED
          && last_replaced_word != null)
      {
        replace_surrounding_text(last_replacement_word_len, 0, last_replaced_word);
        last_replaced_word = null;
        _last_action = LastAction.OTHER;
      }
      else
      {
        send_key_down_up(KeyEvent.KEYCODE_DEL);
        last_autocorrect_original = null;
        last_replaced_word = null;
        _last_action = LastAction.OTHER;
      }
    }
    catch (Throwable t)
    {
      Logs.warn("Error in handle_backspace", t);
    }
  }

  public IReceiver getReceiver()
  {
    return _recv;
  }

  public static interface IReceiver extends Suggestions.Callback
  {
    public void handle_event_key(KeyValue.Event ev);
    public void set_shift_state(boolean state, boolean lock);
    public void set_compose_pending(boolean pending);
    public void selection_state_changed(boolean selection_is_ongoing);
    public InputConnection getCurrentInputConnection();
    public Handler getHandler();
    public Context getContext();
    public void switch_to_layout_index(int index);
    public void switch_to_layout_name(String layoutName);
    public java.util.List<KeyboardData> get_active_layouts();
    public int get_current_layout_index();
    public void switch_to_theme_name(String themeName);
    public String get_current_theme_name();
  }

  class Autocapitalisation_callback implements Autocapitalisation.Callback
  {
    @Override
    public void update_shift_state(boolean should_enable, boolean should_disable)
    {
      if (should_enable)
        _recv.set_shift_state(true, false);
      else if (should_disable)
        _recv.set_shift_state(false, false);
    }
  }

  public static enum LastAction
  {
    AUTOCORRECTED,
    SUGGESTION_ENTERED,
    REVERTED_AUTOCORRECT,
    OTHER
  }
}
