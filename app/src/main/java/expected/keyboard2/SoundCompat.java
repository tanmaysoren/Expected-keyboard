package expected.keyboard2;

import android.view.View;

public class SoundCompat {
  public static void playClick(View v) {
    try { if (v != null) v.playSoundEffect(android.view.SoundEffectConstants.CLICK); } catch (Exception ignored) {}
  }
  public static void playClick(View v, Object o, Config c) {
    playClick(v);
  }
  public static void playClick(View v, Config c) {
    playClick(v);
  }
}
