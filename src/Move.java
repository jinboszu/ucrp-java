/*
 * Copyright (c) 2023 Bo Jin <jinbostar@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.io.PrintStream;

public class Move {
    public final int p; // priority value
    public final int s; // source stack
    public final int d; // destination stack

    /**
     * Create a move
     *
     * @param p priority value
     * @param s source stack
     * @param d destination stack
     */
    public Move(int p, int s, int d) {
        this.p = p;
        this.s = s;
        this.d = d;
    }

    /**
     * Print a path
     *
     * @param ps   print stream
     * @param path array of moves
     * @param len  number of moves
     */
    public static void print_moves(PrintStream ps, Move[] path, int len) {
        if (len == Integer.MAX_VALUE) {
            ps.print("?\n");
        } else {
            ps.print("[");
            for (int i = 0; i < len; i++) {
                ps.printf("%s(%d: %d -> %d)", i == 0 ? "" : ", ", path[i].p, path[i].s, path[i].d);
            }
            ps.print("]\n");
        }
    }
}
