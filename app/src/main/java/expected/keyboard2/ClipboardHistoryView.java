package expected.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ClipboardHistoryView extends NonScrollListView
  implements ClipboardHistoryService.OnClipboardHistoryChange
{
  List<String> _history;
  ClipboardHistoryService _service;
  ClipboardEntriesAdapter _adapter;

  public ClipboardHistoryView(Context ctx, AttributeSet attrs)
  {
    super(ctx, attrs);
    _history = new ArrayList<>();
    _adapter = this.new ClipboardEntriesAdapter();
    _service = ClipboardHistoryService.get_service(ctx);
    if (_service != null)
    {
      _service.set_on_clipboard_history_change(this);
      _history = _service.clear_expired_and_get_history();
    }
    setAdapter(_adapter);
  }

  ClipboardPinView findPinView()
  {
    View root = getRootView();
    if (root != null)
    {
      ClipboardPinView v = root.findViewById(R.id.clipboard_pin_view);
      if (v != null) return v;
    }
    ViewParent parent = getParent();
    while (parent instanceof View)
    {
      ClipboardPinView v = ((View) parent).findViewById(R.id.clipboard_pin_view);
      if (v != null) return v;
      parent = parent.getParent();
    }
    return null;
  }

  /** The history entry at index [pos] is removed from the history and added to
      the list of pinned clipboards. */
  public void pin_entry(int pos)
  {
    if (pos < 0 || pos >= _history.size())
      return;
    ClipboardPinView v = findPinView();
    String clip = _history.get(pos);
    if (v != null && clip != null)
      v.add_entry(clip);
    if (_service != null && clip != null)
      _service.remove_history_entry(clip);
  }

  /** Send the specified entry to the editor. */
  public void paste_entry(int pos)
  {
    if (pos >= 0 && pos < _history.size())
    {
      String clip = _history.get(pos);
      if (clip != null)
        ClipboardHistoryService.paste(clip);
    }
  }

  @Override
  public void on_clipboard_history_change()
  {
    post(new Runnable() {
      @Override
      public void run()
      {
        update_data();
      }
    });
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility)
  {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == View.VISIBLE)
      update_data();
  }

  void update_data()
  {
    if (_service != null)
    {
      List<String> list = _service.clear_expired_and_get_history();
      if (list != null)
        _history = list;
    }
    if (_adapter != null)
      _adapter.notifyDataSetChanged();
    invalidate();
  }

  class ClipboardEntriesAdapter extends BaseAdapter
  {
    public ClipboardEntriesAdapter() {}

    @Override
    public int getCount() { return _history != null ? _history.size() : 0; }
    @Override
    public Object getItem(int pos) { return (pos >= 0 && pos < _history.size()) ? _history.get(pos) : ""; }
    @Override
    public long getItemId(int pos) { return (pos >= 0 && pos < _history.size()) ? _history.get(pos).hashCode() : pos; }

    @Override
    public View getView(final int pos, View v, ViewGroup _parent)
    {
      if (v == null)
        v = View.inflate(getContext(), R.layout.clipboard_history_entry, null);
      if (pos < 0 || pos >= _history.size())
        return v;

      TextView tv = v.findViewById(R.id.clipboard_entry_text);
      if (tv != null)
        tv.setText(_history.get(pos));

      View addPin = v.findViewById(R.id.clipboard_entry_addpin);
      if (addPin != null)
      {
        addPin.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v) { pin_entry(pos); }
        });
      }

      View pasteBtn = v.findViewById(R.id.clipboard_entry_paste);
      if (pasteBtn != null)
      {
        pasteBtn.setOnClickListener(new View.OnClickListener()
        {
          @Override
          public void onClick(View v) { paste_entry(pos); }
        });
      }
      return v;
    }
  }
}
