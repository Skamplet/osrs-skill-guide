package com.skillguide.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * A quest recommended as an efficient shortcut to skill XP
 * (e.g. Waterfall Quest for early Attack/Strength, or The Tourist Trap for Fletching/Smithing).
 *
 * The step-by-step walkthrough is left to the existing Quest Helper plugin;
 * here we only store the "why" plus a name that matches Quest Helper so we can point to it.
 */
@Data
public class QuestRecommendation
{
	/** Display name, e.g. "Waterfall Quest". */
	private String name;

	/**
	 * Name of the quest constant in net.runelite.api.Quest, e.g. "WATERFALL_QUEST".
	 * Used to look up your quest status in the client.
	 */
	private String quest;

	/** A short explanation of why the quest is worth doing. */
	private String reason;

	/** True if this is a members-only quest (e.g. The Tourist Trap). Hidden in F2P mode. */
	private boolean members;

	/** Level at which it makes sense to do the quest (used to highlight relevant quests). */
	private int recommendedAtLevel;

	/** Skill XP rewards, e.g. {"ATTACK": 13750, "STRENGTH": 13750}. May be empty. */
	private Map<String, Integer> skillRewards = new HashMap<>();

	/** The name the quest has in the Quest Helper plugin (usually = name). */
	private String questHelperName;

	/** Link to the OSRS wiki. May be null. */
	private String wikiUrl;
}
