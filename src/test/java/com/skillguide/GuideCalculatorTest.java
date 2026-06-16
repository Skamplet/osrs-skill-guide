package com.skillguide;

import com.skillguide.model.TrainingMethod;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GuideCalculatorTest
{
	@Test
	public void actionsRequired_roundsUp()
	{
		// 100 XP remaining, 35 XP per action -> ceil(100/35) = 3
		assertEquals(3, GuideCalculator.actionsRequired(0, 100, 35));
	}

	@Test
	public void actionsRequired_zeroWhenAlreadyReached()
	{
		assertEquals(0, GuideCalculator.actionsRequired(500, 100, 35));
		assertEquals(0, GuideCalculator.actionsRequired(100, 100, 35));
	}

	@Test
	public void actionsRequired_zeroWhenNoXpPerAction()
	{
		assertEquals(0, GuideCalculator.actionsRequired(0, 100, 0));
	}

	@Test
	public void isRelevant_respectsBounds()
	{
		TrainingMethod m = new TrainingMethod();
		m.setMinLevel(15);
		m.setMaxLevel(30);
		assertFalse(GuideCalculator.isRelevant(m, 14));
		assertTrue(GuideCalculator.isRelevant(m, 15));
		assertTrue(GuideCalculator.isRelevant(m, 30));
		assertFalse(GuideCalculator.isRelevant(m, 31));
	}
}
