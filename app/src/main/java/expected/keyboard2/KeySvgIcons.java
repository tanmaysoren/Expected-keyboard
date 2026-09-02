package expected.keyboard2;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import androidx.core.graphics.PathParser;
import java.util.HashMap;
import java.util.Map;

/**
 * KeySvgIcons provides crisp, resolution-independent vector SVG
 * icons for functional keys on the keyboard.
 */
public class KeySvgIcons {

  public static class SvgIconItem {
    public final Path path;
    public final Path fillPath;
    public final float fillOpacityUnclicked;
    public final float fillOpacityClicked;
    public final float viewBoxSize;
    public final boolean isStroke;
    public final float strokeWidth;

    public SvgIconItem(Path strokePath, Path fillPath, float fillOpacityUnclicked, float fillOpacityClicked, float viewBoxSize, float strokeWidth) {
      this.path = strokePath;
      this.fillPath = fillPath;
      this.fillOpacityUnclicked = fillOpacityUnclicked;
      this.fillOpacityClicked = fillOpacityClicked;
      this.viewBoxSize = viewBoxSize;
      this.isStroke = true;
      this.strokeWidth = strokeWidth;
    }

    public SvgIconItem(Path path, float viewBoxSize, boolean isStroke, float strokeWidth) {
      this.path = path;
      this.fillPath = null;
      this.fillOpacityUnclicked = 0f;
      this.fillOpacityClicked = 0f;
      this.viewBoxSize = viewBoxSize;
      this.isStroke = isStroke;
      this.strokeWidth = strokeWidth;
    }

    public SvgIconItem(Path path, float viewBoxSize) {
      this(path, viewBoxSize, false, 0f);
    }
  }

  private static final Map<Character, SvgIconItem> CHAR_ICONS = new HashMap<>();
  private static final Map<String, SvgIconItem> STRING_ICONS = new HashMap<>();

