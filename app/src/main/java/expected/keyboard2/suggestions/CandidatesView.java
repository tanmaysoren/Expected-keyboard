package expected.keyboard2.suggestions;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build.VERSION;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;
import expected.keyboard2.Config;
import expected.keyboard2.KeyEventHandler;
import expected.keyboard2.KeyValue;
import expected.keyboard2.Pointers;
import expected.keyboard2.R;
import expected.keyboard2.VibratorCompat;

public class CandidatesView extends LinearLayout
{
  public static final int NUM_WORDS = 5;
  static final int NUM_CANDIDATES = NUM_WORDS + 1;
  private static final int ANIMATION_DURATION = 150;

  /** Candidates currently visible. Entries can be [null] when there are less
      than [NUM_CANDIDATES] suggestions.
      - Entries at indexes [0] to [4] are word suggestions.
      - Entry at index [5] is the emoji suggestion. */
  String[] _items = new String[NUM_CANDIDATES];

  /** Text views showing the candidates in [_items]. */
  TextView[] _item_views = new TextView[NUM_CANDIDATES];

  /** Containers for suggestions vs utility action bar */
  private View _suggestionsContainer;
  private View _utilityBarContainer;
  private ImageButton _btnToolsToggle;

  private boolean _utilityBarActive = false;
  private boolean _userManualToggle = false;

  /** Message when no dictionary is installed. Visible when no candidates are
      shown. Might be [null]. */
  View _status_no_dict = null;

  public CandidatesView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();

    _suggestionsContainer = findViewById(R.id.suggestions_container);
    _utilityBarContainer = findViewById(R.id.utility_bar_container);
    _btnToolsToggle = findViewById(R.id.btn_tools_toggle);

    setup_item_view(0, R.id.candidates_1);
    setup_item_view(1, R.id.candidates_2);
    setup_item_view(2, R.id.candidates_3);
    setup_item_view(3, R.id.candidates_4);
    setup_item_view(4, R.id.candidates_5);
    setup_item_view(5, R.id.candidates_emoji);
    setup_utility_bar();

