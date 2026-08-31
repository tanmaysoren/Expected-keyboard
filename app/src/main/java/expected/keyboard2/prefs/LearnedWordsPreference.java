package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Map;
import expected.keyboard2.DirectBootAwarePreferences;
import expected.keyboard2.prediction.UserDictionary;

public class LearnedWordsPreference extends Preference {
  
  public LearnedWordsPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setTitle("Learned words");
    setSummary("View, delete, and block learned words");
  }

  @Override
  protected void onClick() {
    super.onClick();
    showDialog();
  }

  private void showDialog() {
    Context ctx = getContext();
    UserDictionary dict = new UserDictionary(ctx);
    List<Map.Entry<String,Integer>> words = dict.getAllWordsSorted();

    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24,24,24,24);

    // Header with toggle learn enabled
    SharedPreferences prefs = DirectBootAwarePreferences.get_protected_prefs(ctx, "futo_user_dictionary");
    boolean learnEnabled = prefs.getBoolean("learn_words_enabled", true);
    TextView header = new TextView(ctx);
    header.setText(learnEnabled ? "Learning: ON (tap to toggle)" : "Learning: OFF (tap to toggle)");
    header.setTextSize(13);
    header.setTextColor(learnEnabled ? 0xFF34D399 : 0xFFF87171);
    header.setPadding(0,0,0,16);
    header.setOnClickListener(v -> {
      boolean cur = prefs.getBoolean("learn_words_enabled", true);
      prefs.edit().putBoolean("learn_words_enabled", !cur).apply();
      dict.setLearnEnabled(!cur);
      header.setText(!cur ? "Learning: ON (tap to toggle)" : "Learning: OFF (tap to toggle)");
      header.setTextColor(!cur ? 0xFF34D399 : 0xFFF87171);
      Toast.makeText(ctx, !cur ? "Learning enabled" : "Learning disabled", Toast.LENGTH_SHORT).show();
    });
    root.addView(header);

    TextView count = new TextView(ctx);
    count.setText(words.size() + " learned words");
    count.setTextSize(12);
    count.setTextColor(0xFF94A3B8);
    count.setPadding(0,0,0,12);
    root.addView(count);

    ListView lv = new ListView(ctx);
    // Custom adapter to show word + freq
    ArrayAdapter<Map.Entry<String,Integer>> adapter = new ArrayAdapter<Map.Entry<String,Integer>>(ctx, android.R.layout.simple_list_item_2, android.R.id.text1, words) {
      @Override
      public View getView(int pos, View convertView, ViewGroup parent) {
        View v = super.getView(pos, convertView, parent);
        TextView t1 = v.findViewById(android.R.id.text1);
        TextView t2 = v.findViewById(android.R.id.text2);
        Map.Entry<String,Integer> e = getItem(pos);
        t1.setText(e.getKey());
        t1.setTextColor(0xFFF1F5F9);
        t1.setTextSize(14);
        t2.setText("freq: " + e.getValue());
        t2.setTextColor(0xFF94A3B8);
        t2.setTextSize(11);
        v.setPadding(24,16,24,16);
        return v;
      }
    };
    lv.setAdapter(adapter);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);

    lv.setOnItemClickListener((parent, view, pos, id) -> {
      Map.Entry<String,Integer> e = words.get(pos);
      new AlertDialog.Builder(ctx)
        .setTitle("Manage \"" + e.getKey() + "\"")
        .setMessage("Freq: " + e.getValue())
        .setPositiveButton("Delete", (d,w) -> {
          dict.deleteWord(e.getKey());
          words.remove(pos);
          adapter.notifyDataSetChanged();
          count.setText(words.size() + " learned words");
          setSummary(words.size() + " words");
          Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)
        .setNeutralButton("Delete & never learn", (d,w) -> {
          dict.addToBlocklist(e.getKey());
          words.remove(pos);
          adapter.notifyDataSetChanged();
          count.setText(words.size() + " learned words");
          setSummary(words.size() + " words");
          Toast.makeText(ctx, "Blocked — will never learn again", Toast.LENGTH_SHORT).show();
        })
        .show();
    });

    AlertDialog dlg = new AlertDialog.Builder(ctx)
      .setTitle("Learned Words")
      .setView(root)
      .setPositiveButton("Close", null)
      .setNeutralButton("Clear all", (d,w) -> {
        new AlertDialog.Builder(ctx)
          .setTitle("Clear all?")
          .setMessage("Delete all " + words.size() + " learned words? Blocklist kept.")
          .setPositiveButton("Clear", (dd,ww) -> {
            dict.clearAllWords();
            words.clear();
            adapter.notifyDataSetChanged();
            count.setText("0 learned words");
            setSummary("0 words");
          })
          .setNegativeButton("Cancel", null)
          .show();
      })
      .setNegativeButton("Blocklist", (d,w) -> showBlocklist(ctx, dict))
      .create();
    dlg.show();
    setSummary(words.size() + " words");
  }

  private void showBlocklist(Context ctx, UserDictionary dict) {
    java.util.Set<String> block = dict.getBlocklist();
    java.util.List<String> list = new java.util.ArrayList<>(block);
    java.util.Collections.sort(list, String.CASE_INSENSITIVE_ORDER);
    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(24,24,24,24);
    TextView hint = new TextView(ctx);
    hint.setText("Blocked words — never learned again. Tap to unblock.");
    hint.setTextSize(11);
    hint.setTextColor(0xFF94A3B8);
    hint.setPadding(0,0,0,12);
    root.addView(hint);
    ListView lv = new ListView(ctx);
    ArrayAdapter<String> ad = new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, list) {
      @Override public View getView(int pos, View cv, ViewGroup p) {
        TextView tv = (TextView) super.getView(pos, cv, p);
        tv.setTextColor(0xFFF1F5F9);
        return tv;
      }
    };
    lv.setAdapter(ad);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);
    lv.setOnItemClickListener((parent, view, pos, id) -> {
      String w = list.get(pos);
      new AlertDialog.Builder(ctx)
        .setTitle("Unblock?")
        .setMessage("Allow \"" + w + "\" to be learned again?")
        .setPositiveButton("Unblock", (d,ww) -> {
          // Directly manipulate prefs blocklist
          android.content.SharedPreferences prefs = DirectBootAwarePreferences.get_protected_prefs(ctx, "futo_user_dictionary");
          java.util.Set<String> b = prefs.getStringSet("user_blocklist", new java.util.HashSet<>());
          java.util.Set<String> nb = new java.util.HashSet<>(b);
          nb.remove(w.toLowerCase(java.util.Locale.ROOT));
          prefs.edit().putStringSet("user_blocklist", nb).apply();
          list.remove(pos);
          ad.notifyDataSetChanged();
          Toast.makeText(ctx, "Unblocked", Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)
        .show();
    });
    new AlertDialog.Builder(ctx)
      .setTitle("Blocklist (" + list.size() + ")")
      .setView(root)
      .setPositiveButton("Close", null)
      .setNegativeButton("Clear blocklist", (d,w) -> {
        android.content.SharedPreferences prefs = DirectBootAwarePreferences.get_protected_prefs(ctx, "futo_user_dictionary");
        prefs.edit().remove("user_blocklist").apply();
        list.clear();
        ad.notifyDataSetChanged();
        Toast.makeText(ctx, "Blocklist cleared", Toast.LENGTH_SHORT).show();
      })
      .show();
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    Context ctx = getContext();
    UserDictionary d = new UserDictionary(ctx);
    setSummary(d.getAllWordsSorted().size() + " words");
  }
}
