/*
 * generated by Xtext
 */
package org.eclipse.xtext.parser.antlr.parser.antlr;

import java.io.InputStream;
import org.eclipse.xtext.parser.antlr.IAntlrTokenFileProvider;

public class Bug406914TestLanguageAntlrTokenFileProvider implements IAntlrTokenFileProvider {

	@Override
	public InputStream getAntlrTokenFile() {
		ClassLoader classLoader = getClass().getClassLoader();
		return classLoader.getResourceAsStream("org/eclipse/xtext/parser/antlr/parser/antlr/internal/InternalBug406914TestLanguage.tokens");
	}
}
