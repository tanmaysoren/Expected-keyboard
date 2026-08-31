package expected.keyboard2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.util.LogPrinter;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import expected.keyboard2.dict.Dictionaries;
import expected.keyboard2.dict.DictionariesActivity;
import expected.keyboard2.dict.DictionarySwitcher;
import expected.keyboard2.prefs.LayoutsPreference;
import expected.keyboard2.suggestions.CandidatesView;
import expected.keyboard2.suggestions.Suggestions;

public class Keyboard2 extends InputMethodService
  implements SharedPreferences.OnSharedPreferenceChangeListener
{
  /** The view containing the keyboard and candidates view. */
  private ViewGroup _keyboard_container_view;
  private Keyboard2View _keyboard_layout_view;
  private CandidatesView _candidates_view;
  private Suggestions _suggestions;
  private KeyEventHandler _keyeventhandler;
  /** If not 'null', the layout to use instead of [_config.current_layout]. */
  private KeyboardData _currentSpecialLayout;
  /** Layout associated with the currently selected locale. Not 'null'. */
  private KeyboardData _localeTextLayout;
  /** Installed and current locales. */
  private Dictionaries _dictionaries;
  private ViewGroup _emojiPane = null;
  private ViewGroup _clipboard_pane = null;
  private ViewGroup _editingPane = null;
  private ViewGroup _layoutPane = null;
  private ViewGroup _themePane = null;
  private View _currentInputView = null;
  private Handler _handler;

  private Config _config;

  private FoldStateTracker _foldStateTracker;

  private final java.util.Map<Integer, KeyboardData> _modifiedLayoutCache = new java.util.HashMap<>();
  private KeyboardData _cachedNumericLayout = null;
  private KeyboardData _cachedGreekMathLayout = null;
  private KeyboardData _cachedPinLayout = null;

  private void clearLayoutCaches()
  {
    _modifiedLayoutCache.clear();
    _cachedNumericLayout = null;
    _cachedGreekMathLayout = null;
    _cachedPinLayout = null;
  }

  /** Layout currently visible before it has been modified. */
  KeyboardData current_layout_unmodified()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    KeyboardData layout = null;
    int layout_i = _config.get_current_layout();
    if (layout_i >= _config.layouts.size())
      layout_i = 0;
    if (layout_i < _config.layouts.size())
      layout = _config.layouts.get(layout_i);
    if (layout == null)
      layout = _localeTextLayout;
    return layout;
  }

  /** Layout currently visible. */
  KeyboardData current_layout()
  {
    if (_currentSpecialLayout != null)
      return _currentSpecialLayout;
    int layout_i = _config.get_current_layout();
    KeyboardData cached = _modifiedLayoutCache.get(layout_i);
    if (cached != null)
      return cached;
    KeyboardData unmodified = current_layout_unmodified();
    KeyboardData modified = LayoutModifier.modify_layout(unmodified);
    _modifiedLayoutCache.put(layout_i, modified);
    return modified;
  }

  void setTextLayout(int l)
  {
    _config.set_current_layout(l);
    _currentSpecialLayout = null;
    // The active dictionary depends on the current layout.
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  void incrTextLayout(int delta)
  {
    int s = _config.layouts.size();
    setTextLayout((_config.get_current_layout() + delta + s) % s);
  }

  void setSpecialLayout(KeyboardData l)
  {
    _currentSpecialLayout = l;
    _keyboard_layout_view.setKeyboard(l);
  }

  KeyboardData loadLayout(int layout_id)
  {
    return KeyboardData.load(getResources(), layout_id);
  }

  /** Load a layout that contains a numpad. */
  KeyboardData loadNumpad(int layout_id)
  {
    return LayoutModifier.modify_numpad(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
  }

  KeyboardData loadNumericLayout()
  {
    if (_cachedNumericLayout != null)
      return _cachedNumericLayout;
    _cachedNumericLayout = loadNumpad((_config.orientation_landscape && _config.split_layout) ?
        R.xml.numeric_landscape : R.xml.numeric);
    return _cachedNumericLayout;
  }

  KeyboardData loadGreekMathLayout()
  {
    if (_cachedGreekMathLayout != null)
      return _cachedGreekMathLayout;
    _cachedGreekMathLayout = loadNumpad(R.xml.greekmath);
    return _cachedGreekMathLayout;
  }

  KeyboardData loadPinentry(int layout_id)
  {
    if (_cachedPinLayout != null)
      return _cachedPinLayout;
    _cachedPinLayout = LayoutModifier.modify_pinentry(KeyboardData.load(getResources(), layout_id),
        current_layout_unmodified());
    return _cachedPinLayout;
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(this);
    _handler = new Handler(getMainLooper());
    _foldStateTracker = new FoldStateTracker(this);
    _dictionaries = Dictionaries.instance(this);
    Config.initGlobalConfig(prefs, getResources(),
        _foldStateTracker.isUnfolded(), _dictionaries);
    _config = Config.globalConfig();
    Receiver recvr = this.new Receiver();
    _suggestions = new Suggestions(recvr, _config);
    _suggestions.setContext(this);
    _keyeventhandler = new KeyEventHandler(recvr, _suggestions);
    KeyValue.Stateful._handler = recvr;
    _config.handler = _keyeventhandler;
    prefs.registerOnSharedPreferenceChangeListener(this);
    Logs.set_debug_logs(getResources().getBoolean(R.bool.debug_logs));
    refreshSubtypeImm();
    create_keyboard_view();
    ClipboardHistoryService.on_startup(this, _keyeventhandler);
    _foldStateTracker.setChangedCallback(() -> { refresh_config(); });
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    _foldStateTracker.close();
  }

  private void create_keyboard_view()
  {
    _keyboard_container_view = (ViewGroup)inflate_view(R.layout.keyboard);
    _keyboard_layout_view = (Keyboard2View)_keyboard_container_view.findViewById(R.id.keyboard_view);
    _candidates_view = (CandidatesView)_keyboard_container_view.findViewById(R.id.candidates_view);
  }

  InputMethodManager get_imm()
  {
    return (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
  }

  private void refreshSubtypeImm()
  {
    KeyboardData default_layout = null;
    _config.device_locales = DeviceLocales.load(this);
    if (_config.device_locales.default_ != null)
    {
      String layout_name = _config.device_locales.default_.default_layout;
      if (layout_name != null)
        default_layout = LayoutsPreference.layout_of_string(getResources(), layout_name);
    }
    _config.extra_keys_subtype = _config.device_locales.extra_keys();
    if (default_layout == null)
      default_layout = loadLayout(R.xml.latn_qwerty_us);
    _localeTextLayout = default_layout;
  }

  private void refresh_current_dictionary()
  {
    _config.should_show_dictionary_switch = false;
    String selected = _dictionaries.get_selected(_config);
    String fallback = (_config.device_locales.default_ != null) ?
      _config.device_locales.default_.dictionary : null;
    _dictionaries.set_current_dictionary(_config,
        (selected != null) ? selected : fallback);
  }

  /** Remember and apply the dictionary chosen by the user for the current
      context. */
  private void select_dictionary(String dict_name)
  {
    _dictionaries.set_selected(_config, dict_name);
    refresh_current_dictionary();
    refresh_candidates_view();
  }

  private void refresh_candidates_view()
  {
    // New logic: separate bar persistence from texts
    // show_suggestion_bar controls container visibility (never disappears when enabled)
    // suggestions_enabled controls whether texts/predictions are queried
    boolean barEnabled = _config.show_suggestion_bar;
    boolean should_show;
    if (!barEnabled) {
      should_show = false;
    } else {
      // When bar is enabled, keep it persistently visible (fixes terminal -> normal bug)
      // This overrides the editor_config and split_layout hiding
      should_show = true;
    }
    // Always refresh config to reset toggle state and ensure suggestions re-enabled after terminal
    _candidates_view.refresh_config(_config);
    if (should_show) {
      _keyeventhandler.dictionary_changed();
    }
    _candidates_view.setVisibility(should_show ? View.VISIBLE : View.GONE);
  }

  /** Might re-create the keyboard view. [_keyboard_layout_view.setKeyboard()] and
      [setInputView()] must be called soon after. */
  private void refresh_config()
  {
    clearLayoutCaches();
    int prev_theme = _config.theme;
    _config.refresh(getResources(), _foldStateTracker.isUnfolded(), _dictionaries);
    refresh_current_dictionary();
    // Refreshing the theme config requires re-creating the views
    if (prev_theme != _config.theme)
    {
      create_keyboard_view();
      _emojiPane = null;
      _clipboard_pane = null;
      _editingPane = null;
      _layoutPane = null;
      setInputView(_keyboard_container_view);
    }
    // Set keyboard background opacity
    if (_keyboard_container_view != null)
    {
      Drawable bg = _keyboard_container_view.getBackground().mutate();
      bg.setAlpha(_config.keyboardOpacity);
      _keyboard_container_view.setBackground(bg);
    }
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setKeyboard(current_layout());
      _keyboard_layout_view.reset();
    }
    refresh_candidates_view();
  }

  private KeyboardData refresh_special_layout()
  {
    if (_config.editor_config.numeric_layout)
    {
      switch (_config.selected_number_layout)
      {
        case PIN:
          return loadPinentry((_config.orientation_landscape && _config.split_layout) ?
              R.xml.pin_landscape : R.xml.pin);
        case NUMBER:
          return loadNumericLayout();
      }
    }
    return null;
  }

  @Override
  public View onCreateInputView()
  {
    if (_keyboard_container_view == null)
      create_keyboard_view();
    ViewParent parent = _keyboard_container_view.getParent();
    if (parent != null && parent instanceof ViewGroup)
      ((ViewGroup) parent).removeView(_keyboard_container_view);
    return _keyboard_container_view;
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean restarting)
  {
    _config.editor_config.refresh(info, getResources());
    refresh_config();
    _currentSpecialLayout = refresh_special_layout();
    if (_keyboard_layout_view != null)
      _keyboard_layout_view.setKeyboard(current_layout());
    _keyeventhandler.started(_config);
    setInputView(_keyboard_container_view);
    if (_config != null && _config.isFloatingMode())
      applyFloatingModeLayout();
    Logs.debug_startup_input_view(info, _config);
  }

  @Override
  public void setInputView(View v)
  {
    _currentInputView = v;
    if (v != null)
    {
      ViewParent parent = v.getParent();
      if (parent != null && parent instanceof ViewGroup)
        ((ViewGroup)parent).removeView(v);
      super.setInputView(v);
      applyFloatingModeLayout();
      v.requestApplyInsets();
    }
  }

  @Override
  public void updateFullscreenMode() {
    super.updateFullscreenMode();
    updateSoftInputWindowLayoutParams();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig)
  {
    super.onConfigurationChanged(newConfig);
    refresh_config();
    if (_config.editor_config.numeric_layout)
      _currentSpecialLayout = refresh_special_layout();
    if (_keyboard_layout_view != null)
      _keyboard_layout_view.setKeyboard(current_layout());
    applyFloatingModeLayout();
    updateInputViewShown();
  }

  private static void updateLayoutSizeOf(final Window window, final int layoutWidth, final int layoutHeight) {
    if (window == null) return;
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && (params.width != layoutWidth || params.height != layoutHeight)) {
      params.width = layoutWidth;
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutSizeOf(final View view, final int layoutWidth, final int layoutHeight) {
    if (view == null) return;
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && (params.width != layoutWidth || params.height != layoutHeight)) {
      params.width = layoutWidth;
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private void updateSoftInputWindowLayoutParams() {
    final Window window = getWindow().getWindow();
    if (window == null) return;

    if (_config != null && _config.isFloatingMode()) {
      window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
      window.setDimAmount(0f);
      WindowManager.LayoutParams wattrs = window.getAttributes();
      if (wattrs != null) {
        wattrs.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        wattrs.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wattrs.flags &= ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        window.setAttributes(wattrs);
      }
      updateLayoutSizeOf(window, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
      final View inputArea = window.findViewById(android.R.id.inputArea);
      if (inputArea != null) {
        updateLayoutSizeOf(inputArea, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        updateLayoutGravityOf(inputArea, Gravity.TOP | Gravity.LEFT);
        if (inputArea.getParent() instanceof View) {
          View parent = (View) inputArea.getParent();
          updateLayoutSizeOf(parent, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
          updateLayoutGravityOf(parent, Gravity.TOP | Gravity.LEFT);
        }
      }
      return;
    }

    if (VERSION.SDK_INT >= 35)
    {
      WindowManager.LayoutParams wattrs = window.getAttributes();
      if (wattrs != null)
      {
        wattrs.layoutInDisplayCutoutMode =
          WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        // Allow to draw behind system bars
        wattrs.setFitInsetsTypes(0);
        window.setDecorFitsSystemWindows(false);
      }
    }
    updateLayoutSizeOf(window, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    final View inputArea = window.findViewById(android.R.id.inputArea);
    if (inputArea != null)
    {
      updateLayoutSizeOf(inputArea, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      updateLayoutGravityOf(inputArea, Gravity.BOTTOM);
      if (inputArea.getParent() instanceof View)
      {
        View parent = (View) inputArea.getParent();
        updateLayoutSizeOf(
                parent,
                ViewGroup.LayoutParams.MATCH_PARENT,
                isFullscreenMode()
                        ? ViewGroup.LayoutParams.MATCH_PARENT
                        : ViewGroup.LayoutParams.WRAP_CONTENT);
        updateLayoutGravityOf(parent, Gravity.BOTTOM);
      }
    }
  }

  @Override
  public void onComputeInsets(android.inputmethodservice.InputMethodService.Insets outInsets)
  {
    super.onComputeInsets(outInsets);
    if (_config != null && _config.isFloatingMode())
    {
      android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
      outInsets.contentTopInsets = dm.heightPixels;
      outInsets.visibleTopInsets = dm.heightPixels;

      View activeView = _currentInputView != null ? _currentInputView : _keyboard_container_view;
      if (activeView != null)
      {
        ViewGroup.LayoutParams rawLp = activeView.getLayoutParams();
        int touchLeft = 0;
        int touchTop = 0;
        int touchRight = dm.widthPixels;
        int touchBottom = dm.heightPixels;

        if (rawLp instanceof ViewGroup.MarginLayoutParams) {
          ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
          touchLeft = lp.leftMargin;
          touchTop = lp.topMargin;
          int defaultH = FloatingKeyboardUtils.readFloatingHeight(this, dm.widthPixels);
          int w = activeView.getWidth() > 0 ? activeView.getWidth() : lp.width;
          int h = activeView.getHeight() > 0 ? activeView.getHeight() : defaultH;
          touchRight = touchLeft + w;
          touchBottom = touchTop + h;
        }

        outInsets.touchableInsets = android.inputmethodservice.InputMethodService.Insets.TOUCHABLE_INSETS_REGION;
        outInsets.touchableRegion.set(touchLeft, touchTop, touchRight, touchBottom);
      }
    }
  }

  public void toggleFloatingMode()
  {
    if (_config != null)
    {
      _config.setFloatingMode(!_config.isFloatingMode());
      applyFloatingModeLayout();
    }
  }

  private void applyFloatingModeLayout()
  {
    final Window window = getWindow().getWindow();
    if (window == null)
      return;

    boolean isFloating = (_config != null && _config.isFloatingMode());

    updateSoftInputWindowLayoutParams();

    View target = _currentInputView != null ? _currentInputView : _keyboard_container_view;
    if (target instanceof ViewGroup)
    {
      ViewGroup targetGroup = (ViewGroup) target;
      View layoutView = target.findViewById(R.id.keyboard_view);
      if (isFloating)
      {
        FloatingKeyboardUtils.applyFloating(window, targetGroup, _config, layoutView);
      }
      else
      {
        FloatingKeyboardUtils.disableFloating(window, targetGroup);
      }
    }
    updateInputViewShown();
  }

  private static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
    final WindowManager.LayoutParams params = window.getAttributes();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      window.setAttributes(params);
    }
  }

  private static void updateLayoutHeightOf(final View view, final int layoutHeight) {
    final ViewGroup.LayoutParams params = view.getLayoutParams();
    if (params != null && params.height != layoutHeight) {
      params.height = layoutHeight;
      view.setLayoutParams(params);
    }
  }

  private static void updateLayoutGravityOf(final View view, final int layoutGravity) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof LinearLayout.LayoutParams) {
      final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    } else if (lp instanceof FrameLayout.LayoutParams) {
      final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
      if (params.gravity != layoutGravity) {
        params.gravity = layoutGravity;
        view.setLayoutParams(params);
      }
    }
  }

  @Override
  public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype)
  {
    clearLayoutCaches();
    refreshSubtypeImm();
    refresh_current_dictionary();
    refresh_candidates_view();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  @Override
  public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
  {
    super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
    _keyeventhandler.selection_updated(oldSelStart, newSelStart, newSelEnd);
    if ((oldSelStart == oldSelEnd) != (newSelStart == newSelEnd))
      _keyboard_layout_view.set_selection_state(newSelStart != newSelEnd);
  }

  @Override
  public void onFinishInputView(boolean finishingInput)
  {
    super.onFinishInputView(finishingInput);
    _keyboard_layout_view.reset();
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences _prefs, String _key)
  {
    refresh_config();
    _keyboard_layout_view.setKeyboard(current_layout());
  }

  @Override
  public boolean onEvaluateFullscreenMode()
  {
    /* Entirely disable fullscreen mode. */
    return false;
  }

  @Override
  public boolean onEvaluateInputViewShown()
  {
    // Ensure soft keyboard is always displayed on input request, including landscape mode.
    return true;
  }

  @Override
  public boolean onShowInputRequested(int flags, boolean configChange)
  {
    if (_config != null && _config.physical_keyboard_hide)
    {
      Configuration config = getResources().getConfiguration();
      if (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO)
        return false;
    }
    return true;
  }

  @Override
  public void onWindowShown()
  {
    super.onWindowShown();
    if (_keyboard_layout_view != null)
    {
      _keyboard_layout_view.setKeyboard(current_layout());
      _keyboard_layout_view.requestLayout();
      _keyboard_layout_view.invalidate();
    }
  }

  public void launch_dictionaries_activity()
  {
    start_activity(DictionariesActivity.class);
  }

  /** Called from [onClick] attributes. */
  public void launch_dictionaries_activity(View v)
  {
    launch_dictionaries_activity();
  }

  void start_activity(Class cls)
  {
    Intent intent = new Intent(this, cls);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  /** Not static */
  public class Receiver implements KeyEventHandler.IReceiver,
         KeyValue.Stateful.Symbol_provider, DictionarySwitcher.Callback
  {
    public void handle_event_key(KeyValue.Event ev)
    {
      switch (ev)
      {
        case CONFIG:
          start_activity(SettingsActivity.class);
          break;

        case SWITCH_TEXT:
          _currentSpecialLayout = null;
          _keyboard_layout_view.setKeyboard(current_layout());
          break;

        case SWITCH_NUMERIC:
          setSpecialLayout(loadNumericLayout());
          break;

        case SWITCH_EMOJI:
          if (_emojiPane == null)
            _emojiPane = (ViewGroup)inflate_view(R.layout.emoji_pane);
          setInputView(_emojiPane);
          break;

        case SWITCH_CLIPBOARD:
          if (_clipboard_pane == null)
            _clipboard_pane = (ViewGroup)inflate_view(R.layout.clipboard_pane);
          setInputView(_clipboard_pane);
          break;

        case SWITCH_EDITING:
          if (_editingPane == null)
            _editingPane = (ViewGroup)inflate_view(R.layout.editing_pane);
          setInputView(_editingPane);
          break;

        case SWITCH_LAYOUT_PANE:
          if (_layoutPane == null)
            _layoutPane = (ViewGroup)inflate_view(R.layout.layout_switcher_pane);
          setInputView(_layoutPane);
          break;

        case SWITCH_THEME_PANE:
          if (_themePane == null)
            _themePane = (ViewGroup)inflate_view(R.layout.theme_switcher_pane);
          setInputView(_themePane);
          break;

        case SWITCH_BACK_EMOJI:
        case SWITCH_BACK_CLIPBOARD:
        case SWITCH_BACK_EDITING:
        case SWITCH_BACK_LAYOUT_PANE:
        case SWITCH_BACK_THEME_PANE:
          setInputView(_keyboard_container_view);
          break;

        case CHANGE_METHOD_PICKER:
          get_imm().showInputMethodPicker();
          break;

        case CHANGE_METHOD_PREV:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToLastInputMethod(getConnectionToken());
          else
            switchToPreviousInputMethod();
          break;

        case CHANGE_METHOD_NEXT:
          if (VERSION.SDK_INT < 28)
            get_imm().switchToNextInputMethod(getConnectionToken(), false);
          else
            switchToNextInputMethod(false);
          break;

        case ACTION:
          InputConnection conn = getCurrentInputConnection();
          if (conn != null)
            conn.performEditorAction(_config.editor_config.actionId);
          break;

        case SWITCH_FORWARD:
          incrTextLayout(1);
          break;

        case SWITCH_BACKWARD:
          incrTextLayout(-1);
          break;

        case SWITCH_GREEKMATH:
          setSpecialLayout(loadGreekMathLayout());
          break;

        case CAPS_LOCK:
          set_shift_state(true, true);
          break;

        case HIDE_SELF:
          Keyboard2.this.requestHideSelf(0);
          break;

        case CHANGE_DICTIONARY:
          new DictionarySwitcher(Keyboard2.this, _dictionaries, this).choose();
          break;

        case TOGGLE_FLOATING:
          toggleFloatingMode();
          break;
      }
    }

    public void set_shift_state(boolean state, boolean lock)
    {
      _keyboard_layout_view.set_shift_state(state, lock);
    }

    public void set_compose_pending(boolean pending)
    {
      _keyboard_layout_view.set_compose_pending(pending);
    }

    public void selection_state_changed(boolean selection_is_ongoing)
    {
      _keyboard_layout_view.set_selection_state(selection_is_ongoing);
    }

    public InputConnection getCurrentInputConnection()
    {
      return Keyboard2.this.getCurrentInputConnection();
    }

    public Handler getHandler()
    {
      return _handler;
    }

    public Context getContext()
    {
      return Keyboard2.this;
    }

    public void set_suggestions(Suggestions suggestions)
    {
      _candidates_view.set_candidates(suggestions);
    }

    public String provide_stateful_key_symbol(KeyValue.Stateful q)
    {
      switch (q)
      {
        case Complete_first: return _suggestions.suggestions[0];
        case Complete_second: return _suggestions.suggestions[1];
        case Complete_third: return _suggestions.suggestions[2];
        case Complete_fourth: return _suggestions.suggestions[3];
        case Complete_fifth: return _suggestions.suggestions[4];
        case Complete_emoji: return _suggestions.emoji_suggestion;
      }
      return "";
    }

    public void on_change_dictionary(String dict_name)
    {
      select_dictionary(dict_name);
    }

    public void launch_dictionaries_activity()
    {
      Keyboard2.this.launch_dictionaries_activity();
    }

    @Override
    public void switch_to_layout_index(int index)
    {
      if (_config.layouts != null && index >= 0 && index < _config.layouts.size())
      {
        clearLayoutCaches();
        _layoutPane = null;
        setTextLayout(index);
        if (_keyboard_layout_view != null)
        {
          _keyboard_layout_view.setKeyboard(current_layout());
          _keyboard_layout_view.requestLayout();
          _keyboard_layout_view.invalidate();
        }
        setInputView(_keyboard_container_view);
      }
    }

    @Override
    public void switch_to_layout_name(String layoutName)
    {
      if (layoutName == null) return;
      if ("system".equals(layoutName)) layoutName = "latn_qwerty_us";
      clearLayoutCaches();
      _layoutPane = null;

      // Check if already in active layouts
      if (_config.layouts != null)
      {
        for (int i = 0; i < _config.layouts.size(); i++)
        {
          KeyboardData existing = _config.layouts.get(i);
          String exName = (existing != null && existing.resourceName != null) ? existing.resourceName : (existing != null ? existing.name : null);
          if (layoutName.equals(exName))
          {
            setTextLayout(i);
            if (_keyboard_layout_view != null)
            {
              _keyboard_layout_view.setKeyboard(current_layout());
              _keyboard_layout_view.requestLayout();
              _keyboard_layout_view.invalidate();
            }
            setInputView(_keyboard_container_view);
            return;
          }
        }
      }

      // Dynamically load the layout
      KeyboardData kd = expected.keyboard2.prefs.LayoutsPreference.layout_of_string(getResources(), layoutName);
      if (kd != null)
      {
        kd.resourceName = layoutName;
        if (_config.layouts == null)
        {
          _config.layouts = new java.util.ArrayList<KeyboardData>();
        }
        _config.layouts.add(kd);
        int targetIdx = _config.layouts.size() - 1;
        setTextLayout(targetIdx);
        expected.keyboard2.prefs.LayoutsPreference.save_keyboard_data_to_preferences(Config.globalPrefs().edit(), _config.layouts);
      }
      if (_keyboard_layout_view != null)
      {
        _keyboard_layout_view.setKeyboard(current_layout());
        _keyboard_layout_view.requestLayout();
        _keyboard_layout_view.invalidate();
      }
      setInputView(_keyboard_container_view);
    }

    @Override
    public java.util.List<KeyboardData> get_active_layouts()
    {
      return _config.layouts;
    }

    @Override
    public int get_current_layout_index()
    {
      return _config.get_current_layout();
    }

    @Override
    public void switch_to_theme_name(String themeName)
    {
      if (themeName == null || themeName.isEmpty()) return;
      SharedPreferences prefs = Config.globalPrefs();
      prefs.edit().putString("theme", themeName).apply();
      _config.theme = _config.getThemeId(getResources(), themeName);
      clearLayoutCaches();
      _layoutPane = null;
      _themePane = null;
      refresh_config();
      setInputView(_keyboard_container_view);
      applyFloatingModeLayout();
    }

    @Override
    public String get_current_theme_name()
    {
      return Config.globalPrefs().getString("theme", "frostedobsidian");
    }
  }

  private IBinder getConnectionToken()
  {
    return getWindow().getWindow().getAttributes().token;
  }

  private View inflate_view(int layout)
  {
    return View.inflate(new ContextThemeWrapper(this, _config.theme), layout, null);
  }
}
