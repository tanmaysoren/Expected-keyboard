package expected.keyboard2.prediction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps keyword triggers to emoji suggestions.
 */
public class EmojiPredictor
{
  private static final Map<String, String> EMOJI_MAP = new HashMap<String, String>();

  static
  {
    EMOJI_MAP.put("smile", "😊");
    EMOJI_MAP.put("happy", "😃");
    EMOJI_MAP.put("laugh", "😂");
    EMOJI_MAP.put("lol", "😂");
    EMOJI_MAP.put("haha", "😆");
    EMOJI_MAP.put("love", "❤️");
    EMOJI_MAP.put("heart", "❤️");
    EMOJI_MAP.put("kiss", "😘");
    EMOJI_MAP.put("wink", "😉");
    EMOJI_MAP.put("cool", "😎");
    EMOJI_MAP.put("fire", "🔥");
    EMOJI_MAP.put("lit", "🔥");
    EMOJI_MAP.put("clap", "👏");
    EMOJI_MAP.put("thumbsup", "👍");
    EMOJI_MAP.put("thumbs", "👍");
    EMOJI_MAP.put("like", "👍");
    EMOJI_MAP.put("ok", "👌");
    EMOJI_MAP.put("pray", "🙏");
    EMOJI_MAP.put("please", "🙏");
    EMOJI_MAP.put("thanks", "🙏");
    EMOJI_MAP.put("party", "🎉");
    EMOJI_MAP.put("celebrate", "🥳");
    EMOJI_MAP.put("birthday", "🎂");
    EMOJI_MAP.put("cake", "🎂");
    EMOJI_MAP.put("star", "⭐");
    EMOJI_MAP.put("hundred", "💯");
    EMOJI_MAP.put("100", "💯");
    EMOJI_MAP.put("check", "✅");
    EMOJI_MAP.put("yes", "✅");
    EMOJI_MAP.put("no", "❌");
    EMOJI_MAP.put("sad", "😢");
    EMOJI_MAP.put("cry", "😭");
    EMOJI_MAP.put("angry", "😡");
    EMOJI_MAP.put("thinking", "🤔");
    EMOJI_MAP.put("think", "🤔");
    EMOJI_MAP.put("cat", "🐱");
    EMOJI_MAP.put("dog", "🐶");
    EMOJI_MAP.put("coffee", "☕");
    EMOJI_MAP.put("pizza", "🍕");
    EMOJI_MAP.put("beer", "🍺");
    EMOJI_MAP.put("wine", "🍷");
    EMOJI_MAP.put("car", "🚗");
    EMOJI_MAP.put("phone", "📱");
    EMOJI_MAP.put("music", "🎵");
    EMOJI_MAP.put("sun", "☀️");
    EMOJI_MAP.put("moon", "🌙");
    EMOJI_MAP.put("money", "💰");
  }

  public static String getEmoji(String word)
  {
    if (word == null || word.length() < 2)
      return null;
    String lower = word.toLowerCase(Locale.ROOT);
    if (EMOJI_MAP.containsKey(lower))
      return EMOJI_MAP.get(lower);

    // Prefix match
    for (Map.Entry<String, String> e : EMOJI_MAP.entrySet())
    {
      if (lower.startsWith(e.getKey()) || e.getKey().startsWith(lower))
      {
        return e.getValue();
      }
    }
    return null;
  }
}
