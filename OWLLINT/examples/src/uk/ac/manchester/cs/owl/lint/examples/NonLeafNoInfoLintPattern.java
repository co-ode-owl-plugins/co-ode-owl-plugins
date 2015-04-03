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
package uk.ac.manchester.cs.owl.lint.examples;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;

import uk.ac.manchester.cs.owl.lint.commons.Match;
import uk.ac.manchester.cs.owl.lint.commons.OntologyWiseLintPattern;

/**
 * Matches with non-leaf subclasses that do not have any new assertions
 * 
 * @author Luigi Iannone
 * 
 *         The University Of Manchester<br>
 *         Bio-Health Informatics Group<br>
 *         Feb 13, 2008
 */
public final class NonLeafNoInfoLintPattern extends
		OntologyWiseLintPattern<OWLClass> {
	@Override
	public Set<Match<OWLClass>> matches(OWLOntology target) {
		Set<Match<OWLClass>> toReturn = new HashSet<Match<OWLClass>>();
		for (OWLClass owlClass : target.getClassesInSignature()) {
			if (this.matches(owlClass, target)) {
				toReturn
						.add(new Match<OWLClass>(
								owlClass,
								target,
								"The named class is not a leaf, yet it has got no relevant information (i.e: no anonymous super-classes nor anonymous equivalent classes )"));
			}
		}
		return toReturn;
	}

	private boolean matches(OWLClass owlClass, OWLOntology target) {
		boolean toReturn = false;
		// If this class is a leaf we just do not bother
        if (!EntitySearcher.getSubClasses(owlClass, target).isEmpty()) {
            Collection<OWLClassExpression> superClasses = EntitySearcher
                    .getSuperClasses(owlClass, target);
			Iterator<OWLClassExpression> it = superClasses.iterator();
			boolean found = false;
			OWLClassExpression anOWLDescription;
			while (!found && it.hasNext()) {
				anOWLDescription = it.next();
				found = anOWLDescription.isAnonymous();
			}
			if (found) {
				toReturn = false;
			} else {
                it = EntitySearcher.getEquivalentClasses(owlClass, target)
                        .iterator();
				while (!found && it.hasNext()) {
					anOWLDescription = it.next();
					found = anOWLDescription.isAnonymous();
				}
				if (found) {
					toReturn = false;
				} else {
                    it = EntitySearcher.getDisjointClasses(owlClass, target)
                            .iterator();
					while (!found && it.hasNext()) {
						anOWLDescription = it.next();
						found = anOWLDescription.isAnonymous();
					}
					toReturn = !found;
				}
			}
		}
		return toReturn;
	}

	@Override
    public boolean isInferenceRequired() {
		return false;
	}
}
