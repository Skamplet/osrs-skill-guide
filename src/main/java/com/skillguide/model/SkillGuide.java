package com.skillguide.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * Guide for one skill: a list of training methods ordered by level.
 */
@Data
public class SkillGuide
{
	/** Name of the skill, must match net.runelite.api.Skill, e.g. "MINING". */
	private String skill;

	/** True if this is a members-only skill (e.g. Fletching). Hidden in F2P mode. */
	private boolean members;

	/** The methods for the skill. Expected to be sorted by minLevel. */
	private List<TrainingMethod> methods = new ArrayList<>();
}
