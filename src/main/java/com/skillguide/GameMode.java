package com.skillguide;

/**
 * Whether the guide should show free-to-play or members content.
 */
public enum GameMode
{
	/** Detect from the world you are currently logged into. */
	AUTO("Auto (detect world)"),
	/** Free-to-play: hide members-only skills, methods and quests. */
	F2P("Free-to-play"),
	/** Members: show everything. */
	P2P("Members");

	private final String label;

	GameMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
