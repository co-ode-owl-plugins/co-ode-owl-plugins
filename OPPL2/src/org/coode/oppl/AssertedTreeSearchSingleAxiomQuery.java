/**
 * Copyright (C) 2008, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.coode.oppl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.coode.oppl.log.Logging;
import org.coode.oppl.search.OPPLAssertedSingleOWLAxiomSearchTree;
import org.coode.oppl.search.OPPLOWLAxiomSearchNode;
import org.coode.oppl.search.SearchTree;
import org.coode.oppl.utils.VariableExtractor;
import org.coode.oppl.variablemansyntax.ConstraintSystem;
import org.coode.oppl.variablemansyntax.PartialOWLObjectInstantiator;
import org.coode.oppl.variablemansyntax.bindingtree.Assignment;
import org.coode.oppl.variablemansyntax.bindingtree.BindingNode;
import org.semanticweb.owl.model.OWLAxiom;
import org.semanticweb.owl.model.OWLOntology;

/**
 * @author Luigi Iannone
 * 
 */
public class AssertedTreeSearchSingleAxiomQuery extends AbstractAxiomQuery {
	private final Map<OWLAxiom, SearchTree<OPPLOWLAxiomSearchNode>> searchTrees = new HashMap<OWLAxiom, SearchTree<OPPLOWLAxiomSearchNode>>();
	private final ConstraintSystem constraintSystem;
	private final Set<OWLOntology> ontologies = new HashSet<OWLOntology>();
	private final Map<BindingNode, Set<OWLAxiom>> instantiations = new HashMap<BindingNode, Set<OWLAxiom>>();

	public AssertedTreeSearchSingleAxiomQuery(Set<OWLOntology> ontologies,
			ConstraintSystem constraintSystem) {
		if (ontologies == null) {
			throw new NullPointerException(
					"The ontologies collection cannot be null");
		}
		if (constraintSystem == null) {
			throw new NullPointerException(
					"The constraint system cannot be null");
		}
		this.constraintSystem = constraintSystem;
		this.ontologies.addAll(ontologies);
	}

	@Override
	protected void match(OWLAxiom axiom) {
		this.clearInstantions();
		Set<BindingNode> leaves = this.getConstraintSystem().getLeaves();
		if (leaves == null) {
			VariableExtractor variableExtractor = new VariableExtractor(this
					.getConstraintSystem());
			OPPLOWLAxiomSearchNode start = new OPPLOWLAxiomSearchNode(axiom,
					new BindingNode(new HashSet<Assignment>(), axiom
							.accept(variableExtractor)));
			this.doMatch(start);
		} else {
			int i = 0;
			for (BindingNode bindingNode : leaves) {
				i++;
				Logging.getQueryLogger().log(Level.FINER,
						"Matching " + i + " out of " + leaves.size());
				PartialOWLObjectInstantiator partialObjectInstantiator = new PartialOWLObjectInstantiator(
						bindingNode, this.getConstraintSystem());
				OWLAxiom newStartAxiom = (OWLAxiom) axiom
						.accept(partialObjectInstantiator);
				OPPLOWLAxiomSearchNode newStart = new OPPLOWLAxiomSearchNode(
						newStartAxiom, bindingNode);
				this.doMatch(newStart);
			}
		}
		this.getConstraintSystem().setLeaves(
				new HashSet<BindingNode>(this.getInstantiations().keySet()));
	}

	/**
	 * @param axiom
	 * @param start
	 */
	private void doMatch(OPPLOWLAxiomSearchNode start) {
		for (OWLOntology ontology : this.getOntologies()) {
			for (OWLAxiom targetAxiom : ontology.getAxioms()) {
				if (start.getAxiom().getAxiomType().equals(
						targetAxiom.getAxiomType())) {
					List<List<OPPLOWLAxiomSearchNode>> solutions = this
							.matchTargetAxiom(start, targetAxiom);
					this.populateInstantiation(start.getAxiom(), solutions);
				}
			}
		}
	}

	/**
	 * @param start
	 * @param solutions
	 */
	private void populateInstantiation(OWLAxiom axiom,
			List<List<OPPLOWLAxiomSearchNode>> solutions) {
		for (List<OPPLOWLAxiomSearchNode> path : solutions) {
			OPPLOWLAxiomSearchNode searchLeaf = path.get(path.size() - 1);
			BindingNode leaf = searchLeaf.getBinding();
			PartialOWLObjectInstantiator partialOWLObjectInstantiator = new PartialOWLObjectInstantiator(
					leaf, this.getConstraintSystem());
			Set<OWLAxiom> leafInstantiations = this.instantiations.get(leaf);
			if (leafInstantiations == null) {
				leafInstantiations = new HashSet<OWLAxiom>();
			}
			leafInstantiations.add((OWLAxiom) axiom
					.accept(partialOWLObjectInstantiator));
			this.instantiations.put(leaf, leafInstantiations);
		}
	}

	/**
	 * @param axiom
	 * @param targetAxiom
	 * @param start
	 */
	private List<List<OPPLOWLAxiomSearchNode>> matchTargetAxiom(
			OPPLOWLAxiomSearchNode start, OWLAxiom targetAxiom) {
		SearchTree<OPPLOWLAxiomSearchNode> searchTree = this
				.getSearchTree(targetAxiom);
		List<List<OPPLOWLAxiomSearchNode>> solutions = new ArrayList<List<OPPLOWLAxiomSearchNode>>();
		searchTree.exhaustiveSearchTree(start, solutions);
		return solutions;
	}

	/**
	 * @param targetAxiom
	 * @return
	 */
	private SearchTree<OPPLOWLAxiomSearchNode> getSearchTree(
			OWLAxiom targetAxiom) {
		SearchTree<OPPLOWLAxiomSearchNode> toReturn = this.searchTrees
				.get(targetAxiom);
		if (toReturn == null) {
			toReturn = new OPPLAssertedSingleOWLAxiomSearchTree(targetAxiom,
					this.getConstraintSystem());
			this.searchTrees.put(targetAxiom, toReturn);
		}
		return toReturn;
	}

	private void clearInstantions() {
		this.instantiations.clear();
	}

	public Map<BindingNode, Set<OWLAxiom>> getInstantiations() {
		return new HashMap<BindingNode, Set<OWLAxiom>>(this.instantiations);
	}

	/**
	 * @return the constraintSystem
	 */
	public ConstraintSystem getConstraintSystem() {
		return this.constraintSystem;
	}

	public Set<OWLOntology> getOntologies() {
		return new HashSet<OWLOntology>(this.ontologies);
	}
}
