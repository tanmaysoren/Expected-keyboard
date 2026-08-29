package expected.keyboard2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EditingPaneView extends LinearLayout
{
  private boolean _selectionMode = false;
  private TextView _btnSelectMode;

  public EditingPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();

    // Top action bar buttons
    setupClick(R.id.edit_btn_back, v -> {
      Config.globalConfig().handler.key_up(KeyValue.getKeyByName("switch_back_editing"), Pointers.Modifiers.EMPTY);
    });

    setupClick(R.id.edit_btn_undo, v -> sendEditing(KeyValue.Editing.UNDO));
    setupClick(R.id.edit_btn_redo, v -> sendEditing(KeyValue.Editing.REDO));
    setupClick(R.id.edit_btn_select_all, v -> sendEditing(KeyValue.Editing.SELECT_ALL));
    setupClick(R.id.edit_btn_cut, v -> sendEditing(KeyValue.Editing.CUT));
    setupClick(R.id.edit_btn_copy, v -> sendEditing(KeyValue.Editing.COPY));
    setupClick(R.id.edit_btn_paste, v -> sendEditing(KeyValue.Editing.PASTE));

    // Directional keys
    setupKey(R.id.edit_btn_up, KeyEvent.KEYCODE_DPAD_UP);
    setupKey(R.id.edit_btn_down, KeyEvent.KEYCODE_DPAD_DOWN);
    setupKey(R.id.edit_btn_left, KeyEvent.KEYCODE_DPAD_LEFT);
    setupKey(R.id.edit_btn_right, KeyEvent.KEYCODE_DPAD_RIGHT);

    // Fast navigation keys
    setupKey(R.id.edit_btn_home, KeyEvent.KEYCODE_MOVE_HOME);
    setupKey(R.id.edit_btn_end, KeyEvent.KEYCODE_MOVE_END);
    setupKey(R.id.edit_btn_page_up, KeyEvent.KEYCODE_PAGE_UP);
    setupKey(R.id.edit_btn_page_down, KeyEvent.KEYCODE_PAGE_DOWN);

    // Word navigation
    setupClick(R.id.edit_btn_word_left, v -> {
      if (_selectionMode) {
        sendKey(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
      } else {
        sendEditing(KeyValue.Editing.WORD_LEFT);
      }
    });

    setupClick(R.id.edit_btn_word_right, v -> {
      if (_selectionMode) {
        sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON);
      } else {
        sendEditing(KeyValue.Editing.WORD_RIGHT);
      }
    });

    // Delete keys
    setupKey(R.id.edit_btn_backspace, KeyEvent.KEYCODE_DEL);
    setupKey(R.id.edit_btn_delete, KeyEvent.KEYCODE_FORWARD_DEL);
    setupKey(R.id.edit_btn_enter, KeyEvent.KEYCODE_ENTER);

    // Select Mode toggle button
    _btnSelectMode = findViewById(R.id.edit_btn_select_mode);
    if (_btnSelectMode != null)
    {
      _btnSelectMode.setOnClickListener(v -> {
        _selectionMode = !_selectionMode;
        updateSelectModeUi();
      });
      updateSelectModeUi();
    }
  }

  private void updateSelectModeUi()
  {
    if (_btnSelectMode != null)
    {
      if (_selectionMode)
      {
        _btnSelectMode.setText(R.string.editing_selecting);
        _btnSelectMode.setSelected(true);
      }
      else
      {
        _btnSelectMode.setText(R.string.editing_select);
        _btnSelectMode.setSelected(false);
      }
    }
  }

  private void setupClick(int resId, View.OnClickListener listener)
  {
    View v = findViewById(resId);
    if (v != null)
    {
      v.setOnClickListener(view -> {
        VibratorCompat.vibrate(view);
        listener.onClick(view);
      });
    }
  }

  private void setupKey(int resId, int keyCode)
  {
    setupClick(resId, v -> {
      int meta = _selectionMode ? (KeyEvent.META_SHIFT_ON | KeyEvent.META_SHIFT_LEFT_ON) : 0;
      sendKey(keyCode, meta);
    });
  }

  private void sendKey(int keyCode, int meta)
  {
    KeyEventHandler handler = (KeyEventHandler) Config.globalConfig().handler;
    if (handler != null)
    {
      handler.send_key_down_up(keyCode, meta);
    }
  }

  private void sendEditing(KeyValue.Editing action)
  {
    KeyEventHandler handler = (KeyEventHandler) Config.globalConfig().handler;
    if (handler != null)
    {
      handler.handle_editing_key(action);
    }
  }
}
