/**
 * Copyright (c) 2018 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xtext.generator;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.binder.AnnotatedBindingBuilder;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenPackage;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.XtextRuntimeModule;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.tests.AbstractXtextTests;
import org.eclipse.xtext.util.Modules2;
import org.eclipse.xtext.util.internal.Log;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xtext.ecoreInference.Xtext2EcoreTransformer;
import org.eclipse.xtext.xtext.generator.AbstractXtextGeneratorFragment;
import org.eclipse.xtext.xtext.generator.CodeConfig;
import org.eclipse.xtext.xtext.generator.DefaultGeneratorModule;
import org.eclipse.xtext.xtext.generator.IXtextGeneratorLanguage;
import org.eclipse.xtext.xtext.generator.StandardLanguage;
import org.eclipse.xtext.xtext.generator.XtextGeneratorNaming;
import org.eclipse.xtext.xtext.generator.ecore.EMFGeneratorFragment2;
import org.eclipse.xtext.xtext.generator.model.IXtextGeneratorFileSystemAccess;
import org.eclipse.xtext.xtext.generator.model.XtextGeneratorFileSystemAccess;
import org.eclipse.xtext.xtext.generator.model.project.IXtextProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.RuntimeProjectConfig;
import org.eclipse.xtext.xtext.generator.model.project.StandardProjectConfig;

/**
 * @author Holger Schill - Initial contribution and API
 */
@SuppressWarnings("all")
public abstract class AbstractGeneratorFragmentTests extends AbstractXtextTests {
  public static class FragmentGeneratorModule extends DefaultGeneratorModule {
    private Grammar grammar;
    
    public FragmentGeneratorModule(final Grammar g) {
      this.grammar = g;
    }
    
    public Class<? extends XtextGeneratorNaming> bindNaming() {
      return XtextGeneratorNaming.class;
    }
    
    public void configureGrammar(final Binder binder) {
      binder.<Grammar>bind(Grammar.class).toInstance(this.grammar);
    }
    
    @Override
    public void configureXtextProjectConfig(final Binder binder) {
      final StandardLanguage lang = new StandardLanguage();
      lang.initialize(this.grammar);
      binder.<IXtextGeneratorLanguage>bind(IXtextGeneratorLanguage.class).toInstance(lang);
    }
    
    public void configureIXtextProjectConfig(final Binder binder) {
      AnnotatedBindingBuilder<IXtextProjectConfig> _bind = binder.<IXtextProjectConfig>bind(IXtextProjectConfig.class);
      AbstractGeneratorFragmentTests.FakeProjectConfig _fakeProjectConfig = new AbstractGeneratorFragmentTests.FakeProjectConfig();
      _bind.toInstance(_fakeProjectConfig);
    }
  }
  
  public static class FakeProjectConfig extends StandardProjectConfig {
    @Override
    public RuntimeProjectConfig getRuntime() {
      return new RuntimeProjectConfig() {
        @Override
        public String getName() {
          return "projectName";
        }
        
        @Override
        public String getEcoreModelFolder() {
          return "rootFolder/ecoreFolder";
        }
        
        @Override
        public IXtextGeneratorFileSystemAccess getSrcGen() {
          return new XtextGeneratorFileSystemAccess("rootFolder/src-gen", false);
        }
        
        @Override
        public IXtextGeneratorFileSystemAccess getRoot() {
          return new XtextGeneratorFileSystemAccess("rootFolder", false);
        }
      };
    }
  }
  
  @Log
  public static class FakeEMFGeneratorFragment2 extends EMFGeneratorFragment2 {
    @Override
    protected GenModel getSaveAndReconcileGenModel(final Grammar grammar, final List<EPackage> packs, final ResourceSet rs) {
      final GenModel genModel = this.getGenModel(rs, grammar);
      genModel.initialize(packs);
      EList<GenPackage> _genPackages = genModel.getGenPackages();
      for (final GenPackage genPackage : _genPackages) {
        {
          genPackage.setBasePackage(this.getBasePackage(grammar));
          if (((!this.getLanguage().getFileExtensions().isEmpty()) && packs.contains(genPackage.getEcorePackage()))) {
            genPackage.setFileExtensions(IterableExtensions.join(this.getLanguage().getFileExtensions(), ","));
          }
        }
      }
      final Set<EPackage> referencedEPackages = this.getReferencedEPackages(packs);
      final List<GenPackage> usedGenPackages = this.getGenPackagesForPackages(genModel, referencedEPackages);
      this.reconcileMissingGenPackagesInUsedModels(usedGenPackages);
      genModel.getUsedGenPackages().addAll(usedGenPackages);
      return genModel;
    }
    
    private final static Logger LOG = Logger.getLogger(FakeEMFGeneratorFragment2.class);
  }
  
  @Override
  public void setUp() {
    try {
      super.setUp();
      this.with(XtextStandaloneSetup.class);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public <T extends AbstractXtextGeneratorFragment> T initializeFragmentWithGrammarFromString(final Class<T> fragmentClass, final String grammarString) {
    try {
      final XtextResource resource = this.getResourceFromString(grammarString);
      EObject _head = IterableExtensions.<EObject>head(resource.getContents());
      final Grammar grammar = ((Grammar) _head);
      XtextRuntimeModule _xtextRuntimeModule = new XtextRuntimeModule();
      AbstractGeneratorFragmentTests.FragmentGeneratorModule _fragmentGeneratorModule = new AbstractGeneratorFragmentTests.FragmentGeneratorModule(grammar);
      final Injector generatorInjector = Guice.createInjector(
        Modules2.mixin(_xtextRuntimeModule, _fragmentGeneratorModule));
      final Xtext2EcoreTransformer transformer = new Xtext2EcoreTransformer(grammar);
      transformer.transform();
      final AbstractGeneratorFragmentTests.FakeEMFGeneratorFragment2 emfGeneratorFragment = generatorInjector.<AbstractGeneratorFragmentTests.FakeEMFGeneratorFragment2>getInstance(AbstractGeneratorFragmentTests.FakeEMFGeneratorFragment2.class);
      emfGeneratorFragment.initialize(generatorInjector);
      emfGeneratorFragment.getSaveAndReconcileGenModel(grammar, transformer.getGeneratedPackages(), resource.getResourceSet());
      return generatorInjector.<T>getInstance(fragmentClass);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  public String concatenationClientToString(final StringConcatenationClient client) {
    final CodeConfig config = this.<CodeConfig>get(CodeConfig.class);
    String _lineDelimiter = config.getLineDelimiter();
    final StringConcatenation stringConcat = new StringConcatenation(_lineDelimiter);
    stringConcat.append(client);
    return stringConcat.toString();
  }
  
  @Override
  public XtextResource doGetResource(final InputStream in, final URI uri) throws Exception {
    final XtextResourceSet rs = this.<XtextResourceSet>get(XtextResourceSet.class);
    rs.setClasspathURIContext(this.getClass());
    Resource _createResource = this.getResourceFactory().createResource(uri);
    final XtextResource resource = ((XtextResource) _createResource);
    rs.getResources().add(resource);
    resource.load(in, null);
    return resource;
  }
}
