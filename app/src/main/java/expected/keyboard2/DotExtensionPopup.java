package expected.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

  public interface Callback {
    void onExtensionSelected(String ext);
  }

  private Callback callback;
  private Runnable onDismissCallback;

  public DotExtensionPopup(Context ctx) {
  }

  public void setOnDismiss(Runnable r) { this.onDismissCallback = r; }

  public void show(View anchor, Rect keyRect, List<String> exts, Callback cb) {
    this.anchorView = anchor;
    this.extensions = exts;
    this.callback = cb;
    Context ctx = anchor.getContext();
    View content = LayoutInflater.from(ctx).inflate(R.layout.popup_dot_extensions, null);
    container = content.findViewById(R.id.popup_extensions_container);
    container.removeAllViews();

    // Sort extensions
    List<String> sorted = new ArrayList<>(exts);
    Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);

    // Get theme label color to match keyboard layout
    int labelColor = 0xFFE2E8F0;
    try {
      android.content.res.TypedArray ta = ctx.obtainStyledAttributes(new int[]{R.attr.colorLabel});
      labelColor = ta.getColor(0, labelColor);
      ta.recycle();
    } catch (Exception ignored) {}
    for (String ext : sorted) {
      TextView tv = new TextView(ctx);
      tv.setText(ext);
      tv.setTextSize(13);
      tv.setTextColor(labelColor);
      tv.setPadding(28, 16, 28, 16);
      tv.setBackgroundResource(R.drawable.popup_extension_item_bg);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      lp.setMargins(6,4,6,4);
      tv.setLayoutParams(lp);
      tv.setGravity(Gravity.CENTER);
      tv.setSingleLine(true);
      tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
      tv.setMinWidth(80);
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
    popup.setElevation(12);
    popup.setAnimationStyle(android.R.style.Animation_Dialog);
    popup.setOnDismissListener(() -> { if (onDismissCallback != null) onDismissCallback.run(); });

    // Measure and always show over asdfghjk row — anchor is Keyboard2View, follow it wherever it is (floating or docked)
    content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    int pw = content.getMeasuredWidth();
    int ph = content.getMeasuredHeight();
    int[] loc = new int[2];
    anchor.getLocationOnScreen(loc);
    int anchorCenterX = loc[0] + keyRect.centerX();
    int keyboardHeight = anchor.getHeight();
    int screenHeight = anchor.getResources().getDisplayMetrics().heightPixels;
    int screenWidth = anchor.getResources().getDisplayMetrics().widthPixels;
    if (keyboardHeight <= 0) {
      // Fallback if not yet laid out — estimate from screen
      keyboardHeight = (int)(screenHeight * 0.36f);
    }
    int keyboardTop = loc[1];
    // If loc is 0 (not laid out yet), fallback to bottom for docked
    if (keyboardTop <= 0 && keyboardHeight > 0) {
      // Try window location as fallback
      int[] winLoc = new int[2];
      try { anchor.getLocationInWindow(winLoc); if (winLoc[1] > 0) keyboardTop = winLoc[1]; } catch (Exception ignored) {}
    }
    if (keyboardTop <= 0) {
      // Last resort: docked at bottom
      keyboardTop = screenHeight - keyboardHeight;
    }
    // Position over asdf row: ~30-34% down from keyboard top (row1 center)
    // This is inside keyboard top area and moves with keyboard (floating or docked)
    int yOff = keyboardTop + (int)(keyboardHeight * 0.30f) - ph / 2;
    int xOff = anchorCenterX - pw/2;
    // Keep inside screen horizontally (centered on dot key)
    if (xOff < 8) xOff = 8;
    if (xOff + pw > screenWidth - 8) xOff = screenWidth - pw - 8;
    // Keep vertically on screen
    if (yOff < 8) yOff = 8;
    if (yOff + ph > screenHeight - 8) yOff = screenHeight - ph - 8;
    popup.showAtLocation(anchor, Gravity.NO_GRAVITY, xOff, yOff);
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
    if (selectedView != null) {
      selectedView.setBackgroundResource(R.drawable.popup_extension_item_bg);
      selectedView.setTextColor(0xFFE2E8F0);
    }
    selectedView = tv;
    selectedText = (String) tv.getTag();
    tv.setBackgroundResource(R.drawable.popup_extension_selected_bg);
    tv.setTextColor(0xFFFFFFFF);
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
    if (popup != null && popup.isShowing()) popup.dismiss();
    popup = null;
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
