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
import java.util.regex.Pattern;

public class CustomEmailsPreference extends Preference {
  
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  public CustomEmailsPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTitle("Custom emails");
    setSummary("Manage emails for login fields");
  }

  @Override
  protected void onClick() {
    super.onClick();
    showDialog();
  }

  private void showDialog() {
    Context ctx = getContext();
    SharedPreferences prefs = getSharedPreferences();
    Set<String> set = prefs.getStringSet("custom_emails", null);
    if (set == null) set = new HashSet<>();
    final ArrayList<String> list = new ArrayList<>(set);
    Collections.sort(list, String.CASE_INSENSITIVE_ORDER);

    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24,24,24,24);

    TextView hint = new TextView(ctx);
    hint.setText("Tap to edit, long-press to delete. Emails appear scrollable in email/phone fields and on 3-letter match elsewhere.");
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
        if (!isValidEmail(newVal)) { Toast.makeText(ctx, "Invalid email", Toast.LENGTH_SHORT).show(); return; }
        if (list.contains(newVal) && !newVal.equals(old)) { Toast.makeText(ctx, "Already exists", Toast.LENGTH_SHORT).show(); return; }
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
      .setTitle("Custom Emails")
      .setView(root)
      .setPositiveButton("Save", (d,w) -> {
        Set<String> ns = new HashSet<>(list);
        prefs.edit().putStringSet("custom_emails", ns).apply();
        setSummary(list.size() + " emails");
        Toast.makeText(ctx, "Saved " + list.size() + " emails", Toast.LENGTH_SHORT).show();
      })
      .setNegativeButton("Cancel", null)
      .setNeutralButton("Add", null)
      .create();
    dlg.setOnShowListener(d -> {
      dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
        showAddDialog(ctx, newVal -> {
          if (newVal == null || newVal.trim().isEmpty()) return;
          newVal = newVal.trim();
          if (!isValidEmail(newVal)) { Toast.makeText(ctx, "Invalid email", Toast.LENGTH_SHORT).show(); return; }
          if (list.contains(newVal)) { Toast.makeText(ctx, "Already exists", Toast.LENGTH_SHORT).show(); return; }
          list.add(newVal);
          Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
          adapter.notifyDataSetChanged();
        });
      });
    });
    dlg.show();
    setSummary(list.size() + " emails");
  }

  private boolean isValidEmail(String e) { return EMAIL_PATTERN.matcher(e).matches(); }

  private interface Cb { void onResult(String v); }
  private void showAddDialog(Context ctx, Cb cb) {
    LinearLayout l = new LinearLayout(ctx); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40,30,40,10);
    TextView tv = new TextView(ctx); tv.setText("Email (e.g. name@example.com):"); tv.setTextColor(0xFFF1F5F9); l.addView(tv);
    EditText et = new EditText(ctx); et.setHint("user@example.com"); et.setTextColor(0xFFF1F5F9); et.setHintTextColor(0xFF94A3B8); et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); et.setSingleLine(true); l.addView(et);
    new AlertDialog.Builder(ctx).setTitle("Add Email").setView(l).setPositiveButton("Add", (d,w) -> cb.onResult(et.getText().toString())).setNegativeButton("Cancel", null).show();
  }
  private void showEditDialog(Context ctx, String old, Cb cb) {
    LinearLayout l = new LinearLayout(ctx); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(40,30,40,10);
    TextView tv = new TextView(ctx); tv.setText("Edit email:"); tv.setTextColor(0xFFF1F5F9); l.addView(tv);
    EditText et = new EditText(ctx); et.setText(old); et.setTextColor(0xFFF1F5F9); et.setSelectAllOnFocus(true); et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); et.setSingleLine(true); l.addView(et);
    new AlertDialog.Builder(ctx).setTitle("Edit").setView(l).setPositiveButton("Save", (d,w) -> cb.onResult(et.getText().toString())).setNegativeButton("Cancel", null).show();
  }

  @Override protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    Set<String> s = getSharedPreferences().getStringSet("custom_emails", null);
    setSummary(s == null ? "No emails" : s.size() + " emails");
  }
}
