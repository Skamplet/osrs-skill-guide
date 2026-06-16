package com.skillguide;

import com.skillguide.model.TrainingMethod;
import net.runelite.api.Experience;

/**
 * Pure XP-to-quantity calculations. No dependency on Swing or client state,
 * so it is easy to unit-test.
 */
public final class GuideCalculator
{
	private GuideCalculator()
	{
	}

	/**
	 * Number of actions required to go from {@code currentXp} to {@code targetXp}
	 * with a method that gives {@code xpPerAction} per action. Never negative.
	 */
	public static long actionsRequired(int currentXp, int targetXp, double xpPerAction)
	{
		if (xpPerAction <= 0 || targetXp <= currentXp)
		{
			return 0;
		}
		double remaining = targetXp - currentXp;
		return (long) Math.ceil(remaining / xpPerAction);
	}

	/**
	 * Is the method relevant at the given level?
	 */
	public static boolean isRelevant(TrainingMethod method, int level)
	{
		return level >= method.getMinLevel() && level <= method.getMaxLevel();
	}

	/**
	 * XP remaining to reach a level (1-99). Returns 0 if already reached.
	 */
	public static int xpToLevel(int currentXp, int level)
	{
		int target = Experience.getXpForLevel(Math.min(Math.max(level, 1), Experience.MAX_VIRT_LEVEL));
		return Math.max(0, target - currentXp);
	}
}
