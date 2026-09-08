package expected.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DotExtensionPopup {
  private PopupWindow popup;
  private View anchorView;
  private List<String> extensions;
  private TextView selectedView = null;
  private String selectedText = null;
  private LinearLayout container;
  private HorizontalScrollView scrollView;

  // Active theme colors
  private int itemTextColor = 0xFFFFFFFF;
  private int selectedBgColor = 0xFF3B82F6;
  private int selectedTextColor = 0xFFFFFFFF;
  private float itemCornerRadius = 12f;

  public interface Callback {
    void onExtensionSelected(String ext);
  }

  private Callback callback;
  private Runnable onDismissCallback;

  public DotExtensionPopup(Context ctx) {
  }

  public void setOnDismiss(Runnable r) { this.onDismissCallback = r; }

  public void show(View anchor, Rect keyRect, List<String> exts, Callback cb) {
    show(anchor, null, keyRect, exts, cb);
  }

  private Drawable makeUnselectedItemBg() {
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.RECTANGLE);
    gd.setColor(Color.TRANSPARENT);
    gd.setCornerRadius(itemCornerRadius);
    return gd;
  }

  private Drawable makeSelectedItemBg() {
    GradientDrawable gd = new GradientDrawable();
    gd.setShape(GradientDrawable.RECTANGLE);
    gd.setColor(selectedBgColor);
    gd.setCornerRadius(itemCornerRadius);
    return gd;
  }

  public void show(View anchor, Rect asdfRowRect, Rect keyRect, List<String> exts, Callback cb) {
    this.anchorView = anchor;
    this.extensions = exts;
    this.callback = cb;
    this.selectedView = null;
    this.selectedText = null;

    Context ctx = anchor.getContext();
    Theme kbTheme = null;
    if (anchor instanceof Keyboard2View) {
      kbTheme = ((Keyboard2View) anchor).getKeyboardTheme();
    }

    // Resolve active keyboard theme so colors match keyboard 100%
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
    itemCornerRadius = 12f * density;

    int popupBgColor = 0xFF1B1B1B;
    int popupBorderColor = 0xFF333333;
    int textColor = 0xFFFFFFFF;
    int selBg = 0xFF3B82F6;

    if (kbTheme != null) {
      if (kbTheme.labelColor != 0) textColor = kbTheme.labelColor;
      if (kbTheme.colorKey != 0) popupBorderColor = kbTheme.colorKey;
      if (kbTheme.keyBorderRadius > 0) {
        itemCornerRadius = Math.max(6f * density, Math.min(14f * density, kbTheme.keyBorderRadius));
      }
      if (kbTheme.colorKeyAction != 0 && kbTheme.colorKeyAction != kbTheme.colorKey) {
        selBg = kbTheme.colorKeyAction;
      } else if (kbTheme.colorKeyActivated != 0 && kbTheme.colorKeyActivated != kbTheme.colorKey) {
        selBg = kbTheme.colorKeyActivated;
      } else if (kbTheme.activatedColor != 0) {
        selBg = kbTheme.activatedColor;
      }
    }

    int[] attrs = new int[] {
      R.attr.colorKeyboard,
      R.attr.colorKey,
      R.attr.colorLabel,
      R.attr.colorKeyAction,
      R.attr.colorKeyActivated,
      R.attr.colorLabelActivated,
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
      int attrKeyAction = ta.getColor(3, 0);
      int attrKeyActivated = ta.getColor(4, 0);
      int attrLabelActivated = ta.getColor(5, 0);
      if (selBg == 0xFF3B82F6) {
        if (attrKeyAction != 0) selBg = attrKeyAction;
        else if (attrKeyActivated != 0) selBg = attrKeyActivated;
        else if (attrLabelActivated != 0) selBg = attrLabelActivated;
      }
      float rad = ta.getDimension(6, 0);
      if (rad > 0 && (kbTheme == null || kbTheme.keyBorderRadius <= 0)) {
        itemCornerRadius = Math.max(6f * density, Math.min(14f * density, rad));
      }
    } catch (Exception ignored) {
    } finally {
      if (ta != null) ta.recycle();
    }

    // High contrast guarantee for popup container and unselected text
    double bgLum = (0.299 * Color.red(popupBgColor) + 0.587 * Color.green(popupBgColor) + 0.114 * Color.blue(popupBgColor)) / 255.0;
    double tLum = (0.299 * Color.red(textColor) + 0.587 * Color.green(textColor) + 0.114 * Color.blue(textColor)) / 255.0;
    if (Math.abs(bgLum - tLum) < 0.28) {
      textColor = (bgLum > 0.5) ? 0xFF0F172A : 0xFFF8FAFC;
    }

    // High contrast guarantee for selected item background vs popup container
    double selLum = (0.299 * Color.red(selBg) + 0.587 * Color.green(selBg) + 0.114 * Color.blue(selBg)) / 255.0;
    if (Math.abs(selLum - bgLum) < 0.16) {
      if (bgLum > 0.5) {
        selBg = 0xFF1D4ED8; // High contrast royal blue on light backgrounds
        selLum = 0.25;
      } else {
        selBg = 0xFF6366F1; // High contrast indigo on dark backgrounds
        selLum = 0.40;
      }
    }

    // High contrast text on top of selectedBgColor
    int selTextColor = (selLum > 0.55) ? 0xFF0F172A : 0xFFFFFFFF;

    this.itemTextColor = textColor;
    this.selectedBgColor = selBg;
    this.selectedTextColor = selTextColor;

    View content = LayoutInflater.from(themedCtx).inflate(R.layout.popup_dot_extensions, null);

    // Apply popup container background dynamically
    GradientDrawable popupBg = new GradientDrawable();
    popupBg.setShape(GradientDrawable.RECTANGLE);
    popupBg.setColor(popupBgColor);
    int strokeWidth = Math.max(1, (int) (1.2f * density));
    int strokeColor = (popupBorderColor != 0 && popupBorderColor != popupBgColor)
        ? popupBorderColor
        : ((bgLum > 0.5) ? 0x22000000 : 0x33FFFFFF);
    popupBg.setStroke(strokeWidth, strokeColor);
    popupBg.setCornerRadius(itemCornerRadius + (4f * density));
    content.setBackground(popupBg);
    int pad = (int) (4 * density);
    content.setPadding(pad, pad, pad, pad);

    scrollView = content.findViewById(R.id.popup_extensions_scroll);
    container = content.findViewById(R.id.popup_extensions_container);
    container.removeAllViews();

    // Sort extensions
    List<String> sorted = new ArrayList<>(exts);
    Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

    // Get screen width and compute dynamic item padding / min width so all items fit cleanly
    int screenWidth = anchor.getResources().getDisplayMetrics().widthPixels;
    int availableWidth = screenWidth - 24; // 12dp margins on each side
    int itemCount = sorted.size();

    // Dynamic padding and font sizing based on item count and screen width
    int padH = (int)(Math.max(4, Math.min(10, (availableWidth / (float)Math.max(1, itemCount) - 26 * density) / 2f)));
    int padV = (int)(6 * density);
    int marginH = (int)(2 * density);

    for (String ext : sorted) {
      TextView tv = new TextView(themedCtx);
      tv.setText(ext);
      tv.setTextSize(12.5f);
      tv.setTextColor(this.itemTextColor);
      tv.setTypeface(null, Typeface.NORMAL);
      tv.setPadding(padH, padV, padH, padV);
      tv.setBackground(makeUnselectedItemBg());
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lp.setMargins(marginH, (int)(2 * density), marginH, (int)(2 * density));
      tv.setLayoutParams(lp);
      tv.setGravity(Gravity.CENTER);
      tv.setSingleLine(true);
      tv.setTag(ext);
      // Click to select
      tv.setOnClickListener(v -> {
        selectAndDismiss(ext, v);
      });
      container.addView(tv);
    }

    // Highlight default .com if exists
    for (int i=0;i<container.getChildCount();i++) {
      TextView tv = (TextView) container.getChildAt(i);
      if (".com".equals(tv.getText().toString())) {
        setSelected(tv);
        break;
      }
    }
    if (selectedView == null && container.getChildCount()>0) {
      setSelected((TextView)container.getChildAt(0));
    }

    // Handle drag selection
    container.setOnTouchListener((v, event) -> {
      int action = event.getAction();
      float x = event.getX();
      float y = event.getY();
      // Find child under touch
      View child = findChildAt(container, x, y);
      if (child instanceof TextView) {
        setSelected((TextView) child);
        if (action == MotionEvent.ACTION_UP) {
          selectAndDismiss((String) child.getTag(), child);
          return true;
        }
      }
      if (action == MotionEvent.ACTION_UP) {
        // If lifted outside, commit selected
        if (selectedText != null) {
          selectAndDismiss(selectedText, selectedView);
        } else {
          dismiss();
        }
      }
      return true;
    });

    popup = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);
    popup.setOutsideTouchable(true);
    popup.setBackgroundDrawable(null);
    popup.setElevation(16);
    popup.setClippingEnabled(false);
    popup.setAnimationStyle(android.R.style.Animation_Dialog);
    popup.setOnDismissListener(() -> { if (onDismissCallback != null) onDismissCallback.run(); });

    // Measure and position directly over the ASDFGHJKL row in the keyboard window
    content.measure(View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    int pw = Math.min(content.getMeasuredWidth(), availableWidth);
    int ph = content.getMeasuredHeight();

    int[] winLoc = new int[2];
    anchor.getLocationInWindow(winLoc);

    int anchorWidth = anchor.getWidth();
    if (anchorWidth <= 0) anchorWidth = screenWidth;

    // Calculate Y position centered exactly over the ASDFGHJKL row
    int localRowCenterY;
    if (asdfRowRect != null) {
      localRowCenterY = asdfRowRect.centerY();
    } else {
      localRowCenterY = (int)(anchor.getHeight() * 0.45f);
    }

    int yInWindow = winLoc[1] + localRowCenterY - (ph / 2);
    int xInWindow = winLoc[0] + (anchorWidth / 2) - (pw / 2);

    // Keep horizontally inside screen boundaries
    if (xInWindow < 12) xInWindow = 12;
    if (xInWindow + pw > screenWidth - 12) xInWindow = screenWidth - pw - 12;
    if (yInWindow < 8) yInWindow = 8;

    popup.showAtLocation(anchor, Gravity.TOP | Gravity.START, xInWindow, yInWindow);
  }

  private View findChildAt(ViewGroup parent, float x, float y) {
    for (int i=0;i<parent.getChildCount();i++) {
      View c = parent.getChildAt(i);
      if (x >= c.getLeft() && x <= c.getRight() && y >= c.getTop() && y <= c.getBottom()) return c;
    }
    // Find closest by x
    if (parent.getChildCount()==0) return null;
    float bestDist = Float.MAX_VALUE;
    View best = null;
    float cx = x;
    for (int i=0;i<parent.getChildCount();i++) {
      View c = parent.getChildAt(i);
      float center = (c.getLeft()+c.getRight())/2f;
      float d = Math.abs(center - cx);
      if (d < bestDist) { bestDist = d; best = c; }
    }
    return best;
  }

  private void setSelected(TextView tv) {
    if (selectedView != null && selectedView != tv) {
      selectedView.setBackground(makeUnselectedItemBg());
      selectedView.setTextColor(itemTextColor);
      selectedView.setTypeface(null, Typeface.NORMAL);
    }
    selectedView = tv;
    selectedText = (tv != null) ? (String) tv.getTag() : null;
    if (tv != null) {
      tv.setBackground(makeSelectedItemBg());
      tv.setTextColor(selectedTextColor);
      tv.setTypeface(null, Typeface.BOLD);
    }

    if (scrollView != null && tv != null) {
      int scrollX = scrollView.getScrollX();
      int scrollWidth = scrollView.getWidth();
      int left = tv.getLeft();
      int right = tv.getRight();
      if (left < scrollX) {
        scrollView.smoothScrollTo(Math.max(0, left - 16), 0);
      } else if (right > scrollX + scrollWidth && scrollWidth > 0) {
        scrollView.smoothScrollTo(right - scrollWidth + 16, 0);
      }
    }
  }

  public void updateSelectionForRawX(float rawX) {
    if (container == null || popup == null || !popup.isShowing()) return;
    int[] loc = new int[2];
    container.getLocationOnScreen(loc);
    float localX = rawX - loc[0];
    float localY = container.getHeight() / 2f;
    View child = findChildAt(container, localX, localY);
    if (child instanceof TextView) {
      setSelected((TextView) child);
    }
  }

  public void commitSelected() {
    if (selectedText != null && selectedView != null) {
      selectAndDismiss(selectedText, selectedView);
    } else {
      dismiss();
    }
  }

  private void selectAndDismiss(String ext, View v) {
    if (callback != null) callback.onExtensionSelected(ext);
    dismiss();
  }

  public void dismiss() {
    if (popup != null && popup.isShowing()) {
      try {
        popup.dismiss();
      } catch (Exception ignored) {}
    }
    popup = null;
    selectedView = null;
    selectedText = null;
    if (onDismissCallback != null) onDismissCallback.run();
  }

  public boolean isShowing() { return popup != null && popup.isShowing(); }

  public static List<String> loadExtensions(Context ctx) {
    SharedPreferences prefs = DirectBootAwarePreferences.get_shared_preferences(ctx);
    Set<String> set = prefs.getStringSet("custom_extensions", null);
    if (set == null) {
      set = new HashSet<>();
      set.add(".com"); set.add(".org"); set.add(".net"); set.add(".edu"); set.add(".gov");
      set.add(".io"); set.add(".co"); set.add(".in"); set.add(".app");
    }
    List<String> list = new ArrayList<>(set);
    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
    return list;
  }
}
