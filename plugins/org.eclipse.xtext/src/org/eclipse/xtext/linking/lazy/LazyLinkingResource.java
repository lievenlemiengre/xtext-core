/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.linking.lazy;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EClassImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.diagnostics.DiagnosticMessage;
import org.eclipse.xtext.diagnostics.ExceptionDiagnostic;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider;
import org.eclipse.xtext.linking.ILinkingDiagnosticMessageProvider.ILinkingDiagnosticContext;
import org.eclipse.xtext.linking.ILinkingService;
import org.eclipse.xtext.linking.impl.LinkingHelper;
import org.eclipse.xtext.linking.impl.XtextLinkingDiagnostic;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.Triple;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class LazyLinkingResource extends XtextResource {

	private final Logger log = Logger.getLogger(getClass());

	@Inject
	private ILinkingService linkingService;

	@Inject
	private LazyURIEncoder encoder;

	@Inject
	private ILinkingDiagnosticMessageProvider diagnosticMessageProvider;

	@Inject
	private LinkingHelper linkingHelper;

	private boolean eagerLinking = false;

	@Override
	protected void doLoad(InputStream inputStream, Map<?, ?> options) throws IOException {
		super.doLoad(inputStream, options);
		if (options != null && Boolean.TRUE.equals(options.get(OPTION_RESOLVE_ALL)))
			EcoreUtil.resolveAll(this);
	}

	@Override
	protected void doLinking() {
		super.doLinking();
		if (isEagerLinking())
			EcoreUtil.resolveAll(this);
	}

	private LinkedHashSet<Triple<EObject, EReference, INode>> resolving = Sets.newLinkedHashSet();

	/**
	 * resolves any lazy cross references in this resource, adding Issues for unresolvable elements to this resource.
	 * This resource might still contain resolvable proxies after this method has been called.
	 * 
	 * @param a {@link CancelIndicator} can be used to stop the resolution.
	 */
	public void resolveLazyCrossReferences(final CancelIndicator mon) {
		final CancelIndicator monitor = mon == null ? CancelIndicator.NullImpl : mon;
		TreeIterator<Object> iterator = EcoreUtil.getAllContents(this, true);
		while (iterator.hasNext()) {
			if (monitor.isCanceled())
				return;
			InternalEObject source = (InternalEObject) iterator.next();
			EStructuralFeature[] eStructuralFeatures = ((EClassImpl.FeatureSubsetSupplier) source.eClass()
					.getEAllStructuralFeatures()).crossReferences();
			if (eStructuralFeatures != null) {
				for (EStructuralFeature crossRef : eStructuralFeatures) {
					if (monitor.isCanceled())
						return;
					resolveLazyCrossReference(source, crossRef);
				}
			}
		}
	}

	protected void resolveLazyCrossReference(InternalEObject source, EStructuralFeature crossRef) {
		if (crossRef.isDerived())
			return;
		if (crossRef.isMany()) {
			@SuppressWarnings("unchecked")
			InternalEList<EObject> list = (InternalEList<EObject>) source.eGet(crossRef);
			for (int i = 0; i < list.size(); i++) {
				EObject proxy = list.basicGet(i);
				if (proxy.eIsProxy()) {
					URI proxyURI = ((InternalEObject) proxy).eProxyURI();
					final String fragment = proxyURI.fragment();
					if (getEncoder().isCrossLinkFragment(this, fragment)) {
						EObject target = getEObject(fragment);
						if (target != null) {
							try {
								source.eSetDeliver(false);
								list.setUnique(i, target);
							} finally {
								source.eSetDeliver(true);
							}
						}
					}
				}
			}
		} else {
			EObject proxy = (EObject) source.eGet(crossRef, false);
			if (proxy != null && proxy.eIsProxy()) {
				URI proxyURI = ((InternalEObject) proxy).eProxyURI();
				final String fragment = proxyURI.fragment();
				if (getEncoder().isCrossLinkFragment(this, fragment)) {
					EObject target = getEObject(fragment);
					if (target != null) {
						try {
							source.eSetDeliver(false);
							source.eSet(crossRef, target);
						} finally {
							source.eSetDeliver(true);
						}
					}
				}
			}
		}
	}

	@Override
	public synchronized EObject getEObject(String uriFragment) {
		try {
			if (getEncoder().isCrossLinkFragment(this, uriFragment)) {
				Triple<EObject, EReference, INode> triple = getEncoder().decode(this, uriFragment);
				try {
					if (!resolving.add(triple))
						return handleCyclicResolution(triple);
					Set<String> unresolveableProxies = getCache().get("UNRESOLVEABLE_PROXIES", this,
							new Provider<Set<String>>() {
								public Set<String> get() {
									return Sets.newHashSet();
								}
							});
					if (unresolveableProxies.contains(uriFragment))
						return null;
					EReference reference = triple.getSecond();
					List<EObject> linkedObjects = getLinkingService().getLinkedObjects(triple.getFirst(), reference,
							triple.getThird());
					if (linkedObjects.isEmpty()) {
						unresolveableProxies.add(uriFragment);
						createAndAddDiagnostic(triple);
						return null;
					}
					if (linkedObjects.size() > 1)
						throw new IllegalStateException("linkingService returned more than one object for fragment "
								+ uriFragment);
					EObject result = linkedObjects.get(0);
					if (!EcoreUtil2.isAssignableFrom(reference.getEReferenceType(), result.eClass())) {
						log.error("An element of type " + result.getClass().getName()
								+ " is not assignable to the reference " + reference.getEContainingClass().getName()
								+ "." + reference.getName());
						unresolveableProxies.add(uriFragment);
						createAndAddDiagnostic(triple);
						return null;
					}
					// remove previously added error markers, since everything should be fine now
					removeDiagnostic(triple);
					return result;
				} finally {
					resolving.remove(triple);
				}
			}
		} catch (RuntimeException e) {
			getErrors().add(new ExceptionDiagnostic(e));
			log.error("resolution of uriFragment '" + uriFragment + "' failed.", e);
			// wrapped because the javaDoc of this method states that WrappedExceptions are thrown
			// logged because EcoreUtil.resolve will ignore any exceptions.
			throw new WrappedException(e);
		}
		return super.getEObject(uriFragment);
	}

	protected EObject handleCyclicResolution(Triple<EObject, EReference, INode> triple) throws AssertionError {
		throw new AssertionError("Cyclic resolution of lazy links : "
				+ getReferences(triple, resolving));
	}

	protected String getReferences(Triple<EObject, EReference, INode> triple,
			LinkedHashSet<Triple<EObject, EReference, INode>> resolving2) {
		StringBuffer buffer = new StringBuffer();
		boolean found = false;
		for (Triple<EObject, EReference, INode> triple2 : resolving2) {
			found = found || triple2.equals(triple);
			if (found)
				buffer.append(getQualifiedName(triple2.getSecond())).append("->");
		}
		buffer.append(getQualifiedName(triple.getSecond()));
		return buffer.toString();
	}

	private String getQualifiedName(EReference eReference) {
		return eReference.getEContainingClass().getName() + "." + eReference.getName();
	}

	protected static class DiagnosticMessageContext implements
			ILinkingDiagnosticMessageProvider.ILinkingDiagnosticContext {

		private final Triple<EObject, EReference, INode> triple;
		private final LinkingHelper linkingHelper;

		protected DiagnosticMessageContext(Triple<EObject, EReference, INode> triple, LinkingHelper helper) {
			this.triple = triple;
			this.linkingHelper = helper;
		}

		public EObject getContext() {
			return triple.getFirst();
		}

		public EReference getReference() {
			return triple.getSecond();
		}

		public String getLinkText() {
			return linkingHelper.getCrossRefNodeAsString(triple.getThird(), true);
		}

	}

	protected void createAndAddDiagnostic(Triple<EObject, EReference, INode> triple) {
		DiagnosticMessage message = createDiagnosticMessage(triple);
		if (message != null) {
			List<Diagnostic> list = getDiagnosticList(message);
			Diagnostic diagnostic = createDiagnostic(triple, message);
			if (!list.contains(diagnostic))
				list.add(diagnostic);
		}
	}

	protected void removeDiagnostic(Triple<EObject, EReference, INode> triple) {
		DiagnosticMessage message = createDiagnosticMessage(triple);
		List<Diagnostic> list = getDiagnosticList(message);
		if (!list.isEmpty()) {
			Diagnostic diagnostic = createDiagnostic(triple, message);
			list.remove(diagnostic);
		}
	}

	protected Diagnostic createDiagnostic(Triple<EObject, EReference, INode> triple, DiagnosticMessage message) {
		Diagnostic diagnostic = new XtextLinkingDiagnostic(triple.getThird(), message.getMessage(),
				message.getIssueCode(), message.getIssueData());
		return diagnostic;
	}

	protected List<Diagnostic> getDiagnosticList(DiagnosticMessage message) throws AssertionError {
		List<Diagnostic> list = null;
		switch (message.getSeverity()) {
			case ERROR:
				list = getErrors();
				break;
			case WARNING:
				list = getWarnings();
				break;
			default:
				throw new AssertionError("Unexpected severity: " + message.getSeverity());
		}
		return list;
	}

	protected DiagnosticMessage createDiagnosticMessage(Triple<EObject, EReference, INode> triple) {
		ILinkingDiagnosticMessageProvider.ILinkingDiagnosticContext context = createDiagnosticMessageContext(triple);
		DiagnosticMessage message = diagnosticMessageProvider.getUnresolvedProxyMessage(context);
		return message;
	}

	protected ILinkingDiagnosticContext createDiagnosticMessageContext(Triple<EObject, EReference, INode> triple) {
		return new DiagnosticMessageContext(triple, linkingHelper);
	}

	public void setLinkingService(ILinkingService linkingService) {
		this.linkingService = linkingService;
	}

	public ILinkingService getLinkingService() {
		return linkingService;
	}

	public void setEncoder(LazyURIEncoder encoder) {
		this.encoder = encoder;
	}

	public LazyURIEncoder getEncoder() {
		return encoder;
	}

	public void setEagerLinking(boolean eagerLinking) {
		this.eagerLinking = eagerLinking;
	}

	public boolean isEagerLinking() {
		return eagerLinking;
	}

	public ILinkingDiagnosticMessageProvider getDiagnosticMessageProvider() {
		return diagnosticMessageProvider;
	}

	public void setDiagnosticMessageProvider(ILinkingDiagnosticMessageProvider diagnosticMessageProvider) {
		this.diagnosticMessageProvider = diagnosticMessageProvider;
	}

	public LinkingHelper getLinkingHelper() {
		return linkingHelper;
	}

	public void setLinkingHelper(LinkingHelper linkingHelper) {
		this.linkingHelper = linkingHelper;
	}
}