  static {
    // 1. CLIPBOARD KEY (User Provided Stroke SVG - viewBox 24)
    String clipboardPath = "M8 5.00005C7.01165 5.00082 6.49359 5.01338 6.09202 5.21799C5.71569 5.40973 5.40973 5.71569 5.21799 6.09202C5 6.51984 5 7.07989 5 8.2V17.8C5 18.9201 5 19.4802 5.21799 19.908C5.40973 20.2843 5.71569 20.5903 6.09202 20.782C6.51984 21 7.07989 21 8.2 21H15.8C16.9201 21 17.4802 21 17.908 20.782C18.2843 20.5903 18.5903 20.2843 18.782 19.908C19 19.4802 19 18.9201 19 17.8V8.2C19 7.07989 19 6.51984 18.782 6.09202C18.5903 5.71569 18.2843 5.40973 17.908 5.21799C17.5064 5.01338 16.9884 5.00082 16 5.00005M8 5.00005V7H16V5.00005M8 5.00005V4.70711C8 4.25435 8.17986 3.82014 8.5 3.5C8.82014 3.17986 9.25435 3 9.70711 3H14.2929C14.7456 3 15.1799 3.17986 15.5 3.5C15.8201 3.82014 16 4.25435 16 4.70711V5.00005M12 11H9M15 15H9";
    addStrokeIcon('\uE017', clipboardPath, 24f, 2f);
    addStrokeIcon('📋', clipboardPath, 24f, 2f);
    addStrokeStringIcon("clipboard", clipboardPath, 24f, 2f);
    addStrokeStringIcon("clip", clipboardPath, 24f, 2f);
    addStrokeStringIcon("switch_clipboard", clipboardPath, 24f, 2f);

    // 2. COPY KEY (User Provided Stroke SVG - viewBox 24)
    String copyPath = "M8 5.00005C7.01165 5.00082 6.49359 5.01338 6.09202 5.21799C5.71569 5.40973 5.40973 5.71569 5.21799 6.09202C5 6.51984 5 7.07989 5 8.2V17.8C5 18.9201 5 19.4802 5.21799 19.908C5.40973 20.2843 5.71569 20.5903 6.09202 20.782C6.51984 21 7.07989 21 8.2 21H15.8C16.9201 21 17.4802 21 17.908 20.782C18.2843 20.5903 18.5903 20.2843 18.782 19.908C19 19.4802 19 18.9201 19 17.8V8.2C19 7.07989 19 6.51984 18.782 6.09202C18.5903 5.71569 18.2843 5.40973 17.908 5.21799C17.5064 5.01338 16.9884 5.00082 16 5.00005M8 5.00005V7H16V5.00005M8 5.00005V4.70711C8 4.25435 8.17986 3.82014 8.5 3.5C8.82014 3.17986 9.25435 3 9.70711 3H14.2929C14.7456 3 15.1799 3.17986 15.5 3.5C15.8201 3.82014 16 4.25435 16 4.70711V5.00005M12 11V17M12 11L14 13M12 11L10 13";
    addStrokeIcon('\uE030', copyPath, 24f, 2f);
    addStrokeIcon('❐', copyPath, 24f, 2f);
    addStrokeStringIcon("copy", copyPath, 24f, 2f);

    // 3. PASTE KEY (User Provided Stroke SVG - viewBox 24)
    String pastePath = "M8 5.00005C7.01165 5.00082 6.49359 5.01338 6.09202 5.21799C5.71569 5.40973 5.40973 5.71569 5.21799 6.09202C5 6.51984 5 7.07989 5 8.2V17.8C5 18.9201 5 19.4802 5.21799 19.908C5.40973 20.2843 5.71569 20.5903 6.09202 20.782C6.51984 21 7.07989 21 8.2 21H15.8C16.9201 21 17.4802 21 17.908 20.782C18.2843 20.5903 18.5903 20.2843 18.782 19.908C19 19.4802 19 18.9201 19 17.8V8.2C19 7.07989 19 6.51984 18.782 6.09202C17.5064 5.01338 16.9884 5.00082 16 5.00005M8 5.00005V7H16V5.00005M8 5.00005V4.70711C8 4.25435 8.17986 3.82014 8.5 3.5C8.82014 3.17986 9.25435 3 9.70711 3H14.2929C14.7456 3 15.1799 3.17986 15.5 3.5C15.8201 3.82014 16 4.25435 16 4.70711V5.00005M12 11V17M12 17L10 15M12 17L14 15";
    addStrokeIcon('\uE032', pastePath, 24f, 2f);
    addStrokeIcon('\uE035', pastePath, 24f, 2f);
    addStrokeStringIcon("paste", pastePath, 24f, 2f);
    addStrokeStringIcon("pasteasplaintext", pastePath, 24f, 2f);

    // 4. SELECT ALL KEY (User Provided SVG - viewBox 30)
    String selectAllPath = "M 5 3 C 3.9092973 3 3 3.9093 3 5 L 3 6 L 5 6 L 5 5 L 6 5 L 6 3 L 5 3 z M 8 3 L 8 5 L 10 5 L 10 3 L 8 3 z M 12 3 L 12 5 L 14 5 L 14 3 L 12 3 z M 16 3 L 16 5 L 18 5 L 18 3 L 16 3 z M 20 3 L 20 5 L 22 5 L 22 3 L 20 3 z M 24 3 L 24 5 L 25 5 L 25 6 L 27 6 L 27 5 C 27 3.9093 26.090703 3 25 3 L 24 3 z M 3 8 L 3 10 L 5 10 L 5 8 L 3 8 z M 25 8 L 25 10 L 27 10 L 27 8 L 25 8 z M 11 9 C 9.9092973 9 9 9.9093 9 11 L 9 19 C 9 20.0907 9.9092973 21 11 21 L 19 21 C 20.090703 21 21 20.0907 21 19 L 21 11 C 21 9.9093 20.090703 9 19 9 L 11 9 z M 11 11 L 19 11 L 19 19 L 11 19 L 11 11 z M 3 12 L 3 14 L 5 14 L 5 12 L 3 12 z M 25 12 L 25 14 L 27 14 L 27 12 L 25 12 z M 3 16 L 3 18 L 5 18 L 5 16 L 3 16 z M 25 16 L 25 18 L 27 18 L 27 16 L 25 16 z M 3 20 L 3 22 L 5 22 L 5 20 L 3 20 z M 25 20 L 25 22 L 27 22 L 27 20 L 25 20 z M 3 24 L 3 25 C 3 26.0907 3.9092973 27 5 27 L 6 27 L 6 25 L 5 25 L 5 24 L 3 24 z M 25 24 L 25 25 L 24 25 L 24 27 L 25 27 C 26.090703 27 27 26.0907 27 25 L 27 24 L 25 24 z M 8 25 L 8 27 L 10 27 L 10 25 L 8 25 z M 12 25 L 12 27 L 14 27 L 14 25 L 12 25 z M 16 25 L 16 27 L 18 27 L 18 25 L 16 25 z M 20 25 L 20 27 L 22 27 L 22 25 L 20 25 z";
    addIcon('\uE033', selectAllPath, 30f);
    addIcon('⬚', selectAllPath, 30f);
    addStringIcon("selectall", selectAllPath, 30f);
    addStringIcon("select_all", selectAllPath, 30f);
    addStringIcon("select-all", selectAllPath, 30f);

    // 5. KEYBOARD SWITCH / CHANGE METHOD KEY (User Provided Stroke SVG - viewBox 24)
    String switchKbPath = "M6 13H6.01M6 17H6.01M10 13H10.01M14 13H14.01M18 17H18.01M18 13H18.01M16 3V5H8V9M10 17H14M5.2 21H18.8C19.9201 21 20.4802 21 20.908 20.782C21.2843 20.5903 21.5903 20.2843 21.782 19.908C22 19.4802 22 18.9201 22 17.8V12.2C22 11.0799 22 10.5198 21.782 10.092C21.5903 9.71569 21.2843 9.40973 20.908 9.21799C20.4802 9 19.9201 9 18.8 9H5.2C4.07989 9 3.51984 9 3.09202 9.21799C2.71569 9.40973 2.40973 9.71569 2.21799 10.092C2 10.5198 2 11.0799 2 12.2V17.8C2 18.9201 2 19.4802 2.21799 19.908C2.40973 20.2843 2.71569 20.5903 3.09202 20.782C3.51984 21 4.0799 21 5.2 21Z";
    addStrokeIcon('\uE009', switchKbPath, 24f, 2f);
    addStrokeIcon('🌐', switchKbPath, 24f, 2f);
    addStrokeStringIcon("change_method", switchKbPath, 24f, 2f);
    addStrokeStringIcon("change_method_prev", switchKbPath, 24f, 2f);
    addStrokeStringIcon("change_method_next", switchKbPath, 24f, 2f);
    addStrokeStringIcon("switch_keyboard", switchKbPath, 24f, 2f);
    addStrokeStringIcon("switchkeyboard", switchKbPath, 24f, 2f);

    // 5b. LANGUAGE / LAYOUT SWITCH FORWARD (SWIPE UP) & BACKWARD (SWIPE DOWN)
    String switchForwardPath = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M3.6 9h16.8 M3.6 15h16.8 M11.5 3a12.5 12.5 0 0 0 0 18 M12.5 3a12.5 12.5 0 0 1 0 18 M12 6.5l-3.5 3.5h2.5v5h2v-5h2.5z";
    addStrokeIcon('\uE013', switchForwardPath, 24f, 1.8f);
    addStrokeStringIcon("switch_forward", switchForwardPath, 24f, 1.8f);
    addStrokeStringIcon("switchforward", switchForwardPath, 24f, 1.8f);

    String switchBackwardPath = "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z M3.6 9h16.8 M3.6 15h16.8 M11.5 3a12.5 12.5 0 0 0 0 18 M12.5 3a12.5 12.5 0 0 1 0 18 M12 17.5l3.5-3.5h-2.5v-5h-2v5H8.5z";
    addStrokeIcon('\uE014', switchBackwardPath, 24f, 1.8f);
    addStrokeStringIcon("switch_backward", switchBackwardPath, 24f, 1.8f);
    addStrokeStringIcon("switchbackward", switchBackwardPath, 24f, 1.8f);

    // 6. COMPOSE KEY (Segmented SVG with 0.15 unclicked opacity and highlighted active state)
    String composeFillPath = "M12 21C16.9706 21 21 16.9706 21 12C21 7.02944 16.9706 3 12 3V12H3C3 16.9706 7.02944 21 12 21Z";
    String composeStrokePath = "M3 12C3 16.9706 7.02944 21 12 21C14.4853 21 16.7353 19.9926 18.364 18.364M3 12C3 7.02944 7.02944 3 12 3M3 12H12M12 3C16.9706 3 21 7.02944 21 12C21 14.4853 19.9926 16.7353 18.364 18.364M12 3V12M18.364 18.364L12 12";
    addCompositeIcon('\uE016', composeStrokePath, composeFillPath, 0.15f, 0.90f, 24f, 1.5f);
    addCompositeIcon('⎄', composeStrokePath, composeFillPath, 0.15f, 0.90f, 24f, 1.5f);
    addCompositeStringIcon("compose", composeStrokePath, composeFillPath, 0.15f, 0.90f, 24f, 1.5f);

    // 7. HOME KEY (Unicode: \uE00B) - Modern Stroke House SVG
    String homePath = "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z M9 22V12h6v10";
    addStrokeIcon('\uE00B', homePath, 24f, 2f);
    addStrokeStringIcon("home", homePath, 24f, 2f);

    // 8. TAB KEY (Standard Keyboard Dual-Arrow ↹ SVG)
    String tabPath = "M4 8h14 M15 5l4 3-4 3 M19 5v6 M20 16H6 M9 13l-4 3 4 3 M5 13v6";
    addStrokeIcon('\uE00F', tabPath, 24f, 2f);
    addStrokeIcon('↹', tabPath, 24f, 2f);
    addStrokeStringIcon("tab", tabPath, 24f, 2f);
    addStrokeStringIcon("\\t", tabPath, 24f, 2f);

    // 9. ESC KEY (Unicode: ⎋) - Modern Stroke Escape Breakout SVG
    String escPath = "M18 12a6 6 0 1 1-10.24-4.24M12 12L4 4M4 4h5M4 4v5";
    addStrokeIcon('\uE01E', escPath, 24f, 2f);
    addStrokeIcon('⎋', escPath, 24f, 2f);
    addStrokeStringIcon("esc", escPath, 24f, 2f);
    addStrokeStringIcon("escape", escPath, 24f, 2f);

    // 10. END KEY (Unicode: \uE00C) - Modern Stroke Jump-To-End SVG
    String endPath = "M4 12h11M11 6l6 6-6 6M19 5v14";
    addStrokeIcon('\uE00C', endPath, 24f, 2f);
    addStrokeStringIcon("end", endPath, 24f, 2f);

    // 11. PAGE UP (User Provided SVG - viewBox 1920)
    String pageUpPath = "M168 113.074h1583.02V.001H168v113.073Zm0 226.144h1583.02V226.146H168v113.072Zm296.929 688.185 80.056 80.055 355.727-355.84V1920h113.073V751.618l355.727 355.84 80.056-80.055-492.319-492.206-492.32 492.206Z";
    addIcon('\uE002', pageUpPath, 1920f);
    addIcon('⇞', pageUpPath, 1920f);
    addStringIcon("pgup", pageUpPath, 1920f);
    addStringIcon("page_up", pageUpPath, 1920f);
    addStringIcon("pageup", pageUpPath, 1920f);

    // 12. PAGE DOWN (User Provided SVG - viewBox 1920)
    String pageDownPath = "M1751.02 1806.927V1920H168v-113.073h1583.02Zm0-226.146v113.073H168v-113.073h1583.02ZM1013.785 0v1168.382l355.727-355.84 80.056 80.055-492.32 492.206L464.93 892.597l80.056-80.055 355.727 355.84V0h113.073Z";
    addIcon('\uE003', pageDownPath, 1920f);
    addIcon('⇟', pageDownPath, 1920f);
    addStringIcon("pgdn", pageDownPath, 1920f);
    addStringIcon("page_down", pageDownPath, 1920f);
    addStringIcon("pagedown", pageDownPath, 1920f);

    // 13. SUPERSCRIPT (x²) - Large & Bold
    String superScriptPath = "M3 9l6 8 M9 9l-6 8 M12 3.5h5.5l-3.5 3.5c-.6.6-.8 1-.8 1.5h4.5";
    addStrokeIcon('\uE064', superScriptPath, 18f, 2.2f);
    addStrokeStringIcon("sup", superScriptPath, 18f, 2.2f);
    addStrokeStringIcon("superscript", superScriptPath, 18f, 2.2f);

    // 14. SUBSCRIPT (x₂) - Large & Bold
    String subScriptPath = "M3 5l6 8 M9 5l-6 8 M12 11.5h5.5l-3.5 3.5c-.6.6-.8 1-.8 1.5h4.5";
    addStrokeIcon('\uE065', subScriptPath, 18f, 2.2f);
    addStrokeStringIcon("sub", subScriptPath, 18f, 2.2f);
    addStrokeStringIcon("subscript", subScriptPath, 18f, 2.2f);

    // 15. SHIFT & CAPS LOCK
    String shiftPath = "M12 4L4 12h4v7h8v-7h4L12 4z";
    addIcon('\uE000', shiftPath, 24f);
    addIcon('\uE00A', shiftPath, 24f);
    addIcon('⇧', shiftPath, 24f);

    String capsLockPath = "M12 3L4 11h4v5h8v-5h4L12 3z M4 18h16v2H4v-2z";
    addIcon('\uE012', capsLockPath, 24f);
    addIcon('⇪', capsLockPath, 24f);

    // 16. BACKSPACE & DELETE
    String backspacePath = "M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-3 12.59L17.59 17 14 13.41 10.41 17 9 15.59 12.59 12 9 8.41 10.41 7 14 10.59 17.59 7 19 8.41 15.41 12 19 15.59z";
    addIcon('\uE011', backspacePath, 24f);
    addIcon('⌫', backspacePath, 24f);

    String deleteWordPath = "M22 3H7c-.69 0-1.23.35-1.59.88L0 12l5.41 8.11c.36.53.9.89 1.59.89h15c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM16 14l-3-3 3-3-1.41-1.41L11.59 9.58 8.59 6.58 7.18 8l3 3-3 3 1.41 1.41 3-3 3 3z";
    addIcon('\uE01B', deleteWordPath, 24f);

    String forwardDelPath = "M2 3h15c.69 0 1.23.35 1.59.88L24 12l-5.41 8.11c-.36.53-.9.89-1.59.89H2c-1.1 0-2-.9-2-2V5c0-1.1.9-2 2-2zm3 12.59L6.41 17 10 13.41 13.59 17 15 15.59 11.41 12 15 8.41 13.59 7 10 10.59 6.41 7 5 8.41 8.59 12 5 15.59z";
    addIcon('\uE010', forwardDelPath, 24f);
    addIcon('⌦', forwardDelPath, 24f);
    addIcon('\uE01C', forwardDelPath, 24f);

    // 17. ENTER & SPACE
    String enterPath = "M19 7v4H5.83l3.58-3.59L8 6l-6 6 6 6 1.41-1.41L5.83 13H21V7h-2z";
    addIcon('\uE00E', enterPath, 24f);
    addIcon('↵', enterPath, 24f);

    String spacePath = "M18 9v4H6V9H4v6h16V9h-2z";
    addIcon('\uE00D', spacePath, 24f);
    addIcon('␣', spacePath, 24f);

    // 19. OTHER FUNCTIONAL KEYS
    String emojiPath = "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z";
    addIcon('\uE001', emojiPath, 24f);
    addIcon('☺', emojiPath, 24f);

    String settingsPath = "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z";
    addIcon('\uE004', settingsPath, 24f);
    addIcon('⚙', settingsPath, 24f);

    String cutPath = "M9.64 7.64c.23-.5.36-1.05.36-1.64 0-2.21-1.79-4-4-4S2 3.79 2 6s1.79 4 4 4c.59 0 1.14-.13 1.64-.36L10 12l-2.36 2.36C7.14 14.13 6.59 14 6 14c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4c0-.59-.13-1.14-.36-1.64L12 14l7 7h3v-1L9.64 7.64zM6 8c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm0 12c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm6-7.5c-.28 0-.5-.22-.5-.5s.22-.5.5-.5.5.22.5.5-.22.5-.5.5zM19 3l-6 6 2 2 7-7V3h-3z";
    addIcon('\uE031', cutPath, 24f);
    addIcon('✂', cutPath, 24f);

    String sharePath = "M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l6.96-4.05c.52.48 1.2.78 1.98.78 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z";
    addIcon('\uE034', sharePath, 24f);
    addIcon('➦', sharePath, 24f);

    String undoPath = "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z";
    addIcon('\uE036', undoPath, 24f);
    addIcon('↶', undoPath, 24f);

    String redoPath = "M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22l2.37.78C4.95 12.81 7.96 10.5 11.5 10.5c1.96 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z";
    addIcon('\uE037', redoPath, 24f);
    addIcon('↷', redoPath, 24f);

    String assistPath = "M19 9l1.25-2.75L23 5l-2.75-1.25L19 1l-1.25 2.75L15 5l2.75 1.25L19 9zm-7.5.5L9 4 6.5 9.5 1 12l5.5 2.5L9 20l2.5-5.5L17 12l-5.5-2.5zM19 15l-1.25 2.75L15 19l2.75 1.25L19 23l1.25-2.75L23 19l-2.75-1.25L19 15z";
    addIcon('\uE038', assistPath, 24f);
    addIcon('✨', assistPath, 24f);

    String composeCancelPath = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";
    addIcon('\uE01A', composeCancelPath, 24f);
    addIcon('✕', composeCancelPath, 24f);

    String dictionaryPath = "M18 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zM6 4h5v8l-2.5-1.5L6 12V4z";
    addIcon('\uE01D', dictionaryPath, 24f);
    addIcon('📖', dictionaryPath, 24f);

    String hideKeyboardPath = "M20 3H4c-1.1 0-1.99.9-1.99 2L2 15c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-9 3h2v2h-2V6zm0 3h2v2h-2V9zM8 6h2v2H8V6zm0 3h2v2H8V9zM5 6h2v2H5V6zm0 3h2v2H5V9zm3 5h8v2H8v-2zm9-3h-2V9h2v2zm0-3h-2V6h2v2zM12 23l4-4H8l4 4z";
    addIcon('⊻', hideKeyboardPath, 24f);

    String toggleFloatingPath = "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z";
    addIcon('⛶', toggleFloatingPath, 24f);

    // 20. NAVIGATION ARROWS (Dpad Left, Right, Up, Down for Dpad / functional navigation keys)
    String arrowLeftPath = "M15.41 16.59L10.83 12l4.58-4.59L14 6l-6 6 6 6 1.41-1.41z";
    addStringIcon("left", arrowLeftPath, 24f);
    addStringIcon("dpad_left", arrowLeftPath, 24f);

    String arrowRightPath = "M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z";
    addStringIcon("right", arrowRightPath, 24f);
    addStringIcon("dpad_right", arrowRightPath, 24f);

    String arrowUpPath = "M12 8l-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z";
    addStringIcon("up", arrowUpPath, 24f);
    addStringIcon("dpad_up", arrowUpPath, 24f);

    String arrowDownPath = "M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z";
    addStringIcon("down", arrowDownPath, 24f);
    addStringIcon("dpad_down", arrowDownPath, 24f);
  }

