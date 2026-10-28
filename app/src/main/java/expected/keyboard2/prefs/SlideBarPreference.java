package expected.keyboard2.prefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.DialogPreference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import expected.keyboard2.R;

/*
 ** SlideBarPreference - Capsule Stepper / Rail Slider Edition
 ** -
 ** The dialog view is built lazily (only when the dialog is opened) so the
 ** settings menu itself inflates quickly. The control is a single capsule with
 ** [-] value [+] inside it, followed by a slim rail slider and preset chips.
 */
public class SlideBarPreference extends DialogPreference
  implements SeekBar.OnSeekBarChangeListener
{
  private static final int STEPS = 100;

  private static final ColorStateList STEP_GLYPH_COLORS = new ColorStateList(
      new int[][] { new int[] { android.R.attr.state_pressed }, new int[0] },
      new int[] { 0xFF000000, 0xFFFFFFFF });

  private LinearLayout _layout;
  private TextView _valueBadge;
  private SeekBar _seekBar;
  private TextView _btnDec;
  private TextView _btnInc;

  private float _min;
  private float _max;
  private float _value;
  private float _defaultVal;

  private String _initialSummary;

  public SlideBarPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _initialSummary = getSummary() != null ? getSummary().toString() : "%s";
    setDialogLayoutResource(0);

    _min = float_of_string(attrs.getAttributeValue(null, "min"));
    _value = _min;
    _max = Math.max(1f, float_of_string(attrs.getAttributeValue(null, "max")));
    _defaultVal = float_of_string(attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "defaultValue"));
  }

  private void buildUi(Context context)
  {
    float dp = context.getResources().getDisplayMetrics().density;

    _layout = new LinearLayout(context);
    _layout.setOrientation(LinearLayout.VERTICAL);
    _layout.setPadding((int)(18 * dp), (int)(18 * dp), (int)(18 * dp), (int)(14 * dp));

    // 1. Header
    TextView titleText = new TextView(context);
    titleText.setText(getTitle() != null ? getTitle() : "VALUE");
    titleText.setTextColor(0xFFFFFFFF);
    titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    titleText.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    _layout.addView(titleText);

    // 2. One capsule: [-]  value  [+]
    LinearLayout capsule = new LinearLayout(context);
    capsule.setOrientation(LinearLayout.HORIZONTAL);
    capsule.setGravity(Gravity.CENTER_VERTICAL);
    capsule.setBackgroundResource(R.drawable.cyber_stepper_capsule);
    LinearLayout.LayoutParams capsuleLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, (int)(40 * dp));
    capsuleLp.topMargin = (int)(14 * dp);
    _layout.addView(capsule, capsuleLp);

    _btnDec = createStepButton(context, "\u2212");
    _btnDec.setContentDescription("Decrease");
    _btnDec.setBackgroundResource(R.drawable.cyber_stepper_left);
    _btnDec.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int current = _seekBar.getProgress();
        _seekBar.setProgress(Math.max(0, current - 5));
      }
    });
    capsule.addView(_btnDec, new LinearLayout.LayoutParams((int)(40 * dp),
        ViewGroup.LayoutParams.MATCH_PARENT));

    capsule.addView(createDivider(context, dp));

    _valueBadge = new TextView(context);
    _valueBadge.setTextColor(0xFFFFFFFF);
    _valueBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    _valueBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    _valueBadge.setGravity(Gravity.CENTER);
    _valueBadge.setSingleLine(true);
    _valueBadge.setEllipsize(TextUtils.TruncateAt.END);
    _valueBadge.setPadding((int)(12 * dp), 0, (int)(12 * dp), 0);
    capsule.addView(_valueBadge, new LinearLayout.LayoutParams(0,
        ViewGroup.LayoutParams.MATCH_PARENT, 1.0f));

    capsule.addView(createDivider(context, dp));

    _btnInc = createStepButton(context, "+");
    _btnInc.setContentDescription("Increase");
    _btnInc.setBackgroundResource(R.drawable.cyber_stepper_right);
    _btnInc.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int current = _seekBar.getProgress();
        _seekBar.setProgress(Math.min(STEPS, current + 5));
      }
    });
    capsule.addView(_btnInc, new LinearLayout.LayoutParams((int)(40 * dp),
        ViewGroup.LayoutParams.MATCH_PARENT));

    // 3. Slim rail slider
    _seekBar = new SeekBar(context);
    _seekBar.setProgressDrawable(context.getDrawable(R.drawable.cyber_seekbar_progress));
    _seekBar.setThumb(context.getDrawable(R.drawable.cyber_seekbar_thumb));
    _seekBar.setMax(STEPS);
    // Leave the default thumb offset (half the thumb width). AbsSeekBar uses
    // it to keep the thumb centre exactly on the progress point, so the thumb
    // stays under the finger and flush with both ends of the rail.
    _seekBar.setPadding((int)(12 * dp), 0, (int)(12 * dp), 0);
    _seekBar.setOnSeekBarChangeListener(this);
    LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, (int)(44 * dp));
    seekLp.topMargin = (int)(8 * dp);
    _layout.addView(_seekBar, seekLp);

    // 4. Range labels
    LinearLayout rangeRow = new LinearLayout(context);
    rangeRow.setOrientation(LinearLayout.HORIZONTAL);
    rangeRow.setGravity(Gravity.CENTER_VERTICAL);
    rangeRow.setPadding((int)(6 * dp), 0, (int)(6 * dp), 0);
    rangeRow.addView(createRangeLabel(context, "MIN " + formatValue(_min), dp));
    rangeRow.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1.0f));
    rangeRow.addView(createRangeLabel(context, "MAX " + formatValue(_max), dp));
    _layout.addView(rangeRow);

    // 5. Quick preset chips
    LinearLayout presetsRow = new LinearLayout(context);
    presetsRow.setOrientation(LinearLayout.HORIZONTAL);
    presetsRow.setGravity(Gravity.CENTER);
    presetsRow.setPadding(0, (int)(12 * dp), 0, 0);

    Button btnMin = createPresetButton(context, "MIN " + formatValue(_min), 0, dp);
    Button btnDefault = createPresetButton(context, "DEFAULT " + formatValue(_defaultVal),
        progressOf(_defaultVal), dp);
    Button btnMax = createPresetButton(context, "MAX " + formatValue(_max), STEPS, dp);

    presetsRow.addView(btnMin);
    presetsRow.addView(btnDefault);
    presetsRow.addView(btnMax);

    _layout.addView(presetsRow);
  }

  private TextView createStepButton(Context context, String glyph)
  {
    TextView btn = new TextView(context);
    btn.setText(glyph);
    btn.setTextColor(STEP_GLYPH_COLORS);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
    btn.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    btn.setGravity(Gravity.CENTER);
    btn.setClickable(true);
    btn.setFocusable(true);
    return btn;
  }

  private View createDivider(Context context, float dp)
  {
    View divider = new View(context);
    divider.setBackgroundColor(0xFF2E2E2E);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)(1 * dp), (int)(22 * dp));
    lp.gravity = Gravity.CENTER_VERTICAL;
    divider.setLayoutParams(lp);
    return divider;
  }

  private TextView createRangeLabel(Context context, String text, float dp)
  {
    TextView label = new TextView(context);
    label.setText(text);
    label.setTextColor(0xFF737373);
    label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    label.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
    label.setLetterSpacing(0.04f);
    return label;
  }

  private Button createPresetButton(Context context, String label, final int targetProgress, float dp)
  {
    Button btn = new Button(context);
    btn.setText(label);
    btn.setTextColor(0xFFD4D4D4);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
    btn.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
    btn.setAllCaps(false);
    btn.setLetterSpacing(0.02f);
    btn.setBackgroundResource(R.drawable.cyber_btn_chip);
    btn.setMinWidth(0);
    btn.setMinimumWidth(0);
    btn.setMinHeight(0);
    btn.setMinimumHeight(0);
    btn.setPadding((int)(6 * dp), 0, (int)(6 * dp), 0);
    btn.setStateListAnimator(null);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int)(34 * dp), 1.0f);
    lp.setMargins((int)(3 * dp), 0, (int)(3 * dp), 0);
    btn.setLayoutParams(lp);
    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        _seekBar.setProgress(Math.max(0, Math.min(STEPS, targetProgress)));
      }
    });
    return btn;
  }

  private int progressOf(float value)
  {
    if (_max <= _min)
      return 0;
    int progress = Math.round((value - _min) * STEPS / (_max - _min));
    return Math.max(0, Math.min(STEPS, progress));
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
  {
    _value = Math.round(progress * (_max - _min)) / (float)STEPS + _min;
    updateText();
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar)
  {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar)
  {
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
  {
    if (restorePersistedValue)
    {
      _value = getPersistedFloat(_min);
    }
    else
    {
      _value = (Float)defaultValue;
      persistFloat(_value);
    }
    if (_seekBar != null)
      _seekBar.setProgress(progressOf(_value));
    updateText();
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index)
  {
    return (a.getFloat(index, _min));
  }

  @Override
  protected void onDialogClosed(boolean positiveResult)
  {
    if (positiveResult)
    {
      persistFloat(_value);
    }
    else
    {
      _value = getPersistedFloat(_min);
      if (_seekBar != null)
        _seekBar.setProgress(progressOf(_value));
    }

    updateText();
  }

  @Override
  protected View onCreateDialogView()
  {
    if (_layout == null)
      buildUi(getContext());

    ViewGroup parent = (ViewGroup)_layout.getParent();
    if (parent != null)
      parent.removeView(_layout);
    return (_layout);
  }

  @Override
  protected void onBindDialogView(View view)
  {
    super.onBindDialogView(view);
    if (_seekBar != null)
      _seekBar.setProgress(progressOf(_value));
    updateText();
  }

  private void updateText()
  {
    String full = String.format(_initialSummary, _value);
    if (_valueBadge != null)
      _valueBadge.setText(formatValue(_value));
    setSummary(full);
  }

  private String formatValue(float value)
  {
    return trimNumber(value) + unitSuffix();
  }

  private String unitSuffix()
  {
    int index = _initialSummary.indexOf("%s");
    if (index >= 0)
      return _initialSummary.substring(index + 2).replace("%%", "%");
    return "\u00d7";
  }

  private static String trimNumber(float value)
  {
    if (value == Math.rint(value))
      return String.valueOf((int)value);

    String text = String.format("%.2f", value);
    while (text.endsWith("0"))
      text = text.substring(0, text.length() - 1);
    if (text.endsWith("."))
      text = text.substring(0, text.length() - 1);
    return text;
  }

  private static float float_of_string(String str)
  {
    if (str == null)
      return (0f);
    try
    {
      return (Float.parseFloat(str));
    }
    catch (NumberFormatException e)
    {
      return (0f);
    }
  }
}
