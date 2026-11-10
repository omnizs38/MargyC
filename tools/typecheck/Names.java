package cat.clawdio.mods;

/**
 * Заглушка для проверки компиляции кода мода без оригинального APK.
 * Настоящий Names.java генерирует patcher.py: там настоящие имена классов Claude.
 * Этот файл нужен только для tools/typecheck.py и CI.
 */
final class Names {
    static final String DRAWER_ITEM = "com.example.DrawerItem";
    static final String FUNCTION0 = "com.example.Function0";
    static final String FUNCTION2 = "com.example.Function2";
    static final String UNIT = "com.example.Unit";
    static final String FAKE_ENTRY = "id=Fable 6969";
    static final String FAKE_OPTION = "Fable 6969=Sonnet";
    static final String FAKE_AVAILABLE = "Fable 6969=Sonnet";
    static final int[] DARK_PALETTE = {0xFF151515, 0xFFFFFFFF};
    static final int[] LIGHT_PALETTE = {0xFFFFFFFF, 0xFF000000};
    static final String MENU_ITEM = "com.example.MenuItem";
    static final String MENU_ITEM_METHOD = "create";
    static final String PAINTER = "com.example.Painter";
    static final String PAINTER_METHOD = "paint";
    static final String ICON = "com.example.Icon";
    static final String MENU_DEFAULTS = "0";
    static final String SESSION_EVENT = "com.example.SessionEvent";
    static final String SESSION_ASSISTANT = "com.example.SessionAssistant";
    static final String SESSION_USER = "com.example.SessionUser";
    static final String SESSION_TEXT = "com.example.SessionText";
    static final String SESSION_USER_TEXT = "com.example.SessionUserText";
    static final String CONV_TITLE = "com.example.ConvTitle";
    static final String SEMANTICS_DELEGATE = "com.example.SemanticsDelegate";
    static final String SEMANTICS_STALE = "stale";
    static final String SEMANTICS_NODES = "getNodes";
    static final String CLAWD = "com.example.Clawd";
    static final String COMPOSE_VIEW = "com.example.ComposeView";
    static final String CLAWD_METHOD = "draw";
    static final String MODIFIER = "com.example.Modifier";
    static final String CLAWD_ANIM = "com.example.ClawdAnim";
    static final String CLAWD_SHEET = "sheet";
    static final String CLAWD_FRAMES = "com.example.ClawdFrames";
    static final String CLAWD_FRAMES_METHOD = "frame";
}
