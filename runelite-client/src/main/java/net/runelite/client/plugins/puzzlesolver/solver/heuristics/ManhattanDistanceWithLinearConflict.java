/*
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * Copyright (c) 2018, Henke <https://github.com/henke96>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.puzzlesolver.solver.heuristics;

import net.runelite.client.plugins.puzzlesolver.solver.PuzzleState;
import static net.runelite.client.plugins.puzzlesolver.solver.PuzzleSolver.DIMENSION;

/**
 * An implementation of the manhattan distance heuristic function.
 *
 * https://heuristicswiki.wikispaces.com/Manhattan+Distance
 */
public class ManhattanDistanceWithLinearConflict implements Heuristic
{
	@Override
	public int computeValue(PuzzleState state)
	{
		int value = 0;

		PuzzleState parent = state.getParent();

		if (parent == null)
		{
			for (int x = 0; x < DIMENSION; x++)
			{
				for (int y = 0; y < DIMENSION; y++)
				{
					int piece = state.getPiece(x, y);

					if (piece == -1)
					{
						continue;
					}

					int goalX = piece % DIMENSION;
					int goalY = piece / DIMENSION;

					value += Math.abs(x - goalX) + Math.abs(y - goalY);
				}
			}
		}
		else
		{
			/*
				If the Manhattan distance for the parent has already been
				calculated, we can take advantage of that and just
				add/subtract from their heuristic value.

				Doing this decreases the execution time of the heuristic by about 25%.
			 */
			value = parent.getPersistentHeuristicValue(this);

			int x = parent.getEmptyPiece() % DIMENSION;
			int y = parent.getEmptyPiece() / DIMENSION;

			int x2 = state.getEmptyPiece() % DIMENSION;
			int y2 = state.getEmptyPiece() / DIMENSION;

			int piece = state.getPiece(x, y);

			if (x2 > x)
			{
				int targetX = piece % DIMENSION;

				// right
				if (targetX > x) value++;
				else value--;
			}
			else if (x2 < x)
			{
				int targetX = piece % DIMENSION;

				// left
				if (targetX < x) value++;
				else value--;
			}
			else if (y2 > y)
			{
				int targetY = piece / DIMENSION;

				// down
				if (targetY > y) value++;
				else value--;
			}
			else
			{
				int targetY = piece / DIMENSION;

				// up
				if (targetY < y) value++;
				else value--;
			}
		}

		int linearConflict = 0;

		// Linear conflict columns.
		for (int x = 0; x < DIMENSION; x++) {
			for (int y1 = 0; y1 < DIMENSION - 1; y1++) {
				int piece1 = state.getPiece(x, y1);
				if (piece1 == -1) { continue; }

				int goal1X = piece1 % DIMENSION;
				int goal1Y = piece1 / DIMENSION;
				if (goal1X != x) { continue; }

				for (int y2 = y1 + 1; y2 < DIMENSION; y2++) {
					int piece2 = state.getPiece(x, y2);
					if (piece2 == -1) { continue; }

					int goal2X = piece2 % DIMENSION;
					int goal2Y = piece2 / DIMENSION;
					if (goal2X != x) { continue; }

					// Share same goal column, y2 comes after y1, but has goal before y1s goal.
					if (goal2Y < goal1Y) {
						linearConflict += 2;
					}
				}
			}
		}

		// Linear conflict rows.
		for (int y = 0; y < DIMENSION; y++) {
			for (int x1 = 0; x1 < DIMENSION - 1; x1++) {
				int piece1 = state.getPiece(x1, y);
				if (piece1 == -1) { continue; }

				int goal1X = piece1 % DIMENSION;
				int goal1Y = piece1 / DIMENSION;
				if (goal1Y != y) { continue; }

				for (int x2 = x1 + 1; x2 < DIMENSION; x2++) {
					int piece2 = state.getPiece(x2, y);
					if (piece2 == -1) { continue; }

					int goal2X = piece2 % DIMENSION;
					int goal2Y = piece2 / DIMENSION;
					if (goal2Y != y) { continue; }

					// Share same goal row, x2 comes after x1, but has goal before x1s goal.
					if (goal2X < goal1X) {
						linearConflict += 2;
					}
				}
			}
		}

		return value | (linearConflict << 16);
	}
}
