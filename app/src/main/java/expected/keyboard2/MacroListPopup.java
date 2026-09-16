package expected.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.util.Map;

public class MacroListPopup {
  private PopupWindow popup;

  // Active theme colors
  private int itemTextColor = 0xFFFFFFFF;

  private Runnable onDismissCallback;

  public MacroListPopup(Context ctx) {}

  public void setOnDismiss(Runnable r) { this.onDismissCallback = r; }

  public void show(View anchor) {
    Context ctx = anchor.getContext();
    Theme kbTheme = null;
    if (anchor instanceof Keyboard2View) {
      kbTheme = ((Keyboard2View) anchor).getKeyboardTheme();
    }

    Config cfg = Config.globalConfig();
    int themeId = (cfg != null && cfg.theme != 0) ? cfg.theme : 0;
    if (themeId == 0) {
      try {
        SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(ctx);
        themeId = Config.getThemeId(ctx.getResources(), prefs.getString("theme", ""));
      } catch (Exception ignored) {}
    }
    Context themedCtx = (themeId != 0) ? new ContextThemeWrapper(ctx, themeId) : ctx;

    float density = themedCtx.getResources().getDisplayMetrics().density;
    float cornerRadius = 12f * density;

    int popupBgColor = 0xFF1B1B1B;
    int popupBorderColor = 0xFF333333;
    int textColor = 0xFFFFFFFF;

    if (kbTheme != null) {
      if (kbTheme.labelColor != 0) textColor = kbTheme.labelColor;
      if (kbTheme.colorKey != 0) popupBorderColor = kbTheme.colorKey;
      if (kbTheme.keyBorderRadius > 0) {
        cornerRadius = Math.max(6f * density, Math.min(14f * density, kbTheme.keyBorderRadius));
      }
    }

    int[] attrs = new int[] {
      R.attr.colorKeyboard,
      R.attr.colorKey,
      R.attr.colorLabel,
      R.attr.keyBorderRadius
    };
    TypedArray ta = null;
    try {
      ta = themedCtx.obtainStyledAttributes(attrs);
      int attrBg = ta.getColor(0, 0);
      if (attrBg != 0) popupBgColor = attrBg;
      int attrKey = ta.getColor(1, 0);
      if (attrKey != 0 && (kbTheme == null || kbTheme.colorKey == 0)) popupBorderColor = attrKey;
      int attrLabel = ta.getColor(2, 0);
      if (attrLabel != 0 && (kbTheme == null || kbTheme.labelColor == 0)) textColor = attrLabel;
      float rad = ta.getDimension(3, 0);
      if (rad > 0 && (kbTheme == null || kbTheme.keyBorderRadius <= 0)) {
        cornerRadius = Math.max(6f * density, Math.min(14f * density, rad));
      }
    } catch (Exception ignored) {
    } finally {
      if (ta != null) ta.recycle();
    }

    double bgLum = (0.299 * Color.red(popupBgColor) + 0.587 * Color.green(popupBgColor) + 0.114 * Color.blue(popupBgColor)) / 255.0;
    double tLum = (0.299 * Color.red(textColor) + 0.587 * Color.green(textColor) + 0.114 * Color.blue(textColor)) / 255.0;
    if (Math.abs(bgLum - tLum) < 0.28) {
      textColor = (bgLum > 0.5) ? 0xFF0F172A : 0xFFF8FAFC;
    }

    this.itemTextColor = textColor;

    View content = LayoutInflater.from(themedCtx).inflate(R.layout.popup_macro_list, null);

    GradientDrawable popupBg = new GradientDrawable();
    popupBg.setShape(GradientDrawable.RECTANGLE);
    popupBg.setColor(popupBgColor);
    popupBg.setCornerRadius(cornerRadius);

    int strokeWidth = Math.max(1, (int) (1.2f * density));
    int strokeColor = (popupBorderColor != 0 && popupBorderColor != popupBgColor)
        ? popupBorderColor
        : ((bgLum > 0.5) ? 0x22000000 : 0x33FFFFFF);
    popupBg.setStroke(strokeWidth, strokeColor);

    content.setBackground(popupBg);

    LinearLayout container = content.findViewById(R.id.popup_macros_container);
    container.removeAllViews();

    Map<String, String> macros = MacroManager.getMacros(ctx);
    if (macros == null || macros.isEmpty()) {
      TextView tv = new TextView(themedCtx);
      tv.setText("No macros added");
      tv.setTextColor(this.itemTextColor);
      tv.setPadding((int)(16*density), (int)(16*density), (int)(16*density), (int)(16*density));
      container.addView(tv);
    } else {
      for (Map.Entry<String, String> entry : macros.entrySet()) {
        TextView tv = new TextView(themedCtx);
        tv.setText(entry.getKey() + " ➡ " + entry.getValue());
        tv.setTextSize(14f);
        tv.setMaxLines(1);
        tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tv.setTextColor(this.itemTextColor);
        tv.setPadding((int)(16*density), (int)(8*density), (int)(16*density), (int)(8*density));
        container.addView(tv);
        
        View divider = new View(themedCtx);
        divider.setBackgroundColor(strokeColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, strokeWidth);
        container.addView(divider, lp);
      }
      if (container.getChildCount() > 0) {
        container.removeViewAt(container.getChildCount() - 1); // remove last divider
      }
    }

    final float[] downX = new float[1];
    final float[] downY = new float[1];
    final float touchSlop = android.view.ViewConfiguration.get(ctx).getScaledTouchSlop();
    android.view.View.OnTouchListener swipeListener = (v, event) -> {
      switch (event.getActionMasked()) {
        case android.view.MotionEvent.ACTION_DOWN:
          downX[0] = event.getRawX();
          downY[0] = event.getRawY();
          break;
        case android.view.MotionEvent.ACTION_MOVE:
        case android.view.MotionEvent.ACTION_UP:
          float dx = event.getRawX() - downX[0];
          float dy = event.getRawY() - downY[0];
          if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy)) {
            dismiss();
            return true;
          }
          break;
      }
      return false;
    };
    content.setOnTouchListener(swipeListener);
    android.widget.ScrollView scrollView = content.findViewById(R.id.popup_macros_scroll);
    if (scrollView != null) {
      scrollView.setOnTouchListener(swipeListener);
    }

    popup = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
    popup.setOutsideTouchable(true);
    popup.setBackgroundDrawable(null);
    popup.setElevation(16);
    popup.setClippingEnabled(false);
    popup.setAnimationStyle(android.R.style.Animation_Dialog);
    popup.setOnDismissListener(() -> { if (onDismissCallback != null) onDismissCallback.run(); });

    int screenWidth = anchor.getResources().getDisplayMetrics().widthPixels;
    int availableWidth = Math.min(screenWidth - 24, (int)(220 * density));

    content.measure(View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec((int)(200 * density), View.MeasureSpec.AT_MOST));
    
    int measuredW = content.getMeasuredWidth();
    int measuredH = content.getMeasuredHeight();
    if (measuredW <= 0) measuredW = (int)(150 * density);
    if (measuredH <= 0) measuredH = (int)(60 * density);

    int pw = Math.min(measuredW, availableWidth);
    int ph = Math.min(measuredH, (int)(200 * density));
    popup.setWidth(pw);
    popup.setHeight(ph);

    int anchorWidth = anchor.getWidth();
    if (anchorWidth <= 0) anchorWidth = screenWidth;

    int[] winLoc = new int[2];
    anchor.getLocationInWindow(winLoc);

    int localRowCenterY = (int)(anchor.getHeight() * 0.45f);
    int yInWindow = winLoc[1] + localRowCenterY - (ph / 2);
    int xInWindow = winLoc[0] + (anchorWidth / 2) - (pw / 2);

    if (xInWindow < 12) xInWindow = 12;
    if (xInWindow + pw > screenWidth - 12) xInWindow = screenWidth - pw - 12;
    if (yInWindow < 8) yInWindow = 8;

    try {
      popup.showAtLocation(anchor, Gravity.TOP | Gravity.START, xInWindow, yInWindow);
    } catch (Exception e) {
      android.util.Log.w("MacroListPopup", "Failed to show popup", e);
    }
  }

  public void dismiss() {
    if (popup != null && popup.isShowing()) {
      try {
        popup.dismiss();
      } catch (Exception ignored) {}
    }
    popup = null;
    if (onDismissCallback != null) onDismissCallback.run();
  }

  public boolean isShowing() { return popup != null && popup.isShowing(); }
}