    if (_btnToolsToggle != null)
    {
      _btnToolsToggle.setOnClickListener(v -> {
        VibratorCompat.vibrate(v);
        _userManualToggle = true;
        toggleUtilityBarWithAnimation(!_utilityBarActive);
      });
    }
  }

  private void toggleUtilityBarWithAnimation(boolean active)
  {
    _utilityBarActive = active;
    if (_suggestionsContainer == null || _utilityBarContainer == null) return;

    if (_utilityBarActive)
    {
      _utilityBarContainer.setVisibility(View.VISIBLE);
      _utilityBarContainer.setAlpha(0f);
      _utilityBarContainer.setTranslationX(50f);

      _suggestionsContainer.animate()
          .alpha(0f)
          .translationX(-50f)
          .setDuration(ANIMATION_DURATION)
          .setInterpolator(new DecelerateInterpolator())
          .withEndAction(() -> {
            _suggestionsContainer.setVisibility(View.GONE);
            _suggestionsContainer.setTranslationX(0f);
            _utilityBarContainer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .start();
          }).start();

      if (_btnToolsToggle != null)
      {
        _btnToolsToggle.setImageResource(R.drawable.ic_futo_chevron_left);
      }
    }
    else
    {
      _suggestionsContainer.setVisibility(View.VISIBLE);
      _suggestionsContainer.setAlpha(0f);
      _suggestionsContainer.setTranslationX(-50f);

      _utilityBarContainer.animate()
          .alpha(0f)
          .translationX(50f)
          .setDuration(ANIMATION_DURATION)
          .setInterpolator(new DecelerateInterpolator())
          .withEndAction(() -> {
            _utilityBarContainer.setVisibility(View.GONE);
            _utilityBarContainer.setTranslationX(0f);
            _suggestionsContainer.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .start();
          }).start();

      if (_btnToolsToggle != null)
      {
        _btnToolsToggle.setImageResource(R.drawable.ic_futo_chevron_right);
      }
    }
  }

  public void setUtilityBarActive(boolean active)
  {
    _utilityBarActive = active;
    if (_suggestionsContainer != null && _utilityBarContainer != null)
    {
      if (_utilityBarActive)
      {
        _suggestionsContainer.setVisibility(View.GONE);
        _utilityBarContainer.setVisibility(View.VISIBLE);
        if (_btnToolsToggle != null)
        {
          _btnToolsToggle.setImageResource(R.drawable.ic_futo_chevron_left);
        }
      }
      else
      {
        _utilityBarContainer.setVisibility(View.GONE);
        _suggestionsContainer.setVisibility(View.VISIBLE);
        if (_btnToolsToggle != null)
        {
          _btnToolsToggle.setImageResource(R.drawable.ic_futo_chevron_right);
        }
      }
    }
  }

  private void setup_utility_bar()
  {
    setupUtilButton(R.id.util_btn_clipboard, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("switch_clipboard"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_edit, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("switch_editing"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_undo, () -> {
      KeyEventHandler handler = (KeyEventHandler) Config.globalConfig().handler;
      if (handler != null) handler.handle_editing_key(KeyValue.Editing.UNDO);
    });

    setupUtilButton(R.id.util_btn_redo, () -> {
      KeyEventHandler handler = (KeyEventHandler) Config.globalConfig().handler;
      if (handler != null) handler.handle_editing_key(KeyValue.Editing.REDO);
    });

    setupUtilButton(R.id.util_btn_numpad, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("switch_numeric"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_emoji, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("switch_emoji"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_layout, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("switch_layout"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_theme, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("switch_theme"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_floating, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("toggle_floating"), Pointers.Modifiers.EMPTY);
    });

    setupUtilButton(R.id.util_btn_settings, () -> {
      Config.globalConfig().handler.key_up(
          KeyValue.getKeyByName("config"), Pointers.Modifiers.EMPTY);
    });
  }

  private void setupUtilButton(int resId, Runnable action)
  {
    View v = findViewById(resId);
    if (v != null)
    {
      v.setOnClickListener(view -> {
        VibratorCompat.vibrate(view);
        action.run();
      });
    }
  }

  public void set_candidates(Suggestions s)
  {
    int s_count = s.count;
    for (int i = 0; i < NUM_WORDS; i++)
      _items[i] = (i < s_count) ? s.suggestions[i] : null;
    _items[NUM_WORDS] = s.emoji_suggestion;

    // If user is typing words, return automatically to suggestions mode
    if (s_count > 0 && !_userManualToggle && _utilityBarActive)
    {
      setUtilityBarActive(false);
    }

    // Hide the status message when showing candidates.
    if (s_count != 0 && _status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);

    for (int i = 0; i < _item_views.length; i++)
    {
      TextView v = _item_views[i];
      if (v != null)
      {
        if (_items[i] != null)
        {
          String newText = _items[i];
          boolean textChanged = !newText.equals(v.getText().toString());
          v.setText(newText);

          if (s.best_autocorrect != null && s.best_autocorrect.equals(_items[i]))
          {
            v.setTypeface(null, android.graphics.Typeface.BOLD);
          }
          else
          {
            v.setTypeface(null, android.graphics.Typeface.NORMAL);
          }

          if (v.getVisibility() != View.VISIBLE)
          {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(0f);
            v.setTranslationY(10f);
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(ANIMATION_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .start();
          }
          else if (textChanged)
          {
            v.setAlpha(0.7f);
            v.animate()
                .alpha(1f)
                .setDuration(80)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
          }
        }
        else
        {
          if (v.getVisibility() == View.VISIBLE)
          {
            v.animate()
                .alpha(0f)
                .setDuration(80)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> v.setVisibility(View.GONE))
                .start();
          }
        }
      }
    }
  }

  void clear_candidates()
  {
    for (int i = 0; i < _item_views.length; i++)
    {
      _items[i] = null;
      if (_item_views[i] != null)
        _item_views[i].setVisibility(View.GONE);
    }
  }

  public void refresh_config(Config config)
  {
    clear_candidates();
    _userManualToggle = false;
    if (_status_no_dict != null)
      _status_no_dict.setVisibility(View.GONE);
    set_sizes(config);
  }

  /** Set the height of the suggestion row and the text size. */
  public void set_sizes(Config config)
  {
    // Make the candidates view about as high as a keyboard row.
    float row_height = config.keyboard_rows_height_pixels * (1 - config.key_vertical_margin);
    ViewGroup.MarginLayoutParams p =
      (ViewGroup.MarginLayoutParams)getLayoutParams();
    if (p != null)
    {
      p.height = (int)row_height;
      setLayoutParams(p);
    }
    // Match the size of labels on the keyboard, scaled by suggestion font preference.
    float text_size = row_height * config.characterSize * config.labelTextSize * config.suggestionFontScale;
    for (int i = 0; i < NUM_CANDIDATES; i++)
    {
      TextView v = _item_views[i];
      if (v != null)
      {
        // Set text size and enable auto size if supported.
        if (VERSION.SDK_INT < 26)
          v.setTextSize(TypedValue.COMPLEX_UNIT_PX, text_size * 0.9f);
        else
          v.setAutoSizeTextTypeUniformWithConfiguration(
              (int)(text_size * 0.35f), (int)text_size, 1, TypedValue.COMPLEX_UNIT_PX);
      }
    }
  }

  void inflate_status_no_dict(Config config)
  {
    if (_status_no_dict == null)
    {
      _status_no_dict = View.inflate(getContext(),
          R.layout.candidates_status_no_dict, null);
      if (_suggestionsContainer instanceof ViewGroup)
      {
        ((ViewGroup)_suggestionsContainer).addView(_status_no_dict);
      }
      else
      {
        addView(_status_no_dict);
      }
    }
    Locale current_locale = (config.device_locales.default_ != null) ?
      Locale.forLanguageTag(config.device_locales.default_.lang_tag) : null;
    TextView tv = _status_no_dict.findViewById(android.R.id.text1);
    if (tv != null && current_locale != null)
      tv.setText(getResources().getString(
            R.string.candidates_status_click_to_install,
            current_locale.getDisplayName()));
    _status_no_dict.setVisibility(View.VISIBLE);
  }

  private void setup_item_view(final int item_index, int item_id)
  {
    TextView v = (TextView)findViewById(item_id);
    if (v != null)
    {
      v.setOnClickListener(new View.OnClickListener()
          {
            @Override
            public void onClick(View _v)
            {
              String it = _items[item_index];
              if (it != null)
                Config.globalConfig().handler.suggestion_entered(it);
            }
          });
      v.setVisibility(View.GONE);
    }
    _item_views[item_index] = v;
  }

  /** Whether the candidates view should be shown for a given editor. */
  public static boolean should_show(EditorInfo info)
  {
    if (info == null)
      return true;
    int inputClass = info.inputType & InputType.TYPE_MASK_CLASS;
    int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
    int flags = info.inputType & InputType.TYPE_MASK_FLAGS;

    // Terminal apps (such as Termux, ConnectBot, JuiceSSH, Emacs) use TYPE_NULL
    if (inputClass == InputType.TYPE_NULL)
    {
      return true;
    }

    switch (inputClass)
    {
      case InputType.TYPE_CLASS_TEXT:
        switch (variation)
        {
          case InputType.TYPE_TEXT_VARIATION_PASSWORD:
          case InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:
          case InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD:
            return false;
          default:
            /* Editor requested that we don't show suggestions. Enable
               suggestions anyway when the flags [NO_SUGGESTIONS] and
               [AUTO_CORRECT] are present at the same time. This happens with
               Google Keep. */
            if ((flags &
                  (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                   | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT))
                == InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
              return false;
            return true;
        }
      case InputType.TYPE_CLASS_NUMBER:
      case InputType.TYPE_CLASS_PHONE:
      case InputType.TYPE_CLASS_DATETIME:
        // Beware of TYPE_NUMBER_VARIATION_PASSWORD
        return false;
      default: return true;
    }
  }
}
