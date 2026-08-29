package expected.keyboard2.prefs;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.preference.DialogPreference;
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
 ** SlideBarPreference - Frosted Obsidian / Glassmorphic Edition
 ** -
 ** Interactive Frosted Obsidian slider dialog with glowing pill badge readout,
 ** tactile [-] [+] stepper buttons, and glass preset chips.
 */
public class SlideBarPreference extends DialogPreference
  implements SeekBar.OnSeekBarChangeListener
{
  private static final int STEPS = 100;

  private LinearLayout _layout;
  private TextView _valueBadge;
  private SeekBar _seekBar;
  private Button _btnDec;
  private Button _btnInc;

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

    buildGlassUi(context);
  }

  private void buildGlassUi(Context context)
  {
    float dp = context.getResources().getDisplayMetrics().density;

    _layout = new LinearLayout(context);
    _layout.setOrientation(LinearLayout.VERTICAL);
    _layout.setPadding((int)(16 * dp), (int)(16 * dp), (int)(16 * dp), (int)(12 * dp));

    // 1. Header with Title & Glowing Pill Readout Badge
    LinearLayout headerLayout = new LinearLayout(context);
    headerLayout.setOrientation(LinearLayout.HORIZONTAL);
    headerLayout.setGravity(Gravity.CENTER_VERTICAL);
    headerLayout.setPadding(0, 0, 0, (int)(16 * dp));

    TextView titleText = new TextView(context);
    titleText.setText(getTitle() != null ? getTitle() : "VALUE");
    titleText.setTextColor(0xFFE2E8F0);
    titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
    titleText.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    headerLayout.addView(titleText, titleLp);

    _valueBadge = new TextView(context);
    _valueBadge.setTextColor(0xFFC4B5FD);
    _valueBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
    _valueBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    _valueBadge.setGravity(Gravity.CENTER);
    _valueBadge.setBackgroundResource(R.drawable.cyber_badge_bg);
    _valueBadge.setPadding((int)(14 * dp), (int)(5 * dp), (int)(14 * dp), (int)(5 * dp));
    headerLayout.addView(_valueBadge);

    _layout.addView(headerLayout);

    // 2. Interactive Range Control Row ( [-] | ====O==== | [+] )
    LinearLayout sliderRow = new LinearLayout(context);
    sliderRow.setOrientation(LinearLayout.HORIZONTAL);
    sliderRow.setGravity(Gravity.CENTER_VERTICAL);
    sliderRow.setPadding(0, (int)(6 * dp), 0, (int)(8 * dp));

    _btnDec = new Button(context);
    _btnDec.setText("−");
    _btnDec.setTextColor(0xFFC4B5FD);
    _btnDec.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    _btnDec.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    _btnDec.setBackgroundResource(R.drawable.cyber_btn_step);
    LinearLayout.LayoutParams btnDecLp = new LinearLayout.LayoutParams((int)(38 * dp), (int)(38 * dp));
    _btnDec.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int current = _seekBar.getProgress();
        _seekBar.setProgress(Math.max(0, current - 5));
      }
    });
    sliderRow.addView(_btnDec, btnDecLp);

    _seekBar = new SeekBar(context);
    _seekBar.setProgressDrawable(context.getDrawable(R.drawable.cyber_seekbar_progress));
    _seekBar.setThumb(context.getDrawable(R.drawable.cyber_seekbar_thumb));
    _seekBar.setMax(STEPS);
    _seekBar.setPadding((int)(16 * dp), 0, (int)(16 * dp), 0);
    _seekBar.setOnSeekBarChangeListener(this);
    LinearLayout.LayoutParams seekLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    sliderRow.addView(_seekBar, seekLp);

    _btnInc = new Button(context);
    _btnInc.setText("+");
    _btnInc.setTextColor(0xFFC4B5FD);
    _btnInc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    _btnInc.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    _btnInc.setBackgroundResource(R.drawable.cyber_btn_step);
    LinearLayout.LayoutParams btnIncLp = new LinearLayout.LayoutParams((int)(38 * dp), (int)(38 * dp));
    _btnInc.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int current = _seekBar.getProgress();
        _seekBar.setProgress(Math.min(STEPS, current + 5));
      }
    });
    sliderRow.addView(_btnInc, btnIncLp);

    _layout.addView(sliderRow);

    // 3. Quick Preset Action Chips Row
    LinearLayout presetsRow = new LinearLayout(context);
    presetsRow.setOrientation(LinearLayout.HORIZONTAL);
    presetsRow.setGravity(Gravity.CENTER);
    presetsRow.setPadding(0, (int)(12 * dp), 0, 0);

    Button btnMin = createPresetButton(context, String.format("Min: %.1f", _min), 0, dp);
    Button btnDefault = createPresetButton(context, String.format("Default: %.2f", _defaultVal), (int)((_defaultVal - _min) * STEPS / (_max - _min)), dp);
    Button btnMax = createPresetButton(context, String.format("Max: %.1f", _max), STEPS, dp);

    presetsRow.addView(btnMin);
    presetsRow.addView(btnDefault);
    presetsRow.addView(btnMax);

    _layout.addView(presetsRow);
  }

  private Button createPresetButton(Context context, String label, final int targetProgress, float dp)
  {
    Button btn = new Button(context);
    btn.setText(label);
    btn.setTextColor(0xFF94A3B8);
    btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    btn.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
    btn.setBackgroundResource(R.drawable.cyber_btn_chip);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int)(32 * dp));
    lp.setMargins((int)(4 * dp), 0, (int)(4 * dp), 0);
    btn.setLayoutParams(lp);
    btn.setPadding((int)(10 * dp), 0, (int)(10 * dp), 0);
    btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        _seekBar.setProgress(Math.max(0, Math.min(STEPS, targetProgress)));
      }
    });
    return btn;
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
    _seekBar.setProgress((int)((_value - _min) * STEPS / (_max - _min)));
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
      persistFloat(_value);
    else
      _seekBar.setProgress((int)((getPersistedFloat(_min) - _min) * STEPS / (_max - _min)));

    updateText();
  }

  @Override
  protected View onCreateDialogView()
  {
    ViewGroup parent = (ViewGroup)_layout.getParent();
    if (parent != null)
      parent.removeView(_layout);
    return (_layout);
  }

  private void updateText()
  {
    String f = String.format(_initialSummary, _value);
    if (_valueBadge != null)
      _valueBadge.setText(f);
    setSummary(f);
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