  private static void addCompositeIcon(char c, String strokePathData, String fillPathData, float fillOpacityUnclicked, float fillOpacityClicked, float viewBox, float strokeWidth) {
    try {
      Path strokePath = PathParser.createPathFromPathData(strokePathData);
      Path fillPath = fillPathData != null ? PathParser.createPathFromPathData(fillPathData) : null;
      CHAR_ICONS.put(c, new SvgIconItem(strokePath, fillPath, fillOpacityUnclicked, fillOpacityClicked, viewBox, strokeWidth));
    } catch (Exception e) {
      // Ignore
    }
  }

  private static void addCompositeStringIcon(String name, String strokePathData, String fillPathData, float fillOpacityUnclicked, float fillOpacityClicked, float viewBox, float strokeWidth) {
    if (name == null || name.isEmpty()) return;
    try {
      Path strokePath = PathParser.createPathFromPathData(strokePathData);
      Path fillPath = fillPathData != null ? PathParser.createPathFromPathData(fillPathData) : null;
      SvgIconItem item = new SvgIconItem(strokePath, fillPath, fillOpacityUnclicked, fillOpacityClicked, viewBox, strokeWidth);
      STRING_ICONS.put(name, item);
      STRING_ICONS.put(name.toLowerCase(), item);
      STRING_ICONS.put(name.replaceAll("\\s+", "").toLowerCase(), item);
    } catch (Exception e) {
      // Ignore
    }
  }

