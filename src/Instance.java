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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Scanner;

public class Instance {
    public int n_stacks; // number of stacks, indexed from 0 to n_stacks - 1
    public int n_tiers; // number of tiers, indexed from 1 to n_tiers
    public int n_blocks; // number of blocks
    public int max_prio; // maximum priority
    public int[] h; // height array
    public int[][] p; // priority matrix

    /**
     * Create space for an instance
     *
     * @param n_stacks number of stacks, indexed from 0 to n_stacks - 1
     * @param n_tiers  number of tiers, indexed from 1 to n_tiers
     */
    private Instance(int n_stacks, int n_tiers) {
        this.n_stacks = n_stacks;
        this.n_tiers = n_tiers;
        this.h = new int[n_stacks];
        this.p = new int[n_stacks][n_tiers + 1];
    }

    /**
     * Read an instance from file
     *
     * @param input input file name
     * @return created instance
     * @throws FileNotFoundException file is not found
     */
    public static Instance read_instance(String input) throws FileNotFoundException {
        try (Scanner scn = new Scanner(new File(input))) {
            Instance inst = new Instance(scn.nextInt(), scn.nextInt());
            inst.n_blocks = scn.nextInt();

            inst.max_prio = 0;
            for (int s = 0; s < inst.n_stacks; s++) {
                inst.h[s] = scn.nextInt();
                for (int t = 1; t <= inst.h[s]; t++) {
                    inst.p[s][t] = scn.nextInt();
                    inst.max_prio = Math.max(inst.max_prio, inst.p[s][t]);
                }
            }

            return inst;
        }
    }

    /**
     * Print the instance
     *
     * @param ps print stream
     */
    public void print_instance(PrintStream ps) {
        for (int t = n_tiers; t >= 1; t--) {
            for (int s = 0; s < n_stacks; s++) {
                if (h[s] < t) {
                    ps.print("[   ]");
                } else {
                    ps.printf("[%3d]", p[s][t]);
                }
            }
            ps.print("\n");
        }

        for (int s = 0; s < n_stacks; s++) {
            ps.print("-----");
        }
        ps.print("\n");

        for (int s = 0; s < n_stacks; s++) {
            ps.printf(" %3d ", s);
        }
        ps.print("\n");
    }
}
