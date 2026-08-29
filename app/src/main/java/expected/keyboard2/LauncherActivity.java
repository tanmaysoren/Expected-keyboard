package expected.keyboard2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import expected.keyboard2.dict.DictionariesActivity;
import expected.keyboard2.R;

public class LauncherActivity extends Activity implements Handler.Callback
{
  /** Text is replaced when receiving key events. */
  TextView _tryhere_text;
  EditText _tryhere_area;
  TextView _status_badge;
  TextView _pill_enable;
  TextView _pill_select;

  /** Periodically restart the animations. */
  List<Animatable> _animations;
  Handler _handler;

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.launcher_activity);
    _tryhere_text = (TextView)findViewById(R.id.launcher_tryhere_text);
    _tryhere_area = (EditText)findViewById(R.id.launcher_tryhere_area);
    _status_badge = (TextView)findViewById(R.id.launcher_status_badge);
    _pill_enable = (TextView)findViewById(R.id.status_pill_enable);
    _pill_select = (TextView)findViewById(R.id.status_pill_select);

    if (VERSION.SDK_INT >= 28)
      _tryhere_area.addOnUnhandledKeyEventListener(
          this.new Tryhere_OnUnhandledKeyEventListener());

    _tryhere_area.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (s != null && s.length() > 0) {
          _tryhere_text.setText("Text length: " + s.length() + " chars | Active typing");
        } else {
          _tryhere_text.setText("Keystroke Event: None");
        }
      }
      @Override
      public void afterTextChanged(Editable s) {}
    });

    _handler = new Handler(getMainLooper(), this);
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    updateImeStatus();
  }

  void updateImeStatus()
  {
    try
    {
      boolean enabled = isImeEnabled(this);
      boolean selected = isImeSelected(this);

      if (_pill_enable != null)
      {
        if (enabled)
        {
          _pill_enable.setText(R.string.futo_status_enabled);
          _pill_enable.setTextColor(0xFF34D399);
          _pill_enable.setBackgroundResource(R.drawable.futo_badge_green);
        }
        else
        {
          _pill_enable.setText(R.string.futo_status_not_enabled);
          _pill_enable.setTextColor(0xFFFBBF24);
          _pill_enable.setBackgroundResource(R.drawable.futo_badge_amber);
        }
      }

      if (_pill_select != null)
      {
        if (selected)
        {
          _pill_select.setText("ACTIVE");
          _pill_select.setTextColor(0xFF34D399);
          _pill_select.setBackgroundResource(R.drawable.futo_badge_green);
        }
        else
        {
          _pill_select.setText("SWITCH");
          _pill_select.setTextColor(0xFFA78BFA);
          _pill_select.setBackgroundResource(R.drawable.futo_chip_bg);
        }
      }

      if (_status_badge != null)
      {
        if (enabled && selected)
        {
          _status_badge.setText(R.string.futo_status_active);
          _status_badge.setTextColor(0xFF34D399);
          _status_badge.setBackgroundResource(R.drawable.futo_badge_green);
        }
        else
        {
          _status_badge.setText(R.string.futo_status_setup_needed);
          _status_badge.setTextColor(0xFFFBBF24);
          _status_badge.setBackgroundResource(R.drawable.futo_badge_amber);
        }
      }
    }
    catch (Throwable ignored)
    {
    }
  }

  public static boolean isImeEnabled(Context context)
  {
    try
    {
      InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
      List<InputMethodInfo> list = imm.getEnabledInputMethodList();
      String pkg = context.getPackageName();
      for (InputMethodInfo imi : list)
      {
        if (imi.getPackageName().equals(pkg))
          return true;
      }
    }
    catch (Throwable ignored) {}
    return false;
  }

  public static boolean isImeSelected(Context context)
  {
    try
    {
      String current = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
      return current != null && current.contains(context.getPackageName());
    }
    catch (Throwable ignored) {}
    return false;
  }

  @Override
  public void onStart()
  {
    super.onStart();
    _animations = new ArrayList<Animatable>();
    _animations.add(find_anim(R.id.launcher_anim_swipe));
    _animations.add(find_anim(R.id.launcher_anim_round_trip));
    _animations.add(find_anim(R.id.launcher_anim_circle));
    _handler.removeMessages(0);
    _handler.sendEmptyMessageDelayed(0, 500);
  }

  @Override
  public boolean handleMessage(Message _msg)
  {
    for (Animatable anim : _animations)
    {
      if (anim != null)
        anim.start();
    }
    _handler.sendEmptyMessageDelayed(0, 3000);
    return true;
  }

  public void launch_imesettings(View _btn)
  {
    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
  }

  public void launch_imepicker(View v)
  {
    InputMethodManager imm =
      (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    imm.showInputMethodPicker();
  }

  public void launch_dictionaries_activity(View v)
  {
    startActivity(new Intent(this, DictionariesActivity.class));
  }

  public void launch_all_settings(View v)
  {
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivity(intent);
  }

  public void launch_settings_prediction(View v)
  {
    Intent intent = new Intent(this, SectionSettingsActivity.class);
    intent.putExtra(SectionSettingsActivity.EXTRA_SECTION, SectionSettingsActivity.SECTION_PREDICTION);
    startActivity(intent);
  }

  public void launch_settings_layouts(View v)
  {
    Intent intent = new Intent(this, SectionSettingsActivity.class);
    intent.putExtra(SectionSettingsActivity.EXTRA_SECTION, SectionSettingsActivity.SECTION_LAYOUTS);
    startActivity(intent);
  }

  public void launch_settings_theme(View v)
  {
    Intent intent = new Intent(this, SectionSettingsActivity.class);
    intent.putExtra(SectionSettingsActivity.EXTRA_SECTION, SectionSettingsActivity.SECTION_THEME);
    startActivity(intent);
  }

  public void launch_settings_typing(View v)
  {
    Intent intent = new Intent(this, SectionSettingsActivity.class);
    intent.putExtra(SectionSettingsActivity.EXTRA_SECTION, SectionSettingsActivity.SECTION_TYPING);
    startActivity(intent);
  }

  public void launch_settings_clipboard(View v)
  {
    Intent intent = new Intent(this, SectionSettingsActivity.class);
    intent.putExtra(SectionSettingsActivity.EXTRA_SECTION, SectionSettingsActivity.SECTION_CLIPBOARD);
    startActivity(intent);
  }

  public void clear_test_area(View v)
  {
    if (_tryhere_area != null)
    {
      _tryhere_area.setText("");
    }
    if (_tryhere_text != null)
    {
      _tryhere_text.setText("Keystroke Event: None");
    }
  }

  Animatable find_anim(int id)
  {
    ImageView img = (ImageView)findViewById(id);
    if (img == null || img.getDrawable() == null)
      return null;
    return (Animatable)img.getDrawable();
  }

  final class Tryhere_OnUnhandledKeyEventListener implements View.OnUnhandledKeyEventListener
  {
    public boolean onUnhandledKeyEvent(View v, KeyEvent ev)
    {
      // Don't handle the back key
      if (ev.getKeyCode() == KeyEvent.KEYCODE_BACK)
        return false;
      // Key release of modifiers would erase interesting data
      if (KeyEvent.isModifierKey(ev.getKeyCode()))
        return false;
      StringBuilder s = new StringBuilder("Key: ");
      if (ev.isAltPressed()) s.append("Alt+");
      if (ev.isShiftPressed()) s.append("Shift+");
      if (ev.isCtrlPressed()) s.append("Ctrl+");
      if (ev.isMetaPressed()) s.append("Meta+");
      String kc = KeyEvent.keyCodeToString(ev.getKeyCode());
      s.append(kc.replaceFirst("^KEYCODE_", ""));
      _tryhere_text.setText(s.toString());
      return false;
    }
  }
}
