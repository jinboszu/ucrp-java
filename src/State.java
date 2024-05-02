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

import java.util.Arrays;

public class State {
    public int n_stacks; // number of stacks, indexed from 0 to n_stacks - 1
    public int n_tiers; // number of tiers, indexed from 1 to n_tiers
    public int n_blocks; // number of blocks
    public int n_bad; // number of badly-placed blocks
    public int s_min; // target stack
    public int[] h; // height array
    public int[] last_change_time; // last_change_time[s]: time of last change to stack s
    public Type[] last_change_type; // last_change_type[s]: type of last change to stack s
    public int[] last_move_out_time; // last_move_out_time[s]: time of last relocation moving out of stack s
    public int[] last_move_in_time; // last_move_in_time[s]: time of last relocation moving into stack s
    public int[][] p; // p[s][t]: priority
    public int[][] q; // q[s][t]: quality, i.e., smallest among p[s][1...h[s]]
    public int[][] b; // b[s][t]: badness, i.e., number of consecutive badly-placed blocks
    public int[][] l; // l[s][t]: time when the block is put into slot (s, t)

    /**
     * Create space for a state
     *
     * @param n_stacks number of stacks, indexed from 0 to n_stacks - 1
     * @param n_tiers  number of tiers, indexed from 1 to n_tiers (0 is ground)
     */
    private State(int n_stacks, int n_tiers) {
        this.n_stacks = n_stacks;
        this.n_tiers = n_tiers;
        this.h = new int[n_stacks];
        this.last_change_time = new int[n_stacks];
        this.last_change_type = new Type[n_stacks];
        this.last_move_out_time = new int[n_stacks];
        this.last_move_in_time = new int[n_stacks];
        this.p = new int[n_stacks][n_tiers + 1];
        this.q = new int[n_stacks][n_tiers + 1];
        this.b = new int[n_stacks][n_tiers + 1];
        this.l = new int[n_stacks][n_tiers + 1];
    }

    /**
     * Create a state
     *
     * @param n_stacks           number of stacks, indexed from 0 to n_stacks - 1
     * @param n_tiers            number of tiers, indexed from 1 to n_tiers (0 is ground)
     * @param n_blocks           number of blocks
     * @param n_bad              number of badly-placed blocks
     * @param h                  h[s]: height of stack s
     * @param last_change_time   last_change_time[s]: time of last change to stack s
     * @param last_change_type   last_change_type[s]: type of last change to stack s
     * @param last_move_out_time last_move_out_time[s]: time of last relocation moving out of stack s
     * @param last_move_in_time  last_move_in_time[s]: time of last relocation moving into stack s
     * @param p                  p[s][t]: priority
     * @param q                  q[s][t]: quality, i.e., smallest among p[s][1...h[s]]
     * @param b                  b[s][t]: badness, i.e., number of consecutive badly-placed blocks
     * @param l                  l[s][t]: time when the block is put into slot (s, t)
     */
    private State(int n_stacks, int n_tiers, int n_blocks, int n_bad, int s_min, int[] h, int[] last_change_time, Type[] last_change_type, int[] last_move_out_time, int[] last_move_in_time, int[][] p, int[][] q, int[][] b, int[][] l) {
        this.n_stacks = n_stacks;
        this.n_tiers = n_tiers;
        this.n_blocks = n_blocks;
        this.n_bad = n_bad;
        this.s_min = s_min;
        this.h = h;
        this.last_change_time = last_change_time;
        this.last_change_type = last_change_type;
        this.last_move_out_time = last_move_out_time;
        this.last_move_in_time = last_move_in_time;
        this.p = p;
        this.q = q;
        this.b = b;
        this.l = l;
    }

    /**
     * Copy a state
     *
     * @return a copy of the state
     */
    public State copy() {
        return new State(n_stacks, n_tiers, n_blocks, n_bad, s_min, h.clone(), last_change_time.clone(), last_change_type.clone(), last_move_out_time.clone(), last_move_in_time.clone(), Arrays.stream(p).map(int[]::clone).toArray(int[][]::new), Arrays.stream(q).map(int[]::clone).toArray(int[][]::new), Arrays.stream(b).map(int[]::clone).toArray(int[][]::new), Arrays.stream(l).map(int[]::clone).toArray(int[][]::new));
    }

