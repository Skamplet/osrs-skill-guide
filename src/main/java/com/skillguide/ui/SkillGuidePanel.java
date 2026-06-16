package com.skillguide.ui;

import com.skillguide.GameMode;
import com.skillguide.GuideCalculator;
import com.skillguide.GuideDataManager;
import com.skillguide.SkillGuideConfig;
import com.skillguide.model.GuideData;
import com.skillguide.model.QuestRecommendation;
import com.skillguide.model.SkillGuide;
import com.skillguide.model.TrainingMethod;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The side panel. It shows:
 *  - a skill selector
 *  - your current level/XP for the selected skill
 *  - recommended training methods for your level, with how many actions are left
 *    to the next level and to your goal
 *  - recommended quests with their status (read from the client) and a pointer to Quest Helper
 *
 * The layout favours readability: clear sections, generous spacing, larger fonts
 * and a "TRAIN NOW" badge on the method that fits your current level.
 */
public class SkillGuidePanel extends PluginPanel
{
	private static final NumberFormat NUM = NumberFormat.getInstance(Locale.US);

	// A little narrower than the panel so wrapped text never touches the edge.
	private static final int WRAP_WIDTH = PluginPanel.PANEL_WIDTH - 40;

	private static final Color CARD_BG = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color SUBTLE = ColorScheme.LIGHT_GRAY_COLOR;

	private final Client client;
	private final ClientThread clientThread;
	private final SkillGuideConfig config;
	private final GuideDataManager guideDataManager;

	private final JLabel headerLabel = new JLabel();
	private final JLabel modeBadge = new JLabel();
	private final JComboBox<String> skillSelector = new JComboBox<>();
	private final JPanel overviewPanel = new JPanel();
	private final JPanel methodsPanel = new JPanel();
	private final JPanel questsPanel = new JPanel();

	@Inject
	public SkillGuidePanel(Client client, ClientThread clientThread,
		SkillGuideConfig config, GuideDataManager guideDataManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.config = config;
		this.guideDataManager = guideDataManager;
	}

	public void init()
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		add(buildTop(), BorderLayout.NORTH);
		add(buildCenter(), BorderLayout.CENTER);

