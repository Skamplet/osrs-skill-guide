package com.skillguide.model;

import lombok.Data;

/**
 * A single training method for a skill, valid within a level range.
 * The fields are populated by Gson from the remote JSON guide.
 */
@Data
public class TrainingMethod
{
	/** Display name, e.g. "Mining iron ore (power-mine)". */
	private String name;

	/** The method is recommended from this level and up. */
	private int minLevel;

	/** The method is recommended up to and including this level (99 if it lasts all the way). */
	private int maxLevel;

	/** XP per action (per ore, per log, per fish, etc.). Used for the quantity calculation. */
	private double xpPerAction;

	/** True if this method requires membership. Hidden when the guide is in F2P mode. */
	private boolean members;

	/** The name of the "action" in singular, e.g. "ore", "log", "fish". Shown in the quantity text. */
	private String actionName;

	/** Free-text tips (gear, location, GP/h, etc.). May be null. */
	private String notes;

	/** Link to the OSRS wiki or another guide. May be null. */
	private String wikiUrl;
}
