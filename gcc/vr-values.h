/* Support routines for Value Range Propagation (VRP).
   Copyright (C) 2016-2019 Free Software Foundation, Inc.

This file is part of GCC.

GCC is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3, or (at your option)
any later version.

GCC is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GCC; see the file COPYING3.  If not see
<http://www.gnu.org/licenses/>.  */

#ifndef GCC_VR_VALUES_H
#define GCC_VR_VALUES_H

class vr_values;

// Class to simplify a statement taking into account range info.
class simplify_with_ranges
{
public:
  // Default constructor has no statement context.
  simplify_with_ranges () : gsi (NULL), stmt (NULL) { }
  simplify_with_ranges (gimple_stmt_iterator *_gsi)
    : gsi (_gsi), stmt (gsi_stmt (*_gsi)) { }
  bool simplify ();

private:
  virtual irange get_value_irange (tree) = 0;

  // Default to nothing, as simplifying switches involves cleaning up
  // edges and switches by the caller.  Perhaps come up with a generic
  // solution if needed.
  virtual bool simplify_switch_using_ranges () { return false; }

  bool op_has_boolean_range_p (tree);
  bool simplify_truth_ops_using_ranges ();
  bool simplify_div_or_mod_using_ranges ();
  bool simplify_min_or_max_using_ranges ();
  bool simplify_abs_using_ranges ();
  bool simplify_bit_ops_using_ranges ();
  bool simplify_cond_using_ranges_1 ();
  bool simplify_conversion_using_ranges ();
  bool simplify_float_conversion_using_ranges ();
  bool simplify_internal_call_using_ranges ();
  bool two_valued_val_range_p (tree, tree *, tree *);

protected:
  // Do not use m_ prefix to avoid heavy changes throughout existing code.
  gimple_stmt_iterator *gsi;
  gimple *stmt;
};

class simplify_with_vranges : public simplify_with_ranges
{
public:
  simplify_with_vranges (gimple_stmt_iterator *gsi, vr_values *vr_values)
    : simplify_with_ranges (gsi), m_vr_values (vr_values) { }
  simplify_with_vranges (vr_values *vr_values) : m_vr_values (vr_values) { }
  irange get_value_irange (tree);
  bool simplify_switch_using_ranges ();
private:
    vr_values *m_vr_values;
};

/* The VR_VALUES class holds the current view of range information
   for all the SSA_NAMEs in the IL.

   It can be used to hold context sensitive range information during
   a dominator walk or it may be used to hold range information in the
   standard VRP pass as ranges are propagated through the lattice to a
   steady state.

   This information is independent of the range information that gets
   attached to SSA_NAMEs.  A pass such as VRP may choose to transfer
   the global information it produces into global range information that
   gets attached to an SSA_NAME.  It's unclear how useful that global
   information will be in a world where we can compute context sensitive
   range information fast or perform on-demand queries.  */
class vr_values
{
  friend class simplify_with_vranges;
  friend class vr_values_misc;

 public:
  vr_values (void);
  ~vr_values (void);

  value_range *get_value_range (const_tree);

  void set_vr_value (tree, value_range *);
  void set_defs_to_varying (gimple *);
  bool update_value_range (const_tree, value_range *);
  void adjust_range_with_scev (value_range_base *, struct loop *, gimple *,
			       tree);
  tree vrp_evaluate_conditional (tree_code, tree, tree, gimple *);
  void dump_all_value_ranges (FILE *);

  void extract_range_for_var_from_comparison_expr (tree, enum tree_code,
						   tree, tree, value_range *);
  void extract_range_from_phi_node (gphi *, value_range *);
  void extract_range_basic (value_range *, gimple *);
  void extract_range_from_stmt (gimple *, edge *, tree *, value_range *);

  void vrp_visit_cond_stmt (gcond *, edge *);

  void simplify_cond_using_ranges_2 (gcond *);
  bool simplify_stmt_using_ranges (gimple_stmt_iterator *);

  /* Indicate that propagation through the lattice is complete.  */
  void set_lattice_propagation_complete (void) { values_propagated = true; }

  /* Allocate a new value_range object.  */
  value_range *allocate_value_range (void)
    { return vrp_value_range_pool.allocate (); }

  /* */
  void cleanup_edges_and_switches (void);

 private:
  irange get_value_irange (tree);
  bool vrp_stmt_computes_nonzero (gimple *);
  bool op_with_boolean_value_range_p (tree);
  value_range *get_vr_for_comparison (int, value_range *);
  tree compare_name_with_value (enum tree_code, tree, tree, bool *, bool);
  tree compare_names (enum tree_code, tree, tree, bool *);
  tree vrp_evaluate_conditional_warnv_with_ops (enum tree_code,
						tree, tree, bool,
						bool *, bool *);
  void extract_range_from_assignment (value_range *, gassign *);
  void extract_range_from_assert (value_range *, tree);
  void extract_range_from_ssa_name (value_range *, tree);
  void extract_range_from_binary_expr (value_range *, enum tree_code,
				       tree, tree, tree);
  void extract_range_from_unary_expr (value_range *, enum tree_code,
				      tree, tree);
  void extract_range_from_cond_expr (value_range *, gassign *);
  void extract_range_from_comparison (value_range *, enum tree_code,
				      tree, tree, tree);
  void vrp_visit_assignment_or_call (gimple*, tree *, value_range *);
  void vrp_visit_switch_stmt (gswitch *, edge *);
  tree vrp_evaluate_conditional_warnv_with_ops_using_ranges
    (enum tree_code, tree, tree, bool *);

  /* Allocation pools for value_range objects.  */
  object_allocator<value_range> vrp_value_range_pool;

  /* This probably belongs in the lattice rather than in here.  */
  bool values_propagated;

  /* Allocations for equivalences all come from this obstack.  */
  bitmap_obstack vrp_equiv_obstack;

  /* Value range array.  After propagation, VR_VALUE[I] holds the range
     of values that SSA name N_I may take.  */
  unsigned int num_vr_values;
  value_range **vr_value;

  /* For a PHI node which sets SSA name N_I, VR_COUNTS[I] holds the
     number of executable edges we saw the last time we visited the
     node.  */
  int *vr_phi_edge_counts;

  /* Vectors of edges that need removing and switch statements that
     need updating.  It is expected that a pass using the simplification
     routines will, at the end of the pass, clean up the edges and
     switch statements.  The class dtor will try to detect cases
     that do not follow that expectation.  */
  struct switch_update {
    gswitch *stmt;
    tree vec;
  };

  vec<edge> to_remove_edges;
  vec<switch_update> to_update_switch_stmts;
};

// Miscellaneous range operations that can work with either irange or
// value_range.

class range_misc
{
public:
  void adjust_range_with_loop (irange &, struct loop *, gimple *, tree);

private:
  // We can't have a generic irange/value_range version here, because
  // the vr_values version may return non-constant invariants such as
  // [&foo, &foo].
  virtual tree singleton (tree) = 0;
  virtual irange get_range (tree) = 0;
};

class vr_values_misc : public range_misc
{
public:
  vr_values_misc (vr_values *v) : m_values (v) { }

  virtual tree singleton (tree);

private:
  virtual irange get_range (tree var)
  { return m_values->get_value_irange (var); }

  vr_values *m_values;
};

extern tree get_output_for_vrp (gimple *);
#endif /* GCC_VR_VALUES_H */
