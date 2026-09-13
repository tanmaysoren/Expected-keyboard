package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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
import expected.keyboard2.Config;
import expected.keyboard2.R;

public class ColorPickerPreference extends DialogPreference {
  private int mColor;
  private int mDefaultColor = 0xFFFFFFFF;
  private View mPreviewDot;
  private View mDialogPreview;
  private SeekBar mSeekR, mSeekG, mSeekB;
  private TextView mHexText;

  public ColorPickerPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setDialogLayoutResource(0);
    setPositiveButtonText(android.R.string.ok);
    setNegativeButtonText(android.R.string.cancel);
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    // Add or find preview swatch in the preference row
    ViewGroup widgetFrame = (ViewGroup) view.findViewById(android.R.id.widget_frame);
    if (widgetFrame != null) {
      widgetFrame.setVisibility(View.VISIBLE);
      widgetFrame.removeAllViews();
      float dp = getContext().getResources().getDisplayMetrics().density;
      mPreviewDot = new View(getContext());
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((int)(26 * dp), (int)(26 * dp));
      lp.gravity = Gravity.CENTER;
      mPreviewDot.setLayoutParams(lp);
      updateSwatch(mPreviewDot, mColor);
      widgetFrame.addView(mPreviewDot);
    }
  }

  private void updateSwatch(View v, int color) {
    if (v == null) return;
    float dp = v.getContext().getResources().getDisplayMetrics().density;
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.OVAL);
    gd.setColor(color);
    gd.setStroke((int)(2 * dp), 0xFFFFFFFF);
    v.setBackground(gd);
  }

  @Override
  protected View onCreateDialogView() {
    Context context = getContext();
    float dp = context.getResources().getDisplayMetrics().density;

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding((int)(20 * dp), (int)(16 * dp), (int)(20 * dp), (int)(16 * dp));
    root.setBackgroundColor(0xFF0F172A);

    // Color Preview Box
    mDialogPreview = new View(context);
    LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int)(48 * dp));
    prevLp.bottomMargin = (int)(12 * dp);
    mDialogPreview.setLayoutParams(prevLp);
    updatePreviewBox(mColor);
    root.addView(mDialogPreview);

    // Hex Code Readout
    mHexText = new TextView(context);
    mHexText.setText(String.format("#%06X", (0xFFFFFF & mColor)));
    mHexText.setTextColor(0xFFF8FAFC);
    mHexText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    mHexText.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    mHexText.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams hexLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    hexLp.bottomMargin = (int)(16 * dp);
    mHexText.setLayoutParams(hexLp);
    root.addView(mHexText);

    // RGB Sliders
    int r = Color.red(mColor);
    int g = Color.green(mColor);
    int b = Color.blue(mColor);

    mSeekR = createColorChannelRow(root, context, "Red", 0xFFEF4444, r, dp);
    mSeekG = createColorChannelRow(root, context, "Green", 0xFF10B981, g, dp);
    mSeekB = createColorChannelRow(root, context, "Blue", 0xFF3B82F6, b, dp);

    SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int red = mSeekR.getProgress();
        int green = mSeekG.getProgress();
        int blue = mSeekB.getProgress();
        int cur = Color.rgb(red, green, blue);
        updatePreviewBox(cur);
        mHexText.setText(String.format("#%06X", (0xFFFFFF & cur)));
      }
      @Override public void onStartTrackingTouch(SeekBar seekBar) {}
      @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    };

    mSeekR.setOnSeekBarChangeListener(listener);
    mSeekG.setOnSeekBarChangeListener(listener);
    mSeekB.setOnSeekBarChangeListener(listener);

    // Preset Swatches Row
    LinearLayout presets = new LinearLayout(context);
    presets.setOrientation(LinearLayout.HORIZONTAL);
    presets.setGravity(Gravity.CENTER);
    presets.setPadding(0, (int)(12 * dp), 0, 0);

    int[] palette = new int[] {
      0xFFFFFFFF, 0xFF000000, 0xFF6366F1, 0xFF00F0FF,
      0xFFFF007F, 0xFF10B981, 0xFFF59E0B, 0xFF8B5CF6,
      0xFF1E2235, 0xFF0D111E
    };

    for (int pColor : palette) {
      View swatch = new View(context);
      LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams((int)(24 * dp), (int)(24 * dp));
      slp.setMargins((int)(3 * dp), 0, (int)(3 * dp), 0);
      swatch.setLayoutParams(slp);
      GradientDrawable sgd = new GradientDrawable();
      sgd.setShape(GradientDrawable.OVAL);
      sgd.setColor(pColor);
      sgd.setStroke((int)(1.5f * dp), 0xFF64748B);
      swatch.setBackground(sgd);
      swatch.setOnClickListener(v -> {
        mSeekR.setProgress(Color.red(pColor));
        mSeekG.setProgress(Color.green(pColor));
        mSeekB.setProgress(Color.blue(pColor));
      });
      presets.addView(swatch);
    }
    root.addView(presets);

    return root;
  }

  private void updatePreviewBox(int color) {
    if (mDialogPreview == null) return;
    float dp = getContext().getResources().getDisplayMetrics().density;
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.RECTANGLE);
    gd.setCornerRadius(8 * dp);
    gd.setColor(color);
    gd.setStroke((int)(2 * dp), 0xFF64748B);
    mDialogPreview.setBackground(gd);
  }

  private SeekBar createColorChannelRow(LinearLayout parent, Context context, String label, int labelColor, int initialProgress, float dp) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0, (int)(4 * dp), 0, (int)(4 * dp));

    TextView txt = new TextView(context);
    txt.setText(label);
    txt.setTextColor(labelColor);
    txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
    txt.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
    LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams((int)(46 * dp), ViewGroup.LayoutParams.WRAP_CONTENT);
    txt.setLayoutParams(tlp);
    row.addView(txt);

    SeekBar seek = new SeekBar(context);
    seek.setMax(255);
    seek.setProgress(initialProgress);
    LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
    seek.setLayoutParams(slp);
    row.addView(seek);

    parent.addView(row);
    return seek;
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    if (positiveResult && mSeekR != null && mSeekG != null && mSeekB != null) {
      int r = mSeekR.getProgress();
      int g = mSeekG.getProgress();
      int b = mSeekB.getProgress();
      int val = Color.rgb(r, g, b);
      if (callChangeListener(val)) {
        mColor = val;
        persistInt(val);
        updateSwatch(mPreviewDot, mColor);
      }
    }
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getInt(index, mDefaultColor);
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    if (restorePersistedValue) {
      mColor = getPersistedInt(mDefaultColor);
    } else {
      mColor = (Integer) defaultValue;
      persistInt(mColor);
    }
    updateSwatch(mPreviewDot, mColor);
  }
}
