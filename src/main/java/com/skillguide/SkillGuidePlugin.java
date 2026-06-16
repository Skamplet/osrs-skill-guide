package com.skillguide;

import com.google.inject.Provides;
import com.skillguide.ui.SkillGuidePanel;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "1-99 Skill Guide",
	description = "Self-updating 1-99 guide with quantity calculations based on your XP and quest status",
	tags = {"skill", "guide", "1-99", "quest", "xp", "training", "leveling"}
)
public class SkillGuidePlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SkillGuideConfig config;

	@Inject
	private GuideDataManager guideDataManager;

	private SkillGuidePanel panel;
	private NavigationButton navButton;

	@Provides
	SkillGuideConfig provideConfig(net.runelite.client.config.ConfigManager configManager)
	{
		return configManager.getConfig(SkillGuideConfig.class);
	}

	@Override
	protected void startUp()
	{
		// 1. Load local data immediately so the panel is never empty.
		guideDataManager.loadLocal();

		panel = injector.getInstance(SkillGuidePanel.class);
		panel.init();

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/com/skillguide/icon.png");
		navButton = NavigationButton.builder()
			.tooltip("1-99 Skill Guide")
			.icon(icon)
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		// 2. Fetch a fresh guide in the background (self-update).
		if (config.autoUpdate())
		{
			refreshGuide();
		}
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		panel = null;
		navButton = null;
	}

	private void refreshGuide()
	{
		guideDataManager.fetchRemote(config.guideUrl(), guideData ->
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.onGuideUpdated();
				}
			}));
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		// When XP changes, recompute the quantities and the live stats overview.
		if (panel != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				panel.refreshOverview();
				panel.refreshCurrentSkill();
			});
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// On login, refresh everything so stats and quest statuses are read fresh.
		if (event.getGameState() == GameState.LOGGED_IN && panel != null)
		{
			SwingUtilities.invokeLater(() ->
			{
				panel.refreshOverview();
				panel.refreshCurrentSkill();
				panel.refreshQuests();
			});
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!SkillGuideConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}

		if ("guideUrl".equals(event.getKey()) || "autoUpdate".equals(event.getKey()))
		{
			if (config.autoUpdate())
			{
				refreshGuide();
			}
		}
		else if (panel != null)
		{
			// targetLevel / gameMode etc.: rebuild the whole panel (skill list, methods, quests).
			SwingUtilities.invokeLater(panel::onGuideUpdated);
		}
	}
}
