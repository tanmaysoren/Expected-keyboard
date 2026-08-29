package expected.keyboard2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import expected.keyboard2.dict.DictionaryListView
import expected.keyboard2.prediction.PredictionEngine
import expected.keyboard2.prediction.WordCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("expected keyboard", appName)
  }

  @Test
  fun `prediction engine provides prefix suggestions and autocorrection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val engine = PredictionEngine.getInstance(context)
    assertNotNull(engine)

    // Test prefix completion
    val helloResults = engine.predict("hel", null, 5)
    assertTrue(helloResults.isNotEmpty())
    val words = helloResults.map { it.word }
    assertTrue(words.contains("hello") || words.contains("help"))

    // Test typo correction ("teh" -> "the", "woukd" -> "would")
    val tehResults = engine.predict("teh", null, 5)
    assertTrue(tehResults.isNotEmpty())
    assertEquals("the", tehResults[0].word)

    // Test next-word prediction (after "thank" -> predicts "you")
    val nextResults = engine.predict("", "thank", 5)
    assertTrue(nextResults.isNotEmpty())
    assertEquals("you", nextResults[0].word)

    // Test emoji prediction
    val emoji = engine.predictEmoji("smile")
    assertEquals("😊", emoji)
  }

  @Test
  fun `dictionary URL is valid`() {
    val url = DictionaryListView.url_of_dictionary("en_US")
    assertNotNull(url)
    assertTrue(url.toString().contains("en_US.dict"))
    assertTrue(url.toString().startsWith("https://raw.githubusercontent.com/Julow/Unexpected-Keyboard-dictionaries/refs/heads/main"))
  }

  @Test
  fun `clipboard text can be retrieved`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("test", "echo hello world"))
    assertTrue(cm.hasPrimaryClip())
    val clip = cm.primaryClip
    assertNotNull(clip)
    assertEquals("echo hello world", clip?.getItemAt(0)?.coerceToText(context).toString())
  }

  @Test
  fun `landscape layouts load and transform properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val res = context.resources
    val numericLand = KeyboardData.load(res, R.xml.numeric_landscape)
    assertNotNull(numericLand)
    assertTrue(numericLand.rows.size > 0)

    val pinLand = KeyboardData.load(res, R.xml.pin_landscape)
    assertNotNull(pinLand)
    assertTrue(pinLand.rows.size > 0)

    val qwerty = KeyboardData.load(res, R.xml.latn_qwerty_us)
    assertNotNull(qwerty)
    val splitMiddle = KeyboardData.load_row(res, R.xml.split_middle_column)
    LayoutModifier.split_middle_column = splitMiddle
    val landLayout = LayoutLandscapeModifier.transform_to_landscape(qwerty)
    assertNotNull(landLayout)
    assertEquals(qwerty.rows.size, landLayout.rows.size)

    val numRow = KeyboardData.load_row(res, R.xml.number_row)
    val transformedNumRow = LayoutLandscapeModifier.transform_number_row(numRow)
    assertNotNull(transformedNumRow)
    assertTrue(transformedNumRow.keys.size > 0)
  }

  @Test
  fun `word navigation offset calculation moves between words accurately`() {
    val sampleText = "The quick brown fox jumps"
    // Cursor at end of "The quick brown fox jumps" (length 25)
    // Moving 1 word left should jump before "jumps" (-5 chars)
    val left1 = KeyEventHandler.getWordOffsetLeft(sampleText, 1)
    assertEquals(-5, left1)

    // Moving 2 words left should jump before "fox" (-9 chars from end)
    val left2 = KeyEventHandler.getWordOffsetLeft(sampleText, 2)
    assertEquals(-9, left2)

    // Moving right from beginning "|The quick brown fox"
    val right1 = KeyEventHandler.getWordOffsetRight(sampleText, 1)
    assertEquals(3, right1) // after "The"

    // Moving right 2 words from beginning "|The quick brown fox"
    val right2 = KeyEventHandler.getWordOffsetRight(sampleText, 2)
    assertEquals(9, right2) // after "The quick"

    // Text with trailing spaces "The quick  "
    val textWithSpaces = "The quick  "
    val leftWithSpaces = KeyEventHandler.getWordOffsetLeft(textWithSpaces, 1)
    assertEquals(-7, leftWithSpaces) // skips 2 spaces and "quick" (7 chars)

    // Boundaries: moving left from empty / start returns 0 (never falls back to letter)
    assertEquals(0, KeyEventHandler.getWordOffsetLeft("", 1))
    assertEquals(0, KeyEventHandler.getWordOffsetLeft(null, 1))

    // Boundaries: moving right from empty / end returns 0 (never falls back to letter)
    assertEquals(0, KeyEventHandler.getWordOffsetRight("", 1))
    assertEquals(0, KeyEventHandler.getWordOffsetRight(null, 1))
  }

  @Test
  fun `space bar word navigation keys resolved as editing keys`() {
    val wordLeft = KeyValue.getKeyByName("word_left")
    val wordRight = KeyValue.getKeyByName("word_right")
    assertNotNull(wordLeft)
    assertNotNull(wordRight)
    assertEquals(KeyValue.Kind.Editing, wordLeft.kind)
    assertEquals(KeyValue.Kind.Editing, wordRight.kind)
    assertEquals(KeyValue.Editing.WORD_LEFT, wordLeft.editing)
    assertEquals(KeyValue.Editing.WORD_RIGHT, wordRight.editing)
  }

  @Test
  fun `suggestions max count is configured for 5 items`() {
    assertEquals(5, expected.keyboard2.suggestions.Suggestions.MAX_COUNT)
    assertEquals(5, expected.keyboard2.suggestions.CandidatesView.NUM_WORDS)
  }

  @Test
  fun `multi dictionary suggestion handling initialized properly`() {
    val suggestions = expected.keyboard2.suggestions.Suggestions(null, null)
    assertEquals(5, suggestions.suggestions.size)
    assertEquals(0, suggestions.count)
  }

  @Test
  fun `default checked extra keys matches configured defaults`() {
    val enabledKeys = setOf(
      "alt", "switch_clipboard", "tab", "esc", "switch_greekmath", "change_method",
      "copy", "paste", "cut", "selectAll", "undo", "redo", "delete_word",
      "forward_delete_word", "subscript", "superscript", "f11_placeholder", "f12_placeholder",
      "€", "ß", "£", "§", "†"
    )
    for (key in expected.keyboard2.prefs.ExtraKeysPreference.extra_keys) {
      val expected = enabledKeys.contains(key)
      assertEquals("Key $key default_checked mismatch", expected, expected.keyboard2.prefs.ExtraKeysPreference.default_checked(key))
    }
  }

  @Test
  fun `switching layouts preserves qwerty symbol positions`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    expected.keyboard2.Config.initGlobalConfig(prefs, context.resources, false, null)

    val qwertyId = expected.keyboard2.R.xml.latn_qwerty_us
    val russianId = expected.keyboard2.R.xml.cyrl_jcuken_ru

    val qwerty1 = expected.keyboard2.KeyboardData.load(context.resources, qwertyId)
    val modQwerty1 = expected.keyboard2.LayoutModifier.modify_layout(qwerty1)

    val russian = expected.keyboard2.KeyboardData.load(context.resources, russianId)
    val modRussian = expected.keyboard2.LayoutModifier.modify_layout(russian)

    val qwerty2 = expected.keyboard2.KeyboardData.load(context.resources, qwertyId)
    val modQwerty2 = expected.keyboard2.LayoutModifier.modify_layout(qwerty2)

    val keys1 = modQwerty1.keys
    val keys2 = modQwerty2.keys
    for ((kv, pos1) in keys1) {
      val pos2 = keys2[kv]
      assertNotNull("Key $kv should exist in qwerty2", pos2)
      assertEquals("Key $kv position changed after switching: was $pos1 but became $pos2", pos1, pos2)
    }
  }

  @Test
  fun `extra keys does not contain page up down home end`() {
    val extraKeysList = expected.keyboard2.prefs.ExtraKeysPreference.extra_keys.toList()
    assertFalse("page_up should be removed", extraKeysList.contains("page_up"))
    assertFalse("page_down should be removed", extraKeysList.contains("page_down"))
    assertFalse("home should be removed", extraKeysList.contains("home"))
    assertFalse("end should be removed", extraKeysList.contains("end"))
  }

  @Test
  fun `default config values match requested defaults`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context)
    prefs.edit().clear().commit()
    expected.keyboard2.Config.initGlobalConfig(prefs, context.resources, false, null)
    val config = expected.keyboard2.Config.globalConfig()

    assertTrue("add_number_row should be true for default", config.add_number_row)
    assertTrue("number_row_symbols should be true for symbols default", config.number_row_symbols)
    assertTrue("inverse_numpad should be true for low_first default", config.inverse_numpad)
    assertFalse("split_layout should be false for never default", config.split_layout)
    assertEquals(240, config.longPressTimeout)
    assertEquals(5, config.longPressInterval)
    assertTrue("double_tap_lock_shift should be true", config.double_tap_lock_shift)
    assertTrue("borderConfig should be true", config.borderConfig)
  }

  @Test
  fun `qwerty key swipable characters layout matches requested positions`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val keyboardData = KeyboardData.load(context.resources, expected.keyboard2.R.xml.latn_qwerty_us)

    // home at nw of q (dir 1)
    val homeKey = KeyValue.getKeyByName("home")
    val qKey = KeyValue.getKeyByName("q")
    val homePos = keyboardData.keys[homeKey]!!
    val qPos = keyboardData.keys[qKey]!!
    assertEquals(qPos.row, homePos.row)
    assertEquals(qPos.col, homePos.col)
    assertEquals(1, homePos.dir) // nw

    // f11 at ne of q (dir 2)
    val f11Key = KeyValue.getKeyByName("f11")
    val f11Pos = keyboardData.keys[f11Key]!!
    assertEquals(qPos.row, f11Pos.row)
    assertEquals(qPos.col, f11Pos.col)
    assertEquals(2, f11Pos.dir) // ne

    // f12 at nw of w (dir 1)
    val f12Key = KeyValue.getKeyByName("f12")
    val wKey = KeyValue.getKeyByName("w")
    val f12Pos = keyboardData.keys[f12Key]!!
    val wPos = keyboardData.keys[wKey]!!
    assertEquals(wPos.row, f12Pos.row)
    assertEquals(wPos.col, f12Pos.col)
    assertEquals(1, f12Pos.dir) // nw

    // ~ at ne of w (dir 2)
    val tildeKey = KeyValue.getKeyByName("~")
    val tildePos = keyboardData.keys[tildeKey]!!
    assertEquals(wPos.row, tildePos.row)
    assertEquals(wPos.col, tildePos.col)
    assertEquals(2, tildePos.dir) // ne

    // page_up at sw of w (dir 3)
    val pgUpKey = KeyValue.getKeyByName("page_up")
    val pgUpPos = keyboardData.keys[pgUpKey]!!
    assertEquals(wPos.row, pgUpPos.row)
    assertEquals(wPos.col, pgUpPos.col)
    assertEquals(3, pgUpPos.dir) // sw

    // page_down at nw of e (dir 1)
    val pgDnKey = KeyValue.getKeyByName("page_down")
    val eKey = KeyValue.getKeyByName("e")
    val pgDnPos = keyboardData.keys[pgDnKey]!!
    val ePos = keyboardData.keys[eKey]!!
    assertEquals(ePos.row, pgDnPos.row)
    assertEquals(ePos.col, pgDnPos.col)
    assertEquals(1, pgDnPos.dir) // nw

    // verify new special characters exist in qwerty layout
    val specialSymbols = listOf("₹", "¥", "µ", "œ", "₽", "æ", "¶", "—", "–", "≈", "›", "‹", "¦", "¢", "₿", "₱", "¿")
    for (sym in specialSymbols) {
      val keyVal = KeyValue.getKeyByName(sym)
      assertNotNull("Symbol $sym should exist as KeyValue", keyVal)
      assertTrue("Symbol $sym should be present on keyboard", keyboardData.keys.containsKey(keyVal))
    }
  }

  @Test
  fun `print screen key generates standard KeyEvent KEYCODE_SYSRQ`() {
    val prtSc = KeyValue.getKeyByName("print_screen")
    assertNotNull(prtSc)
    assertEquals(KeyValue.Kind.Keyevent, prtSc.kind)
    assertEquals(android.view.KeyEvent.KEYCODE_SYSRQ, prtSc.keyevent)
    assertEquals("PrtSc", prtSc.string)

    val prtScAlias = KeyValue.getKeyByName("prtsc")
    assertNotNull(prtScAlias)
    assertEquals(android.view.KeyEvent.KEYCODE_SYSRQ, prtScAlias.keyevent)
  }

  @Test
  fun `print screen key is placed in QWERTY layout at top right of letter p`() {
    val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
    val keyboardData = KeyboardData.load(context.resources, expected.keyboard2.R.xml.latn_qwerty_us)
    val prtScKey = KeyValue.getKeyByName("print_screen")
    val pKey = KeyValue.getKeyByName("p")
    val pos = keyboardData.keys[prtScKey]!!
    val pPos = keyboardData.keys[pKey]!!
    assertEquals("print_screen should be on same row as p", pPos.row, pos.row)
    assertEquals("print_screen should be on same column as p", pPos.col, pos.col)
    assertEquals("print_screen should be in NE / top-right (dir 2)", 2, pos.dir)
  }
}
