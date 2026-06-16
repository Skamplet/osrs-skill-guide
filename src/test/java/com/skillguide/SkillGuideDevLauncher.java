package com.skillguide;

import java.io.File;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Development launcher. Starts a RuneLite client with this plugin already loaded,
 * so you can test it live.
 *
 * IMPORTANT – isolation: before any RuneLite code runs, we point 'user.home' at a
 * local .dev-home folder inside the project. RuneLite stores EVERYTHING (config,
 * plugin settings, cache) under user.home/.runelite, so this dev client NEVER touches
 * your real ~/.runelite or your normal plugin settings.
 *
 * Run via:  ./gradlew.bat run     or from your IDE by running this main method.
 */
public class SkillGuideDevLauncher
{
	public static void main(String[] args) throws Exception
	{
		// Must happen FIRST, before RuneLite classes are loaded, or the isolation won't work.
		File devHome = new File(System.getProperty("user.dir"), ".dev-home");
		//noinspection ResultOfMethodCallIgnored
		devHome.mkdirs();
		System.setProperty("user.home", devHome.getAbsolutePath());

		ExternalPluginManager.loadBuiltin(SkillGuidePlugin.class);
		RuneLite.main(args);
	}
}
