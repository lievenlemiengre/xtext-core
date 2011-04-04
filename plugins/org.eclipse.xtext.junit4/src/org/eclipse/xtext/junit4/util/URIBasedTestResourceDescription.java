/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.junit4.util;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IReferenceDescription;
import org.eclipse.xtext.resource.impl.AbstractResourceDescription;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class URIBasedTestResourceDescription extends AbstractResourceDescription {

	private URI uri;
	
	public URIBasedTestResourceDescription(URI uri) {
		this.uri = uri;
	}

	@Override
	protected List<IEObjectDescription> computeExportedObjects() {
		return Collections.emptyList();
	}
	
	public Iterable<QualifiedName> getImportedNames() {
		return Collections.emptyList();
	}

	public Iterable<IReferenceDescription> getReferenceDescriptions() {
		return Collections.emptyList();
	}

	public URI getURI() {
		return uri;
	}
	
}