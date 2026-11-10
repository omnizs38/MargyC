package cat.clawdio.mods.api;

/**
 * Entry point of a Clawdio mod. The class named by "entry" in manifest.json must implement this
 * interface and have a public no-arg constructor.
 *
 * onCreate is called once per app start, from Application.onCreate, on the main thread:
 * move long work to your own thread.
 */
public interface ClawdioPlugin {
    void onCreate(PluginContext ctx) throws Exception;
}
