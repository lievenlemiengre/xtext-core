/**
 * generated by Xtext
 */
package org.eclipse.xtext.testlanguages.backtracking.formatting2;

import com.google.inject.Inject;
import java.util.Arrays;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.formatting2.AbstractFormatter2;
import org.eclipse.xtext.formatting2.IFormattableDocument;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.AliasedRequiredCapability;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.Function;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.Model;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.ProvidedCapability;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.RequiredCapability;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.SimpleTypeRef;
import org.eclipse.xtext.testlanguages.backtracking.beeLangTestLanguage.Unit;
import org.eclipse.xtext.testlanguages.backtracking.services.BeeLangTestLanguageGrammarAccess;
import org.eclipse.xtext.xbase.lib.Extension;

@SuppressWarnings("all")
public class BeeLangTestLanguageFormatter extends AbstractFormatter2 {
  @Inject
  @Extension
  private BeeLangTestLanguageGrammarAccess _beeLangTestLanguageGrammarAccess;
  
  protected void _format(final Model model, @Extension final IFormattableDocument document) {
    EList<Unit> _units = model.getUnits();
    for (final Unit unit : _units) {
      document.<Unit>format(unit);
    }
    EList<Function> _functions = model.getFunctions();
    for (final Function function : _functions) {
      document.<Function>format(function);
    }
  }
  
  protected void _format(final Unit unit, @Extension final IFormattableDocument document) {
    EList<SimpleTypeRef> _implements = unit.getImplements();
    for (final SimpleTypeRef simpleTypeRef : _implements) {
      document.<SimpleTypeRef>format(simpleTypeRef);
    }
    EList<ProvidedCapability> _providedCapabilities = unit.getProvidedCapabilities();
    for (final ProvidedCapability providedCapability : _providedCapabilities) {
      document.<ProvidedCapability>format(providedCapability);
    }
    EList<AliasedRequiredCapability> _requiredCapabilities = unit.getRequiredCapabilities();
    for (final AliasedRequiredCapability aliasedRequiredCapability : _requiredCapabilities) {
      document.<AliasedRequiredCapability>format(aliasedRequiredCapability);
    }
    EList<RequiredCapability> _metaRequiredCapabilities = unit.getMetaRequiredCapabilities();
    for (final RequiredCapability requiredCapability : _metaRequiredCapabilities) {
      document.<RequiredCapability>format(requiredCapability);
    }
    EList<Function> _functions = unit.getFunctions();
    for (final Function function : _functions) {
      document.<Function>format(function);
    }
  }
  
  public void format(final Object model, final IFormattableDocument document) {
    if (model instanceof XtextResource) {
      _format((XtextResource)model, document);
      return;
    } else if (model instanceof Model) {
      _format((Model)model, document);
      return;
    } else if (model instanceof Unit) {
      _format((Unit)model, document);
      return;
    } else if (model instanceof EObject) {
      _format((EObject)model, document);
      return;
    } else if (model == null) {
      _format((Void)null, document);
      return;
    } else if (model != null) {
      _format(model, document);
      return;
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(model, document).toString());
    }
  }
}