    /**
     * Initialize a state from an instance
     *
     * @param inst instance
     */
    public static State initialize(Instance inst) {
        State state = new State(inst.n_stacks, inst.n_tiers);
        state.n_blocks = inst.n_blocks;
        state.n_bad = 0;

        for (int s = 0; s < state.n_stacks; s++) {
            state.h[s] = inst.h[s];
            state.update_slot(s, 0, inst.max_prio + 1, 0);
            for (int t = 1; t <= state.h[s]; t++) {
                state.update_slot(s, t, inst.p[s][t], 0);
                state.n_bad += state.b[s][t] > 0 ? 1 : 0;
            }

            state.last_change_time[s] = 0;
            state.last_change_type[s] = Type.NEVER;
            state.last_move_out_time[s] = 0;
            state.last_move_in_time[s] = 0;
        }

        state.reset_target();
        return state;
    }

    /**
     * Check if the target block is retrievable
     *
     * @return true if the target block is retrievable
     */
    public boolean is_retrievable() {
        return n_blocks > 0 && b[s_min][h[s_min]] == 0;
    }

    /**
     * Three-way comparison between two stacks
     *
     * @param s1 a stack
     * @param s2 another stack
     * @return ascending order of qualities with ties broken by less topmost blockage
     */
    private int compare(int s1, int s2) {
        return q[s1][h[s1]] != q[s2][h[s2]] ? q[s1][h[s1]] - q[s2][h[s2]] : b[s1][h[s1]] - b[s2][h[s2]];
    }

    /**
     * Reset the target stack
     */
    private void reset_target() {
        if (n_blocks == 0) {
            s_min = -1;
        } else {
            s_min = 0;
            for (int s = 1; s < n_stacks; s++) {
                if (compare(s, s_min) < 0) {
                    s_min = s;
                }
            }
        }
    }

    /**
     * Update matrix information for a slot
     *
     * @param s stack
     * @param t tier
     * @param p priority
     * @param l time
     */
    private void update_slot(int s, int t, int p, int l) {
        this.p[s][t] = p;
        if (t == 0 || p <= this.q[s][t - 1]) {
            this.q[s][t] = p;
            this.b[s][t] = 0;
        } else {
            this.q[s][t] = this.q[s][t - 1];
            this.b[s][t] = this.b[s][t - 1] + 1;
        }
        this.l[s][t] = l;
    }

    /**
     * Move a block out of a stack
     *
     * @param s source stack
     * @param l time of this relocation
     */
    private void move_out(int s, int l) {
        n_bad -= b[s][h[s]--] > 0 ? 1 : 0;
        last_change_time[s] = l;
        last_change_type[s] = Type.MOVE_OUT;
        last_move_out_time[s] = l;
    }

    /**
     * Move a block into a stack
     *
     * @param d destination stack
     * @param p priority value
     * @param l time of this relocation
     */
    private void move_in(int d, int p, int l) {
        update_slot(d, ++h[d], p, l);
        n_bad += b[d][h[d]] > 0 ? 1 : 0;
        last_change_time[d] = l;
        last_change_type[d] = Type.MOVE_IN;
        last_move_in_time[d] = l;
    }

    /**
     * Relocate the topmost block of a stack to another stack
     *
     * @param s source stack
     * @param d destination stack
     * @param l time of this relocation
     */
    public void relocate(int s, int d, int l) {
        int p = this.p[s][h[s]];
        move_out(s, l);
        move_in(d, p, l);
    }

    /**
     * Retrieve the target block from the top of the target stack
     *
     * @param l time of this retrieval
     */
    public void retrieve(int l) {
        n_blocks--;
        h[s_min]--;
        last_change_time[s_min] = l;
        last_change_type[s_min] = Type.RETRIEVE;
        reset_target();
    }
}