		onGuideUpdated();
	}

	private JPanel buildTop()
	{
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

		JLabel title = new JLabel("1-99 Skill Guide");
		title.setFont(FontManager.getRunescapeBoldFont().deriveFont(16f));
		title.setForeground(Color.WHITE);
		title.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(title);

		headerLabel.setFont(FontManager.getRunescapeSmallFont());
		headerLabel.setForeground(SUBTLE);
		headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
		top.add(headerLabel);

		modeBadge.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		modeBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
		modeBadge.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
		top.add(modeBadge);

		JLabel pick = new JLabel("Skill");
		pick.setFont(FontManager.getRunescapeSmallFont());
		pick.setForeground(SUBTLE);
		pick.setAlignmentX(Component.LEFT_ALIGNMENT);
		top.add(pick);

		skillSelector.setFocusable(false);
		skillSelector.setAlignmentX(Component.LEFT_ALIGNMENT);
		skillSelector.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		skillSelector.addActionListener(e -> refreshCurrentSkill());
		top.add(skillSelector);

		return top;
	}

	private JPanel buildCenter()
	{
		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

		center.add(sectionHeader("YOUR CHARACTER"));
		overviewPanel.setLayout(new BoxLayout(overviewPanel, BoxLayout.Y_AXIS));
		overviewPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		center.add(overviewPanel);

		center.add(Box.createVerticalStrut(14));

		center.add(sectionHeader("TRAINING METHODS"));
		methodsPanel.setLayout(new BoxLayout(methodsPanel, BoxLayout.Y_AXIS));
		methodsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		center.add(methodsPanel);

		center.add(Box.createVerticalStrut(14));

		center.add(sectionHeader("RECOMMENDED QUESTS"));
		questsPanel.setLayout(new BoxLayout(questsPanel, BoxLayout.Y_AXIS));
		questsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		center.add(questsPanel);

		return center;
	}

	/** Called when a new guide has been fetched/loaded: rebuild the skill list. */
	public void onGuideUpdated()
	{
		GuideData data = guideDataManager.getGuideData();
		if (data == null)
		{
			headerLabel.setText("No guide loaded");
			return;
		}

		headerLabel.setText("Guide v" + data.getVersion() + " · updated " + data.getUpdated());

		boolean members = membersMode();
		updateModeBadge(members);
		String previouslySelected = (String) skillSelector.getSelectedItem();
		skillSelector.removeAllItems();
		for (SkillGuide guide : data.getSkills())
		{
			if (!members && guide.isMembers())
			{
				continue; // hide members-only skills in F2P
			}
			skillSelector.addItem(prettySkill(guide.getSkill()));
		}
		if (previouslySelected != null)
		{
			skillSelector.setSelectedItem(previouslySelected);
		}

		refreshOverview();
		refreshCurrentSkill();
		refreshQuests();
	}

	/**
	 * Refresh the live character data: all skill levels and the completed-quest count.
	 * Called on start-up and whenever your XP changes.
	 */
	public void refreshOverview()
	{
		overviewPanel.removeAll();

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			overviewPanel.add(infoLabel("Log in to see your stats & quests"));
			finish(overviewPanel);
			return;
		}

		// Summary card: total level, combat level, and a quest count we fill in asynchronously.
		JPanel summary = card(ColorScheme.MEDIUM_GRAY_COLOR);

		int totalLevel = 0;
		for (Skill s : Skill.values())
		{
			totalLevel += client.getRealSkillLevel(s);
		}
		JLabel total = new JLabel("Total level: " + NUM.format(totalLevel));
		total.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		total.setForeground(Color.WHITE);
		summary.add(total);

		if (client.getLocalPlayer() != null)
		{
			summary.add(small("Combat level: " + client.getLocalPlayer().getCombatLevel()));
		}

		JLabel questCount = value("Quests: …");
		summary.add(questCount);
		overviewPanel.add(summary);
		overviewPanel.add(Box.createVerticalStrut(8));

		// A compact two-column grid of every skill's current level (read live).
		JPanel grid = new JPanel(new GridLayout(0, 2, 10, 2));
		grid.setBackground(getBackground());
		grid.setAlignmentX(Component.LEFT_ALIGNMENT);
		for (Skill s : Skill.values())
		{
			JLabel l = new JLabel(prettySkill(s.name()) + "  " + client.getRealSkillLevel(s));
			l.setFont(FontManager.getRunescapeSmallFont());
			l.setForeground(SUBTLE);
			grid.add(l);
		}
		overviewPanel.add(grid);

		// Count completed quests across ALL quests on the client thread, then update the label.
		clientThread.invokeLater(() ->
		{
			int done = 0;
			int totalQuests = 0;
			for (Quest q : Quest.values())
			{
				totalQuests++;
				if (q.getState(client) == QuestState.FINISHED)
				{
					done++;
				}
			}
			final int finished = done;
			final int all = totalQuests;
			SwingUtilities.invokeLater(() ->
			{
				questCount.setText("Quests: " + finished + " / " + all + " completed");
				finish(overviewPanel);
			});
		});

		finish(overviewPanel);
	}

	/** Recompute methods + quantities for the selected skill. */
	public void refreshCurrentSkill()
	{
		methodsPanel.removeAll();

		GuideData data = guideDataManager.getGuideData();
		String skillName = rawSkill((String) skillSelector.getSelectedItem());
		if (data == null || skillName == null)
		{
			finish(methodsPanel);
			return;
		}

		SkillGuide guide = data.getSkillGuide(skillName);
		if (guide == null)
		{
			methodsPanel.add(infoLabel("No data for " + skillName));
			finish(methodsPanel);
			return;
		}

		Skill skill = parseSkill(skillName);
		int currentXp = (client.getGameState() == GameState.LOGGED_IN && skill != null)
			? client.getSkillExperience(skill) : 0;
		int currentLevel = Experience.getLevelForXp(currentXp);
		int targetLevel = Math.min(Math.max(config.targetLevel(), 2), 99);
		int targetXp = Experience.getXpForLevel(targetLevel);

		methodsPanel.add(currentStatusCard(currentLevel, currentXp, targetLevel, targetXp));
		methodsPanel.add(Box.createVerticalStrut(10));

		boolean members = membersMode();
		boolean any = false;
		for (TrainingMethod method : guide.getMethods())
		{
			if (!members && method.isMembers())
			{
				continue; // hide members-only methods in F2P
			}
			if (method.getMaxLevel() < currentLevel)
			{
				continue; // already past this method
			}
			boolean trainNow = GuideCalculator.isRelevant(method, currentLevel);
			methodsPanel.add(methodCard(method, currentXp, targetXp, trainNow));
			methodsPanel.add(Box.createVerticalStrut(8));
			any = true;
		}
		if (!any)
		{
			methodsPanel.add(infoLabel("You've reached your goal for this skill."));
		}

		finish(methodsPanel);
	}

	// ---- cards ----

	private JPanel currentStatusCard(int level, int xp, int targetLevel, int targetXp)
	{
		JPanel card = card(ColorScheme.MEDIUM_GRAY_COLOR);

		JLabel lvl = new JLabel("Level " + level);
		lvl.setFont(FontManager.getRunescapeBoldFont().deriveFont(18f));
		lvl.setForeground(Color.WHITE);
		card.add(lvl);

		card.add(small(NUM.format(xp) + " XP"));

		int xpToTarget = Math.max(0, targetXp - xp);
		if (xpToTarget > 0)
		{
			card.add(value(NUM.format(xpToTarget) + " XP to level " + targetLevel));
		}
		else
		{
			JLabel done = value("Goal reached!");
			done.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
			card.add(done);
		}
		return card;
	}

	private JPanel methodCard(TrainingMethod m, int currentXp, int targetXp, boolean trainNow)
	{
		JPanel card = card(trainNow ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR);

		if (trainNow)
		{
			JLabel badge = new JLabel("▶ TRAIN NOW");
			badge.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
			badge.setForeground(ColorScheme.BRAND_ORANGE);
			card.add(badge);
			card.add(Box.createVerticalStrut(3));
		}

		JLabel name = new JLabel(m.getName());
		name.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		name.setForeground(Color.WHITE);
		card.add(name);

		card.add(small("Lv " + m.getMinLevel() + "–" + m.getMaxLevel()
			+ " · " + trimNum(m.getXpPerAction()) + " XP each"));
		card.add(Box.createVerticalStrut(6));

		int nextLevelXp = nextRelevantLevelXp(m, currentXp);
		long toNext = GuideCalculator.actionsRequired(currentXp, nextLevelXp, m.getXpPerAction());
		long toCap = GuideCalculator.actionsRequired(currentXp,
			Math.min(targetXp, Experience.getXpForLevel(Math.min(m.getMaxLevel(), 99))), m.getXpPerAction());

		if (toNext > 0)
		{
			card.add(value("Next level:  " + NUM.format(toNext) + " " + plural(m, toNext)));
		}
		if (toCap > 0)
		{
			card.add(value("To lv " + m.getMaxLevel() + ":  " + NUM.format(toCap) + " " + plural(m, toCap)));
		}

		if (notEmpty(m.getNotes()))
		{
			card.add(Box.createVerticalStrut(6));
			card.add(wrapped(m.getNotes(), SUBTLE));
		}
		return card;
	}

	/** Re-read and display the status of the recommended quests. */
	public void refreshQuests()
	{
		questsPanel.removeAll();
		GuideData data = guideDataManager.getGuideData();
		if (data == null || data.getQuestGuides().isEmpty())
		{
			questsPanel.add(infoLabel("No quest recommendations"));
			finish(questsPanel);
			return;
		}

		boolean members = membersMode();
		Map<String, JLabel> statusLabels = new HashMap<>();
		boolean any = false;
		for (QuestRecommendation q : data.getQuestGuides())
		{
			if (!members && q.isMembers())
			{
				continue; // hide members-only quests in F2P
			}
			questsPanel.add(questCard(q, statusLabels));
			questsPanel.add(Box.createVerticalStrut(8));
			any = true;
		}
		if (!any)
		{
			questsPanel.add(infoLabel("No quest recommendations for this mode"));
			finish(questsPanel);
			return;
		}

		// Read quest status on the client thread, then update labels on the EDT.
		clientThread.invokeLater(() ->
		{
			Map<String, QuestState> states = new HashMap<>();
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				for (QuestRecommendation q : data.getQuestGuides())
				{
					Quest quest = parseQuest(q.getQuest());
					if (quest != null)
					{
						states.put(q.getQuest(), quest.getState(client));
					}
				}
			}
			SwingUtilities.invokeLater(() -> applyQuestStates(states, statusLabels));
		});

		finish(questsPanel);
	}

	private void applyQuestStates(Map<String, QuestState> states, Map<String, JLabel> labels)
	{
		labels.forEach((questKey, label) ->
		{
			QuestState state = states.get(questKey);
			if (state == null)
			{
				label.setText("Log in to see status");
				label.setForeground(SUBTLE);
				return;
			}
			switch (state)
			{
				case FINISHED:
					label.setText("✔ Completed");
					label.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
					break;
				case IN_PROGRESS:
					label.setText("● In progress — use Quest Helper");
					label.setForeground(ColorScheme.BRAND_ORANGE);
					break;
				default:
					label.setText("○ Not started — use Quest Helper");
					label.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
			}
		});
	}

	private JPanel questCard(QuestRecommendation q, Map<String, JLabel> statusLabels)
	{
		JPanel card = card(ColorScheme.MEDIUM_GRAY_COLOR);

		JLabel name = new JLabel(q.getName());
		name.setFont(FontManager.getRunescapeBoldFont().deriveFont(15f));
		name.setForeground(Color.WHITE);
		card.add(name);

		JLabel status = new JLabel("Status: …");
		status.setFont(FontManager.getRunescapeSmallFont());
		card.add(status);
		statusLabels.put(q.getQuest(), status);

		if (notEmpty(q.getReason()))
		{
			card.add(Box.createVerticalStrut(5));
			card.add(wrapped(q.getReason(), SUBTLE));
		}
		return card;
	}

	// ---- small UI helpers ----

	private JPanel card(Color accent)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setBackground(CARD_BG);
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, accent),
			BorderFactory.createEmptyBorder(8, 10, 8, 10)));
		// Make every child left-aligned and full width.
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		return card;
	}

	private JPanel sectionHeader(String text)
	{
		JPanel wrap = new JPanel(new GridBagLayout());
		wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrap.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.fill = GridBagConstraints.NONE;

		JLabel label = new JLabel(text);
		label.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		label.setForeground(ColorScheme.BRAND_ORANGE);
		wrap.add(label, c);

		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new java.awt.Insets(0, 8, 0, 0);
		JPanel rule = new JPanel();
		rule.setBackground(ColorScheme.MEDIUM_GRAY_COLOR);
		rule.setPreferredSize(new Dimension(10, 1));
		wrap.add(rule, c);

		return wrap;
	}

	private JLabel value(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeFont());
		l.setForeground(Color.WHITE);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private JLabel small(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(SUBTLE);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private JLabel infoLabel(String text)
	{
		JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeFont());
		l.setForeground(SUBTLE);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		l.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
		return l;
	}

	private JLabel wrapped(String text, Color color)
	{
		JLabel l = new JLabel("<html><div style='width:" + WRAP_WIDTH + "px'>" + escape(text) + "</div></html>");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	// ---- logic helpers ----

	/** Updates the little F2P/Members badge under the header. */
	private void updateModeBadge(boolean members)
	{
		String suffix = config.gameMode() == GameMode.AUTO ? " (auto)" : "";
		if (members)
		{
			modeBadge.setText("● Members" + suffix);
			modeBadge.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		}
		else
		{
			modeBadge.setText("● Free-to-play" + suffix);
			modeBadge.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
		}
	}

	/**
	 * Whether members content should be shown. P2P = always, F2P = never,
	 * AUTO = members only if you're logged into a members world.
	 */
	private boolean membersMode()
	{
		switch (config.gameMode())
		{
			case P2P:
				return true;
			case F2P:
				return false;
			default: // AUTO
				return client.getGameState() == GameState.LOGGED_IN
					&& client.getWorldType() != null
					&& client.getWorldType().contains(WorldType.MEMBERS);
		}
	}

	private int nextRelevantLevelXp(TrainingMethod m, int currentXp)
	{
		int currentLevel = Experience.getLevelForXp(currentXp);
		int nextLevel = Math.min(currentLevel + 1, 99);
		nextLevel = Math.min(nextLevel, m.getMaxLevel());
		return Experience.getXpForLevel(Math.max(nextLevel, 2));
	}

	private static String actionUnit(TrainingMethod m)
	{
		return notEmpty(m.getActionName()) ? m.getActionName() : "action";
	}

	private static String plural(TrainingMethod m, long count)
	{
		String unit = actionUnit(m);
		return count == 1 ? unit : unit + "s";
	}

	private static String trimNum(double d)
	{
		return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
	}

	/** Turns "WOODCUTTING" into "Woodcutting" for display. */
	private static String prettySkill(String name)
	{
		if (name == null || name.isEmpty())
		{
			return name;
		}
		return name.charAt(0) + name.substring(1).toLowerCase(Locale.US);
	}

	/** Reverse of {@link #prettySkill}: "Woodcutting" -> "WOODCUTTING". */
	private static String rawSkill(String display)
	{
		return display == null ? null : display.toUpperCase(Locale.US);
	}

	private static Skill parseSkill(String name)
	{
		try
		{
			return Skill.valueOf(name.toUpperCase(Locale.US));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static Quest parseQuest(String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return Quest.valueOf(name.toUpperCase(Locale.US));
		}
		catch (IllegalArgumentException e)
		{
			return null;
		}
	}

	private static boolean notEmpty(String s)
	{
		return s != null && !s.isEmpty();
	}

	private static String escape(String s)
	{
		if (s == null)
		{
			return "";
		}
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void finish(JPanel panel)
	{
		panel.revalidate();
		panel.repaint();
	}
}
