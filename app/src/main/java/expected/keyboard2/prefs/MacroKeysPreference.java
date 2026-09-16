package expected.keyboard2.prefs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
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
import java.util.LinkedHashMap;
import java.util.Map;
import expected.keyboard2.DirectBootAwarePreferences;
import expected.keyboard2.MacroManager;
import expected.keyboard2.R;

public class MacroKeysPreference extends Preference
{
  private static class MacroItem
  {
    String trigger;
    String expansion;

    MacroItem(String t, String e)
    {
      this.trigger = t;
      this.expansion = e;
    }
  }

  public MacroKeysPreference(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    setTitle("Macro Keys");
    setSummary("Add shortcuts that auto-expand on swiping BL on 'I'");
  }

  @Override
  protected void onClick()
  {
    super.onClick();
    showDialog();
  }

  private void showDialog()
  {
    Context ctx = getContext();
    SharedPreferences prefs = getSharedPreferences();
    LinkedHashMap<String, String> macroMap = MacroManager.getMacros(prefs);
    final ArrayList<MacroItem> list = new ArrayList<MacroItem>();
    for (Map.Entry<String, String> entry : macroMap.entrySet())
    {
      list.add(new MacroItem(entry.getKey(), entry.getValue()));
    }

    LinearLayout root = new LinearLayout(ctx);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(32, 24, 32, 24);

    TextView hint = new TextView(ctx);
    hint.setText("Add shortcuts like @, a-z, 0-9, or words. When typed, swipe bottom-left (↙) on letter 'I' to expand into text.\n\nTap to edit, long-press to delete.");
    hint.setTextSize(12);
    hint.setTextColor(0xFF94A3B8);
    hint.setPadding(0, 0, 0, 16);
    root.addView(hint);

    TextView emptyView = new TextView(ctx);
    emptyView.setText("No macros added yet. Tap \"Add\" to create your first macro.");
    emptyView.setTextSize(14);
    emptyView.setTextColor(0xFF64748B);
    emptyView.setGravity(Gravity.CENTER);
    emptyView.setPadding(16, 48, 16, 48);

    ListView lv = new ListView(ctx);
    lv.setDivider(null);
    lv.setDividerHeight(12);

    final ArrayAdapter<MacroItem> adapter = new ArrayAdapter<MacroItem>(ctx, 0, list)
    {
      @Override
      public View getView(int pos, View convertView, ViewGroup parent)
      {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(20, 18, 20, 18);
        row.setBackgroundColor(0xFF1E293B);

        MacroItem item = getItem(pos);

        TextView triggerTv = new TextView(ctx);
        triggerTv.setText(item.trigger);
        triggerTv.setTextColor(0xFF38BDF8);
        triggerTv.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        triggerTv.setTextSize(14);
        triggerTv.setPadding(12, 6, 12, 6);
        triggerTv.setBackgroundColor(0xFF0F172A);
        row.addView(triggerTv);

        TextView arrowTv = new TextView(ctx);
        arrowTv.setText(" → ");
        arrowTv.setTextColor(0xFF94A3B8);
        arrowTv.setTextSize(14);
        row.addView(arrowTv);

        TextView expTv = new TextView(ctx);
        expTv.setText(item.expansion);
        expTv.setTextColor(0xFFF1F5F9);
        expTv.setTextSize(14);
        expTv.setSingleLine(true);
        expTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(expTv, expLp);

        return row;
      }
    };
    lv.setAdapter(adapter);

    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
    root.addView(lv, lp);
    root.addView(emptyView);

    Runnable updateEmptyState = () -> {
      if (list.isEmpty())
      {
        lv.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
      }
      else
      {
        lv.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
      }
    };
    updateEmptyState.run();

    // Click to edit
    lv.setOnItemClickListener((parent, view, position, id) -> {
      MacroItem item = list.get(position);
      showEditDialog(ctx, item.trigger, item.expansion, (newTrigger, newExp) -> {
        if (newTrigger == null || newTrigger.trim().isEmpty())
        {
          Toast.makeText(ctx, "Trigger cannot be empty", Toast.LENGTH_SHORT).show();
          return;
        }
        item.trigger = newTrigger.trim();
        item.expansion = (newExp != null) ? newExp : "";
        adapter.notifyDataSetChanged();
        updateEmptyState.run();
      });
    });

    // Long click to delete
    lv.setOnItemLongClickListener((parent, view, position, id) -> {
      MacroItem item = list.get(position);
      new AlertDialog.Builder(ctx)
        .setTitle("Delete Macro?")
        .setMessage("Delete macro \"" + item.trigger + "\" → \"" + item.expansion + "\" ?")
        .setPositiveButton("Delete", (d, w) -> {
          list.remove(position);
          adapter.notifyDataSetChanged();
          updateEmptyState.run();
        })
        .setNegativeButton("Cancel", null)
        .show();
      return true;
    });

    AlertDialog dlg = new AlertDialog.Builder(ctx)
      .setTitle("Macro Keys")
      .setView(root)
      .setPositiveButton("Save", (d, w) -> {
        LinkedHashMap<String, String> newMap = new LinkedHashMap<String, String>();
        for (MacroItem it : list)
        {
          if (it.trigger != null && !it.trigger.trim().isEmpty())
          {
            newMap.put(it.trigger.trim(), it.expansion != null ? it.expansion : "");
          }
        }
        MacroManager.saveMacros(prefs, newMap);
        // Also write directly to device-protected prefs so the keyboard
        // (which reads from device-protected storage) picks up the change
        // immediately, without waiting for SectionSettingsActivity.onStop().
        MacroManager.saveMacros(
            DirectBootAwarePreferences.get_shared_preferences(ctx), newMap);
        updateSummary(newMap.size());
        Toast.makeText(ctx, "Saved " + newMap.size() + " macro(s)", Toast.LENGTH_SHORT).show();
      })
      .setNegativeButton("Cancel", null)
      .setNeutralButton("Add", null)
      .create();

    dlg.setOnShowListener(d -> {
      dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
        showAddDialog(ctx, (newTrigger, newExp) -> {
          if (newTrigger == null || newTrigger.trim().isEmpty())
          {
            Toast.makeText(ctx, "Trigger cannot be empty", Toast.LENGTH_SHORT).show();
            return;
          }
          String trig = newTrigger.trim();
          // Check if already exists
          for (MacroItem it : list)
          {
            if (it.trigger.equalsIgnoreCase(trig))
            {
              it.expansion = (newExp != null) ? newExp : "";
              adapter.notifyDataSetChanged();
              updateEmptyState.run();
              return;
            }
          }
          list.add(new MacroItem(trig, newExp != null ? newExp : ""));
          adapter.notifyDataSetChanged();
          updateEmptyState.run();
        });
      });
    });

    dlg.show();
    updateSummary(list.size());
  }

  private interface Callback
  {
    void onResult(String trigger, String expansion);
  }

  private void showAddDialog(Context ctx, Callback cb)
  {
    LinearLayout l = new LinearLayout(ctx);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(40, 24, 40, 16);

    TextView t1 = new TextView(ctx);
    t1.setText("Trigger Shortcut (e.g. @, gpo, sh, 1):");
    t1.setTextColor(0xFF38BDF8);
    t1.setTextSize(13);
    l.addView(t1);

    EditText etTrigger = new EditText(ctx);
    etTrigger.setHint("e.g. @ or gpo");
    etTrigger.setTextColor(0xFFF1F5F9);
    etTrigger.setHintTextColor(0xFF94A3B8);
    etTrigger.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    etTrigger.setSingleLine(true);
    l.addView(etTrigger);

    TextView t2 = new TextView(ctx);
    t2.setText("Expands To (any text, code, or command):");
    t2.setTextColor(0xFF38BDF8);
    t2.setTextSize(13);
    t2.setPadding(0, 20, 0, 0);
    l.addView(t2);

    EditText etExp = new EditText(ctx);
    etExp.setHint("e.g. git push");
    etExp.setTextColor(0xFFF1F5F9);
    etExp.setHintTextColor(0xFF94A3B8);
    etExp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    etExp.setMaxLines(4);
    l.addView(etExp);

    new AlertDialog.Builder(ctx)
      .setTitle("Add Macro")
      .setView(l)
      .setPositiveButton("Add", (d, w) -> {
        cb.onResult(etTrigger.getText().toString(), etExp.getText().toString());
      })
      .setNegativeButton("Cancel", null)
      .show();
  }

  private void showEditDialog(Context ctx, String oldTrigger, String oldExp, Callback cb)
  {
    LinearLayout l = new LinearLayout(ctx);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(40, 24, 40, 16);

    TextView t1 = new TextView(ctx);
    t1.setText("Trigger Shortcut:");
    t1.setTextColor(0xFF38BDF8);
    t1.setTextSize(13);
    l.addView(t1);

    EditText etTrigger = new EditText(ctx);
    etTrigger.setText(oldTrigger);
    etTrigger.setTextColor(0xFFF1F5F9);
    etTrigger.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    etTrigger.setSingleLine(true);
    l.addView(etTrigger);

    TextView t2 = new TextView(ctx);
    t2.setText("Expands To:");
    t2.setTextColor(0xFF38BDF8);
    t2.setTextSize(13);
    t2.setPadding(0, 20, 0, 0);
    l.addView(t2);

    EditText etExp = new EditText(ctx);
    etExp.setText(oldExp);
    etExp.setTextColor(0xFFF1F5F9);
    etExp.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    etExp.setMaxLines(4);
    l.addView(etExp);

    new AlertDialog.Builder(ctx)
      .setTitle("Edit Macro")
      .setView(l)
      .setPositiveButton("Save", (d, w) -> {
        cb.onResult(etTrigger.getText().toString(), etExp.getText().toString());
      })
      .setNegativeButton("Cancel", null)
      .show();
  }

  private void updateSummary(int count)
  {
    if (count <= 0)
    {
      setSummary("No macros configured (inactive on keyboard)");
    }
    else
    {
      setSummary(count + " macro" + (count > 1 ? "s" : "") + " configured (swipable on 'I' key)");
    }
  }

  @Override
  protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
  {
    super.onSetInitialValue(restorePersistedValue, defaultValue);
    SharedPreferences prefs = getSharedPreferences();
    int count = MacroManager.getMacros(prefs).size();
    updateSummary(count);
  }
}
