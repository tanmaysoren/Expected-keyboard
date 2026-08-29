package expected.keyboard2.theme;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import expected.keyboard2.Config;
import expected.keyboard2.KeyEventHandler;
import expected.keyboard2.KeyValue;
import expected.keyboard2.Pointers;
import expected.keyboard2.R;
import expected.keyboard2.VibratorCompat;

/**
 * High-performance Theme Switcher Pane.
 * Allows users to switch themes from the utility section instantly and reliably.
 */
public class ThemeSwitcherPaneView extends LinearLayout
{
  public static class ThemeItem
  {
    public final String key;
    public final String name;
    public final String category; // "cyber", "dark", "light", "monet"
    public final int bgColor;
    public final int accentColor;
    public final int textColor;
    public final boolean isSelected;

    public ThemeItem(String key, String name, String category, int bgColor, int accentColor, int textColor, boolean isSelected)
    {
      this.key = key;
      this.name = name;
      this.category = category;
      this.bgColor = bgColor;
      this.accentColor = accentColor;
      this.textColor = textColor;
      this.isSelected = isSelected;
    }
  }

  private ImageButton btnBack;
  private ImageButton btnClose;
  private ListView listView;
  private Button chipFilterAll;
  private Button chipFilterCyber;
  private Button chipFilterDark;
  private Button chipFilterLight;
  private Button chipFilterSystem;

  private String currentCategoryFilter = "all";
  private final List<ThemeItem> allThemeItems = new ArrayList<>();
  private final List<ThemeItem> currentDisplayItems = new ArrayList<>();
  private ThemeAdapter adapter;

  public ThemeSwitcherPaneView(Context context)
  {
    super(context);
  }

