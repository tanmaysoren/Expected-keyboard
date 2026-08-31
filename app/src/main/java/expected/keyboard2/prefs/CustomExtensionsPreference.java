package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.text.InputType;
import android.util.AttributeSet;
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

public class CustomExtensionsPreference extends Preference {
  
  public CustomExtensionsPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTitle("Custom extensions");
    setSummary("Manage dot key popup extensions");
  }

  @Override
  protected void onClick() {
    super.onClick();
    showDialog();
  }

  private void showDialog() {
    Context ctx = getContext();
    SharedPreferences prefs = getSharedPreferences();
    Set<String> set = prefs.getStringSet("custom_extensions", null);
    if (set == null) {
      set = new HashSet<>();
      set.add(".com"); set.add(".org"); set.add(".net"); set.add(".edu"); set.add(".gov");
      set.add(".io"); set.add(".co"); set.add(".in"); set.add(".app");
    }
    final ArrayList<String> list = new ArrayList<>(set);
    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);

    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24,24,24,24);

    TextView hint = new TextView(ctx);
    hint.setText("Tap to edit, long-press to delete. These show in the '.' popup (hold '.' and slide).");
    hint.setTextSize(11);
    hint.setTextColor(0xFF94A3B8);
    hint.setPadding(0,0,0,16);
    root.addView(hint);

    ListView lv = new ListView(ctx);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, list) {
      @Override public View getView(int pos, View cv, ViewGroup p) {
        TextView tv = (TextView) super.getView(pos, cv, p);
        tv.setTextColor(0xFFF1F5F9);
        tv.setPadding(24,24,24,24);
        tv.setTextSize(14);
        return tv;
      }
    };
    lv.setAdapter(adapter);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);

    lv.setOnItemClickListener((parent, view, pos, id) -> {
      String old = list.get(pos);
      showEditDialog(ctx, old, newVal -> {
        if (newVal == null || newVal.trim().isEmpty()) return;
        newVal = newVal.trim();
        if (!newVal.startsWith(".")) newVal = "." + newVal;
        list.set(pos, newVal);
        Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
        adapter.notifyDataSetChanged();
      });
    });
    lv.setOnItemLongClickListener((parent, view, pos, id) -> {
      new AlertDialog.Builder(ctx)
        .setTitle("Delete?")
        .setMessage("Delete \"" + list.get(pos) + "\" ?")
        .setPositiveButton("Delete", (d,w) -> {
          list.remove(pos);
          adapter.notifyDataSetChanged();
        })
        .setNegativeButton("Cancel", null)
        .show();
      return true;
    });

    AlertDialog dlg = new AlertDialog.Builder(ctx)
      .setTitle("Custom Extensions")
      .setView(root)
      .setPositiveButton("Save", (d,w) -> {
        Set<String> ns = new HashSet<>(list);
        prefs.edit().putStringSet("custom_extensions", ns).apply();
        setSummary(list.size() + " extensions");
        Toast.makeText(ctx, "Saved " + list.size() + " extensions", Toast.LENGTH_SHORT).show();
      })
      .setNegativeButton("Cancel", null)
      .setNeutralButton("Add", null)
      .create();
    dlg.setOnShowListener(d -> {
      dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
        showAddDialog(ctx, newVal -> {
          if (newVal == null || newVal.trim().isEmpty()) return;
          newVal = newVal.trim();
          if (!newVal.startsWith(".")) newVal = "." + newVal;
          if (list.contains(newVal)) { Toast.makeText(ctx, "Already exists", Toast.LENGTH_SHORT).show(); return; }
          list.add(newVal);
          Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
          adapter.notifyDataSetChanged();
        });
      });
    });
    dlg.show();
    setSummary(list.size() + " extensions");
  }

  private interface Cb { void onResult(String v); }
  private void showAddDialog(Context ctx, Cb cb) {
    LinearLayout l = new LinearLayout(ctx); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40,30,40,10);
    TextView tv = new TextView(ctx); tv.setText("Extension (e.g. .dev):"); tv.setTextColor(0xFFF1F5F9); l.addView(tv);
    EditText et = new EditText(ctx); et.setHint(".com"); et.setTextColor(0xFFF1F5F9); et.setHintTextColor(0xFF94A3B8); et.setInputType(InputType.TYPE_CLASS_TEXT); et.setSingleLine(true); l.addView(et);
    new AlertDialog.Builder(ctx).setTitle("Add Extension").setView(l).setPositiveButton("Add", (d,w) -> cb.onResult(et.getText().toString())).setNegativeButton("Cancel", null).show();
  }
  private void showEditDialog(Context ctx, String old, Cb cb) {
    LinearLayout l = new LinearLayout(ctx); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40,30,40,10);
    TextView tv = new TextView(ctx); tv.setText("Edit extension:"); tv.setTextColor(0xFFF1F5F9); l.addView(tv);
    EditText et = new EditText(ctx); et.setText(old); et.setTextColor(0xFFF1F5F9); et.setSelectAllOnFocus(true); et.setInputType(InputType.TYPE_CLASS_TEXT); et.setSingleLine(true); l.addView(et);
    new AlertDialog.Builder(ctx).setTitle("Edit").setView(l).setPositiveButton("Save", (d,w) -> cb.onResult(et.getText().toString())).setNegativeButton("Cancel", null).show();
  }

  @Override protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    Set<String> s = getSharedPreferences().getStringSet("custom_extensions", null);
    setSummary(s == null ? "Default extensions" : s.size() + " extensions");
  }
}
