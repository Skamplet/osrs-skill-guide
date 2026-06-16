package com.skillguide;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SkillGuideConfig.GROUP)
public interface SkillGuideConfig extends Config
{
	String GROUP = "skillguide";

	@ConfigItem(
		keyName = "guideUrl",
		name = "Guide URL",
		description = "URL to the remote JSON guide. Only change it if you want to use your own source."
	)
	default String guideUrl()
	{
		// Replace with your own GitHub raw URL once you publish your data file.
		return "https://raw.githubusercontent.com/Skamplet/osrs-skill-guide-data/main/guide-data.json";
	}

	@ConfigItem(
		keyName = "autoUpdate",
		name = "Auto-update guide",
		description = "Fetch the latest guide from the URL on start-up."
	)
	default boolean autoUpdate()
	{
		return true;
	}

	@ConfigItem(
		keyName = "gameMode",
		name = "Game mode",
		description = "Free-to-play hides members-only skills, methods and quests. Auto detects it from your current world."
	)
	default GameMode gameMode()
	{
		return GameMode.AUTO;
	}

	@ConfigItem(
		keyName = "targetLevel",
		name = "Target level",
		description = "Default target level for the quantity calculation (usually 99)."
	)
	default int targetLevel()
	{
		return 99;
	}

	@ConfigItem(
		keyName = "showCompletedQuests",
		name = "Show completed quests",
		description = "Also show recommended quests you have already completed."
	)
	default boolean showCompletedQuests()
	{
		return false;
	}
}