  public ThemeSwitcherPaneView(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate()
  {
    super.onFinishInflate();

    btnBack = findViewById(R.id.theme_pane_btn_back);
    btnClose = findViewById(R.id.theme_pane_btn_close);
    listView = findViewById(R.id.theme_pane_list);

    chipFilterAll = findViewById(R.id.chip_theme_filter_all);
    chipFilterCyber = findViewById(R.id.chip_theme_filter_cyber);
    chipFilterDark = findViewById(R.id.chip_theme_filter_dark);
    chipFilterLight = findViewById(R.id.chip_theme_filter_light);
    chipFilterSystem = findViewById(R.id.chip_theme_filter_system);

    setupListeners();
    loadThemesData();
  }

  private void setupListeners()
  {
    if (btnBack != null)
    {
      btnBack.setOnClickListener(v -> {
        VibratorCompat.vibrate(v);
        returnToKeyboard();
      });
    }

    if (btnClose != null)
    {
      btnClose.setOnClickListener(v -> {
        VibratorCompat.vibrate(v);
        returnToKeyboard();
      });
    }

    setupCategoryChip(chipFilterAll, "all");
    setupCategoryChip(chipFilterCyber, "cyber");
    setupCategoryChip(chipFilterDark, "dark");
    setupCategoryChip(chipFilterLight, "light");
    setupCategoryChip(chipFilterSystem, "monet");
  }

  private void setupCategoryChip(Button chip, String category)
  {
    if (chip == null) return;
    chip.setOnClickListener(v -> {
      VibratorCompat.vibrate(v);
      currentCategoryFilter = category;
      updateCategoryChipStyles();
      applyFilters();
    });
  }

  private void updateCategoryChipStyles()
  {
    setChipSelected(chipFilterAll, "all".equals(currentCategoryFilter));
    setChipSelected(chipFilterCyber, "cyber".equals(currentCategoryFilter));
    setChipSelected(chipFilterDark, "dark".equals(currentCategoryFilter));
    setChipSelected(chipFilterLight, "light".equals(currentCategoryFilter));
    setChipSelected(chipFilterSystem, "monet".equals(currentCategoryFilter));
  }

  private void setChipSelected(Button btn, boolean selected)
  {
    if (btn == null) return;
    if (selected)
    {
      btn.setBackgroundResource(R.drawable.normal_chip_bg);
      btn.setTextColor(Color.parseColor("#FFFFFF"));
    }
    else
    {
      btn.setBackgroundResource(R.drawable.normal_chip_bg);
      btn.setTextColor(Color.parseColor("#94A3B8"));
    }
  }

  private void returnToKeyboard()
  {
    Config config = Config.globalConfig();
    if (config != null && config.handler != null)
    {
      config.handler.key_up(
          KeyValue.getKeyByName("switch_back_theme"), Pointers.Modifiers.EMPTY);
    }
  }

  private void loadThemesData()
  {
    allThemeItems.clear();

    Config config = Config.globalConfig();
    KeyEventHandler.IReceiver receiver = null;
    if (config != null && config.handler instanceof KeyEventHandler)
    {
      receiver = ((KeyEventHandler) config.handler).getReceiver();
    }

    String currentThemeKey = (receiver != null) ? receiver.get_current_theme_name() : "frostedobsidian";
    if (currentThemeKey == null || currentThemeKey.isEmpty())
    {
      currentThemeKey = "frostedobsidian";
    }

    Resources res = getContext().getResources();
    String[] themeEntries = res.getStringArray(R.array.pref_theme_entries);
    String[] themeValues = res.getStringArray(R.array.pref_theme_values);

    for (int i = 0; i < themeValues.length; i++)
    {
      String key = themeValues[i];
      String name = (i < themeEntries.length) ? themeEntries[i] : key;
      boolean isSelected = key.equalsIgnoreCase(currentThemeKey);

      String category = "dark";
      int bg = Color.parseColor("#121218");
      int accent = Color.parseColor("#60A5FA");
      int text = Color.parseColor("#F8FAFC");

      switch (key.toLowerCase())
      {
        case "frostedobsidian":
          category = "cyber";
          bg = Color.parseColor("#090B10");
          accent = Color.parseColor("#00F0FF");
          text = Color.parseColor("#F0FDF4");
          break;
        case "cyberneon":
          category = "cyber";
          bg = Color.parseColor("#080714");
          accent = Color.parseColor("#FF007F");
          text = Color.parseColor("#FFE4E6");
          break;
        case "dracula":
          category = "dark";
          bg = Color.parseColor("#282A36");
          accent = Color.parseColor("#BD93F9");
          text = Color.parseColor("#F8F8F2");
          break;
        case "dark":
          category = "dark";
          bg = Color.parseColor("#1E1E24");
          accent = Color.parseColor("#4A72B2");
          text = Color.parseColor("#FFFFFF");
          break;
        case "black":
        case "altblack":
          category = "dark";
          bg = Color.parseColor("#000000");
          accent = Color.parseColor("#333333");
          text = Color.parseColor("#FFFFFF");
          break;
        case "rosepine":
          category = "dark";
          bg = Color.parseColor("#191724");
          accent = Color.parseColor("#EBBCBA");
          text = Color.parseColor("#E0DEF4");
          break;
        case "cobalt":
          category = "dark";
          bg = Color.parseColor("#0D1B2A");
          accent = Color.parseColor("#3A86FF");
          text = Color.parseColor("#E0E1DD");
          break;
        case "pine":
          category = "dark";
          bg = Color.parseColor("#1B2A1B");
          accent = Color.parseColor("#4EBA6F");
          text = Color.parseColor("#F0FFF0");
          break;
        case "jungle":
          category = "dark";
          bg = Color.parseColor("#14281D");
          accent = Color.parseColor("#52B788");
          text = Color.parseColor("#D8F3DC");
          break;
        case "desert":
          category = "light";
          bg = Color.parseColor("#F4ECD8");
          accent = Color.parseColor("#DDA15E");
          text = Color.parseColor("#283618");
          break;
        case "light":
          category = "light";
          bg = Color.parseColor("#ECEFF1");
          accent = Color.parseColor("#90CAF9");
          text = Color.parseColor("#263238");
          break;
        case "white":
          category = "light";
          bg = Color.parseColor("#FFFFFF");
          accent = Color.parseColor("#E0E0E0");
          text = Color.parseColor("#111827");
          break;
        case "everforestlight":
          category = "light";
          bg = Color.parseColor("#FDF6E3");
          accent = Color.parseColor("#8F9A52");
          text = Color.parseColor("#5C6A72");
          break;
        case "epaper":
          category = "light";
          bg = Color.parseColor("#F0F0F0");
          accent = Color.parseColor("#CCCCCC");
          text = Color.parseColor("#000000");
          break;
        case "epaperblack":
          category = "dark";
          bg = Color.parseColor("#000000");
          accent = Color.parseColor("#222222");
          text = Color.parseColor("#FFFFFF");
          break;
        case "monet":
        case "monetdark":
        case "monetlight":
        case "system":
          category = "monet";
          bg = Color.parseColor("#1A1C23");
          accent = Color.parseColor("#7C3AED");
          text = Color.parseColor("#EDE9FE");
          break;
      }

      allThemeItems.add(new ThemeItem(key, name, category, bg, accent, text, isSelected));
    }

    updateCategoryChipStyles();
    applyFilters();

    if (adapter == null)
    {
      adapter = new ThemeAdapter(getContext(), currentDisplayItems);
      listView.setAdapter(adapter);
    }
    else
    {
      adapter.notifyDataSetChanged();
    }

    final KeyEventHandler.IReceiver finalReceiver = receiver;
    listView.setOnItemClickListener((parent, view, position, id) -> {
      VibratorCompat.vibrate(view);
      if (position >= 0 && position < currentDisplayItems.size())
      {
        ThemeItem item = currentDisplayItems.get(position);
        if (finalReceiver != null)
        {
          finalReceiver.switch_to_theme_name(item.key);
        }
        Toast.makeText(getContext(), "Theme applied: " + item.name, Toast.LENGTH_SHORT).show();
      }
    });
  }

  private void applyFilters()
  {
    currentDisplayItems.clear();
    for (ThemeItem item : allThemeItems)
    {
      if ("all".equals(currentCategoryFilter) || currentCategoryFilter.equalsIgnoreCase(item.category))
      {
        currentDisplayItems.add(item);
      }
    }
    if (adapter != null)
    {
      adapter.notifyDataSetChanged();
    }
  }

  private static class ThemeAdapter extends BaseAdapter
  {
    private final Context context;
    private final List<ThemeItem> items;

    public ThemeAdapter(Context context, List<ThemeItem> items)
    {
      this.context = context;
      this.items = items;
    }

    @Override
    public int getCount()
    {
      return items.size();
    }

    @Override
    public Object getItem(int position)
    {
      return items.get(position);
    }

    @Override
    public long getItemId(int position)
    {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
      ViewHolder holder;
      if (convertView == null)
      {
        convertView = LayoutInflater.from(context).inflate(R.layout.item_theme_pane, parent, false);
        holder = new ViewHolder();
        holder.rootLayout = convertView.findViewById(R.id.theme_item_root);
        holder.colorPreviewBox = convertView.findViewById(R.id.theme_item_color_preview);
        holder.accentDot = convertView.findViewById(R.id.theme_item_accent_dot);
        holder.txtName = convertView.findViewById(R.id.theme_item_name);
        holder.txtTag = convertView.findViewById(R.id.theme_item_tag);
        holder.imgSelected = convertView.findViewById(R.id.theme_item_check);
        convertView.setTag(holder);
      }
      else
      {
        holder = (ViewHolder) convertView.getTag();
      }

      ThemeItem item = items.get(position);
      holder.txtName.setText(item.name);

      // Set category tag text
      String tagText = "DARK";
      if ("cyber".equals(item.category)) tagText = "CYBER";
      else if ("light".equals(item.category)) tagText = "LIGHT";
      else if ("monet".equals(item.category)) tagText = "MONET";
      holder.txtTag.setText(tagText);

      // Style preview swatches
      GradientDrawable previewBg = new GradientDrawable();
      previewBg.setColor(item.bgColor);
      previewBg.setCornerRadius(6 * context.getResources().getDisplayMetrics().density);
      previewBg.setStroke(1, Color.parseColor("#334155"));
      holder.colorPreviewBox.setBackground(previewBg);

      GradientDrawable dotBg = new GradientDrawable();
      dotBg.setShape(GradientDrawable.OVAL);
      dotBg.setColor(item.accentColor);
      holder.accentDot.setBackground(dotBg);

      if (item.isSelected)
      {
        holder.rootLayout.setBackgroundResource(R.drawable.normal_btn_bg);
        holder.imgSelected.setVisibility(View.VISIBLE);
        holder.txtName.setTextColor(Color.parseColor("#FFFFFF"));
      }
      else
      {
        holder.rootLayout.setBackgroundResource(R.drawable.normal_btn_bg);
        holder.imgSelected.setVisibility(View.GONE);
        holder.txtName.setTextColor(Color.parseColor("#E2E8F0"));
      }

      return convertView;
    }

    private static class ViewHolder
    {
      LinearLayout rootLayout;
      View colorPreviewBox;
      View accentDot;
      TextView txtName;
      TextView txtTag;
      ImageView imgSelected;
    }
  }
}
