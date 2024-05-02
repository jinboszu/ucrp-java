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

public class LowerBound {

    /**
     * Compute the value of LB-TS
     *
     * @param state the state
     * @return LB-TS
     */
    public static int lb_ts(State state) {
        int n_stacks = state.n_stacks; // number of stacks
        int n_tiers = state.n_tiers; // number of tiers

        int[][] p = state.p; // p[s][t]: priority
        int[][] q = state.q; // q[s][t]: quality, i.e., smallest among p[s][1...h[s]]
        int[][] b = state.b; // b[s][t]: badness, i.e., number of consecutive badly-placed blocks

        int k = 0; // number of blocking layers identified

        int[] h = state.h.clone(); // initialize h
        int lowest = Integer.MAX_VALUE; // lowest = min{h[s] | s=1,...,S}
        for (int s = 0; s < n_stacks; s++) {
            lowest = Math.min(lowest, h[s]);
        }

        while (lowest > 0) {
            int q_min = Integer.MAX_VALUE; // q_min = min{q[s][h[s]] | s=1,...,S}
            int q_max = 0; // q_max = max{q[s][h[s]] | s=1,...,S, h[s] < n_tiers}
            for (int s = 0; s < n_stacks; s++) {
                q_min = Math.min(q_min, q[s][h[s]]);
                if (h[s] < n_tiers) {
                    q_max = Math.max(q_max, q[s][h[s]]);
                }
            }

            boolean satisfied = true;
            for (int s = 0; s < n_stacks; s++) {
                if (p[s][h[s]] == q_min || (b[s][h[s]] > 0 && p[s][h[s]] <= q_max)) {
                    // One of the conditions is violated at stack s
                    lowest = Math.min(lowest, --h[s]);
                    satisfied = false;
                    break;
                }
            }

            if (satisfied) { // a new blocking layer is identified
                k++;
                for (int s = 0; s < n_stacks; s++) {
                    lowest = Math.min(lowest, --h[s]);
                }
            }
        }

        return state.n_bad + k;
    }
}
