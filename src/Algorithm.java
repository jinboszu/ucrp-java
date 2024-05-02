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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Algorithm {

    private static class Branch implements Comparable<Branch> {
        public int pri;
        public int src;
        public int dst;
        public int q_src;
        public int q_dst;
        public int child_lb;
        public State child_state;

        public Branch(int pri, int src, int dst, int q_src, int q_dst, int child_lb, State child_state) {
            this.pri = pri;
            this.src = src;
            this.dst = dst;
            this.q_src = q_src;
            this.q_dst = q_dst;
            this.child_lb = child_lb;
            this.child_state = child_state;
        }

        @Override
        public int compareTo(Branch o) {
            return child_lb != o.child_lb ? child_lb - o.child_lb : q_dst != o.q_dst ? q_dst - o.q_dst : q_src - o.q_src;
        }
    }

    private int n_stacks;
    private int n_tiers;
    private int max_prio;

    private Move[] path;
    private State[] hist;

    private long n_timer;
    private long timer_cycle;

    private int best_lb;
    private int best_ub;
    private Move[] best_sol;
    private double start_time;
    private double end_time;
    private double time_to_best_lb;
    private double time_to_best_ub;
    private long n_nodes;
    private long n_probe;

    private void debug_info(String status) {
        System.out.printf("[%s] best_lb = %d @ %.3f / best_ub = %d @ %.3f / time = %.3f / nodes = %d / probe = %d\n", status, best_lb, time_to_best_lb - start_time, best_ub, time_to_best_ub - start_time, Time.get_time() - start_time, n_nodes, n_probe);
        System.out.flush();
    }

    private boolean search(int level) {
        n_nodes++;

        /*
         * Check time limit
         */
        if (++n_timer == timer_cycle) {
            n_timer = 0;
            if (Time.get_time() >= end_time) {
                return true;
            }
            debug_info("running");
        }

        /*
         * Current state
         */
        State curr_state = hist[level];

        /*
         * Prepare Rule 3 (TC)
         *
         * min_last_change_left[s] = min{last_change_time[s'] | s' < s && h[s'] < n_tiers}
         */
        int[] min_last_change_left = new int[n_stacks];
        int min_last_change_temp = Integer.MAX_VALUE;
        for (int s = 0; s < n_stacks; s++) {
            min_last_change_left[s] = min_last_change_temp;
            if (curr_state.h[s] < n_tiers) {
                min_last_change_temp = Math.min(min_last_change_temp, curr_state.last_change_time[s]);
            }
        }

        /*
         * Prepare Rule 4 (IB)
         *
         * max_last_move_out_right[s] = max{last_move_out_time[s'] | s' > s}
         */
        int[] max_last_move_out_right = new int[n_stacks];
        int max_last_move_out_temp = 0;
        for (int s = n_stacks - 1; s >= 0; s--) {
            max_last_move_out_right[s] = max_last_move_out_temp;
            max_last_move_out_temp = Math.max(max_last_move_out_temp, curr_state.last_move_out_time[s]);
        }

        /*
         * Prepare Rule 10 (SC)
         *
         * max_group_src_right[s] = max{k | pk == p[s][h[s]] && sk > s && last_change_type[sk] == MOVE_OUT}
         */
        int min_prio = curr_state.q[curr_state.s_min][curr_state.h[curr_state.s_min]];
        int[] max_group_src_right = new int[n_stacks];
        int[] max_group_src_temp_offset = new int[max_prio - min_prio];
        for (int s = n_stacks - 1; s >= 0; s--) {
            max_group_src_right[s] = curr_state.h[s] == 0 ? 0 : max_group_src_temp_offset[curr_state.p[s][curr_state.h[s]] - min_prio - 1];
            if (curr_state.last_change_type[s] == Type.MOVE_OUT) {
                int k = curr_state.last_change_time[s];
                int pk = path[k - 1].p;
                if (pk > min_prio) {
                    max_group_src_temp_offset[pk - min_prio - 1] = Math.max(max_group_src_temp_offset[pk - min_prio - 1], k);
                }
            }
        }

        /*
         * Prepare branching
         */
        ArrayList<Branch> branches = new ArrayList<>();

        /*
         * Enumerate source stack
         */
        for (int sn = 0; sn < n_stacks; sn++) {
            /*
             * Check feasibility
             */
            if (curr_state.h[sn] == 0) {
                continue;
            }

            int pn = curr_state.p[sn][curr_state.h[sn]]; // priority value
            int lv = curr_state.l[sn][curr_state.h[sn]]; // last relocation time

            if (lv > 0) {
                int k = lv; // last time the block is relocated
                int sk = path[k - 1].s;

                /*
                 * Check Rule 1 (TA)
                 */
                if (curr_state.last_change_time[sk] == k && curr_state.last_change_type[sk] == Type.MOVE_OUT) {
                    continue; // TA: merge two relocations and perform later
                }
            }

            /*
             * Check Rule 3 (TC)
             *
             * if exists s' < sn such that h[s'] < n_tiers && last_change_time[s'] < k
             *
             * min_last_change_left[s] = min{last_change_time[s'] | s' < s && h[s'] < n_tiers}
             */
            if (min_last_change_left[sn] < lv) {
                continue; // TC: choose alternative transitive stack
            }

            /*
             * Check Rule 10 (SC)
             *
             * if exists k > last_change_time[sn] such that pk = pn && sk > sn && last_change_type[sk] == MOVE_OUT
             *
             * max_group_src_right[s] = max{k | pk == p[s][h[s]] && sk > s && last_change_type[sk] == MOVE_OUT}
             */
            if (curr_state.last_change_time[sn] < max_group_src_right[sn]) {
                continue; // SC: swap source stacks of two relocations
            }

            /*
             * Prepare Rule 11 (SD)
             *
             * max_group_dst_right[d] = max{k | pk == pn && dk > d && last_change_type[dk] == MOVE_IN}
             */
            int[] max_group_dst_right = new int[n_stacks];
            int max_group_dst_temp = 0;
            for (int d = n_stacks - 1; d >= 0; d--) {
                max_group_dst_right[d] = max_group_dst_temp;
                if (curr_state.last_change_type[d] == Type.MOVE_IN) {
                    int k = curr_state.last_change_time[d];
                    int pk = path[k - 1].p;
                    if (pk == pn) {
                        max_group_dst_temp = Math.max(max_group_dst_temp, k);
                    }
                }
            }

            /*
             * Enumerate destination stack
             */
            boolean first_empty = true;
            for (int dn = 0; dn < n_stacks; dn++) {
                /*
                 * Check feasibility
                 */
                if (dn == sn || curr_state.h[dn] == n_tiers) {
                    continue;
                }

                /*
                 * Check Rule 7 (EA)
                 */
                if (curr_state.h[dn] == 0) {
                    if (first_empty) {
                        first_empty = false;
                    } else {
                        continue; // EA: choose the leftmost empty stack
                    }
                }

                /*
                 * Check Rule 2 (TB)
                 */
                if (curr_state.last_change_time[dn] < lv) {
                    continue; // TB: merge two relocations and perform earlier
                }

                /*
                 * Check Rule 4 (IB)
                 *
                 * if exists s' > s such that last_move_out_time[s'] > max{last_change_time[sn], last_change_time[dn]}
                 *
                 * max_last_move_out_right[s] = max{last_move_out_time[s'] | s' > s}
                 */
                if (Math.max(curr_state.last_change_time[sn], curr_state.last_change_time[dn]) < max_last_move_out_right[sn]) {
                    continue; // IB: perform (pn, sn, dn) before (*, s', *)
                }

                if (curr_state.last_change_type[dn] == Type.MOVE_OUT) {
                    int k = curr_state.last_change_time[dn];
                    int pk = path[k - 1].p;
                    int dk = path[k - 1].d;
                    if (pk == pn) {
                        /*
                         * Check Rule 8 (SA)
                         */
                        if (curr_state.last_change_time[sn] < k) {
                            continue; // SA: merge two relocations and perform earlier
                        }

                        /*
                         * Check Rule 9 (SB)
                         */
                        if (curr_state.last_change_time[dk] == k) {
                            continue; // SB: merge two relocations and perform later
                        }
                    }
                }

                /*
                 * Check Rule 11 (SD)
                 *
                 * if exists k > last_change_time[dn] such that pk = pn && dk > dn && last_change_type[dk] == MOVE_IN
                 *
                 * max_group_dst_right[d] = max{k | pk == pn && dk > d && last_change_type[dk] == MOVE_IN}
                 */
                if (curr_state.last_change_time[dn] < max_group_dst_right[dn]) {
                    continue; // SD: swap destination stacks of two relocations
                }

                /*
                 * Child node
                 */
                State child_state = curr_state.copy();
                child_state.relocate(sn, dn, level + 1);

                /*
                 * Update path when generating branches
                 */
                path[level] = new Move(pn, sn, dn);

                /*
                 * Retrieve
                 */
                boolean dominated = false;
                while (child_state.is_retrievable()) {
                    int s_min = child_state.s_min;
                    int p = child_state.p[s_min][child_state.h[s_min]];
                    int l = child_state.l[s_min][child_state.h[s_min]];

                    if (l > 0) {
                        int k = l;
                        int sk = path[k - 1].s;

                        /*
                         * Check Rule 5 (RA)
                         */
                        if (child_state.last_move_out_time[sk] == k && child_state.last_move_in_time[sk] < k && hist[k - 1].q[sk][hist[k - 1].h[sk]] == p) {
                            dominated = true; // RA: k-th relocation can be left out
                            break; // no need to continue retrievals
                        }

                        for (int d = 0; d < s_min; d++) {
                            /*
                             * Check Rule 6 (RB)
                             */
                            if (hist[k - 1].h[d] < n_tiers && child_state.last_move_out_time[d] < k && child_state.last_move_in_time[d] < k && hist[k - 1].q[d][hist[k - 1].h[d]] >= p) {
                                dominated = true; // RB: choose alternative transitive stack
                                break; // no need to test more
                            }
                        }
                        if (dominated) {
                            break; // no need to continue retrievals
                        }
                    }

                    child_state.retrieve(level + 1);
                }

                if (dominated) {
                    continue; // dominated according to RA or RB
                }

                /*
                 * Goal test
                 */
                if (child_state.n_blocks == 0) {
                    best_ub = level + 1;
                    best_sol = Arrays.copyOf(path, best_ub);
                    time_to_best_ub = Time.get_time();
                    debug_info("goal");
                    return true;
                }

                /*
                 * Child lower bound
                 */
                int child_lb = LowerBound.lb_ts(child_state);

                /*
                 * Lower bounding
                 */
                if (level + 1 + child_lb > best_lb) {
                    continue;
                }

                /*
                 * Probing
                 */
                if (level + 1 + child_lb == best_lb - 1) {
                    n_probe++;

                    int new_len_jzw = UpperBound.jzw(child_state.copy(), path, level + 1, best_ub - 1);
                    if (new_len_jzw != Integer.MAX_VALUE) {
                        best_ub = new_len_jzw;
                        best_sol = Arrays.copyOf(path, best_ub);
                        time_to_best_ub = Time.get_time();
                        debug_info("update");
                        if (best_lb == best_ub) {
                            return true;
                        }
                    }

                    int new_len_sm2 = UpperBound.sm2(child_state.copy(), path, level + 1, best_ub - 1);
                    if (new_len_sm2 != Integer.MAX_VALUE) {
                        best_ub = new_len_sm2;
                        best_sol = Arrays.copyOf(path, best_ub);
                        time_to_best_ub = Time.get_time();
                        debug_info("update");
                        if (best_lb == best_ub) {
                            return true;
                        }
                    }
                }

                /*
                 * Non-dominated branches
                 */
                branches.add(new Branch(pn, sn, dn, curr_state.q[sn][curr_state.h[sn]], curr_state.q[dn][curr_state.h[dn]], child_lb, child_state));
            }
        }

        /*
         * Depth-first search
         */
        if (!branches.isEmpty()) {
            Collections.sort(branches);

            for (Branch b : branches) {
                path[level] = new Move(b.pri, b.src, b.dst);
                hist[level + 1] = b.child_state;

                if (search(level + 1)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Solve an instance by iterative deepening branch-and-bound
     *
     * @param inst       instance to be solved
     * @param time_limit time limit in seconds
     * @return solution report
     */
    public Report solve(Instance inst, int time_limit) {
        /*
         * Parameters
         */
        n_stacks = inst.n_stacks;
        n_tiers = inst.n_tiers;
        max_prio = inst.max_prio;
        start_time = Time.get_time();
        end_time = start_time + time_limit;

        /*
         * Root state
         */
        State root_state = State.initialize(inst);
        while (root_state.is_retrievable()) {
            root_state.retrieve(0);
        }
        if (root_state.n_blocks == 0) {
            return new Report(0, 0, 0, 0, null, 0, 0, 0, 0, 0);
        }

        /*
         * Check if there is a solution
         */
        int init_len_jzw = UpperBound.jzw(root_state.copy(), null, 0, Integer.MAX_VALUE);
        int init_len_sm2 = UpperBound.sm2(root_state.copy(), null, 0, Integer.MAX_VALUE);
        int max_depth = Math.min(init_len_jzw, init_len_sm2);
        if (max_depth == Integer.MAX_VALUE) {
            return null;
        }

        /*
         * Temporary variables for branch-and-bound
         */
        path = new Move[max_depth];
        hist = new State[max_depth + 1];

        /*
         * Root lower bound
         */
        int root_lb = LowerBound.lb_ts(root_state);

        /*
         * Initialize best lower and upper bounds
         */
        best_lb = root_lb;
        time_to_best_lb = start_time;
        best_sol = new Move[max_depth];
        best_ub = init_len_jzw < init_len_sm2 ? UpperBound.jzw(root_state.copy(), best_sol, 0, Integer.MAX_VALUE) : UpperBound.sm2(root_state.copy(), best_sol, 0, Integer.MAX_VALUE);
        time_to_best_ub = start_time;

        /*
         * Initialize history
         */
        hist[0] = root_state;

        /*
         * Iterative deepening search
         */
        n_nodes = 0;
        n_probe = 0;
        n_timer = 0;
        timer_cycle = 100000;

        debug_info("start");
        while (best_lb < best_ub) {
            if (search(0)) {
                break;
            }
            best_lb++;
            time_to_best_lb = Time.get_time();
            debug_info("deepen");
        }
        debug_info("end");

        /*
         * Report
         */
        return new Report(root_lb, max_depth, best_lb, best_ub, Arrays.copyOf(best_sol, best_ub), time_to_best_lb - start_time, time_to_best_ub - start_time, Time.get_time() - start_time, n_nodes, n_probe);
    }
}
