package expected.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public final class FloatingKeyboardUtils {

  public static final String PREF_FLOATING_POS_X_PREFIX = "pref_floating_pos_x_";
  public static final String PREF_FLOATING_POS_Y_PREFIX = "pref_floating_pos_y_";
  public static final String PREF_FLOATING_WIDTH_PREFIX = "pref_floating_width_";
  public static final String PREF_FLOATING_HEIGHT_PREFIX = "pref_floating_height_";

  public static final String TAG_DRAG_HANDLE = "floating_drag_handle_container";
  public static final String TAG_RESIZE_HANDLE = "floating_resize_container";

  public static void applyFloating(final Window window, final ViewGroup containerView, final Config config, final View keyboardLayoutView) {
    if (containerView == null) return;

    Context context = containerView.getContext();
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    int screenWidth = dm.widthPixels;
    int screenHeight = dm.heightPixels;

    int storedWidth = readFloatingWidth(context, screenWidth);
    int storedHeight = readFloatingHeight(context, screenWidth);
    int[] pos = readFloatingPosition(context, screenWidth, screenHeight, storedWidth, storedHeight);

    int floatingX = pos[0];
    int floatingY = pos[1];

    ViewGroup.LayoutParams rawLp = containerView.getLayoutParams();
    FrameLayout.LayoutParams flp;
    if (rawLp instanceof FrameLayout.LayoutParams) {
      flp = (FrameLayout.LayoutParams) rawLp;
    } else if (rawLp instanceof ViewGroup.MarginLayoutParams) {
      flp = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams) rawLp);
    } else {
      flp = new FrameLayout.LayoutParams(storedWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    flp.gravity = Gravity.TOP | Gravity.LEFT;
    flp.width = storedWidth;
    flp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
    flp.leftMargin = floatingX;
    flp.topMargin = floatingY;
    containerView.setLayoutParams(flp);

    if (config != null) {
      config.updateFloatingHeight(dm, storedHeight);
    }

    adjustInnerViewHeights(containerView, storedHeight, true);
    ensureHandleBars(containerView, config, keyboardLayoutView);
  }

  public static void disableFloating(final Window window, final ViewGroup containerView) {
    if (containerView == null) return;

    ViewGroup.LayoutParams rawLp = containerView.getLayoutParams();
    if (rawLp instanceof ViewGroup.MarginLayoutParams) {
      ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) rawLp;
      lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
      lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
      lp.leftMargin = 0;
      lp.topMargin = 0;
      if (lp instanceof FrameLayout.LayoutParams) {
        ((FrameLayout.LayoutParams) lp).gravity = Gravity.BOTTOM;
      }
      containerView.setLayoutParams(lp);
    }

    adjustInnerViewHeights(containerView, 0, false);

    View dragHandle = containerView.findViewWithTag(TAG_DRAG_HANDLE);
    if (dragHandle != null) {
      dragHandle.setVisibility(View.GONE);
    }
    View resizeHandle = containerView.findViewWithTag(TAG_RESIZE_HANDLE);
    if (resizeHandle != null) {
      resizeHandle.setVisibility(View.GONE);
    }
  }

  public static void adjustInnerViewHeights(ViewGroup containerView, int floatingHeight, boolean isFloating) {
    if (containerView == null) return;
    Context context = containerView.getContext();
    DisplayMetrics dm = context.getResources().getDisplayMetrics();

    // 1. EmojiGridView inside emoji_pane
    View emojiGrid = containerView.findViewById(R.id.emoji_grid);
    if (emojiGrid != null) {
      ViewGroup.LayoutParams lp = emojiGrid.getLayoutParams();
      if (lp != null) {
        if (isFloating) {
          int topBars = (int) (16 * dm.density) + (int) (40 * dm.density);
          int bottomBar = (int) (46 * dm.density);
          int gridH = Math.max((int) (80 * dm.density), floatingHeight - topBars - bottomBar);
          lp.height = gridH;
        } else {
          lp.height = context.getResources().getDimensionPixelSize(R.dimen.emoji_grid_height);
        }
        emojiGrid.setLayoutParams(lp);
      }
    }

    // 2. ScrollView inside clipboard_pane
    ScrollView scrollView = findScrollView(containerView);
    if (scrollView != null) {
      ViewGroup.LayoutParams lp = scrollView.getLayoutParams();
      if (lp != null) {
        if (isFloating) {
          int topBars = (int) (16 * dm.density);
          int bottomBar = (int) (46 * dm.density);
          int scrollH = Math.max((int) (80 * dm.density), floatingHeight - topBars - bottomBar);
          lp.height = scrollH;
        } else {
          lp.height = context.getResources().getDimensionPixelSize(R.dimen.clipboard_view_height);
        }
        scrollView.setLayoutParams(lp);
      }
    }
  }

  private static ScrollView findScrollView(ViewGroup root) {
    if (root == null) return null;
    for (int i = 0; i < root.getChildCount(); i++) {
      View child = root.getChildAt(i);
      if (child instanceof ScrollView) {
        return (ScrollView) child;
      } else if (child instanceof ViewGroup) {
        ScrollView found = findScrollView((ViewGroup) child);
        if (found != null) return found;
      }
    }
    return null;
  }

  private static void ensureHandleBars(final ViewGroup containerView, final Config config, final View keyboardLayoutView) {
    Context context = containerView.getContext();
    DisplayMetrics dm = context.getResources().getDisplayMetrics();

    // Top Drag Handle Bar
    View dragHandleContainer = containerView.findViewWithTag(TAG_DRAG_HANDLE);
    if (dragHandleContainer == null) {
      LinearLayout handleContainer = new LinearLayout(context);
      handleContainer.setTag(TAG_DRAG_HANDLE);
      handleContainer.setOrientation(LinearLayout.HORIZONTAL);
      handleContainer.setGravity(Gravity.CENTER);
      int handleHeight = (int) (16 * dm.density);
      handleContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, handleHeight));

      ImageView dragHandle = new ImageView(context);
      dragHandle.setImageResource(R.drawable.ic_drag_indicator);
      int iconSize = (int) (16 * dm.density);
      LinearLayout.LayoutParams dragLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, iconSize);
      dragHandle.setLayoutParams(dragLp);
      dragHandle.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
      dragHandle.setContentDescription("Drag Floating Keyboard");

      handleContainer.addView(dragHandle);
      setupDragListener(dragHandle, containerView, context);

      containerView.addView(handleContainer, 0);
      dragHandleContainer = handleContainer;
    }
    dragHandleContainer.setVisibility(View.VISIBLE);

    // Bottom Resize Handle Bar
    View bottomResizeContainer = containerView.findViewWithTag(TAG_RESIZE_HANDLE);
    if (bottomResizeContainer == null) {
      FrameLayout bottomContainer = new FrameLayout(context);
      bottomContainer.setTag(TAG_RESIZE_HANDLE);
      int bottomHeight = (int) (18 * dm.density);
      LinearLayout.LayoutParams bottomLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bottomHeight);
      bottomLp.topMargin = (int) (2 * dm.density);
      bottomLp.bottomMargin = (int) (2 * dm.density);
      bottomContainer.setLayoutParams(bottomLp);

      // Center grab bar pill indicator
      View centerPill = new View(context);
      int pillWidth = (int) (36 * dm.density);
      int pillHeight = (int) (4 * dm.density);
      FrameLayout.LayoutParams pillLp = new FrameLayout.LayoutParams(pillWidth, pillHeight);
      pillLp.gravity = Gravity.CENTER;
      centerPill.setLayoutParams(pillLp);
      android.graphics.drawable.GradientDrawable pillDrawable = new android.graphics.drawable.GradientDrawable();
      pillDrawable.setColor(0x559E9E9E);
      pillDrawable.setCornerRadius(2 * dm.density);
      centerPill.setBackground(pillDrawable);
      bottomContainer.addView(centerPill);

      // Enlarged corner resize handle button
      ImageView resizeHandle = new ImageView(context);
      resizeHandle.setImageResource(R.drawable.ic_resize);
      int handleTouchWidth = (int) (48 * dm.density);
      int handleTouchHeight = (int) (20 * dm.density);
      FrameLayout.LayoutParams resizeLp = new FrameLayout.LayoutParams(handleTouchWidth, handleTouchHeight);
      resizeLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
      resizeHandle.setLayoutParams(resizeLp);
      resizeHandle.setPadding((int)(8 * dm.density), (int)(2 * dm.density), (int)(6 * dm.density), (int)(2 * dm.density));
      resizeHandle.setScaleType(ImageView.ScaleType.FIT_END);
      resizeHandle.setContentDescription("Resize Floating Keyboard");

      bottomContainer.addView(resizeHandle);
      setupDragListener(bottomContainer, containerView, context);
      setupResizeListener(resizeHandle, containerView, context, config, keyboardLayoutView);

      containerView.addView(bottomContainer);
      bottomResizeContainer = bottomContainer;
    }
    bottomResizeContainer.setVisibility(View.VISIBLE);
  }

  private static void setupDragListener(View dragView, final ViewGroup containerView, final Context context) {
    dragView.setOnTouchListener(new View.OnTouchListener() {
      private float downRawX, downRawY;
      private int initialLeft, initialTop;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        ViewGroup.LayoutParams rawLp = containerView.getLayoutParams();
        if (!(rawLp instanceof ViewGroup.MarginLayoutParams)) return false;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int availableWidth = dm.widthPixels;
        int availableHeight = dm.heightPixels;

        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            downRawX = event.getRawX();
            downRawY = event.getRawY();
            ViewGroup.MarginLayoutParams marginLp = (ViewGroup.MarginLayoutParams) rawLp;
            initialLeft = marginLp.leftMargin;
            initialTop = marginLp.topMargin;
            if (v.getParent() != null) {
              v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;

          case MotionEvent.ACTION_MOVE:
            float dx = event.getRawX() - downRawX;
            float dy = event.getRawY() - downRawY;

            int newX = initialLeft + (int) dx;
            int newY = initialTop + (int) dy;

            int containerW = containerView.getWidth() > 0 ? containerView.getWidth() : rawLp.width;
            int containerH = containerView.getHeight() > 0 ? containerView.getHeight() : (int) (240 * dm.density);

            int maxX = Math.max(0, availableWidth - containerW);
            int maxY = Math.max(0, availableHeight - containerH);

            newX = Math.max(0, Math.min(newX, maxX));
            newY = Math.max(0, Math.min(newY, maxY));

            FrameLayout.LayoutParams flp;
            if (rawLp instanceof FrameLayout.LayoutParams) {
              flp = (FrameLayout.LayoutParams) rawLp;
            } else {
              flp = new FrameLayout.LayoutParams((ViewGroup.MarginLayoutParams) rawLp);
            }
            flp.gravity = Gravity.TOP | Gravity.LEFT;
            flp.leftMargin = newX;
            flp.topMargin = newY;
            containerView.setLayoutParams(flp);

            saveFloatingPosition(context, availableWidth, newX, newY);

            containerView.requestLayout();
            if (containerView.getRootView() != null) {
              containerView.getRootView().requestApplyInsets();
            }
            return true;
        }
        return false;
      }
    });
  }

  private static void setupResizeListener(View resizeView, final ViewGroup containerView, final Context context, final Config config, final View keyboardLayoutView) {
    resizeView.setOnTouchListener(new View.OnTouchListener() {
      private float downRawX, downRawY;
      private int initialWidth, initialHeight;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        ViewGroup.LayoutParams rawLp = containerView.getLayoutParams();
        if (!(rawLp instanceof ViewGroup.MarginLayoutParams)) return false;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int availableWidth = dm.widthPixels;
        int availableHeight = dm.heightPixels;

        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            downRawX = event.getRawX();
            downRawY = event.getRawY();
            ViewGroup.MarginLayoutParams marginLp = (ViewGroup.MarginLayoutParams) rawLp;
            initialWidth = containerView.getWidth() > 0 ? containerView.getWidth() : marginLp.width;
            initialHeight = containerView.getHeight() > 0 ? containerView.getHeight() : readFloatingHeight(context, availableWidth);
            if (v.getParent() != null) {
              v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return true;

          case MotionEvent.ACTION_MOVE:
            float dx = event.getRawX() - downRawX;
            float dy = event.getRawY() - downRawY;

            int minWidth = (int) (180 * dm.density);
            int maxWidth = (int) (availableWidth * 0.98f);
            int minHeight = (int) (120 * dm.density);
            int maxHeight = (int) (availableHeight * 0.88f);

            int newWidth = Math.max(minWidth, Math.min(maxWidth, initialWidth + (int) dx));
            int newHeight = Math.max(minHeight, Math.min(maxHeight, initialHeight + (int) dy));

            ViewGroup.MarginLayoutParams currentMarginLp = (ViewGroup.MarginLayoutParams) rawLp;
            if (currentMarginLp.leftMargin + newWidth > availableWidth) {
              newWidth = Math.max(minWidth, availableWidth - currentMarginLp.leftMargin);
            }
            if (currentMarginLp.topMargin + newHeight > availableHeight) {
              newHeight = Math.max(minHeight, availableHeight - currentMarginLp.topMargin);
            }

            FrameLayout.LayoutParams flp;
            if (rawLp instanceof FrameLayout.LayoutParams) {
              flp = (FrameLayout.LayoutParams) rawLp;
            } else {
              flp = new FrameLayout.LayoutParams(currentMarginLp);
            }
            flp.gravity = Gravity.TOP | Gravity.LEFT;
            flp.width = newWidth;
            flp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            containerView.setLayoutParams(flp);

            saveFloatingSize(context, availableWidth, newWidth, newHeight);

            if (config != null) {
              config.updateFloatingHeight(dm, newHeight);
            }

            adjustInnerViewHeights(containerView, newHeight, true);

            View candidatesView = containerView.findViewById(R.id.candidates_view);
            if (candidatesView instanceof expected.keyboard2.suggestions.CandidatesView) {
              ((expected.keyboard2.suggestions.CandidatesView) candidatesView).set_sizes(config);
            }

            if (keyboardLayoutView instanceof Keyboard2View) {
              ((Keyboard2View) keyboardLayoutView).reset();
            } else if (keyboardLayoutView != null) {
              keyboardLayoutView.requestLayout();
            }
            containerView.requestLayout();
            if (containerView.getRootView() != null) {
              containerView.getRootView().requestApplyInsets();
            }
            return true;
        }
        return false;
      }
    });
  }

  public static int calculateDockedHeight(Context context, int screenWidth) {
    if (context == null) return (int) (240 * 2.0f);
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    Config config = Config.globalConfig();
    int keyboardHeightPercent = 35;
    boolean isLandscape = dm.widthPixels > dm.heightPixels;
    if (config != null) {
      isLandscape = config.orientation_landscape;
      keyboardHeightPercent = isLandscape ? 50 : 35;
    } else if (isLandscape) {
      keyboardHeightPercent = 50;
    }

    float base_height = Math.min(dm.heightPixels, dm.widthPixels * 16.f / 9.f);
    int rows_height = (int) (base_height * keyboardHeightPercent / 395);
    int candidateHeight = (config != null && config.suggestions_enabled) ? rows_height : (int) (20 * dm.density);
    int handleHeight = (int) (16 * dm.density);

    int dockedHeight = (int) (rows_height * 3.95f) + candidateHeight + handleHeight;
    int maxHeight = (int) (dm.heightPixels * 0.90f);
    return Math.min(dockedHeight, maxHeight);
  }

  private static String getOrientationSuffix(Context context) {
    if (context == null) return "_portrait";
    DisplayMetrics dm = context.getResources().getDisplayMetrics();
    return dm.widthPixels > dm.heightPixels ? "_landscape" : "_portrait";
  }

  public static int readFloatingWidth(Context context, int screenWidth) {
    if (context == null) return screenWidth;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String suffix = getOrientationSuffix(context);
    int defaultWidth = suffix.equals("_landscape") ? (int)(screenWidth * 0.55f) : (int)(screenWidth * 0.85f);

    if (prefs.contains(PREF_FLOATING_WIDTH_PREFIX + suffix)) {
      return prefs.getInt(PREF_FLOATING_WIDTH_PREFIX + suffix, defaultWidth);
    }
    return defaultWidth;
  }

  public static int readFloatingHeight(Context context, int screenWidth) {
    if (context == null) return (int) (240 * 2.0f);
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String suffix = getOrientationSuffix(context);
    int defaultHeight = calculateDockedHeight(context, screenWidth);

    if (prefs.contains(PREF_FLOATING_HEIGHT_PREFIX + suffix)) {
      return prefs.getInt(PREF_FLOATING_HEIGHT_PREFIX + suffix, defaultHeight);
    }
    return defaultHeight;
  }

  public static void saveFloatingSize(Context context, int screenWidth, int width, int height) {
    if (context == null) return;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String suffix = getOrientationSuffix(context);
    prefs.edit()
        .putInt(PREF_FLOATING_WIDTH_PREFIX + suffix, width)
        .putInt(PREF_FLOATING_HEIGHT_PREFIX + suffix, height)
        .apply();
  }

  public static int[] readFloatingPosition(Context context, int screenWidth, int screenHeight, int floatWidth, int floatHeight) {
    if (context == null) {
      return new int[]{0, Math.max(0, screenHeight - floatHeight)};
    }
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String suffix = getOrientationSuffix(context);
    int defaultX = (screenWidth - floatWidth) / 2;
    int defaultY = Math.max(0, screenHeight - floatHeight);

    int x = prefs.getInt(PREF_FLOATING_POS_X_PREFIX + suffix, defaultX);
    int y = prefs.getInt(PREF_FLOATING_POS_Y_PREFIX + suffix, defaultY);

    int maxX = Math.max(0, screenWidth - floatWidth);
    int maxY = Math.max(0, screenHeight - floatHeight);

    x = Math.max(0, Math.min(x, maxX));
    y = Math.max(0, Math.min(y, maxY));

    return new int[]{x, y};
  }

  public static void saveFloatingPosition(Context context, int screenWidth, int x, int y) {
    if (context == null) return;
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String suffix = getOrientationSuffix(context);
    prefs.edit()
        .putInt(PREF_FLOATING_POS_X_PREFIX + suffix, x)
        .putInt(PREF_FLOATING_POS_Y_PREFIX + suffix, y)
        .apply();
  }
}
