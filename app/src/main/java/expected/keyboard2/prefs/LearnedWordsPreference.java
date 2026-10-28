package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.preference.Preference;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.List;
import java.util.Locale;
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
    UserDictionary dict = UserDictionary.getInstance(ctx);
    int removedDuplicates = dict.removeDuplicatesAndSave();
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
    header.setTextColor(learnEnabled ? 0xFFFFFFFF : 0xFFA3A3A3);
    header.setPadding(0,0,0,16);
    header.setOnClickListener(v -> {
      boolean cur = prefs.getBoolean("learn_words_enabled", true);
      prefs.edit().putBoolean("learn_words_enabled", !cur).apply();
      dict.setLearnEnabled(!cur);
      header.setText(!cur ? "Learning: ON (tap to toggle)" : "Learning: OFF (tap to toggle)");
      header.setTextColor(!cur ? 0xFFFFFFFF : 0xFFA3A3A3);
      Toast.makeText(ctx, !cur ? "Learning enabled" : "Learning disabled", Toast.LENGTH_SHORT).show();
    });
    root.addView(header);

    TextView count = new TextView(ctx);
    count.setTextSize(12);
    count.setTextColor(0xFFA3A3A3);
    count.setPadding(0,0,0,12);
    root.addView(count);

    // Search box to filter the learned words list.
    EditText search = new EditText(ctx);
    search.setHint("Search learned words");
    search.setSingleLine(true);
    search.setTextSize(14);
    search.setTextColor(0xFFFFFFFF);
    search.setHintTextColor(0xFF737373);
    search.setBackgroundResource(expected.keyboard2.R.drawable.cyber_edittext_bg);
    search.setPadding(16, 12, 16, 12);
    LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    searchLp.bottomMargin = 12;
    root.addView(search, searchLp);

    ListView lv = new ListView(ctx);
    lv.setDivider(new ColorDrawable(0xFF2A2A2A));
    lv.setDividerHeight(1);
    final List<Map.Entry<String,Integer>> filtered = new ArrayList<>(words);
    // Custom adapter to show word + freq
    ArrayAdapter<Map.Entry<String,Integer>> adapter = new ArrayAdapter<Map.Entry<String,Integer>>(ctx, android.R.layout.simple_list_item_2, android.R.id.text1, filtered) {
      @Override
      public View getView(int pos, View convertView, ViewGroup parent) {
        View v = super.getView(pos, convertView, parent);
        TextView t1 = v.findViewById(android.R.id.text1);
        TextView t2 = v.findViewById(android.R.id.text2);
        Map.Entry<String,Integer> e = getItem(pos);
        t1.setText(e.getKey());
        t1.setTextColor(0xFFFFFFFF);
        t1.setTextSize(14);
        t2.setText("freq: " + e.getValue());
        t2.setTextColor(0xFFA3A3A3);
        t2.setTextSize(11);
        v.setPadding(24,16,24,16);
        return v;
      }
    };
    lv.setAdapter(adapter);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);

    update_word_count(count, words.size(), words.size(), removedDuplicates);

    search.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
      @Override public void afterTextChanged(Editable s) {
        String q = s.toString().trim().toLowerCase(Locale.ROOT);
        filtered.clear();
        if (q.isEmpty()) {
          filtered.addAll(words);
        } else {
          for (Map.Entry<String,Integer> e : words) {
            if (e.getKey().toLowerCase(Locale.ROOT).contains(q))
              filtered.add(e);
          }
        }
        adapter.notifyDataSetChanged();
        update_word_count(count, filtered.size(), words.size(), removedDuplicates);
      }
    });

    lv.setOnItemClickListener((parent, view, pos, id) -> {
      if (pos < 0 || pos >= filtered.size())
        return;
      Map.Entry<String,Integer> e = filtered.get(pos);
      new AlertDialog.Builder(ctx)
        .setTitle("Manage \"" + e.getKey() + "\"")
        .setMessage("Freq: " + e.getValue())
        .setPositiveButton("Delete", (d,w) -> {
          dict.deleteWord(e.getKey());
          words.remove(e);
          filtered.remove(e);
          adapter.notifyDataSetChanged();
          update_word_count(count, filtered.size(), words.size(), removedDuplicates);
          setSummary(words.size() + " words");
          Toast.makeText(ctx, "Deleted", Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)
        .setNeutralButton("Delete & never learn", (d,w) -> {
          dict.addToBlocklist(e.getKey());
          words.remove(e);
          filtered.remove(e);
          adapter.notifyDataSetChanged();
          update_word_count(count, filtered.size(), words.size(), removedDuplicates);
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
            filtered.clear();
            adapter.notifyDataSetChanged();
            update_word_count(count, 0, 0, 0);
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

  private static void update_word_count(TextView count, int shown, int total, int removedDuplicates) {
    String text;
    if (shown == total)
      text = total + " learned words";
    else
      text = shown + " of " + total + " learned words";
    if (removedDuplicates > 0)
      text += " (" + removedDuplicates + " duplicates removed)";
    count.setText(text);
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
    hint.setTextColor(0xFFA3A3A3);
    hint.setPadding(0,0,0,12);
    root.addView(hint);
    ListView lv = new ListView(ctx);
    ArrayAdapter<String> ad = new ArrayAdapter<String>(ctx, android.R.layout.simple_list_item_1, list) {
      @Override public View getView(int pos, View cv, ViewGroup p) {
        TextView tv = (TextView) super.getView(pos, cv, p);
        tv.setTextColor(0xFFFFFFFF);
        return tv;
      }
    };
    lv.setAdapter(ad);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);

    final AlertDialog[] dlgRef = new AlertDialog[1];

    lv.setOnItemClickListener((parent, view, pos, id) -> {
      String w = list.get(pos);
      new AlertDialog.Builder(ctx)
        .setTitle("Unblock?")
        .setMessage("Allow \"" + w + "\" to be learned again?")
        .setPositiveButton("Unblock", (d,ww) -> {
          dict.removeFromBlocklist(w);
          list.remove(pos);
          ad.notifyDataSetChanged();
          if (dlgRef[0] != null)
            dlgRef[0].setTitle("Blocklist (" + list.size() + ")");
          Toast.makeText(ctx, "Unblocked", Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton("Cancel", null)
        .show();
    });
    AlertDialog bdlg = new AlertDialog.Builder(ctx)
      .setTitle("Blocklist (" + list.size() + ")")
      .setView(root)
      .setPositiveButton("Close", null)
      .setNegativeButton("Clear blocklist", (d,w) -> {
        dict.clearBlocklist();
        list.clear();
        ad.notifyDataSetChanged();
        if (dlgRef[0] != null)
          dlgRef[0].setTitle("Blocklist (0)");
        Toast.makeText(ctx, "Blocklist cleared", Toast.LENGTH_SHORT).show();
      })
      .create();
    dlgRef[0] = bdlg;
    bdlg.show();
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    Context ctx = getContext();
    UserDictionary d = UserDictionary.getInstance(ctx);
    d.removeDuplicatesAndSave();
    setSummary(d.getAllWordsSorted().size() + " words");
  }
}