  private static void addIcon(char c, String pathData, float viewBox) {
    try {
      Path p = PathParser.createPathFromPathData(pathData);
      if (p != null) p.setFillType(Path.FillType.EVEN_ODD);
      CHAR_ICONS.put(c, new SvgIconItem(p, viewBox));
    } catch (Exception e) {
      // Ignore
    }
  }

  private static void addStrokeIcon(char c, String pathData, float viewBox, float strokeWidth) {
    try {
      Path p = PathParser.createPathFromPathData(pathData);
      CHAR_ICONS.put(c, new SvgIconItem(p, viewBox, true, strokeWidth));
    } catch (Exception e) {
      // Ignore
    }
  }

  private static void addStringIcon(String name, String pathData, float viewBox) {
    if (name == null || name.isEmpty()) return;
    try {
      Path p = PathParser.createPathFromPathData(pathData);
      if (p != null) p.setFillType(Path.FillType.EVEN_ODD);
      SvgIconItem item = new SvgIconItem(p, viewBox);
      STRING_ICONS.put(name, item);
      STRING_ICONS.put(name.toLowerCase(), item);
      STRING_ICONS.put(name.replaceAll("\\s+", "").toLowerCase(), item);
    } catch (Exception e) {
      // Ignore
    }
  }

  private static void addStrokeStringIcon(String name, String pathData, float viewBox, float strokeWidth) {
    if (name == null || name.isEmpty()) return;
    try {
      Path p = PathParser.createPathFromPathData(pathData);
      SvgIconItem item = new SvgIconItem(p, viewBox, true, strokeWidth);
      STRING_ICONS.put(name, item);
      STRING_ICONS.put(name.toLowerCase(), item);
      STRING_ICONS.put(name.replaceAll("\\s+", "").toLowerCase(), item);
    } catch (Exception e) {
      // Ignore
    }
  }

