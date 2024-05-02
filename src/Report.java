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

public class Report {
    public int init_lb; // initial lower bound
    public int init_ub; // initial upper bound
    public int best_lb; // best lower bound
    public int best_ub; // best upper bound
    public Move[] best_sol; // best solution
    public double time_to_best_lb; // time to the best lower bound
    public double time_to_best_ub; // time to the best upper bound
    public double time_used; // total time used in seconds
    public long n_nodes; // number of nodes explored
    public long n_probe; // number of nodes probed

    /**
     * Create a report
     *
     * @param init_lb         initial lower bound
     * @param init_ub         initial upper bound
     * @param best_lb         best lower bound
     * @param best_ub         best upper bound
     * @param best_sol        best solution
     * @param time_to_best_lb time to the best lower bound
     * @param time_to_best_ub time to the best upper bound
     * @param time_used       total time used in seconds
     * @param n_nodes         number of nodes explored
     * @param n_probe         number of nodes probed
     */
    public Report(int init_lb, int init_ub, int best_lb, int best_ub, Move[] best_sol, double time_to_best_lb, double time_to_best_ub, double time_used, long n_nodes, long n_probe) {
        this.init_lb = init_lb;
        this.init_ub = init_ub;
        this.best_lb = best_lb;
        this.best_ub = best_ub;
        this.best_sol = best_sol;
        this.time_to_best_lb = time_to_best_lb;
        this.time_to_best_ub = time_to_best_ub;
        this.time_used = time_used;
        this.n_nodes = n_nodes;
        this.n_probe = n_probe;
    }
}
