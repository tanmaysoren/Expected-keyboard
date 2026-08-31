package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TerminalCommandsPreference extends Preference {
  
  public TerminalCommandsPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTitle("Terminal Commands");
    setSummary("Add, edit or remove commands for terminal apps (Termux)");
  }

  @Override
  protected void onClick() {
    super.onClick();
    showDialog();
  }

  private void showDialog() {
    Context ctx = getContext();
    SharedPreferences prefs = getSharedPreferences();
    Set<String> set = prefs.getStringSet("terminal_commands", null);
    if (set == null) {
      set = new HashSet<>();
      set.add("ls"); set.add("ls -la"); set.add("cd"); set.add("pwd");
      set.add("git status"); set.add("clear"); set.add("exit"); set.add("vim");
    }
    final ArrayList<String> list = new ArrayList<>(set);
    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);

    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24, 24, 24, 24);

    TextView hint = new TextView(ctx);
    hint.setText("Tap to edit, long-press to delete. Commands appear scrollable in terminal apps.");
    hint.setTextSize(12);
    hint.setTextColor(0xFF94A3B8);
    hint.setPadding(0,0,0,16);
    root.addView(hint);

    ListView lv = new ListView(ctx);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, list) {
      @Override
      public View getView(int pos, View convertView, ViewGroup parent) {
        TextView tv = (TextView) super.getView(pos, convertView, parent);
        tv.setTextColor(0xFFF1F5F9);
        tv.setPadding(24,24,24,24);
        tv.setTextSize(14);
        return tv;
      }
    };
    lv.setAdapter(adapter);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);

    // Add/Edit/Delete handlers
    lv.setOnItemClickListener((parent, view, position, id) -> {
      String old = list.get(position);
      showEditDialog(ctx, old, newVal -> {
        if (newVal == null || newVal.trim().isEmpty()) return;
        newVal = newVal.trim();
        list.set(position, newVal);
        adapter.notifyDataSetChanged();
      });
    });
    lv.setOnItemLongClickListener((parent, view, position, id) -> {
      new AlertDialog.Builder(ctx)
        .setTitle("Delete?")
        .setMessage("Delete \"" + list.get(position) + "\" ?")
        .setPositiveButton("Delete", (d,w) -> {
          list.remove(position);
          adapter.notifyDataSetChanged();
        })
        .setNegativeButton("Cancel", null)
        .show();
      return true;
    });

    AlertDialog dlg = new AlertDialog.Builder(ctx)
      .setTitle("Terminal Commands")
      .setView(root)
      .setPositiveButton("Save", (d,w) -> {
        Set<String> newSet = new HashSet<>(list);
        SharedPreferences.Editor e = prefs.edit();
        e.putStringSet("terminal_commands", newSet);
        e.apply();
        setSummary(list.size() + " commands");
        Toast.makeText(ctx, "Saved " + list.size() + " commands", Toast.LENGTH_SHORT).show();
      })
      .setNegativeButton("Cancel", null)
      .setNeutralButton("Add", null)
      .create();

    dlg.setOnShowListener(d -> {
      // Override neutral button to not dismiss immediately
      dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
        showAddDialog(ctx, newVal -> {
          if (newVal == null || newVal.trim().isEmpty()) {
            Toast.makeText(ctx, "Empty", Toast.LENGTH_SHORT).show(); return;
          }
          newVal = newVal.trim();
          if (list.contains(newVal)) {
            Toast.makeText(ctx, "Already exists", Toast.LENGTH_SHORT).show(); return;
          }
          list.add(newVal);
          Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
          adapter.notifyDataSetChanged();
        });
      });
    });
    dlg.show();
    setSummary(list.size() + " commands");
  }

  private interface Callback { void onResult(String val); }

  private void showAddDialog(Context ctx, Callback cb) {
    LinearLayout l = new LinearLayout(ctx);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(40,30,40,10);
    TextView tv = new TextView(ctx); tv.setText("Command / script:"); tv.setTextColor(0xFFF1F5F9); l.addView(tv);
    EditText et = new EditText(ctx);
    et.setHint("e.g. git log --oneline");
    et.setTextColor(0xFFF1F5F9); et.setHintTextColor(0xFF94A3B8);
    et.setInputType(InputType.TYPE_CLASS_TEXT);
    et.setSingleLine(true);
    l.addView(et);
    new AlertDialog.Builder(ctx)
      .setTitle("Add Command")
      .setView(l)
      .setPositiveButton("Add", (d,w) -> cb.onResult(et.getText().toString()))
      .setNegativeButton("Cancel", null)
      .show();
  }

  private void showEditDialog(Context ctx, String old, Callback cb) {
    LinearLayout l = new LinearLayout(ctx);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(40,30,40,10);
    TextView tv = new TextView(ctx); tv.setText("Edit command:"); tv.setTextColor(0xFFF1F5F9); l.addView(tv);
    EditText et = new EditText(ctx);
    et.setText(old);
    et.setTextColor(0xFFF1F5F9);
    et.setSelectAllOnFocus(true);
    et.setInputType(InputType.TYPE_CLASS_TEXT);
    et.setSingleLine(true);
    l.addView(et);
    new AlertDialog.Builder(ctx)
      .setTitle("Edit")
      .setView(l)
      .setPositiveButton("Save", (d,w) -> cb.onResult(et.getText().toString()))
      .setNegativeButton("Cancel", null)
      .show();
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    SharedPreferences prefs = getSharedPreferences();
    Set<String> s = prefs.getStringSet("terminal_commands", null);
    setSummary(s == null ? "Default commands" : s.size() + " commands");
  }
}
