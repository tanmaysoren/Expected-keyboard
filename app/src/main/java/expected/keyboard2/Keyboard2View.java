package expected.keyboard2;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Canvas;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.inputmethodservice.InputMethodService;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import java.util.Arrays;
import java.util.List;

public class Keyboard2View extends View
  implements View.OnTouchListener, Pointers.IPointerEventHandler
{
  private KeyboardData _keyboard;

  /** The key holding the shift key is used to set shift state from
      autocapitalisation. */
  private KeyboardData.Key _shift_key;

  /** Used to add fake pointers. */
  private KeyboardData.Key _compose_key;

  private Pointers _pointers;

  private Pointers.Modifiers _mods;

  private static int _currentWhat = 0;

  private Config _config;

  private float _keyWidth;
  private float _mainLabelSize;
  private float _subLabelSize;
  private float _marginRight;
  private float _marginLeft;
  private float _marginBottom;
  private int _insets_left = 0;
  private int _insets_right = 0;
  private int _insets_bottom = 0;

  private Theme _theme;
  private Theme.Computed _tc;

  private static RectF _tmpRect = new RectF();

  enum Vertical
  {
    TOP,
    CENTER,
    BOTTOM
  }

  public Keyboard2View(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    _theme = new Theme(getContext(), attrs);
    _config = Config.globalConfig();
    _pointers = new Pointers(this, _config);
    refresh_navigation_bar(context);
    setOnTouchListener(this);
    int layout_id = (attrs == null) ? 0 :
      attrs.getAttributeResourceValue(null, "layout", 0);
    if (layout_id == 0)
      reset();
    else
      setKeyboard(KeyboardData.load(getResources(), layout_id));
  }

  private Window getParentWindow(Context context)
  {
    if (context instanceof InputMethodService)
      return ((InputMethodService)context).getWindow().getWindow();
    if (context instanceof ContextWrapper)
      return getParentWindow(((ContextWrapper)context).getBaseContext());
    return null;
  }

  public void refresh_navigation_bar(Context context)
  {
    if (VERSION.SDK_INT < 21)
      return;
    // The intermediate Window is a [Dialog].
    Window w = getParentWindow(context);
    w.setNavigationBarColor(_theme.colorNavBar);
    if (VERSION.SDK_INT < 26)
      return;
    int uiFlags = getSystemUiVisibility();
    if (_theme.isLightNavBar)
      uiFlags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    else
      uiFlags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    setSystemUiVisibility(uiFlags);
  }

  public void setKeyboard(KeyboardData kw)
  {
    _keyboard = kw;
    if (_keyboard != null)
    {
      _shift_key = _keyboard.findKeyWithValue(KeyValue.SHIFT);
      _compose_key = _keyboard.findKeyWithValue(KeyValue.COMPOSE);
      KeyModifier.set_modmap(_keyboard.modmap);
    }
    else
    {
      _shift_key = null;
      _compose_key = null;
    }
    updateMetricsAndTheme(getWidth());
    reset();
  }

  public void reset()
  {
    _mods = Pointers.Modifiers.EMPTY;
    _pointers.clear();
    requestLayout();
    invalidate();
  }

  void set_fake_ptr_latched(KeyboardData.Key key, KeyValue kv, boolean latched,
      boolean lock)
  {
    if (_keyboard == null || key == null)
      return;
    _pointers.set_fake_pointer_state(key, kv, latched, lock);
  }

  /** Called by auto-capitalisation. */
  public void set_shift_state(boolean latched, boolean lock)
  {
    set_fake_ptr_latched(_shift_key, KeyValue.SHIFT, latched, lock);
  }

  /** Called from [KeyEventHandler]. */
  public void set_compose_pending(boolean pending)
  {
    set_fake_ptr_latched(_compose_key, KeyValue.COMPOSE, pending, false);
  }

  /** Called from [Keybard2.onUpdateSelection].  */
  public void set_selection_state(boolean selection_state)
  {
    if (_config.editor_config.selection_mode_enabled)
      set_fake_ptr_latched(KeyboardData.Key.EMPTY,
          KeyValue.SELECTION_MODE, selection_state, true);
  }

  public KeyValue modifyKey(KeyValue k, Pointers.Modifiers mods)
  {
    return KeyModifier.modify(k, mods);
  }

  public void onPointerDown(KeyValue k, boolean isSwipe)
  {
    updateFlags();
    _config.handler.key_down(k, isSwipe);
    invalidate();
    vibrate();
  }

  public void onPointerUp(KeyValue k, Pointers.Modifiers mods)
  {
    // [key_up] must be called before [updateFlags]. The latter might disable
    // flags.
    _config.handler.key_up(k, mods);
    updateFlags();
    invalidate();
  }

  public void onPointerHold(KeyValue k, Pointers.Modifiers mods)
  {
    _config.handler.key_up(k, mods);
    updateFlags();
  }

  public void onPointerFlagsChanged(boolean shouldVibrate)
  {
    updateFlags();
    invalidate();
    if (shouldVibrate)
      vibrate();
  }

  private void updateFlags()
  {
    _mods = _pointers.getModifiers();
    _config.handler.mods_changed(_mods);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event)
  {
    int p;
    switch (event.getActionMasked())
    {
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP:
        _pointers.onTouchUp(event.getPointerId(event.getActionIndex()));
        break;
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN:
        p = event.getActionIndex();
        float tx = event.getX(p);
        float ty = event.getY(p);
        KeyboardData.Key key = getKeyAtPosition(tx, ty);
        if (key != null)
          _pointers.onTouchDown(tx, ty, event.getPointerId(p), key);
        break;
      case MotionEvent.ACTION_MOVE:
        for (p = 0; p < event.getPointerCount(); p++)
          _pointers.onTouchMove(event.getX(p), event.getY(p), event.getPointerId(p));
        break;
      case MotionEvent.ACTION_CANCEL:
        _pointers.onTouchCancel();
        break;
      default:
        return (false);
    }
    return (true);
  }

  private KeyboardData.Row getRowAtPosition(float ty)
  {
    float y = _config.marginTop;
    if (ty < y)
      return null;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += (row.shift + row.height) * _tc.row_height;
      if (ty < y)
        return row;
    }
    return null;
  }

  private KeyboardData.Key getKeyAtPosition(float tx, float ty)
  {
    KeyboardData.Row row = getRowAtPosition(ty);
    float x = _marginLeft;
    if (row == null || tx < x)
      return null;
    for (KeyboardData.Key key : row.keys)
    {
      float xLeft = x + key.shift * _keyWidth;
      float xRight = xLeft + key.width * _keyWidth;
      if (tx < xLeft)
        return null;
      if (tx < xRight)
        return key;
      x = xRight;
    }
    return null;
  }

  private void vibrate()
  {
    VibratorCompat.vibrate(this, _config);
  }

  private void updateMetricsAndTheme(int width)
  {
    if (_keyboard == null)
      return;
    if (width <= 0)
    {
      width = getWidth();
      if (width <= 0)
      {
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        width = (dm != null && dm.widthPixels > 0) ? dm.widthPixels : 1080;
      }
    }
    if (_config != null && _config.isFloatingMode()) {
      float density = getContext().getResources().getDisplayMetrics().density;
      _marginLeft = (int) (2 * density);
      _marginRight = (int) (2 * density);
      _marginBottom = (int) (2 * density);
    } else {
      _marginLeft = Math.max(_config.horizontal_margin, _insets_left);
      _marginRight = Math.max(_config.horizontal_margin, _insets_right);
      _marginBottom = _config.margin_bottom + _insets_bottom;
    }
    float keysW = _keyboard.keysWidth > 0 ? _keyboard.keysWidth : 10f;
    _keyWidth = Math.max(1f, (width - _marginLeft - _marginRight) / keysW);
    Theme.getKeyFont(getContext());
    _tc = new Theme.Computed(_theme, _config, _keyWidth, _keyboard);
    // Compute the size of labels based on the width or the height of keys. The
    // margin around keys is taken into account. Keys normal aspect ratio is
    // assumed to be 3/2 for a 10 columns layout. It's generally more, the
    // width computation is useful when the keyboard is unusually high.
    float labelBaseSize = Math.min(
        _tc.row_height - _tc.vertical_margin,
        (_keyWidth - _tc.horizontal_margin) * 1.35f
      ) * _config.characterSize;
    _mainLabelSize = labelBaseSize * _config.labelTextSize;
    _subLabelSize = labelBaseSize * _config.sublabelTextSize;
  }

  @Override
  public void onMeasure(int wSpec, int hSpec)
  {
    if (_keyboard == null)
    {
      setMeasuredDimension(MeasureSpec.getSize(wSpec), 0);
      return;
    }
    int width = MeasureSpec.getSize(wSpec);
    if (width <= 0)
    {
      DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
      width = (dm != null && dm.widthPixels > 0) ? dm.widthPixels : 1080;
    }
    updateMetricsAndTheme(width);
    int height =
      (int)(_tc.row_height * _keyboard.keysHeight
          + _config.marginTop + _marginBottom);
    setMeasuredDimension(width, height);
  }

  Rect _cached_exclusion_rect = new Rect();
  List<Rect> _cached_exclusion_rects = Arrays.asList(_cached_exclusion_rect);
  @Override
  public void onLayout(boolean changed, int left, int top, int right, int bottom)
  {
    if (!changed)
      return;
    // Since SDK 30, this is done automatically:
    // https://android.googlesource.com/platform/frameworks/base/+/android11-release/core/java/android/inputmethodservice/InputMethodService.java#852
    if (VERSION.SDK_INT == 29)
    {
      // Disable the back-gesture on the keyboard area
      _cached_exclusion_rect.set(
          left + (int)_marginLeft,
          top + (int)_config.marginTop,
          right - (int)_marginRight,
          bottom - (int)_marginBottom);
      setSystemGestureExclusionRects(_cached_exclusion_rects);
    }
  }

  @Override
  public WindowInsets onApplyWindowInsets(WindowInsets wi)
  {
    // LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS is set in [Keyboard2#updateSoftInputWindowLayoutParams] for SDK_INT >= 35.
    if (VERSION.SDK_INT < 35)
      return wi;
    int insets_types =
      WindowInsets.Type.systemBars()
      | WindowInsets.Type.displayCutout();
    Insets insets = wi.getInsets(insets_types);
    _insets_left = insets.left;
    _insets_right = insets.right;
    _insets_bottom = insets.bottom;
    return WindowInsets.CONSUMED;
  }

  /** Horizontal and vertical position of the 9 indexes. */
  static final Paint.Align[] LABEL_POSITION_H = new Paint.Align[]{
    Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT,
    Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT,
    Paint.Align.CENTER, Paint.Align.CENTER
  };

  static final Vertical[] LABEL_POSITION_V = new Vertical[]{
    Vertical.CENTER, Vertical.TOP, Vertical.TOP, Vertical.BOTTOM,
    Vertical.BOTTOM, Vertical.CENTER, Vertical.CENTER, Vertical.TOP,
    Vertical.BOTTOM
  };

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (_keyboard == null)
      return;
    if (_tc == null)
      updateMetricsAndTheme(getWidth());
    if (_tc == null)
      return;
    float y = _tc.margin_top;
    for (KeyboardData.Row row : _keyboard.rows)
    {
      y += row.shift * _tc.row_height;
      float x = _marginLeft + _tc.margin_left;
      float keyH = row.height * _tc.row_height - _tc.vertical_margin;
      for (KeyboardData.Key k : row.keys)
      {
        x += k.shift * _keyWidth;
        float keyW = _keyWidth * k.width - _tc.horizontal_margin;
        boolean isKeyDown = _pointers.isKeyDown(k);
        Theme.Computed.Key tc_key;
        if (isKeyDown)
          tc_key = _tc.key_activated;
        else
          switch (k.role)
          {
            case Action: tc_key = _tc.key_action; break;
            case Space_bar: tc_key = _tc.key_space_bar; break;
            case Suggestion: tc_key = _tc.key_suggestion; break;
            default:
            case Normal: tc_key = _tc.key; break;
          }
        drawKeyFrame(canvas, x, y, keyW, keyH, tc_key);
        if (k.keys[0] != null)
          drawLabel(canvas, k.keys[0], keyW / 2f + x, y, keyH, isKeyDown, tc_key);
        for (int i = 1; i < 9; i++)
        {
          if (k.keys[i] != null)
            drawSubLabel(canvas, k.keys[i], x, y, keyW, keyH, i, isKeyDown, tc_key);
        }
        drawIndication(canvas, k, x, y, keyW, keyH, _tc);
        x += _keyWidth * k.width;
      }
      y += row.height * _tc.row_height;
    }
  }

  @Override
  public void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
  }

  /** Draw borders and background of the key. */
  void drawKeyFrame(Canvas canvas, float x, float y, float keyW, float keyH,
      Theme.Computed.Key tc)
  {
    float r = tc.border_radius;
    float w = tc.border_width;
    float padding = w / 2.f;
    _tmpRect.set(x + padding, y + padding, x + keyW - padding, y + keyH - padding);
    canvas.drawRoundRect(_tmpRect, r, r, tc.bg_paint);
    if (w > 0.f)
    {
      float overlap = r - r * 0.85f + w; // sin(45°)
      drawBorder(canvas, x, y, x + overlap, y + keyH, tc.border_left_paint, tc);
      drawBorder(canvas, x + keyW - overlap, y, x + keyW, y + keyH, tc.border_right_paint, tc);
      drawBorder(canvas, x, y, x + keyW, y + overlap, tc.border_top_paint, tc);
      drawBorder(canvas, x, y + keyH - overlap, x + keyW, y + keyH, tc.border_bottom_paint, tc);
    }
  }

  /** Clip to draw a border at a time. This allows to call [drawRoundRect]
      several time with the same parameters but a different Paint. */
  void drawBorder(Canvas canvas, float clipl, float clipt, float clipr,
      float clipb, Paint paint, Theme.Computed.Key tc)
  {
    float r = tc.border_radius;
    canvas.save();
    canvas.clipRect(clipl, clipt, clipr, clipb);
    canvas.drawRoundRect(_tmpRect, r, r, paint);
    canvas.restore();
  }

  private int labelColor(KeyValue k, boolean isKeyDown, boolean sublabel)
  {
    if (isKeyDown)
    {
      int flags = _pointers.getKeyFlags(k);
      if (flags != -1)
      {
        if ((flags & Pointers.FLAG_P_LOCKED) != 0)
          return _theme.lockedColor;
        return _theme.activatedColor;
      }
      return _theme.pressedColor;
    }
    if (k.hasFlagsAny(KeyValue.FLAG_SECONDARY | KeyValue.FLAG_GREYED))
    {
      if (k.hasFlagsAny(KeyValue.FLAG_GREYED))
        return _theme.greyedLabelColor;
      return _theme.secondaryLabelColor;
    }
    return sublabel ? _theme.subLabelColor : _theme.labelColor;
  }

  private void drawLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyH, boolean isKeyDown, Theme.Computed.Key tc)
  {
    kv = modifyKey(kv, _mods);
    if (kv == null)
      return;
    float textSize = scaleTextSize(kv, true);
    Paint p = tc.label_paint(kv.isSpecialFont(), labelColor(kv, isKeyDown, false), textSize);
    String text = kv.getString();
    if (KeySvgIcons.hasIcon(text))
    {
      float centerX = x;
      float centerY = y + keyH / 2f;
      float iconScale = (text.equalsIgnoreCase("sup") || text.equalsIgnoreCase("sub") || text.equalsIgnoreCase("superscript") || text.equalsIgnoreCase("subscript")) ? 1.6f : 1.15f;
      float iconSize = textSize * iconScale;
      if (KeySvgIcons.drawIcon(canvas, text, centerX, centerY, iconSize, p, isKeyDown))
        return;
    }
    canvas.drawText(text, x, (keyH - p.ascent() - p.descent()) / 2f + y, p);
  }

  private void drawSubLabel(Canvas canvas, KeyValue kv, float x, float y,
      float keyW, float keyH, int sub_index, boolean isKeyDown,
      Theme.Computed.Key tc)
  {
    Paint.Align a = LABEL_POSITION_H[sub_index];
    Vertical v = LABEL_POSITION_V[sub_index];
    kv = modifyKey(kv, _mods);
    if (kv == null)
      return;
    float textSize = scaleTextSize(kv, false);
    Paint p = tc.sublabel_paint(kv.isSpecialFont(), labelColor(kv, isKeyDown, true), textSize, a);
    float subPadding = _config.keyPadding;
    String label = kv.getString();

    // Check if this sublabel is layout switch forward (swipe up) or backward (swipe down)
    boolean isSwitchForward = (kv.getKind() == KeyValue.Kind.Event && kv.getEvent() == KeyValue.Event.SWITCH_FORWARD)
        || (kv.getKind() == KeyValue.Kind.Char && kv.getChar() == 0xE013)
        || "switch_forward".equalsIgnoreCase(label) || "switchforward".equalsIgnoreCase(label);
    boolean isSwitchBackward = (kv.getKind() == KeyValue.Kind.Event && kv.getEvent() == KeyValue.Event.SWITCH_BACKWARD)
        || (kv.getKind() == KeyValue.Kind.Char && kv.getChar() == 0xE014)
        || "switch_backward".equalsIgnoreCase(label) || "switchbackward".equalsIgnoreCase(label);

    if (isSwitchForward || isSwitchBackward)
    {
      String targetLang = getTargetLayoutLabel(isSwitchForward ? 1 : -1);
      float iconSize = textSize * 0.88f;
      float iconY = y;
      if (v == Vertical.TOP)
        iconY += subPadding + textSize / 2f;
      else if (v == Vertical.BOTTOM)
        iconY += keyH - subPadding - textSize / 2f;
      else
        iconY += keyH / 2f;

      String iconKey = isSwitchForward ? "switch_forward" : "switch_backward";

      if (targetLang != null && !targetLang.isEmpty())
      {
        Paint textPaint = tc.sublabel_paint(false, labelColor(kv, isKeyDown, true), textSize * 0.78f, Paint.Align.LEFT);
        float textWidth = textPaint.measureText(targetLang);
        float spacing = 3f * getResources().getDisplayMetrics().density;
        float totalWidth = iconSize + spacing + textWidth;
        float startX = (x + keyW / 2f) - totalWidth / 2f;

        float drawIconX = startX + iconSize / 2f;
        float drawTextX = startX + iconSize + spacing;
        float drawTextY = iconY - (textPaint.ascent() + textPaint.descent()) / 2f;

        KeySvgIcons.drawIcon(canvas, iconKey, drawIconX, iconY, iconSize, p, isKeyDown);
        canvas.drawText(targetLang, drawTextX, drawTextY, textPaint);
        return;
      }
      else
      {
        float iconX = x + keyW / 2f;
        KeySvgIcons.drawIcon(canvas, iconKey, iconX, iconY, iconSize, p, isKeyDown);
        return;
      }
    }

    if (KeySvgIcons.hasIcon(label))
    {
      float iconX = x;
      if (a == Paint.Align.CENTER)
        iconX += keyW / 2f;
      else if (a == Paint.Align.LEFT)
        iconX += subPadding + textSize / 2f;
      else
        iconX += keyW - subPadding - textSize / 2f;

      float iconY = y;
      if (v == Vertical.CENTER)
        iconY += keyH / 2f;
      else if (v == Vertical.TOP)
        iconY += subPadding + textSize / 2f;
      else
        iconY += keyH - subPadding - textSize / 2f;

      float iconSizeScale = 1.15f;
      if (label.equalsIgnoreCase("sup") || label.equalsIgnoreCase("sub") || label.equalsIgnoreCase("superscript") || label.equalsIgnoreCase("subscript")) {
        iconSizeScale = 1.4f;
      }
      if (KeySvgIcons.drawIcon(canvas, label, iconX, iconY, textSize * iconSizeScale, p, isKeyDown))
        return;
    }

    if (v == Vertical.CENTER)
      y += (keyH - p.ascent() - p.descent()) / 2f;
    else
      y += (v == Vertical.TOP) ? subPadding - p.ascent() : keyH - subPadding - p.descent();
    if (a == Paint.Align.CENTER)
      x += keyW / 2f;
    else
      x += (a == Paint.Align.LEFT) ? subPadding : keyW - subPadding;
    int label_len = label.length();
    // Limit the label of string keys to 3 characters
    if (label_len > 3 && kv.getKind() == KeyValue.Kind.String)
      label_len = 3;
    canvas.drawText(label, 0, label_len, x, y, p);
  }

  private String getTargetLayoutLabel(int delta)
  {
    if (_config == null || _config.layouts == null || _config.layouts.size() <= 1)
      return null;
    int size = _config.layouts.size();
    int targetIndex = (_config.get_current_layout() + delta + size) % size;
    KeyboardData targetData = _config.layouts.get(targetIndex);
    return getLayoutDisplayName(targetData);
  }

  public static String getLayoutDisplayName(KeyboardData data)
  {
    if (data == null)
    {
      String lang = java.util.Locale.getDefault().getLanguage();
      return (lang != null && !lang.isEmpty()) ? lang.toUpperCase(java.util.Locale.ENGLISH) : "DEF";
    }
    if (data.name != null && !data.name.isEmpty())
    {
      String name = data.name.trim();
      int open = name.indexOf('(');
      int close = name.indexOf(')', open);
      if (open >= 0 && close > open)
      {
        String inside = name.substring(open + 1, close).trim();
        String code = mapLanguageNameToCode(inside);
        if (code != null) return code;
        if (inside.length() <= 4) return inside.toUpperCase(java.util.Locale.ENGLISH);
      }
      String code = mapLanguageNameToCode(name);
      if (code != null) return code;
      if (name.length() <= 4) return name.toUpperCase(java.util.Locale.ENGLISH);
    }
    if (data.script != null && !data.script.isEmpty())
    {
      String s = data.script.toLowerCase(java.util.Locale.ENGLISH);
      if (s.contains("deva")) return "HI";
      if (s.contains("beng")) return "BN";
      if (s.contains("cyrl") || s.contains("cyrillic")) return "RU";
      if (s.contains("arab")) return "AR";
      if (s.contains("grek") || s.contains("greek")) return "EL";
      if (s.contains("hebr")) return "HE";
      if (s.contains("hang") || s.contains("korean")) return "KO";
      if (s.contains("georgian")) return "KA";
      if (s.contains("tamil") || s.contains("tam")) return "TA";
      if (s.contains("telugu") || s.contains("tel")) return "TE";
      if (s.contains("kann")) return "KN";
      if (s.contains("guj")) return "GU";
      if (s.contains("thai")) return "TH";
      if (s.contains("latn") || s.contains("latin")) return "EN";
    }
    if (data.name != null && data.name.length() >= 2)
    {
      return data.name.substring(0, Math.min(3, data.name.length())).toUpperCase(java.util.Locale.ENGLISH);
    }
    return "LANG";
  }

  private static String mapLanguageNameToCode(String str)
  {
    String s = str.toLowerCase(java.util.Locale.ENGLISH);
    if (s.equals("us") || s.equals("gb") || s.equals("english") || s.equals("en")) return "EN";
    if (s.contains("español") || s.contains("espanol") || s.equals("es")) return "ES";
    if (s.contains("français") || s.contains("francais") || s.contains("french") || s.equals("fr")) return "FR";
    if (s.contains("deutsch") || s.contains("german") || s.equals("de")) return "DE";
    if (s.contains("русский") || s.contains("russian") || s.equals("ru")) return "RU";
    if (s.contains("हिंदी") || s.contains("hindi") || s.equals("hi")) return "HI";
    if (s.contains("বাংলা") || s.contains("bengali") || s.contains("bangla") || s.equals("bn")) return "BN";
    if (s.contains("italiano") || s.contains("italian") || s.equals("it")) return "IT";
    if (s.contains("português") || s.contains("portuguese") || s.contains("brasileiro") || s.equals("pt") || s.equals("br")) return "PT";
    if (s.contains("україн") || s.contains("ukrainian") || s.equals("uk") || s.equals("ua")) return "UK";
    if (s.contains("turkish") || s.contains("türk") || s.equals("tr")) return "TR";
    if (s.contains("korean") || s.equals("kr") || s.equals("ko")) return "KO";
    if (s.contains("japanese") || s.equals("jp") || s.equals("ja")) return "JA";
    if (s.contains("chinese") || s.equals("cn") || s.equals("zh")) return "ZH";
    if (s.contains("czech") || s.contains("česk") || s.equals("cs") || s.equals("cz")) return "CS";
    if (s.contains("polski") || s.contains("polish") || s.equals("pl")) return "PL";
    if (s.contains("nederlands") || s.contains("dutch") || s.equals("nl")) return "NL";
    if (s.contains("svenska") || s.contains("swedish") || s.equals("se") || s.equals("sv")) return "SV";
    if (s.contains("norsk") || s.contains("norwegian") || s.equals("no")) return "NO";
    if (s.contains("dansk") || s.contains("danish") || s.equals("da")) return "DA";
    if (s.contains("suomi") || s.contains("finnish") || s.equals("fi")) return "FI";
    if (s.contains("magyar") || s.contains("hungarian") || s.equals("hu")) return "HU";
    if (s.contains("română") || s.contains("romanian") || s.equals("ro")) return "RO";
    if (s.contains("българ") || s.contains("bulgarian") || s.equals("bg")) return "BG";
    if (s.contains("arabic") || s.contains("عرب") || s.equals("ar")) return "AR";
    if (s.contains("persian") || s.contains("farsi") || s.contains("فارس") || s.equals("fa") || s.equals("ir")) return "FA";
    if (s.contains("hebrew") || s.contains("עבר") || s.equals("he") || s.equals("il")) return "HE";
    if (s.contains("greek") || s.contains("ελλην") || s.equals("el") || s.equals("gr")) return "EL";
    if (s.contains("georgian") || s.contains("ქართ") || s.equals("ka") || s.equals("ge")) return "KA";
    if (s.contains("assamese") || s.contains("অসম") || s.equals("as")) return "AS";
    if (s.contains("gujarati") || s.contains("ગુજ") || s.equals("gu")) return "GU";
    if (s.contains("kannada") || s.contains("ಕನ್ನ") || s.equals("kn")) return "KN";
    if (s.contains("tamil") || s.contains("தமிழ்") || s.equals("ta")) return "TA";
    if (s.contains("telugu") || s.contains("తెలు") || s.equals("te")) return "TE";
    if (s.contains("malayalam") || s.contains("മല") || s.equals("ml")) return "ML";
    if (s.contains("thai") || s.contains("ไทย") || s.equals("th")) return "TH";
    if (s.contains("vietnamese") || s.contains("tiếng việt") || s.equals("vi")) return "VI";
    if (s.contains("colemak")) return "CLM";
    if (s.contains("dvorak")) return "DVK";
    if (s.contains("workman")) return "WRK";
    if (s.contains("neo 2") || s.contains("neo2")) return "NEO";
    if (s.contains("bepo")) return "BEPO";
    if (s.contains("bone")) return "BONE";
    return null;
  }

  private void drawIndication(Canvas canvas, KeyboardData.Key k, float x,
      float y, float keyW, float keyH, Theme.Computed tc)
  {
    if (k.indication == null || k.indication.equals(""))
      return;
    Paint p = tc.indication_paint;
    p.setTextSize(_subLabelSize);
    canvas.drawText(k.indication, 0, k.indication.length(),
        x + keyW / 2f, (keyH - p.ascent() - p.descent()) * 4/5 + y, p);
  }

  private float scaleTextSize(KeyValue k, boolean main_label)
  {
    float smaller_font = k.hasFlagsAny(KeyValue.FLAG_SMALLER_FONT) ? 0.75f : 1.f;
    float label_size = main_label ? _mainLabelSize : _subLabelSize;
    return label_size * smaller_font;
  }
}
