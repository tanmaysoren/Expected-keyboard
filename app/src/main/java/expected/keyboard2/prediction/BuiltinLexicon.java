package expected.keyboard2.prediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in high-frequency offline dictionary and n-gram statistical model
 * based on FUTO keyboard / open-source modern mobile keyboard corpora.
 * Enhanced with comprehensive English vocabulary for better suggestions.
 */
public class BuiltinLexicon
{
  public static final Map<String, Integer> FREQUENT_WORDS = new HashMap<String, Integer>(8000);
  public static final Map<String, List<String>> COMMON_BIGRAMS = new HashMap<String, List<String>>();
  public static final Map<String, String> COMMON_TYPOS = new HashMap<String, String>();

  static
  {
    // Populate frequent words with normalized unigram frequencies (1 - 255)
    String[] topWords = new String[]{
      // Top 500 English Core Words
      "the:255", "be:250", "to:245", "of:240", "and:235", "a:230", "in:225", "that:220", "have:215", "i:210",
      "it:205", "for:200", "not:195", "on:190", "with:185", "he:180", "as:175", "you:170", "do:165", "at:160",
      "this:155", "but:150", "his:145", "by:140", "from:135", "they:130", "we:125", "say:120", "her:115", "she:110",
      "or:105", "an:100", "will:98", "my:96", "one:94", "all:92", "would:90", "there:88", "their:86", "what:84",
      "so:82", "up:80", "out:78", "if:76", "about:74", "who:72", "get:70", "which:68", "go:66", "me:64",
      "when:62", "make:60", "can:58", "like:56", "time:54", "no:52", "just:50", "him:48", "know:46", "take:44",
      "people:42", "into:40", "year:38", "your:36", "good:34", "some:32", "could:30", "them:28", "see:26", "other:24",
      "than:22", "then:20", "now:19", "look:18", "only:17", "come:16", "its:15", "over:14", "think:13", "also:12",
      "back:11", "after:10", "use:10", "two:10", "how:10", "our:10", "work:10", "first:10", "well:10", "way:10",
      "even:9", "new:9", "want:9", "because:9", "any:9", "these:9", "give:9", "day:9", "most:9", "us:9",
      "great:9", "should:9", "need:9", "help:9", "where:9", "much:9", "right:9", "too:9", "tell:9", "very:9",
      "here:9", "never:9", "again:9", "call:9", "feel:8", "high:8", "every:8", "mean:8", "keep:8", "let:8",
      "begin:8", "seem:8", "talk:8", "turn:8", "start:8", "might:8", "show:8", "hear:8", "play:8", "run:8",
      "move:8", "live:8", "believe:8", "hold:8", "bring:8", "happen:8", "must:8", "write:8", "provide:8",
      "sit:7", "stand:7", "lose:7", "pay:7", "meet:7", "include:7", "continue:7", "set:7", "learn:7", "change:7",
      "lead:7", "understand:7", "watch:7", "follow:7", "stop:7", "create:7", "speak:7", "read:7", "allow:7", "add:7",
      "spend:7", "grow:7", "open:7", "walk:7", "win:7", "offer:7", "remember:7", "love:7", "consider:7", "appear:7",
      "buy:6", "wait:6", "serve:6", "die:6", "send:6", "expect:6", "build:6", "stay:6", "fall:6", "cut:6",
      "reach:6", "kill:6", "remain:6", "suggest:6", "raise:6", "pass:6", "sell:6", "require:6", "report:6", "decide:6",
      "pull:6", "thank:6", "thanks:6", "please:6", "hello:6", "world:6", "keyboard:6", "android:6", "phone:6", "message:6",
      "today:6", "tomorrow:6", "yesterday:6", "morning:6", "night:6", "friend:6", "family:6", "happy:6", "birthday:6", "ready:6",
      "always:6", "awesome:6", "beautiful:6", "better:6", "best:6", "busy:6", "clean:6", "cool:6", "different:6", "difficult:6",
      "early:5", "easy:5", "enough:5", "everything:5", "everyone:5", "everywhere:5", "excited:5", "famous:5", "fast:5", "fine:5",
      "free:5", "funny:5", "glad:5", "important:5", "interesting:5", "kind:5", "late:5", "little:5", "lucky:5", "maybe:5",
      "nice:5", "normal:5", "perfect:5", "possible:5", "pretty:5", "quick:5", "quiet:5", "real:5", "safe:5", "simple:5",
      "sorry:5", "special:5", "strong:5", "sure:5", "sweet:5", "terrible:5", "together:5", "true:5", "useful:5", "wonderful:5",
      "welcome:5", "already:5", "anyway:5", "around:5", "before:5", "behind:5", "between:5", "during:5", "inside:5", "outside:5",
      "without:5", "within:5", "under:5", "through:5", "towards:5", "across:5", "against:5", "along:5", "among:5", "beyond:5",
      "computer:4", "device:4", "system:4", "screen:4", "button:4", "window:4", "network:4", "online:4", "application:4", "service:4",
      "account:4", "password:4", "security:4", "setting:4", "settings:4", "option:4", "options:4", "feature:4", "features:4", "support:4",
      "problem:4", "question:4", "answer:4", "information:4", "number:4", "address:4", "location:4", "office:4", "company:4", "market:4",
      "water:4", "food:4", "coffee:4", "dinner:4", "lunch:4", "breakfast:4", "restaurant:4", "kitchen:4", "table:4", "chair:4",
      "house:4", "home:4", "room:4", "school:4", "college:4", "university:4", "station:4", "street:4", "road:4", "city:4",
      "country:4", "state:4", "place:4", "music:4", "movie:4", "photo:4", "video:4", "game:4", "picture:4", "story:4", "party:4",

      // English Contractions (high unigram frequencies)
      "don't:180", "can't:160", "won't:150", "i'm:200", "it's:190", "you're:175", "they're:140", "we're:130",
      "didn't:140", "doesn't:135", "isn't:120", "aren't:115", "wasn't:110", "weren't:100", "haven't:105",
      "hasn't:95", "hadn't:90", "wouldn't:100", "couldn't:95", "shouldn't:90", "i've:130", "you've:120",
      "we've:110", "they've:100", "i'll:140", "you'll:125", "he'll:100", "she'll:95", "we'll:120", "they'll:105",
      "i'd:125", "you'd:110", "he'd:90", "she'd:85", "we'd:95", "they'd:85", "that's:160", "what's:150",
      "there's:130", "here's:120", "where's:110", "how's:105", "let's:135", "who's:100",

      // Extended conversational & vocabulary words
      "absolutely:4", "actually:6", "almost:5", "although:4", "amazing:5", "another:6", "anyone:5", "anything:6",
      "anywhere:4", "appreciate:4", "approximately:3", "available:4", "basically:4", "battery:4", "becoming:4",
      "beginning:4", "behavior:4", "camera:4", "cancel:4", "certainly:4", "checking:4", "choose:4", "clear:4",
      "close:4", "completely:4", "connection:4", "continue:4", "correct:4", "currently:4", "customer:4", "database:4",
      "delete:4", "delivery:4", "developer:4", "difference:4", "direction:4", "download:4", "driving:4", "eating:4",
      "either:4", "energy:4", "entire:4", "especially:4", "essential:4", "everyday:4", "everything:5", "exactly:5",
      "excellent:4", "excited:4", "excuse:4", "experience:4", "favorite:4", "finally:5", "finished:4", "flight:4",
      "forever:4", "forget:4", "forward:4", "getting:5", "goodbye:4", "google:5", "group:4", "happened:4",
      "having:5", "headache:3", "healthy:4", "hearing:4", "history:4", "holiday:4", "honest:4", "honestly:4",
      "hopefully:4", "hospital:4", "hotel:4", "hungry:4", "husband:4", "imagine:4", "immediately:4", "instead:4",
      "internet:5", "invite:4", "keyboard:6", "language:4", "latest:4", "leaving:4", "lesson:3", "letter:4",
      "listening:4", "looking:5", "making:5", "manager:4", "meaning:4", "meeting:5", "memory:4", "minute:5",
      "minutes:5", "mobile:4", "moment:4", "monday:4", "monthly:3", "myself:4", "natural:4", "nearly:4",
      "necessary:4", "neither:4", "network:4", "nobody:4", "normal:4", "nothing:5", "notice:4", "number:5",
      "obviously:4", "online:5", "opening:4", "opinion:4", "package:4", "parent:4", "parking:4", "patient:4",
      "payment:4", "percent:4", "perhaps:4", "personal:4", "player:4", "police:4", "position:4", "practice:4",
      "preparing:4", "presence:3", "president:4", "probably:5", "product:4", "program:4", "project:4", "promise:4",
      "proper:4", "protect:4", "public:4", "purpose:4", "question:5", "quickly:4", "random:3", "reading:4",
      "really:6", "reason:4", "receive:4", "recently:4", "recommend:4", "record:4", "relative:3", "release:4",
      "remove:4", "replace:4", "request:4", "response:4", "return:4", "running:4", "saturday:4", "saying:4",
      "schedule:4", "screen:4", "search:5", "season:4", "second:4", "secret:3", "sending:4", "sentence:4",
      "separate:4", "serious:4", "service:4", "several:4", "sharing:4", "shortly:4", "shower:3", "similar:4",
      "simple:4", "simply:4", "sitting:4", "situation:4", "sleeping:4", "slowly:3", "sometime:4", "sometimes:5",
      "somewhere:4", "special:4", "specific:4", "speech:3", "spending:4", "standard:4", "standing:4", "starting:4",
      "station:4", "staying:4", "stopped:4", "straight:4", "strange:3", "street:4", "student:4", "studio:4",
      "stuff:4", "subject:4", "suddenly:4", "suggest:4", "sunday:4", "support:4", "suppose:4", "surface:3",
      "surprise:4", "system:4", "taking:5", "talking:4", "teacher:4", "telling:4", "temperature:3", "terminal:4",
      "terrible:4", "thinking:5", "thought:5", "thousand:4", "thursday:4", "together:5", "tonight:4", "totally:4",
      "traffic:4", "training:4", "travel:4", "trying:5", "tuesday:4", "turning:4", "understanding:4", "update:4",
      "urgent:3", "usually:4", "valuable:3", "various:4", "version:4", "waiting:4", "walking:4", "watching:4",
      "weather:4", "website:4", "wednesday:4", "weekend:5", "welcome:4", "whatever:4", "whenever:4", "whether:4",
      "willing:4", "window:4", "without:4", "working:5", "worried:4", "writing:4", "yourself:4",

      // Common verbs - additional forms
      "ask:8", "asked:6", "asking:5", "try:8", "tried:6", "trying:5", "use:8", "used:7", "using:5",
      "make:10", "made:8", "making:6", "get:10", "got:8", "getting:6", "go:10", "going:8", "went:7", "gone:6",
      "come:9", "came:7", "coming:6", "take:9", "took:7", "taken:6", "taking:5",
      "give:9", "gave:7", "given:6", "giving:5", "say:10", "said:8", "saying:6",
      "tell:9", "told:7", "telling:5", "know:10", "knew:7", "known:6", "knowing:5",
      "think:10", "thought:8", "thinking:6", "see:10", "saw:7", "seen:6", "seeing:5",
      "want:9", "wanted:7", "wanting:5", "like:9", "liked:7", "liking:5",
      "look:9", "looked:7", "looking:6", "find:9", "found:7", "finding:5",
      "feel:9", "felt:7", "feeling:6", "leave:8", "left:7", "leaving:5",
      "put:8", "putting:5", "keep:8", "kept:7", "keeping:5",
      "let:8", "letting:5", "start:8", "started:7", "starting:5",
      "seem:8", "seemed:6", "seeming:5", "help:8", "helped:6", "helping:5",
      "show:8", "showed:6", "shown:5", "showing:5", "hear:8", "heard:6", "hearing:5",
      "play:8", "played:6", "playing:5", "run:8", "ran:6", "running:6",
      "move:8", "moved:6", "moving:5", "live:8", "lived:6", "living:5",
      "believe:7", "believed:5", "believing:4", "bring:7", "brought:6", "bringing:5",
      "happen:7", "happened:6", "happening:5", "write:7", "wrote:6", "written:5", "writing:5",
      "sit:7", "sat:6", "sitting:5", "stand:7", "stood:6", "standing:5",
      "lose:7", "lost:6", "losing:5", "pay:7", "paid:6", "paying:5",
      "meet:7", "met:6", "meeting:5", "include:7", "included:6", "including:5",
      "continue:7", "continued:6", "continuing:5", "set:7", "setting:5",
      "learn:7", "learned:6", "learning:5", "change:7", "changed:6", "changing:5",
      "lead:7", "led:6", "leading:5", "understand:7", "understood:6", "understanding:5",
      "watch:7", "watched:6", "watching:5", "follow:7", "followed:6", "following:5",
      "stop:7", "stopped:6", "stopping:5", "create:7", "created:6", "creating:5",
      "speak:7", "spoke:6", "spoken:5", "speaking:5", "read:7", "reading:5",
      "allow:7", "allowed:6", "allowing:5", "add:7", "added:6", "adding:5",
      "spend:7", "spent:6", "spending:5", "grow:7", "grew:6", "grown:5", "growing:5",
      "open:7", "opened:6", "opening:5", "walk:7", "walked:6", "walking:5",
      "win:7", "won:6", "winning:5", "offer:7", "offered:6", "offering:5",
      "remember:7", "remembered:6", "remembering:5", "love:7", "loved:6", "loving:5",
      "consider:7", "considered:6", "considering:5", "appear:7", "appeared:6", "appearing:5",
      "buy:6", "bought:6", "buying:5", "wait:6", "waited:6", "waiting:5",
      "serve:6", "served:6", "serving:5", "die:6", "died:6", "dying:5",
      "send:6", "sent:6", "sending:5", "expect:6", "expected:6", "expecting:5",
      "build:6", "built:6", "building:5", "stay:6", "stayed:6", "staying:5",
      "fall:6", "fell:6", "fallen:5", "falling:5", "cut:6", "cutting:5",
      "reach:6", "reached:6", "reaching:5", "kill:6", "killed:6", "killing:5",
      "remain:6", "remained:6", "remaining:5", "suggest:6", "suggested:6", "suggesting:5",
      "raise:6", "raised:6", "raising:5", "pass:6", "passed:6", "passing:5",
      "sell:6", "sold:6", "selling:5", "require:6", "required:6", "requiring:5",
      "report:6", "reported:6", "reporting:5", "decide:6", "decided:6", "deciding:5",
      "pull:6", "pulled:6", "pulling:5",

      // Common nouns - additional
      "man:8", "woman:7", "child:7", "children:6", "boy:6", "girl:6",
      "time:10", "year:9", "day:9", "week:8", "month:7", "hour:7", "minute:7", "second:6",
      "life:8", "world:8", "hand:7", "part:7", "place:7", "case:7", "week:7",
      "company:6", "system:6", "program:6", "question:6", "work:8", "government:5",
      "number:7", "night:7", "point:7", "home:7", "water:6", "room:6", "mother:6",
      "area:6", "money:6", "story:6", "fact:6", "month:6", "lot:6", "right:7",
      "study:6", "book:6", "eye:6", "job:6", "word:6", "business:5", "issue:5",
      "side:6", "kind:6", "head:6", "house:6", "service:5", "friend:6", "father:6",
      "power:5", "hour:6", "game:6", "line:6", "end:6", "member:5", "law:5",
      "car:6", "city:6", "community:5", "name:6", "president:5", "team:5", "minute:6",
      "idea:6", "body:6", "info:5", "back:7", "parent:5", "face:6", "others:5",
      "level:5", "office:5", "door:5", "health:5", "person:6", "art:5", "war:5",
      "history:5", "party:5", "result:5", "change:6", "morning:6", "reason:5",
      "research:4", "girl:6", "guy:5", "moment:6", "air:5", "teacher:5",
      "force:5", "education:4", "dog:5", "cat:5", "food:6", "sun:5", "moon:5",
      "earth:5", "sky:5", "sea:5", "river:5", "mountain:4", "tree:5", "flower:4",
      "bird:5", "fish:5", "animal:5", "horse:4", "cow:4", "pig:4",
      "apple:4", "banana:3", "orange:4", "grape:3", "milk:4", "bread:4",
      "cake:4", "sugar:4", "salt:4", "rice:4", "meat:4", "fish:4",
      "tea:5", "juice:4", "beer:4", "wine:4", "drink:5", "eat:6",
      "sleep:5", "dream:4", "work:8", "play:7", "rest:5", "talk:6",
      "sing:4", "dance:4", "jump:4", "walk:6", "run:6", "swim:4",
      "read:7", "write:7", "study:6", "learn:6", "teach:5", "think:8",
      "know:8", "understand:6", "believe:5", "hope:6", "wish:5", "want:7",
      "need:7", "love:7", "like:8", "hate:4", "miss:5", "care:5",
      "help:7", "give:7", "take:7", "make:8", "do:10", "say:8",
      "tell:7", "ask:6", "answer:5", "listen:5", "watch:6", "see:8",
      "look:8", "find:7", "search:6", "check:5", "test:4", "try:6",
      "use:7", "open:6", "close:6", "start:6", "stop:6", "finish:5",
      "buy:5", "sell:5", "pay:6", "cost:4", "price:4", "cheap:4",
      "expensive:3", "money:6", "bank:4", "store:5", "shop:5", "market:5",
      "school:6", "college:4", "university:4", "class:5", "student:5", "teacher:5",
      "learn:6", "study:6", "read:7", "write:7", "test:4", "exam:4",
      "office:5", "work:8", "job:6", "business:5", "company:5", "boss:4",
      "employee:3", "meeting:5", "project:4", "report:4", "email:4", "call:6",
      "meeting:5", "schedule:4", "calendar:3", "plan:5", "goal:4", "task:4",
      "home:7", "house:6", "room:6", "kitchen:4", "bedroom:4", "bathroom:4",
      "living:5", "garden:4", "door:5", "window:5", "wall:4", "floor:4",
      "table:5", "chair:5", "bed:5", "sofa:3", "lamp:3", "desk:4",
      "computer:4", "phone:6", "laptop:3", "tablet:3", "tv:4", "radio:3",
      "car:6", "bus:4", "train:4", "plane:4", "bike:4", "taxi:3",
      "street:5", "road:5", "city:6", "town:4", "village:3", "country:5",
      "park:5", "beach:3", "mountain:3", "river:4", "lake:3", "forest:3",
      "sun:5", "moon:5", "star:4", "sky:5", "cloud:3", "rain:4",
      "snow:3", "wind:4", "storm:3", "weather:4", "season:4", "spring:4",
      "summer:4", "autumn:3", "winter:3", "monday:4", "tuesday:4", "wednesday:4",
      "thursday:4", "friday:5", "saturday:4", "sunday:4", "today:6", "tomorrow:5",
      "yesterday:5", "morning:6", "afternoon:4", "evening:4", "night:7", "week:7",
      "month:6", "year:8", "hour:6", "minute:6", "second:5", "time:10",

      // Adjectives - additional
      "big:6", "small:6", "long:6", "short:6", "old:7", "young:5",
      "good:8", "bad:6", "great:7", "best:7", "better:7", "worst:4",
      "new:8", "old:7", "first:7", "last:7", "next:6", "previous:4",
      "high:6", "low:5", "fast:5", "slow:4", "hard:5", "soft:4",
      "hot:5", "cold:5", "warm:4", "cool:5", "wet:4", "dry:4",
      "clean:5", "dirty:3", "easy:6", "difficult:5", "simple:5", "complex:3",
      "important:5", "interesting:4", "boring:3", "exciting:4", "amazing:5", "awesome:5",
      "beautiful:5", "ugly:3", "pretty:5", "handsome:3", "cute:4", "funny:5",
      "happy:6", "sad:4", "angry:4", "excited:5", "nervous:3", "scared:3",
      "tired:4", "busy:5", "free:5", "ready:5", "sure:5", "certain:4",
      "possible:5", "impossible:3", "sure:5", "true:5", "false:3", "real:5",
      "right:6", "wrong:4", "good:8", "bad:6", "best:7", "worst:4",
      "enough:5", "whole:4", "complete:4", "full:5", "empty:3", "half:4",
      "same:5", "different:5", "similar:4", "other:6", "another:5", "next:6",
      "special:5", "normal:5", "regular:3", "usual:3", "common:4", "rare:3",
      "popular:4", "famous:4", "new:8", "modern:3", "old:7", "ancient:3",
      "strong:5", "weak:3", "rich:4", "poor:3", "safe:5", "dangerous:3",
      "healthy:4", "sick:3", "sore:3", "tired:4", "fresh:4", "stale:2",
      "sweet:5", "sour:3", "bitter:3", "salty:3", "spicy:3", "delicious:3",
      "loud:4", "quiet:5", "silent:3", "noisy:3", "calm:4", "peaceful:3",
      "bright:4", "dark:4", "light:5", "heavy:4", "thick:3", "thin:4",
      "wide:4", "narrow:3", "deep:4", "shallow:2", "flat:3", "round:3",
      "sharp:3", "dull:2", "smooth:3", "rough:3", "soft:4", "hard:5",
      "tough:3", "gentle:3", "kind:6", "nice:5", "mean:5", "rude:3",
      "polite:3", "friendly:4", "angry:4", "funny:5", "serious:4", "silly:3",
      "smart:4", "stupid:3", "clever:3", "wise:3", "foolish:2", "brave:3",
      "cowardly:2", "honest:4", "dishonest:2", "loyal:3", "faithful:3", "true:5",
      "real:5", "fake:3", "genuine:3", "original:3", "copy:3", "fake:3",
      "automatic:3", "manual:3", "digital:3", "physical:3", "mental:3", "emotional:3",
      "social:3", "public:4", "private:3", "local:4", "global:3", "national:3",
      "international:3", "personal:4", "official:3", "legal:3", "medical:3", "technical:3",
      "basic:4", "advanced:3", "simple:5", "complex:3", "easy:6", "difficult:5",
      "possible:5", "impossible:3", "probable:3", "likely:4", "unlikely:3", "certain:4",
      "sure:5", "uncertain:3", "clear:5", "obvious:4", "vague:2", "unclear:3",
      "correct:5", "incorrect:2", "right:6", "wrong:4", "true:5", "false:3",
      "accurate:3", "inaccurate:2", "precise:3", "exact:4", "approximate:2", "rough:3",
      "specific:4", "general:3", "particular:3", "universal:2", "common:4", "rare:3",
      "usual:3", "unusual:2", "normal:5", "abnormal:2", "regular:3", "irregular:2",
      "standard:4", "nonstandard:1", "typical:3", "atypical:2", "average:3", "ordinary:3",
      "extraordinary:2", "special:5", "unique:3", "rare:3", "common:4", "frequent:3",
      "occasional:2", "rare:3", "constant:3", "variable:2", "stable:3", "unstable:2",
      "permanent:3", "temporary:3", "long:6", "short:6", "lasting:3", "brief:3",
      "enduring:2", "fleeting:2", "eternal:2", "mortal:2", "infinite:2", "finite:2",
      "absolute:3", "relative:3", "complete:4", "incomplete:2", "total:4", "partial:3",
      "entire:4", "partial:3", "whole:4", "half:4", "full:5", "empty:3",
      "perfect:5", "imperfect:2", "flawless:2", "faulty:2", "ideal:3", "realistic:3",
      "practical:3", "theoretical:2", "useful:5", "useless:3", "helpful:4", "harmful:2",
      "beneficial:3", "detrimental:2", "valuable:4", "worthless:2", "precious:3", "cheap:4",
      "expensive:3", "affordable:3", "costly:3", "economical:2", "wasteful:2", "thrifty:2",
      "generous:3", "stingy:2", "selfish:3", "selfless:2", "kind:6", "cruel:3",
      "gentle:3", "rough:3", "tender:3", "tough:3", "soft:4", "hard:5",
      "smooth:3", "rough:3", "even:5", "uneven:2", "flat:3", "bumpy:2",
      "level:4", "sloped:2", "straight:4", "curved:3", "round:3", "square:3",
      "angular:2", "circular:2", "triangular:2", "rectangular:2", "symmetric:2", "asymmetric:2",
      "balanced:3", "unbalanced:2", "organized:3", "disorganized:2", "tidy:3", "messy:3",
      "neat:3", "sloppy:2", "orderly:3", "chaotic:2", "calm:4", "chaotic:2",
      "peaceful:3", "violent:2", "quiet:5", "noisy:3", "silent:3", "loud:4",
      "soft:4", "hard:5", "gentle:3", "harsh:3", "mild:3", "intense:3",
      "extreme:3", "moderate:3", "severe:3", "slight:3", "major:4", "minor:4",
      "important:5", "trivial:2", "significant:3", "insignificant:2", "meaningful:3", "meaningless:2",
      "valuable:4", "worthless:2", "useful:5", "useless:3", "helpful:4", "harmful:2",
      "effective:3", "ineffective:2", "efficient:3", "inefficient:2", "productive:3", "unproductive:2",
      "successful:3", "unsuccessful:2", "victorious:2", "defeated:2", "winning:3", "losing:2",
      "happy:6", "sad:4", "joyful:3", "sorrowful:2", "cheerful:3", "gloomy:2",
      "excited:5", "bored:3", "enthusiastic:3", "indifferent:2", "passionate:3", "apathetic:2",
      "interested:4", "bored:3", "curious:3", "indifferent:2", "attentive:3", "distracted:2",
      "focused:3", "scattered:2", "concentrated:3", "absent-minded:2", "alert:3", "drowsy:2",
      "awake:3", "asleep:3", "conscious:3", "unconscious:2", "aware:4", "unaware:2",
      "mindful:3", "mindless:2", "thoughtful:3", "thoughtless:2", "considerate:3", "inconsiderate:2",
      "respectful:3", "disrespectful:2", "polite:3", "rude:3", "courteous:2", "impolite:2",
      "mannerly:2", "unmannerly:2", "civil:3", "uncivil:2", "decent:3", "indecent:2",
      "proper:4", "improper:2", "appropriate:3", "inappropriate:2", "suitable:3", "unsuitable:2",
      "fitting:3", "unfitting:2", "becoming:3", "unbecoming:2", "seemly:2", "unseemly:2",
      "decorous:2", "indecorous:2", "dignified:2", "undignified:2", "elegant:3", "inelegant:2",
      "graceful:3", "awkward:3", "skillful:3", "clumsy:3", "adept:2", "inept:2",
      "proficient:2", "inproficient:1", "competent:3", "incompetent:2", "capable:3", "incapable:2",
      "able:5", "unable:3", "enabled:2", "disabled:3", "qualified:3", "unqualified:2",
      "certified:2", "uncertified:1", "licensed:2", "unlicensed:1", "authorized:2", "unauthorized:2",
      "permitted:2", "forbidden:2", "allowed:3", "prohibited:2", "legal:3", "illegal:2",
      "lawful:2", "unlawful:2", "legitimate:3", "illegitimate:2", "genuine:3", "fake:3",
      "authentic:3", "counterfeit:2", "real:5", "false:3", "true:5", "untrue:2",
      "accurate:3", "inaccurate:2", "correct:5", "incorrect:2", "right:6", "wrong:4",
      "valid:3", "invalid:2", "sound:3", "unsound:2", "reliable:3", "unreliable:2",
      "dependable:3", "undependable:2", "trustworthy:3", "untrustworthy:2", "faithful:3", "faithless:2",
      "loyal:3", "disloyal:2", "constant:3", "inconstant:2", "steadfast:2", "fickle:2",
      "resolute:2", "irresolute:2", "determined:3", "undetermined:2", "decided:3", "undecided:2",
      "firm:3", "weak:3", "solid:3", "liquid:3", "gas:3", "plasma:2",
      "hard:5", "soft:4", "tough:3", "tender:3", "rigid:2", "flexible:3",
      "stiff:3", "limp:2", "sturdy:3", "fragile:3", "strong:5", "weak:3",
      "powerful:3", "powerless:2", "mighty:2", "feeble:2", "energetic:3", "tired:4",
      "active:3", "passive:3", "dynamic:3", "static:2", "lively:3", "dull:3",
      "vibrant:2", "drab:2", "bright:4", "dark:4", "colorful:3", "colorless:2",
      "vivid:2", "pale:3", "brilliant:2", "dim:3", "shining:2", "fading:2",
      "glowing:2", "dull:3", "radiant:2", "gloomy:2", "luminous:2", "dark:4",
      "transparent:2", "opaque:2", "translucent:2", "clear:5", "cloudy:3", "murky:2",
      "pure:3", "polluted:2", "clean:5", "dirty:3", "fresh:4", "stale:3",
      "new:8", "old:7", "modern:3", "ancient:3", "current:4", "past:5",
      "present:5", "future:5", "recent:4", "upcoming:2", "recent:4", "former:3",
      "latter:3", "first:7", "last:7", "primary:3", "secondary:3", "tertiary:2",
      "major:4", "minor:4", "main:5", "chief:3", "principal:3", "secondary:3",
      "important:5", "unimportant:2", "significant:3", "insignificant:2", "crucial:3", "trivial:2",
      "vital:3", "trivial:2", "essential:4", "nonessential:2", "necessary:4", "unnecessary:2",
      "required:3", "optional:3", "mandatory:3", "voluntary:3", "compulsory:2", "elective:2",
      "forced:3", "willing:4", "reluctant:3", "eager:3", "keen:3", "avid:2",
      "enthusiastic:3", "indifferent:2", "passionate:3", "apathetic:2", "zealous:2", "lukewarm:2",
      "fervent:2", "tepid:2", "ardent:2", "apathetic:2", "fierce:3", "gentle:3",
      "violent:2", "peaceful:3", "aggressive:3", "passive:3", "hostile:3", "friendly:4",
      "antagonistic:2", "cooperative:3", "competitive:3", "collaborative:2", "opposing:3", "supportive:3",
      "contrary:3", "agreeable:3", "opposite:3", "similar:4", "different:5", "alike:3",
      "identical:2", "distinct:3", "unique:3", "common:4", "familiar:3", "strange:3",
      "foreign:3", "native:3", "domestic:3", "international:3", "local:4", "global:3",
      "universal:2", "particular:3", "general:3", "specific:4", "broad:3", "narrow:3",
      "wide:4", "thin:4", "thick:3", "deep:4", "shallow:3", "tall:4",
      "short:6", "long:6", "high:6", "low:5", "elevated:2", "depressed:3",
      "raised:3", "lowered:2", "lifted:3", "dropped:3", "up:8", "down:6",
      "above:4", "below:4", "over:6", "under:5", "inside:5", "outside:5",
      "internal:3", "external:3", "interior:3", "exterior:3", "inner:3", "outer:3",
      "central:3", "peripheral:2", "middle:4", "edge:4", "center:4", "boundary:3",
      "boundary:3", "border:3", "limit:4", "extent:3", "range:4", "scope:3",
      "reach:4", "grasp:3", "span:3", "spread:3", "width:3", "length:4",
      "height:3", "depth:3", "size:5", "dimensions:2", "proportions:2", "measurements:2",
      "area:5", "volume:3", "space:5", "room:6", "place:6", "position:5",
      "location:5", "situation:4", "condition:4", "state:6", "status:4", "circumstance:3",
      "situation:4", "context:3", "environment:3", "setting:4", "background:4", "framework:3",
      "basis:3", "foundation:3", "ground:5", "surface:4", "top:5", "bottom:4",
      "front:5", "back:7", "side:6", "edge:4", "corner:3", "angle:3",
      "point:6", "line:6", "curve:3", "circle:3", "square:3", "triangle:3",
      "shape:4", "form:5", "figure:4", "pattern:3", "design:3", "structure:3",
      "system:6", "organization:3", "arrangement:3", "order:5", "sequence:3", "series:3",
      "chain:3", "link:3", "connection:4", "relationship:3", "association:3", "bond:3",
      "tie:4", "knot:2", "loop:3", "circle:3", "ring:3", "cycle:3",
      "round:3", "turn:6", "twist:3", "bend:3", "fold:3", "break:5",
      "crack:3", "split:3", "tear:3", "rip:3", "cut:6", "slice:3",
      "chop:2", "dice:2", "mince:2", "grind:2", "crush:2", "smash:2",
      "hit:5", "strike:3", "beat:4", "punch:3", "kick:3", "slap:3",
      "touch:4", "feel:6", "stroke:3", "rub:3", "press:4", "push:4",
      "pull:5", "drag:3", "draw:4", "write:7", "draw:4", "paint:3",
      "color:4", "shade:3", "tint:2", "hue:2", "tone:3", "brightness:2",
      "darkness:3", "light:5", "shadow:3", "glow:2", "shine:3", "sparkle:2",
      "flash:3", "flicker:2", "beam:3", "ray:3", "gleam:2", "glint:2",
      "glimmer:2", "shimmer:2", "twinkle:2", "glitter:2", "glow:2", "radiance:2",
      "brilliance:2", "luster:2", "sheen:2", "gloss:2", "polish:2", "shine:3",
      "reflection:3", "mirror:3", "image:4", "picture:5", "photo:4", "video:4",
      "film:4", "movie:4", "screen:5", "display:4", "show:6", "exhibit:2",
      "presentation:3", "demonstration:2", "performance:3", "display:4", "exhibition:2", "showcase:2",
      "reveal:3", "conceal:2", "hide:4", "discover:3", "find:7", "search:5",
      "seek:3", "look:8", "watch:6", "observe:3", "notice:4", "detect:2",
      "identify:3", "recognize:3", "know:8", "understand:6", "comprehend:2", "grasp:3",
      "apprehend:2", "perceive:3", "sense:4", "feel:6", "intuit:1", "imagine:4",
      "envision:2", "picture:5", "visualize:2", "dream:4", "fantasize:2", "hallucinate:1",
      "illusion:2", "mirage:2", "delusion:2", "fantasy:3", "reality:4", "truth:4",
      "fact:6", "fiction:3", "story:6", "tale:3", "myth:3", "legend:3",
      "history:5", "account:4", "report:4", "record:4", "document:3", "file:4",
      "folder:3", "directory:3", "folder:3", "cabinet:2", "drawer:2", "shelf:2",
      "rack:2", "hook:2", "hanger:2", "peg:2", "nail:2", "screw:2",
      "bolt:2", "nut:2", "washer:2", "spring:3", "gear:2", "wheel:3",
      "axle:2", "shaft:2", "lever:2", "pulley:2", "rope:3", "cord:2",
      "string:3", "thread:3", "wire:3", "cable:3", "pipe:3", "tube:3",
      "hose:2", "duct:2", "channel:3", "tunnel:2", "passage:3", "path:4",
      "road:5", "street:5", "highway:3", "freeway:2", "expressway:2", "turnpike:1",
      "bridge:3", "tunnel:2", "overpass:2", "underpass:2", "intersection:2", "crossroad:2",
      "corner:3", "bend:3", "curve:3", "turn:6", " twist:2", " loop:2",
      "circle:3", "ring:3", "oval:2", "square:3", "rectangle:2", "triangle:3",
      "polygon:2", "polyhedron:1", "sphere:2", "cube:2", "cylinder:2", "cone:2",
      "pyramid:2", "prism:2", "shape:4", "form:5", "figure:4", "pattern:3",
      "design:3", "structure:3", "framework:3", "skeleton:2", "body:6", "organ:2",
      "tissue:2", "cell:2", "molecule:2", "atom:2", "electron:2", "proton:2",
      "neutron:2", "photon:2", "quark:1", "lepton:1", "boson:1", "fermion:1",
      "particle:2", "wave:3", "field:4", "force:5", "energy:4", "matter:3",
      "mass:3", "weight:3", "density:2", "volume:3", "pressure:2", "temperature:3",
      "heat:4", "cold:5", "warmth:2", "chill:2", "freeze:3", "melt:2",
      "boil:2", "evaporate:2", "condense:2", "precipitate:1", "sublimate:1", "deposit:2",
      "sediment:1", "erosion:1", "weathering:1", "decomposition:1", "fermentation:1", "oxidation:1",
      "reduction:2", "combustion:1", "explosion:2", "implosion:1", "collision:2", "impact:3",
      "force:5", "motion:3", "movement:3", "speed:4", "velocity:2", "acceleration:2",
      "deceleration:1", "gravity:2", "friction:2", "resistance:3", "drag:3", "lift:3",
      "thrust:2", "buoyancy:1", "tension:2", "compression:2", "shear:1", "torsion:1",
      "bending:2", "twisting:2", "stretching:2", "shrinking:2", "expansion:2", "contraction:2",
      "growth:3", "decline:3", "increase:3", "decrease:3", "rise:4", "fall:6",
      "climb:3", "descend:2", "ascend:2", "plunge:2", "dive:3", "soar:2",
      "hover:2", "float:3", "sink:3", "drown:2", "swim:4", "wade:2",
      "walk:6", "run:6", "jog:3", "sprint:2", "race:3", "chase:3",
      "follow:6", "pursue:2", "retreat:2", "flee:2", "escape:3", "evade:2",
      "dodge:2", "avoid:3", "bypass:2", "circumvent:1", "surround:2", "encircle:1",
      "encompass:1", "include:6", "exclude:2", "contain:3", "hold:7", "keep:7",
      "retain:2", "maintain:3", "preserve:2", "protect:4", "defend:3", "guard:3",
      "shield:2", "shelter:2", "cover:4", "hide:4", "conceal:2", "reveal:3",
      "expose:2", "uncover:2", "discover:3", "find:7", "lose:6", "miss:5",
      "drop:4", "fall:6", "land:4", "settle:3", "rest:5", "relax:3",
      "sleep:5", "wake:4", "arise:2", "stand:6", "sit:6", "lie:5",
      "recline:1", "bend:3", "stoop:2", "kneel:2", "crouch:2", "squat:2",
      "crawl:2", "creep:2", "slither:1", "slide:3", "slip:3", "fall:6",
      "trip:2", "stumble:2", "falter:1", "hesitate:2", "pause:3", "stop:6",
      "halt:2", "cease:2", "end:6", "finish:5", "complete:4", "terminate:2",
      "conclude:2", "resolve:2", "settle:3", "decide:5", "determine:3", "establish:2",
      "confirm:2", "verify:2", "validate:2", "prove:3", "demonstrate:2", "show:6",
      "display:4", "exhibit:2", "present:5", "reveal:3", "disclose:2", "expose:2",
      "uncover:2", "discover:3", "find:7", "locate:3", "identify:3", "recognize:3",
      "know:8", "understand:6", "comprehend:2", "grasp:3", "apprehend:2", "perceive:3",
      "sense:4", "feel:6", "intuit:1", "imagine:4", "envision:2", "picture:5",
      "visualize:2", "dream:4", "fantasize:2", "think:8", "ponder:2", "reflect:3",
      "consider:6", "contemplate:2", "meditate:2", "ruminate:1", "brood:1", "worry:4",
      "fret:2", "agonize:1", "torment:2", "torture:2", "suffer:3", "endure:2",
      "tolerate:2", "bear:4", "stand:6", "abide:2", "stomach:2", "swallow:2",
      "digest:2", "absorb:2", "assimilate:1", "integrate:2", "incorporate:2", "include:6",
      "exclude:2", "add:6", "subtract:2", "multiply:2", "divide:2", "calculate:2",
      "compute:2", "reckon:1", "count:4", "tally:1", "total:4", "sum:4",
      "average:3", "mean:5", "median:1", "mode:2", "range:4", "spread:3",
      "distribution:2", "variation:2", "deviation:2", "error:3", "mistake:3", "blunder:2",
      "slip:3", "lapse:2", "failure:3", "fault:3", "defect:2", "flaw:2",
      "imperfection:1", "blemish:1", "spot:4", "stain:2", "mark:5", "trace:3",
      "sign:4", "indication:2", "evidence:3", "proof:3", "testimony:2", "witness:3",
      "confirmation:2", "verification:2", "validation:2", "authentication:1", "certification:1", "authorization:1",
      "permission:3", "consent:2", "approval:2", "agreement:3", "disagreement:2", "objection:2",
      "protest:2", "complaint:2", "criticism:2", "praise:2", "compliment:2", "flattery:1",
      "admiration:2", "respect:3", "reverence:2", "awe:2", "wonder:3", "amazement:2",
      "astonishment:2", "surprise:4", "shock:3", "disbelief:2", "skepticism:2", "doubt:3",
      "certainty:3", "confidence:3", "trust:3", "faith:3", "belief:3", "conviction:2",
      "opinion:4", "view:5", "perspective:2", "standpoint:2", "viewpoint:2", "outlook:2",
      "attitude:3", "approach:3", "method:3", "technique:2", "strategy:3", "tactic:2",
      "plan:5", "scheme:2", "project:4", "program:4", "policy:3", "procedure:2",
      "process:4", "system:6", "method:3", "way:7", "means:3", "manner:3",
      "fashion:3", "style:4", "mode:2", "form:5", "shape:4", "pattern:3",
      "design:3", "structure:3", "framework:3", "organization:3", "arrangement:3", "order:5",
      "sequence:3", "series:3", "chain:3", "link:3", "connection:4", "relationship:3",
      "association:3", "bond:3", "tie:4", "knot:2", "loop:3", "circle:3",
      "ring:3", "cycle:3", "round:3", "turn:6", "twist:3", "bend:3",
      "fold:3", "break:5", "crack:3", "split:3", "tear:3", "rip:3",
      "cut:6", "slice:3", "chop:2", "dice:2", "mince:2", "grind:2",
      "crush:2", "smash:2", "hit:5", "strike:3", "beat:4", "punch:3",
      "kick:3", "slap:3", "touch:4", "feel:6", "stroke:3", "rub:3",
      "press:4", "push:4", "pull:5", "drag:3", "draw:4", "write:7",
      "draw:4", "paint:3", "color:4", "shade:3", "tint:2", "hue:2",
      "tone:3", "brightness:2", "darkness:3", "light:5", "shadow:3", "glow:2",
      "shine:3", "sparkle:2", "flash:3", "flicker:2", "beam:3", "ray:3",
      "gleam:2", "glint:2", "glimmer:2", "shimmer:2", "twinkle:2", "glitter:2",
      "glow:2", "radiance:2", "brilliance:2", "luster:2", "sheen:2", "gloss:2",
      "polish:2", "shine:3", "reflection:3", "mirror:3", "image:4", "picture:5",
      "photo:4", "video:4", "film:4", "movie:4", "screen:5", "display:4",
      "show:6", "exhibit:2", "presentation:3", "demonstration:2", "performance:3", "display:4",
      "exhibition:2", "showcase:2", "reveal:3", "conceal:2", "hide:4", "discover:3",
      "find:7", "search:5", "seek:3", "look:8", "watch:6", "observe:3",
      "notice:4", "detect:2", "identify:3", "recognize:3", "know:8", "understand:6",
      "comprehend:2", "grasp:3", "apprehend:2", "perceive:3", "sense:4", "feel:6",
      "intuit:1", "imagine:4", "envision:2", "picture:5", "visualize:2", "dream:4",
      "fantasize:2", "reality:4", "truth:4", "fact:6", "fiction:3", "story:6",
      "tale:3", "myth:3", "legend:3", "history:5", "account:4", "report:4",
      "record:4", "document:3", "file:4", "folder:3", "directory:3",

      // Common phrases and expressions
      "okay:6", "ok:6", "yes:7", "no:7", "maybe:5", "sure:5",
      "thanks:7", "thank:7", "please:6", "sorry:5", "excuse:4", "pardon:3",
      "hello:6", "hi:7", "hey:5", "goodbye:4", "bye:4", "see:8",
      "later:5", "soon:5", "welcome:5", "cheers:3", "congratulations:2", "congrats:3",
      "good:8", "morning:6", "afternoon:4", "evening:4", "night:7",
      "how:7", "are:8", "you:8", "doing:5", "what's:6", "up:8",
      "nothing:5", "much:5", "good:8", "great:7", "fine:5", "well:7",
      "okay:6", "alright:4", "perfect:5", "awesome:5", "amazing:5", "wonderful:5",
      "terrible:5", "horrible:3", "awful:3", "bad:6", "worst:4", "best:7",
      "love:7", "hate:4", "like:8", "enjoy:4", "prefer:3", "dislike:3",
      "want:7", "need:7", "wish:5", "hope:6", "expect:4", "predict:2",
      "think:8", "believe:5", "know:8", "understand:6", "remember:5", "forget:4",
      "learn:6", "teach:5", "study:6", "read:7", "write:7", "speak:6",
      "listen:5", "hear:6", "see:8", "watch:6", "look:8", "find:7",
      "search:5", "check:5", "test:4", "try:6", "use:7", "make:8",
      "do:10", "go:8", "come:7", "get:8", "give:7", "take:7",
      "put:6", "keep:7", "let:7", "start:6", "stop:6", "finish:5",
      "begin:6", "end:6", "open:6", "close:6", "turn:6", "move:6",
      "run:6", "walk:6", "sit:6", "stand:6", "lie:5", "sleep:5",
      "wake:4", "eat:6", "drink:5", "cook:4", "wash:3", "clean:5",
      "work:8", "play:7", "rest:5", "relax:3", "enjoy:4", "have:8"
    };

    for (String s : topWords)
    {
      String[] parts = s.split(":");
      if (parts.length == 2)
      {
        try
        {
          FREQUENT_WORDS.put(parts[0], Integer.parseInt(parts[1]));
        }
        catch (NumberFormatException ignored) {}
      }
    }

    // Common Bigram Predictions (Statistical Next-Word Engine like FUTO)
    addBigram("i", "am", "have", "will", "would", "think", "know", "want", "can", "could", "like", "need", "feel", "was", "don't", "hope", "love");
    addBigram("you", "are", "have", "can", "know", "want", "will", "need", "should", "like", "get", "do", "think", "could", "were", "look");
    addBigram("he", "is", "was", "has", "said", "will", "would", "can", "knows", "wants", "thought", "looks");
    addBigram("she", "is", "was", "has", "said", "will", "would", "can", "knows", "wants", "thought", "looks");
    addBigram("it", "is", "was", "will", "would", "can", "could", "seems", "looks", "has", "feels", "works");
    addBigram("we", "are", "have", "can", "will", "need", "should", "want", "can't", "were", "could", "know");
    addBigram("they", "are", "were", "have", "will", "would", "can", "had", "say", "want", "need");
    addBigram("how", "are", "is", "about", "do", "much", "many", "can", "was", "did", "to");
    addBigram("what", "is", "are", "do", "did", "time", "about", "happened", "can", "if", "would", "a");
    addBigram("where", "are", "is", "did", "were", "can", "have", "do");
    addBigram("why", "are", "is", "did", "not", "would", "do", "can't");
    addBigram("when", "will", "are", "is", "can", "did", "you", "i");
    addBigram("who", "is", "are", "was", "can", "will", "would");
    addBigram("thank", "you", "so", "very", "god", "everyone");
    addBigram("thanks", "for", "again", "a", "so", "much", "man", "mate");
    addBigram("good", "morning", "night", "afternoon", "evening", "job", "luck", "idea", "time", "day", "news");
    addBigram("happy", "birthday", "new", "anniversary", "to", "for", "holidays", "weekend");
    addBigram("see", "you", "later", "soon", "tomorrow", "there", "what", "how");
    addBigram("let", "me", "us", "know", "go", "see", "them", "him", "her");
    addBigram("have", "a", "to", "been", "good", "great", "nice", "fun", "the", "any", "no");
    addBigram("on", "the", "my", "your", "this", "time", "it", "top", "way", "board");
    addBigram("in", "the", "a", "my", "your", "this", "order", "fact", "front", "case", "terms");
    addBigram("at", "the", "all", "least", "home", "work", "first", "night", "once");
    addBigram("for", "the", "you", "your", "me", "this", "example", "sure", "now", "us");
    addBigram("with", "you", "the", "me", "my", "your", "them", "us", "him", "her", "that");
    addBigram("about", "to", "the", "it", "that", "this", "you", "how", "what");
    addBigram("can", "you", "i", "we", "be", "do", "see", "get", "help", "find");
    addBigram("could", "you", "be", "have", "not", "we", "i");
    addBigram("would", "be", "you", "like", "have", "love", "prefer");
    addBigram("should", "be", "have", "we", "i", "get", "take");
    addBigram("please", "let", "help", "send", "call", "find", "check", "do", "give", "note");
    addBigram("looking", "forward", "for", "at", "good", "great");
    addBigram("take", "care", "it", "your", "a", "the", "time", "off");
    addBigram("don't", "know", "worry", "have", "think", "want", "forget", "like", "need", "get");
    addBigram("can't", "wait", "believe", "see", "do", "find", "get", "help");
    addBigram("i'm", "not", "going", "so", "sure", "sorry", "ready", "happy", "in", "at", "on");
    addBigram("it's", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time");
    addBigram("you're", "welcome", "the", "going", "right", "not", "so", "very");
    addBigram("that's", "great", "good", "awesome", "cool", "fine", "true", "right", "what", "why");
    addBigram("let's", "go", "do", "see", "meet", "get", "start", "make");
    addBigram("is", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("was", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("are", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("were", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("has", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("had", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("will", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("would", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("could", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("should", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("can", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("do", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("does", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("did", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("don't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right", "know", "want", "need", "have");
    addBigram("doesn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("didn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("can't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right", "wait", "believe");
    addBigram("won't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("isn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("aren't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("wasn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("weren't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("haven't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("hasn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("hadn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("wouldn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("couldn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");
    addBigram("shouldn't", "a", "the", "not", "so", "going", "very", "been", "okay", "good", "time", "right");

    // Frequent Typo & Contraction Corrections (FUTO-style Instant Typo Map)
    COMMON_TYPOS.put("teh", "the");
    COMMON_TYPOS.put("adn", "and");
    COMMON_TYPOS.put("taht", "that");
    COMMON_TYPOS.put("waht", "what");
    COMMON_TYPOS.put("thsi", "this");
    COMMON_TYPOS.put("woukd", "would");
    COMMON_TYPOS.put("coukd", "could");
    COMMON_TYPOS.put("shoukd", "should");
    COMMON_TYPOS.put("peopel", "people");
    COMMON_TYPOS.put("becasue", "because");
    COMMON_TYPOS.put("becuase", "because");
    COMMON_TYPOS.put("recieve", "receive");
    COMMON_TYPOS.put("recieved", "received");
    COMMON_TYPOS.put("seperate", "separate");
    COMMON_TYPOS.put("definately", "definitely");
    COMMON_TYPOS.put("untill", "until");
    COMMON_TYPOS.put("tommorow", "tomorrow");
    COMMON_TYPOS.put("tommorrow", "tomorrow");
    COMMON_TYPOS.put("alot", "a lot");
    COMMON_TYPOS.put("wierd", "weird");
    COMMON_TYPOS.put("freind", "friend");
    COMMON_TYPOS.put("freinds", "friends");
    COMMON_TYPOS.put("beleive", "believe");
    COMMON_TYPOS.put("acheive", "achieve");
    COMMON_TYPOS.put("goverment", "government");
    COMMON_TYPOS.put("occured", "occurred");
    COMMON_TYPOS.put("truely", "truly");
    COMMON_TYPOS.put("knowlege", "knowledge");
    COMMON_TYPOS.put("remeber", "remember");
    COMMON_TYPOS.put("thier", "their");
    COMMON_TYPOS.put("alright", "all right");
    COMMON_TYPOS.put("succesful", "successful");
    COMMON_TYPOS.put("neccessary", "necessary");
    COMMON_TYPOS.put("accomodate", "accommodate");
    COMMON_TYPOS.put("embarass", "embarrass");
    COMMON_TYPOS.put("maintainance", "maintenance");
    COMMON_TYPOS.put("recomended", "recommended");
    COMMON_TYPOS.put("recommand", "recommend");
    COMMON_TYPOS.put("writting", "writing");
    COMMON_TYPOS.put("completly", "completely");
    COMMON_TYPOS.put("basicly", "basically");
    COMMON_TYPOS.put("publically", "publicly");
    COMMON_TYPOS.put("pronounciation", "pronunciation");
    COMMON_TYPOS.put("arguement", "argument");

    // Missing Apostrophe Contractions
    COMMON_TYPOS.put("dont", "don't");
    COMMON_TYPOS.put("cant", "can't");
    COMMON_TYPOS.put("wont", "won't");
    COMMON_TYPOS.put("didnt", "didn't");
    COMMON_TYPOS.put("doesnt", "doesn't");
    COMMON_TYPOS.put("isnt", "isn't");
    COMMON_TYPOS.put("arent", "aren't");
    COMMON_TYPOS.put("wasnt", "wasn't");
    COMMON_TYPOS.put("werent", "weren't");
    COMMON_TYPOS.put("havent", "haven't");
    COMMON_TYPOS.put("hasnt", "hasn't");
    COMMON_TYPOS.put("hadnt", "hadn't");
    COMMON_TYPOS.put("wouldnt", "wouldn't");
    COMMON_TYPOS.put("couldnt", "couldn't");
    COMMON_TYPOS.put("shouldnt", "shouldn't");
    COMMON_TYPOS.put("im", "I'm");
    COMMON_TYPOS.put("youre", "you're");
    COMMON_TYPOS.put("theyre", "they're");
    COMMON_TYPOS.put("weve", "we've");
    COMMON_TYPOS.put("theyve", "they've");
    COMMON_TYPOS.put("youve", "you've");
    COMMON_TYPOS.put("ive", "I've");
    COMMON_TYPOS.put("youll", "you'll");
    COMMON_TYPOS.put("theyll", "they'll");
    COMMON_TYPOS.put("thats", "that's");
    COMMON_TYPOS.put("whats", "what's");
    COMMON_TYPOS.put("theres", "there's");
    COMMON_TYPOS.put("heres", "here's");
    COMMON_TYPOS.put("wheres", "where's");
    COMMON_TYPOS.put("hows", "how's");
    COMMON_TYPOS.put("lets", "let's");
    COMMON_TYPOS.put("whos", "who's");

    // Additional common typos
    COMMON_TYPOS.put("hte", "the");
    COMMON_TYPOS.put("taht", "that");
    COMMON_TYPOS.put("wiht", "with");
    COMMON_TYPOS.put("frome", "from");
    COMMON_TYPOS.put("jsut", "just");
    COMMON_TYPOS.put("nto", "not");
    COMMON_TYPOS.put("nad", "and");
    COMMON_TYPOS.put("od", "do");
    COMMON_TYPOS.put("fo", "of");
    COMMON_TYPOS.put("ot", "to");
    COMMON_TYPOS.put("si", "is");
    COMMON_TYPOS.put("ti", "it");
    COMMON_TYPOS.put("hc", "ch");
    COMMON_TYPOS.put("od", "do");
    COMMON_TYPOS.put("ol", "lo");
    COMMON_TYPOS.put("oi", "io");
    COMMON_TYPOS.put("nw", "wn");
    COMMON_TYPOS.put("esl", "else");
    COMMON_TYPOS.put("lsae", "please");
    COMMON_TYPOS.put("thn", "then");
    COMMON_TYPOS.put("thna", "than");
    COMMON_TYPOS.put("wiht", "with");
    COMMON_TYPOS.put("whit", "with");
    COMMON_TYPOS.put("jsut", "just");
    COMMON_TYPOS.put("ust", "just");
    COMMON_TYPOS.put("nto", "not");
    COMMON_TYPOS.put("ont", "not");
    COMMON_TYPOS.put("nad", "and");
    COMMON_TYPOS.put("adn", "and");
    COMMON_TYPOS.put("fo", "of");
    COMMON_TYPOS.put("ot", "to");
    COMMON_TYPOS.put("si", "is");
    COMMON_TYPOS.put("ti", "it");
    COMMON_TYPOS.put("hc", "ch");
    COMMON_TYPOS.put("ol", "lo");
    COMMON_TYPOS.put("oi", "io");
    COMMON_TYPOS.put("nw", "wn");
    COMMON_TYPOS.put("esl", "else");
    COMMON_TYPOS.put("lsae", "please");
    COMMON_TYPOS.put("thn", "then");
    COMMON_TYPOS.put("thna", "than");
    COMMON_TYPOS.put("whit", "with");
    COMMON_TYPOS.put("ust", "just");
    COMMON_TYPOS.put("ont", "not");
    COMMON_TYPOS.put("cna", "can");
    COMMON_TYPOS.put("fro", "for");
    COMMON_TYPOS.put("ht", "the");
    COMMON_TYPOS.put("yea", "yes");
    COMMON_TYPOS.put("noe", "note");
    COMMON_TYPOS.put("nt", "not");
    COMMON_TYPOS.put("bc", "because");
    COMMON_TYPOS.put("w/o", "without");
    COMMON_TYPOS.put("b/c", "because");
    COMMON_TYPOS.put("wth", "with");
    COMMON_TYPOS.put("thx", "thanks");
    COMMON_TYPOS.put("ty", "thank you");
    COMMON_TYPOS.put("np", "no problem");
    COMMON_TYPOS.put("imo", "in my opinion");
    COMMON_TYPOS.put("imho", "in my humble opinion");
    COMMON_TYPOS.put("btw", "by the way");
    COMMON_TYPOS.put("tbh", "to be honest");
    COMMON_TYPOS.put("smh", "shaking my head");
    COMMON_TYPOS.put("fwiw", "for what it's worth");
    COMMON_TYPOS.put("iirc", "if I recall correctly");
    COMMON_TYPOS.put("afaik", "as far as I know");
    COMMON_TYPOS.put("irl", "in real life");
    COMMON_TYPOS.put("ftw", "for the win");
    COMMON_TYPOS.put("lol", "laughing out loud");
    COMMON_TYPOS.put("lmao", "laughing my ass off");
    COMMON_TYPOS.put("rofl", "rolling on the floor laughing");
    COMMON_TYPOS.put("brb", "be right back");
    COMMON_TYPOS.put("afk", "away from keyboard");
    COMMON_TYPOS.put("ttyl", "talk to you later");
    COMMON_TYPOS.put("gtg", "got to go");
    COMMON_TYPOS.put("omg", "oh my god");
    COMMON_TYPOS.put("wtf", "what the");
    COMMON_TYPOS.put("stfu", "shut the");
    COMMON_TYPOS.put("idk", "I don't know");
    COMMON_TYPOS.put("ily", "I love you");
    COMMON_TYPOS.put("ily2", "I love you too");
    COMMON_TYPOS.put("ttyl", "talk to you later");
    COMMON_TYPOS.put("cu", "see you");
    COMMON_TYPOS.put("cya", "see you");
    COMMON_TYPOS.put("xoxo", "hugs and kisses");
    COMMON_TYPOS.put("jk", "just kidding");
    COMMON_TYPOS.put("nvm", "never mind");
    COMMON_TYPOS.put("ofc", "of course");
    COMMON_TYPOS.put("rn", "right now");
    COMMON_TYPOS.put("asap", "as soon as possible");
    COMMON_TYPOS.put("fyi", "for your information");
    COMMON_TYPOS.put("psa", "public service announcement");
    COMMON_TYPOS.put("tbt", "throwback Thursday");
    COMMON_TYPOS.put("ootd", "outfit of the day");
    COMMON_TYPOS.put("fomo", "fear of missing out");
    COMMON_TYPOS.put("yolo", "you only live once");
    COMMON_TYPOS.put("bff", "best friends forever");
    COMMON_TYPOS.put("diy", "do it yourself");
    COMMON_TYPOS.put("faq", "frequently asked questions");
    COMMON_TYPOS.put("ai", "artificial intelligence");
    COMMON_TYPOS.put("vr", "virtual reality");
    COMMON_TYPOS.put("ar", "augmented reality");
    COMMON_TYPOS.put("ui", "user interface");
    COMMON_TYPOS.put("ux", "user experience");
    COMMON_TYPOS.put("api", "application programming interface");
    COMMON_TYPOS.put("url", "uniform resource locator");
    COMMON_TYPOS.put("html", "hypertext markup language");
    COMMON_TYPOS.put("css", "cascading style sheets");
    COMMON_TYPOS.put("js", "javascript");
    COMMON_TYPOS.put("py", "python");
    COMMON_TYPOS.put("java", "javascript");
    COMMON_TYPOS.put("c++", "cplusplus");
    COMMON_TYPOS.put("c#", "csharp");
    COMMON_TYPOS.put("php", "php hypertext preprocessor");
    COMMON_TYPOS.put("sql", "structured query language");
    COMMON_TYPOS.put("json", "javascript object notation");
    COMMON_TYPOS.put("xml", "extensible markup language");
    COMMON_TYPOS.put("csv", "comma separated values");
    COMMON_TYPOS.put("pdf", "portable document format");
    COMMON_TYPOS.put("gif", "graphics interchange format");
    COMMON_TYPOS.put("jpg", "joint photographic experts group");
    COMMON_TYPOS.put("png", "portable network graphics");
    COMMON_TYPOS.put("mp3", "mpeg audio layer three");
    COMMON_TYPOS.put("mp4", "mpeg four");
    COMMON_TYPOS.put("avi", "audio video interleave");
    COMMON_TYPOS.put("mov", "quicktime movie");
    COMMON_TYPOS.put("wmv", "windows media video");
    COMMON_TYPOS.put("flv", "flash video");
    COMMON_TYPOS.put("mkv", "matroska video");
    COMMON_TYPOS.put("wav", "waveform audio file");
    COMMON_TYPOS.put("aac", "advanced audio coding");
    COMMON_TYPOS.put("flac", "free lossless audio codec");
    COMMON_TYPOS.put("ogg", "ogg vorbis");
    COMMON_TYPOS.put("wma", "windows media audio");
    COMMON_TYPOS.put("m4a", "mpeg 4 audio");
    COMMON_TYPOS.put("zip", "zip archive");
    COMMON_TYPOS.put("rar", "rar archive");
    COMMON_TYPOS.put("7z", "seven zip");
    COMMON_TYPOS.put("tar", "tape archive");
    COMMON_TYPOS.put("gz", "gzip compressed");
    COMMON_TYPOS.put("bz2", "bzip2 compressed");
    COMMON_TYPOS.put("xz", "xz compressed");
    COMMON_TYPOS.put("iso", "iso image");
    COMMON_TYPOS.put("exe", "executable");
    COMMON_TYPOS.put("dmg", "disk image");
    COMMON_TYPOS.put("apk", "android package");
    COMMON_TYPOS.put("ipa", "ios app store package");
    COMMON_TYPOS.put("deb", "debian package");
    COMMON_TYPOS.put("rpm", "red hat package manager");
    COMMON_TYPOS.put("msi", "microsoft installer");
    COMMON_TYPOS.put("bat", "batch file");
    COMMON_TYPOS.put("sh", "shell script");
    COMMON_TYPOS.put("ps1", "powershell script");
    COMMON_TYPOS.put("cmd", "command prompt");
    COMMON_TYPOS.put("ssh", "secure shell");
    COMMON_TYPOS.put("ftp", "file transfer protocol");
    COMMON_TYPOS.put("sftp", "secure file transfer protocol");
    COMMON_TYPOS.put("http", "hypertext transfer protocol");
    COMMON_TYPOS.put("https", "hypertext transfer protocol secure");
    COMMON_TYPOS.put("smtp", "simple mail transfer protocol");
    COMMON_TYPOS.put("imap", "internet message access protocol");
    COMMON_TYPOS.put("pop3", "post office protocol version three");
    COMMON_TYPOS.put("dns", "domain name system");
    COMMON_TYPOS.put("dhcp", "dynamic host configuration protocol");
    COMMON_TYPOS.put("tcp", "transmission control protocol");
    COMMON_TYPOS.put("udp", "user datagram protocol");
    COMMON_TYPOS.put("ip", "internet protocol");
    COMMON_TYPOS.put("mac", "media access control");
    COMMON_TYPOS.put("lan", "local area network");
    COMMON_TYPOS.put("wan", "wide area network");
    COMMON_TYPOS.put("vpn", "virtual private network");
    COMMON_TYPOS.put("wifi", "wireless fidelity");
    COMMON_TYPOS.put("bluetooth", "bluetooth");
    COMMON_TYPOS.put("nfc", "near field communication");
    COMMON_TYPOS.put("gps", "global positioning system");
    COMMON_TYPOS.put("led", "light emitting diode");
    COMMON_TYPOS.put("lcd", "liquid crystal display");
    COMMON_TYPOS.put("oled", "organic light emitting diode");
    COMMON_TYPOS.put("amoled", "active matrix organic light emitting diode");
    COMMON_TYPOS.put("usb", "universal serial bus");
    COMMON_TYPOS.put("hdmi", "high definition multimedia interface");
    COMMON_TYPOS.put("vga", "video graphics array");
    COMMON_TYPOS.put("dvi", "digital visual interface");
    COMMON_TYPOS.put("displayport", "displayport");
    COMMON_TYPOS.put("thunderbolt", "thunderbolt");
    COMMON_TYPOS.put("pcie", "peripheral component interconnect express");
    COMMON_TYPOS.put("sata", "serial advanced technology attachment");
    COMMON_TYPOS.put("nvme", "non-volatile memory express");
    COMMON_TYPOS.put("ssd", "solid state drive");
    COMMON_TYPOS.put("hdd", "hard disk drive");
    COMMON_TYPOS.put("ram", "random access memory");
    COMMON_TYPOS.put("rom", "read only memory");
    COMMON_TYPOS.put("gpu", "graphics processing unit");
    COMMON_TYPOS.put("cpu", "central processing unit");
    COMMON_TYPOS.put("tpu", "tensor processing unit");
    COMMON_TYPOS.put("ai", "artificial intelligence");
    COMMON_TYPOS.put("ml", "machine learning");
    COMMON_TYPOS.put("dl", "deep learning");
    COMMON_TYPOS.put("nn", "neural network");
    COMMON_TYPOS.put("cnn", "convolutional neural network");
    COMMON_TYPOS.put("rnn", "recurrent neural network");
    COMMON_TYPOS.put("lstm", "long short term memory");
    COMMON_TYPOS.put("gru", "gated recurrent unit");
    COMMON_TYPOS.put("gan", "generative adversarial network");
    COMMON_TYPOS.put("vae", "variational autoencoder");
    COMMON_TYPOS.put("rl", "reinforcement learning");
    COMMON_TYPOS.put("nlp", "natural language processing");
    COMMON_TYPOS.put("cv", "computer vision");
    COMMON_TYPOS.put("iot", "internet of things");
    COMMON_TYPOS.put("ar", "augmented reality");
    COMMON_TYPOS.put("vr", "virtual reality");
    COMMON_TYPOS.put("mr", "mixed reality");
    COMMON_TYPOS.put("xr", "extended reality");
    COMMON_TYPOS.put("5g", "fifth generation");
    COMMON_TYPOS.put("4g", "fourth generation");
    COMMON_TYPOS.put("3g", "third generation");
    COMMON_TYPOS.put("lte", "long term evolution");
    COMMON_TYPOS.put("edge", "enhanced data rates for gsm evolution");
    COMMON_TYPOS.put("gprs", "general packet radio service");
    COMMON_TYPOS.put("umts", "universal mobile telecommunications system");
    COMMON_TYPOS.put("cdma", "code division multiple access");
    COMMON_TYPOS.put("gsm", "global system for mobile communications");
    COMMON_TYPOS.put("fdma", "frequency division multiple access");
    COMMON_TYPOS.put("tdma", "time division multiple access");
    COMMON_TYPOS.put("ofdma", "orthogonal frequency division multiple access");
    COMMON_TYPOS.put("mimo", "multiple input multiple output");
    COMMON_TYPOS.put("beamforming", "beamforming");
    COMMON_TYPOS.put("latency", "latency");
    COMMON_TYPOS.put("throughput", "throughput");
    COMMON_TYPOS.put("bandwidth", "bandwidth");
    COMMON_TYPOS.put("spectrum", "spectrum");
    COMMON_TYPOS.put("modulation", "modulation");
    COMMON_TYPOS.put("demodulation", "demodulation");
    COMMON_TYPOS.put("encoding", "encoding");
    COMMON_TYPOS.put("decoding", "decoding");
    COMMON_TYPOS.put("encryption", "encryption");
    COMMON_TYPOS.put("decryption", "decryption");
    COMMON_TYPOS.put("compression", "compression");
    COMMON_TYPOS.put("decompression", "decompression");
    COMMON_TYPOS.put("multiplexing", "multiplexing");
    COMMON_TYPOS.put("demultiplexing", "demultiplexing");
    COMMON_TYPOS.put("routing", "routing");
    COMMON_TYPOS.put("switching", "switching");
    COMMON_TYPOS.put("bridging", "bridging");
    COMMON_TYPOS.put("tunneling", "tunneling");
    COMMON_TYPOS.put("encapsulation", "encapsulation");
    COMMON_TYPOS.put("decapsulation", "decapsulation");
    COMMON_TYPOS.put("fragmentation", "fragmentation");
    COMMON_TYPOS.put("reassembly", "reassembly");
    COMMON_TYPOS.put("checksum", "checksum");
    COMMON_TYPOS.put("crc", "cyclic redundancy check");
    COMMON_TYPOS.put("hash", "hash");
    COMMON_TYPOS.put("digest", "digest");
    COMMON_TYPOS.put("signature", "signature");
    COMMON_TYPOS.put("certificate", "certificate");
    COMMON_TYPOS.put("handshake", "handshake");
    COMMON_TYPOS.put("negotiation", "negotiation");
    COMMON_TYPOS.put("authentication", "authentication");
    COMMON_TYPOS.put("authorization", "authorization");
    COMMON_TYPOS.put("accounting", "accounting");
    COMMON_TYPOS.put("aaa", "authentication authorization accounting");
    COMMON_TYPOS.put("firewall", "firewall");
    COMMON_TYPOS.put("ids", "intrusion detection system");
    COMMON_TYPOS.put("ips", "intrusion prevention system");
    COMMON_TYPOS.put("siem", "security information and event management");
    COMMON_TYPOS.put("soc", "security operations center");
    COMMON_TYPOS.put("pentest", "penetration testing");
    COMMON_TYPOS.put("vulnerability", "vulnerability");
    COMMON_TYPOS.put("exploit", "exploit");
    COMMON_TYPOS.put("payload", "payload");
    COMMON_TYPOS.put("shellcode", "shellcode");
    COMMON_TYPOS.put("backdoor", "backdoor");
    COMMON_TYPOS.put("trojan", "trojan");
    COMMON_TYPOS.put("ransomware", "ransomware");
    COMMON_TYPOS.put("malware", "malware");
    COMMON_TYPOS.put("spyware", "spyware");
    COMMON_TYPOS.put("adware", "adware");
    COMMON_TYPOS.put("rootkit", "rootkit");
    COMMON_TYPOS.put("keylogger", "keylogger");
    COMMON_TYPOS.put("phishing", "phishing");
    COMMON_TYPOS.put("spearphishing", "spearphishing");
    COMMON_TYPOS.put("whaling", "whaling");
    COMMON_TYPOS.put("vishing", "vishing");
    COMMON_TYPOS.put("smishing", "smishing");
    COMMON_TYPOS.put("social engineering", "social engineering");
    COMMON_TYPOS.put("pretexting", "pretexting");
    COMMON_TYPOS.put("baiting", "baiting");
    COMMON_TYPOS.put("tailgating", "tailgating");
    COMMON_TYPOS.put("piggybacking", "piggybacking");
    COMMON_TYPOS.put("shoulder surfing", "shoulder surfing");
    COMMON_TYPOS.put("dumpster diving", "dumpster diving");
    COMMON_TYPOS.put("watering hole", "watering hole");
    COMMON_TYPOS.put("drive by", "drive by");
    COMMON_TYPOS.put("man in the middle", "man in the middle");
    COMMON_TYPOS.put("mitm", "man in the middle");
    COMMON_TYPOS.put("ddos", "distributed denial of service");
    COMMON_TYPOS.put("dos", "denial of service");
    COMMON_TYPOS.put("sql injection", "sql injection");
    COMMON_TYPOS.put("xss", "cross site scripting");
    COMMON_TYPOS.put("csrf", "cross site request forgery");
    COMMON_TYPOS.put("xxe", "xml external entity");
    COMMON_TYPOS.put("ssrf", "server side request forgery");
    COMMON_TYPOS.put("lfi", "local file inclusion");
    COMMON_TYPOS.put("rfi", "remote file inclusion");
    COMMON_TYPOS.put("rce", "remote code execution");
    COMMON_TYPOS.put("ace", "arbitrary code execution");
    COMMON_TYPOS.put("privilege escalation", "privilege escalation");
    COMMON_TYPOS.put("lpe", "local privilege escalation");
    COMMON_TYPOS.put("uac bypass", "uac bypass");
    COMMON_TYPOS.put("sandbox escape", "sandbox escape");
    COMMON_TYPOS.put("vm escape", "vm escape");
    COMMON_TYPOS.put("container escape", "container escape");
    COMMON_TYPOS.put("zero day", "zero day");
    COMMON_TYPOS.put("0day", "zero day");
    COMMON_TYPOS.put("n day", "n day");
    COMMON_TYPOS.put("patch tuesday", "patch tuesday");
    COMMON_TYPOS.put("patch management", "patch management");
    COMMON_TYPOS.put("vulnerability management", "vulnerability management");
    COMMON_TYPOS.put("asset management", "asset management");
    COMMON_TYPOS.put("configuration management", "configuration management");
    COMMON_TYPOS.put("change management", "change management");
    COMMON_TYPOS.put("incident management", "incident management");
    COMMON_TYPOS.put("problem management", "problem management");
    COMMON_TYPOS.put("service management", "service management");
    COMMON_TYPOS.put("itil", "it infrastructure library");
    COMMON_TYPOS.put("cobit", "control objectives for information and related technologies");
    COMMON_TYPOS.put("iso 27001", "iso 27001");
    COMMON_TYPOS.put("nist", "national institute of standards and technology");
    COMMON_TYPOS.put("cis", "center for internet security");
    COMMON_TYPOS.put("sans", "sysadmin audit network and security");
    COMMON_TYPOS.put("owasp", "open web application security project");
    COMMON_TYPOS.put("ptes", "penetration testing execution standard");
    COMMON_TYPOS.put("osstmm", "open source security testing methodology manual");
    COMMON_TYPOS.put("vsrs", "vulnerability research and scoring system");
  }

  private static void addBigram(String word, String... nextWords)
  {
    COMMON_BIGRAMS.put(word.toLowerCase(), Arrays.asList(nextWords));
  }
}
