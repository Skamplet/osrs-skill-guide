package com.skillguide.model;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;

/**
 * The root object for the whole guide. This is exactly the structure the remote JSON file must follow.
 */
@Data
public class GuideData
{
	/** Version number bumped each time the guide is updated. Used to show "new version". */
	private int version;

	/** Date of the last update, free text, e.g. "2026-06-16". */
	private String updated;

	/** One SkillGuide per skill. */
	private List<SkillGuide> skills = new ArrayList<>();

	/** Recommended quests (Waterfall, The Tourist Trap, etc.). */
	private List<QuestRecommendation> questGuides = new ArrayList<>();

	@Nullable
	public SkillGuide getSkillGuide(String skillName)
	{
		for (SkillGuide guide : skills)
		{
			if (guide.getSkill() != null && guide.getSkill().equalsIgnoreCase(skillName))
			{
				return guide;
			}
		}
		return null;
	}
}
