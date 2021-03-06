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
package uk.ac.manchester.cs.owl.lint.test;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;

import org.semanticweb.owlapi.lint.Lint;
import org.semanticweb.owlapi.lint.LintReport;

import uk.ac.manchester.cs.owl.lint.LintManagerFactory;
import uk.ac.manchester.cs.owl.lint.examples.NonLeafNoInfoLintPattern;

/**
 * @author Luigi Iannone
 * 
 *         The University Of Manchester<br>
 *         Bio-Health Informatics Group<br>
 *         Feb 14, 2008
 */
public class NonLeafNoInfoTestCase extends LintTestCase {
	/**
	 */
	@Override
	protected Lint<?> createLint() {
		return LintManagerFactory.getInstance().getLintManager()
				.getLintFactory().createLint(
						Collections.singleton(new NonLeafNoInfoLintPattern()));
	}

	/**
	 * @see uk.ac.manchester.cs.owl.lint.test.LintTestCase#getPhysicalOntologyIRIString()
	 */
	@Override
	protected String getPhysicalOntologyIRIString() {
		URL resource = this.getClass().getResource("lintExamples.owl");
		String toReturn = null;
		if (resource == null) {
			fail("Could not load the ontology");
		} else {
			try {
				toReturn = resource.toURI().toString();
			} catch (URISyntaxException e) {
				fail(e.getMessage());
			}
		}
		return toReturn;
	}

	/**
	 * @see uk.ac.manchester.cs.owl.lint.test.LintTestCase#testDetected()
	 */
	@Override
	public void testDetected() throws Exception {
		LintReport<?> detected = this.lint.detected(this.getAllOntologies());
		assertTrue(
				"Lint does not detect anything and it really shoud not happen",
				!detected.getAffectedOntologies().isEmpty());
		System.out.println(detected);
	}
}
