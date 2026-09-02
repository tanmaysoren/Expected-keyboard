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
    show(anchor, null, keyRect, exts, cb);
  }

  public void show(View anchor, Rect asdfRowRect, Rect keyRect, List<String> exts, Callback cb) {
    this.anchorView = anchor;
    this.extensions = exts;
    this.callback = cb;
    Context ctx = anchor.getContext();
    View content = LayoutInflater.from(ctx).inflate(R.layout.popup_dot_extensions, null);
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

    // Get theme label color to match keyboard layout
    int labelColor = 0xFFE2E8F0;
    try {
      android.content.res.TypedArray ta = ctx.obtainStyledAttributes(new int[]{R.attr.colorLabel});
      labelColor = ta.getColor(0, labelColor);
      ta.recycle();
    } catch (Exception ignored) {}

    // Dynamic padding and font sizing based on item count and screen width
    // e.g. for 9 items on a 360dp-420dp screen, compact padding ensures all fit on screen without cut-off
    float density = ctx.getResources().getDisplayMetrics().density;
    int padH = (int)(Math.max(4, Math.min(10, (availableWidth / (float)Math.max(1, itemCount) - 26 * density) / 2f)));
    int padV = (int)(6 * density);
    int marginH = (int)(2 * density);

    for (String ext : sorted) {
      TextView tv = new TextView(ctx);
      tv.setText(ext);
      tv.setTextSize(12.5f);
      tv.setTextColor(labelColor);
      tv.setPadding(padH, padV, padH, padV);
      tv.setBackgroundResource(R.drawable.popup_extension_item_bg);
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

  private android.widget.HorizontalScrollView scrollView;

  private void setSelected(TextView tv) {
    if (selectedView != null) {
      selectedView.setBackgroundResource(R.drawable.popup_extension_item_bg);
      selectedView.setTextColor(0xFFE2E8F0);
    }
    selectedView = tv;
    selectedText = (String) tv.getTag();
    tv.setBackgroundResource(R.drawable.popup_extension_selected_bg);
    tv.setTextColor(0xFFFFFFFF);

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