  private static SvgIconItem findItem(String s) {
    if (s == null || s.isEmpty()) return null;
    SvgIconItem item = STRING_ICONS.get(s);
    if (item != null) return item;

    String normalized = s.toLowerCase().trim();
    item = STRING_ICONS.get(normalized);
    if (item != null) return item;

    String noSpaces = normalized.replaceAll("\\s+", "");
    item = STRING_ICONS.get(noSpaces);
    if (item != null) return item;

    if (s.length() == 1) {
      return CHAR_ICONS.get(s.charAt(0));
    }
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      if (CHAR_ICONS.containsKey(ch)) {
        return CHAR_ICONS.get(ch);
      }
    }
    return null;
  }

  /**
   * Returns true if the string corresponds to a registered functional key SVG or character icon.
   */
  public static boolean hasIcon(String s) {
    return findItem(s) != null;
  }

  /**
   * Draws the functional key SVG path on the provided Canvas, centered at (centerX, centerY).
   */
  public static boolean drawIcon(Canvas canvas, String s, float centerX, float centerY, float targetSize, Paint paint, boolean isKeyDown) {
    SvgIconItem item = findItem(s);
    if (item == null) return false;

    canvas.save();
    canvas.translate(centerX, centerY);
    float scale = targetSize / item.viewBoxSize;
    canvas.scale(scale, scale);
    canvas.translate(-item.viewBoxSize / 2.0f, -item.viewBoxSize / 2.0f);

    Paint.Style origStyle = paint.getStyle();
    float origStrokeWidth = paint.getStrokeWidth();
    Paint.Cap origCap = paint.getStrokeCap();
    Paint.Join origJoin = paint.getStrokeJoin();
    int origAlpha = paint.getAlpha();

    if (item.fillPath != null) {
      float opacity = isKeyDown ? item.fillOpacityClicked : item.fillOpacityUnclicked;
      paint.setStyle(Paint.Style.FILL);
      paint.setAlpha((int) (origAlpha * opacity));
      canvas.drawPath(item.fillPath, paint);
      paint.setAlpha(origAlpha);
    }

    if (item.isStroke) {
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(item.strokeWidth);
      paint.setStrokeCap(Paint.Cap.ROUND);
      paint.setStrokeJoin(Paint.Join.ROUND);
      canvas.drawPath(item.path, paint);
    } else {
      paint.setStyle(Paint.Style.FILL);
      canvas.drawPath(item.path, paint);
    }

    paint.setStyle(origStyle);
    paint.setStrokeWidth(origStrokeWidth);
    paint.setStrokeCap(origCap);
    paint.setStrokeJoin(origJoin);
    paint.setAlpha(origAlpha);

    canvas.restore();
    return true;
  }

  public static boolean drawIcon(Canvas canvas, String s, float centerX, float centerY, float targetSize, Paint paint) {
    return drawIcon(canvas, s, centerX, centerY, targetSize, paint, false);
  }
}
