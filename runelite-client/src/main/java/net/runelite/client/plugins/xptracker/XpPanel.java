/*
 * Copyright (c) 2017, Cameron <moberg@tuta.io>
 * Copyright (c) 2018, Psikoi <https://github.com/psikoi>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.xptracker;

import com.google.common.collect.ImmutableList;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.DragAndDropReorderPane;
import net.runelite.client.ui.components.PluginErrorPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.QuantityFormatter;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;

class XpPanel extends PluginPanel
{
	private final Map<Skill, XpInfoBox> infoBoxes = new HashMap<>();

	private final JLabel overallExpGained = new JLabel(XpInfoBox.htmlLabel("Gained: ", 0));
	private final JLabel overallExpHour = new JLabel(XpInfoBox.htmlLabel("Per hour: ", 0));

	private final JPanel overallPanel = new JPanel();

	/* This displays the "No exp gained" text */
	private final PluginErrorPanel errorPanel = new PluginErrorPanel();

	private final XpTrackerConfig xpTrackerConfig;
	private final Client client;

	/**
	 * Skills, ordered in the way they should be displayed in the panel.
	 */
	private static final List<Skill> SKILLS = ImmutableList.of(
		Skill.ATTACK, Skill.HITPOINTS, Skill.MINING,
		Skill.STRENGTH, Skill.AGILITY, Skill.SMITHING,
		Skill.DEFENCE, Skill.HERBLORE, Skill.FISHING,
		Skill.RANGED, Skill.THIEVING, Skill.COOKING,
		Skill.PRAYER, Skill.CRAFTING, Skill.FIREMAKING,
		Skill.MAGIC, Skill.FLETCHING, Skill.WOODCUTTING,
		Skill.RUNECRAFT, Skill.SLAYER, Skill.FARMING,
		Skill.CONSTRUCTION, Skill.HUNTER, Skill.OVERALL
	);

	private final Map<Skill, JLabel> skillLabels = new EnumMap<>(Skill.class);

	private final JPanel statsPanel = new JPanel();

	XpPanel(XpTrackerPlugin xpTrackerPlugin, XpTrackerConfig xpTrackerConfig, Client client, SkillIconManager iconManager)
	{
		super();
		this.client = client;
		this.xpTrackerConfig = xpTrackerConfig;

		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		final JPanel layoutPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(layoutPanel, BoxLayout.Y_AXIS);
		layoutPanel.setLayout(boxLayout);
		add(layoutPanel, BorderLayout.NORTH);

		overallPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		overallPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overallPanel.setLayout(new BorderLayout());
		overallPanel.setVisible(false); // this will only become visible when the player gets exp

		// Create open xp tracker menu
		final JMenuItem openXpTracker = new JMenuItem("Open Wise Old Man");
		openXpTracker.addActionListener(e -> LinkBrowser.browse(XpPanel.buildXpTrackerUrl(
			client.getLocalPlayer(), Skill.OVERALL, client.getWorldType().contains(WorldType.LEAGUE))));

		// Create reset all menu
		final JMenuItem reset = new JMenuItem("Reset All");
		reset.addActionListener(e -> xpTrackerPlugin.resetAndInitState());

		// Create reset all per hour menu
		final JMenuItem resetPerHour = new JMenuItem("Reset All/hr");
		resetPerHour.addActionListener(e -> xpTrackerPlugin.resetAllSkillsPerHourState());

		// Create pause all menu
		final JMenuItem pauseAll = new JMenuItem("Pause All");
		pauseAll.addActionListener(e -> xpTrackerPlugin.pauseAllSkills(true));

		// Create unpause all menu
		final JMenuItem unpauseAll = new JMenuItem("Unpause All");
		unpauseAll.addActionListener(e -> xpTrackerPlugin.pauseAllSkills(false));


		// Create popup menu
		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
		popupMenu.add(openXpTracker);
		popupMenu.add(reset);
		popupMenu.add(resetPerHour);
		popupMenu.add(pauseAll);
		popupMenu.add(unpauseAll);
		overallPanel.setComponentPopupMenu(popupMenu);

		final JLabel overallIcon = new JLabel(new ImageIcon(iconManager.getSkillImage(Skill.OVERALL)));

		final JPanel overallInfo = new JPanel();
		overallInfo.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		overallInfo.setLayout(new GridLayout(2, 1));
		overallInfo.setBorder(new EmptyBorder(0, 10, 0, 0));

		overallExpGained.setFont(FontManager.getRunescapeSmallFont());
		overallExpHour.setFont(FontManager.getRunescapeSmallFont());

		overallInfo.add(overallExpGained);
		overallInfo.add(overallExpHour);

		overallPanel.add(overallIcon, BorderLayout.WEST);
		overallPanel.add(overallInfo, BorderLayout.CENTER);

		final JComponent infoBoxPanel = new DragAndDropReorderPane();

		layoutPanel.add(overallPanel);
		layoutPanel.add(infoBoxPanel);

		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL)
			{
				break;
			}
			infoBoxes.put(skill, new XpInfoBox(xpTrackerPlugin, xpTrackerConfig, client, infoBoxPanel, skill, iconManager));
		}

		// Panel that holds skill icons
		statsPanel.setLayout(new GridLayout(8, 3));
		statsPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		statsPanel.setVisible(xpTrackerConfig.showStatsPanel());

		// For each skill on the ingame skill panel, create a Label and add it to the UI
		for (Skill skill : SKILLS)
		{
			JPanel panel = makeSkillPanel(skill);
			statsPanel.add(panel);
		}
		//stats panel is added to parent to force it to fix to the bottom of the panel
		this.getParent().add(statsPanel, BorderLayout.SOUTH);

		errorPanel.setContent("Exp trackers", "You have not gained experience yet.");
		add(errorPanel);
	}

	static String buildXpTrackerUrl(final Actor player, final Skill skill, boolean leagueWorld)
	{
		if (player == null)
		{
			return "";
		}

		final String host = leagueWorld ? "trailblazer.wiseoldman.net" : "wiseoldman.net";

		return new HttpUrl.Builder()
			.scheme("https")
			.host(host)
			.addPathSegment("players")
			.addPathSegment(player.getName())
			.addPathSegment("gained")
			.addPathSegment("skilling")
			.addQueryParameter("metric", skill.getName().toLowerCase())
			.addQueryParameter("period", "week")
			.build()
			.toString();
	}

	void resetAllInfoBoxes()
	{
		infoBoxes.forEach((skill, xpInfoBox) -> xpInfoBox.reset());
	}

	void resetSkill(Skill skill)
	{
		XpInfoBox xpInfoBox = infoBoxes.get(skill);
		if (xpInfoBox != null)
		{
			xpInfoBox.reset();
		}
	}

	void updateSkillExperience(boolean updated, boolean paused, Skill skill, XpSnapshotSingle xpSnapshotSingle)
	{
		final XpInfoBox xpInfoBox = infoBoxes.get(skill);

		if (xpInfoBox != null)
		{
			xpInfoBox.update(updated, paused, xpSnapshotSingle);
		}

		updateSkillPanelTooltip(skill);
	}

	void updateTotal(XpSnapshotSingle xpSnapshotTotal)
	{
		// if player has gained exp and hasn't switched displays yet, hide error panel and show overall info
		if (xpSnapshotTotal.getXpGainedInSession() > 0 && !overallPanel.isVisible())
		{
			overallPanel.setVisible(true);
			remove(errorPanel);
		}
		else if (xpSnapshotTotal.getXpGainedInSession() == 0 && overallPanel.isVisible())
		{
			overallPanel.setVisible(false);
			add(errorPanel);
		}

		SwingUtilities.invokeLater(() -> rebuildAsync(xpSnapshotTotal));
	}

	private void rebuildAsync(XpSnapshotSingle xpSnapshotTotal)
	{
		overallExpGained.setText(XpInfoBox.htmlLabel("Gained: ", xpSnapshotTotal.getXpGainedInSession()));
		overallExpHour.setText(XpInfoBox.htmlLabel("Per hour: ", xpSnapshotTotal.getXpPerHour()));

		updateAllSkillPanelTooltips();
	}

	/**
	 * Builds a JPanel displaying an icon and level/number associated with it
	 * Simplified version of HiscorePanel.makeHiscorePanel
	 */
	private JPanel makeSkillPanel(Skill skill)
	{
		JLabel label = new JLabel();
		label.setToolTipText(skill.getName());
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setText(StringUtils.leftPad("--", 2));

		String directory = "/skill_icons_small/";
		String skillName = (skill.name().toLowerCase());
		String skillIcon = directory + skillName + ".png";
//		log.debug("Loading skill icon from {}", skillIcon);

		label.setIcon(new ImageIcon(ImageUtil.loadImageResource(getClass(), skillIcon)));

		label.setIconTextGap(4);

		JPanel skillPanel = new JPanel();
		skillPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		skillPanel.setBorder(new EmptyBorder(2, 0, 2, 0));
		skillLabels.put(skill, label);
		skillPanel.add(label);

		return skillPanel;
	}

	private void setSkillPanelTooltip(Skill skill)
	{
		String content = "";
		String openingTags = "<html><body style = 'padding: 5px;color:#989898'>";
		String closingTags = "</html><body>";

		if (skill == Skill.OVERALL)
		{
			String totalXpString = QuantityFormatter.formatNumber(client.getOverallExperience());
			content += "<p><span style = 'color:white'>Total XP:</span> " + totalXpString + "</p>";
		}
		else
		{
			int currentXp = client.getSkillExperience(skill);
			int currentLevel = Experience.getLevelForXp(currentXp);

			String currentXpString = QuantityFormatter.formatNumber(currentXp);
			String nextLevelXpString;
			String remainingXpString;
			if (currentLevel + 1 <= Experience.MAX_VIRT_LEVEL)
			{
				int nextLevelXp = Experience.getXpForLevel(currentLevel + 1);
				nextLevelXpString = QuantityFormatter.formatNumber(nextLevelXp);
				remainingXpString = QuantityFormatter.formatNumber(nextLevelXp - currentXp);
			}
			else
			{
				nextLevelXpString = "--";
				remainingXpString = "0";
			}

			content += "<p><span style = 'color:white'>" + skill.getName() + "XP:</span> " + currentXpString + "</p>";
			content += "<p><span style = 'color:white'>Next level at:</span> " + nextLevelXpString + "</p>";
			content += "<p><span style = 'color:white'>Remaining XP:</span> " + remainingXpString + "</p>";
		}

		skillLabels.get(skill).setToolTipText(openingTags + content + closingTags);
	}

	void updateSkillStatsPanel(Skill skill)
	{
		int level = Experience.getLevelForXp(client.getSkillExperience(skill));
		if (!xpTrackerConfig.virtualLevelStatsPanel() && level > 99)
		{
			level = 99;
		}

		skillLabels.get(skill).setText(StringUtils.leftPad(String.valueOf(level), 2));
		skillLabels.get(Skill.OVERALL).setText(StringUtils.leftPad(String.valueOf(client.getTotalLevel()), 2));
	}

	void updateAllStatsPanel()
	{
		if (statsPanel.isVisible() != xpTrackerConfig.showStatsPanel())
		{
			statsPanel.setVisible(xpTrackerConfig.showStatsPanel());
		}

		int level;
		for (Skill skill : skillLabels.keySet())
		{
			if (skill == Skill.OVERALL)
			{
				skillLabels.get(skill).setText(StringUtils.leftPad(String.valueOf(client.getTotalLevel()), 2));
			}
			else
			{
				level = Experience.getLevelForXp(client.getSkillExperience(skill));
				if (!xpTrackerConfig.virtualLevelStatsPanel() && level > 99)
				{
					level = 99;
				}

				skillLabels.get(skill).setText(StringUtils.leftPad(String.valueOf(level), 2));
			}
		}
	}

	void updateSkillPanelTooltip(Skill skill)
	{
		setSkillPanelTooltip(skill);
		setSkillPanelTooltip(Skill.OVERALL);
	}

	void updateAllSkillPanelTooltips()
	{
		for (Skill skill : skillLabels.keySet())
		{
			setSkillPanelTooltip(skill);
		}
	}
}
