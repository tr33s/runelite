/*
 * Copyright (c) 2018, Eadgars Ruse <https://github.com/Eadgars-Ruse>
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
package net.runelite.client.plugins.cluescrolls.clues;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import static net.runelite.client.plugins.cluescrolls.ClueScrollOverlay.TITLED_CONTENT_COLOR;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import static net.runelite.client.plugins.cluescrolls.ClueScrollPlugin.CLUE_SCROLL_IMAGE;
import static net.runelite.client.plugins.cluescrolls.ClueScrollPlugin.SPADE_IMAGE;
import static net.runelite.client.plugins.cluescrolls.ClueScrollWorldOverlay.IMAGE_Z_OFFSET;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdArea;
import net.runelite.client.plugins.cluescrolls.clues.hotcold.HotColdLocation;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

@Getter
@RequiredArgsConstructor
public class HotColdClue extends ClueScroll implements TextClueScroll, NpcClueScroll
{
	private static final HotColdClue CLUE =
		new HotColdClue("Buried beneath the ground, who knows where it's found. Lucky for you, A man called Jorral may have a clue.",
			"Jorral",
			"Speak to Jorral to receive a strange device.");

	// list of potential places to dig
	private List<HotColdLocation> digLocations = new ArrayList<>();
	private final String text;
	private final String npc;
	private final String solution;
	private WorldPoint location;
	private WorldPoint lastWorldPoint;

	@Override
	public void makeOverlayHint(PanelComponent panelComponent, ClueScrollPlugin plugin)
	{
		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Hot/Cold Clue")
			.build());
		panelComponent.setPreferredSize(new Dimension(200, 0));

		// strange device has not been tested yet, show how to get it
		if (lastWorldPoint == null && location == null)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Clue:")
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(getText())
				.leftColor(TITLED_CONTENT_COLOR)
				.build());

			if (getNpc() != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("NPC:")
					.build());
				panelComponent.getChildren().add(LineComponent.builder()
					.left(getNpc())
					.leftColor(TITLED_CONTENT_COLOR)
					.build());
			}

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Solution:")
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left(getSolution())
				.leftColor(TITLED_CONTENT_COLOR)
				.build());
		}
		// strange device has been tested, show possible locations for final dig spot
		else
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Possible areas:")
				.build());
			Map<HotColdArea, Integer> locationCounts = new HashMap<>();

			for (HotColdLocation hotColdLocation : digLocations)
			{
				Rectangle2D r = hotColdLocation.getRect();
				HotColdArea hotColdArea = hotColdLocation.getHotColdArea();

				if (locationCounts.containsKey(hotColdArea))
				{
					locationCounts.put(hotColdArea, locationCounts.get(hotColdArea) + 1);
				}
				else
				{
					locationCounts.put(hotColdArea, 1);
				}
			}

			if (digLocations.size() > 10)
			{
				for (HotColdArea area : locationCounts.keySet())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(area.getName())
						.right(Integer.toString(locationCounts.get(area)))
						.build());
				}
			}
			else
			{
				for (HotColdArea s : locationCounts.keySet())
				{
					panelComponent.getChildren().add(LineComponent.builder()
						.left(s.getName() + ":")
						.build());

					for (HotColdLocation hotColdLocation : digLocations)
					{
						if (hotColdLocation.getHotColdArea() == s)
						{
							Rectangle2D r = hotColdLocation.getRect();
							panelComponent.getChildren().add(LineComponent.builder()
								.left("- " + hotColdLocation.getArea())
								.leftColor(Color.LIGHT_GRAY)
								.build());
						}
					}
				}
			}
		}
	}

	@Override
	public void makeWorldOverlayHint(Graphics2D graphics, ClueScrollPlugin plugin)
	{
		// when final location has been found
		if (this.location != null)
		{
			LocalPoint localLocation = LocalPoint.fromWorld(plugin.getClient(), getLocation());

			if (localLocation != null)
			{
				OverlayUtil.renderTileOverlay(plugin.getClient(), graphics, localLocation, SPADE_IMAGE, Color.ORANGE);
			}

			return;
		}

		// when strange device hasn't been activated yet, show Jorral
		if (lastWorldPoint == null)
		{
			// Mark NPC
			if (plugin.getNpcsToMark() != null)
			{
				for (NPC npc : plugin.getNpcsToMark())
				{
					OverlayUtil.renderActorOverlayImage(graphics, npc, CLUE_SCROLL_IMAGE, Color.ORANGE, IMAGE_Z_OFFSET);
				}
			}
		}

		// once the number of possible dig locations is below 10, highlight their ground areas
		if (digLocations.size() < 10)
		{
			// Mark potential dig locations
			for (HotColdLocation hotColdLocation : digLocations)
			{
				Rectangle2D r = hotColdLocation.getRect();

				for (int i = (int) r.getMinX(); i <= r.getMaxX(); i++)
				{
					for (int j = (int) r.getMinY(); j <= r.getMaxY(); j++)
					{
						LocalPoint localLocation = LocalPoint.fromWorld(plugin.getClient(), new WorldPoint(i, j, 0));

						if (localLocation != null)
						{
							Polygon poly = Perspective.getCanvasTilePoly(plugin.getClient(), localLocation);
							graphics.setColor(new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 50));

							if (poly != null)
							{
								graphics.fillPolygon(poly);
							}
						}
					}
				}
			}
		}
	}

	public static HotColdClue forText(String text)
	{
		if (CLUE.text.equalsIgnoreCase(text))
		{
			return CLUE;
		}

		return null;
	}

	public void updatePossibleArea(WorldPoint currentWp, String temperature, String difference)
	{
		this.location = null;

		if (digLocations.size() == 0)
		{
			digLocations.addAll(Arrays.asList(HotColdLocation.values()));
		}

		int maxSquaresAway = 5000;
		int minSquaresAway = 0;

		switch (temperature)
		{
			// when the strange device reads a temperature, that means that the center of the final dig location
			// is a range of squares away from the player's current location (Chebyshev AKA Chess-board distance)
			case "ice cold":
				maxSquaresAway = 5000;
				minSquaresAway = 500;
				break;
			case "very cold":
				maxSquaresAway = 499;
				minSquaresAway = 200;
				break;
			case "cold":
				maxSquaresAway = 199;
				minSquaresAway = 150;
				break;
			case "warm":
				maxSquaresAway = 149;
				minSquaresAway = 100;
				break;
			case "hot":
				maxSquaresAway = 99;
				minSquaresAway = 70;
				break;
			case "very hot":
				maxSquaresAway = 69;
				minSquaresAway = 30;
				break;
			case "incredibly hot":
				maxSquaresAway = 29;
				minSquaresAway = 5;
				break;
		}

		// rectangle r1 encompasses all of the points that are within the max possible distance from the player
		Point p1 = new Point(currentWp.getX() - maxSquaresAway, currentWp.getY() - maxSquaresAway);
		Rectangle r1 = new Rectangle((int) p1.getX(), (int) p1.getY(), 2 * maxSquaresAway + 1, 2 * maxSquaresAway + 1);
		// rectangle r2 encompasses all of the points that are within the min possible distance from the player
		Point p2 = new Point(currentWp.getX() - minSquaresAway, currentWp.getY() - minSquaresAway);
		Rectangle r2 = new Rectangle((int) p2.getX(), (int) p2.getY(), 2 * minSquaresAway + 1, 2 * minSquaresAway + 1);

		// eliminate from consideration dig spots that lie entirely within the min range or entirely outside of the max range
		digLocations.removeIf(entry -> r2.contains(entry.getRect()) || !r1.intersects(entry.getRect()));

		// if a previous world point has been recorded, we can consider the warmer/colder result from the strange device
		if (lastWorldPoint != null)
		{
			switch (difference)
			{
				case "but colder than":
					// eliminate spots that are absolutely warmer
					digLocations.removeIf(entry -> isFirstPointCloserRect(currentWp, lastWorldPoint, entry.getRect()));
					break;
				case "and warmer than":
					// eliminate spots that are absolutely colder
					digLocations.removeIf(entry -> isFirstPointCloserRect(lastWorldPoint, currentWp, entry.getRect()));
					break;
				case "and the same temperature as":
					// I couldn't figure out a clean implementation for this case
					// not necessary for quickly determining final location
			}
		}

		lastWorldPoint = currentWp;
	}

	private boolean isFirstPointCloserRect(WorldPoint firstWp, WorldPoint secondWp, Rectangle2D r)
	{
		WorldPoint p1 = new WorldPoint((int) r.getMaxX(), (int) r.getMaxY(), 0);

		if (!isFirstPointCloser(firstWp, secondWp, p1))
		{
			return false;
		}

		WorldPoint p2 = new WorldPoint((int) r.getMaxX(), (int) r.getMinY(), 0);

		if (!isFirstPointCloser(firstWp, secondWp, p2))
		{
			return false;
		}

		WorldPoint p3 = new WorldPoint((int) r.getMinX(), (int)r.getMaxY(), 0);

		if (!isFirstPointCloser(firstWp, secondWp, p3))
		{
			return false;
		}

		WorldPoint p4 = new WorldPoint((int) r.getMinX(), (int) r.getMinY(), 0);
		return (isFirstPointCloser(firstWp, secondWp, p4));
	}

	private boolean isFirstPointCloser(WorldPoint firstWp, WorldPoint secondWp, WorldPoint wp)
	{
		int firstDistance = firstWp.distanceTo2D(wp);
		int secondDistance = secondWp.distanceTo2D(wp);
		return (firstDistance < secondDistance);
	}

	public void markFinalSpot(WorldPoint wp)
	{
		this.location = wp;
		resetHotCold();
	}

	public void resetHotCold()
	{
		this.lastWorldPoint = null;
		digLocations.clear();
	}
}