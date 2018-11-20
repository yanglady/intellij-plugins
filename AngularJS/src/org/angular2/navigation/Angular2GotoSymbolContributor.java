// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.angular2.navigation;

import com.intellij.lang.javascript.psi.JSImplicitElementProvider;
import com.intellij.lang.javascript.psi.stubs.JSElementIndexingData;
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement;
import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import org.angular2.entities.Angular2Directive;
import org.angular2.entities.Angular2DirectiveSelector;
import org.angular2.entities.Angular2DirectiveSelectorPsiElement;
import org.angular2.entities.Angular2EntityUtils;
import org.angular2.index.Angular2SourceDirectiveIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static org.angular2.entities.Angular2EntitiesProvider.getDirective;

public class Angular2GotoSymbolContributor implements ChooseByNameContributorEx {
  @NotNull
  @Override
  public String[] getNames(Project project, boolean includeNonProjectItems) {
    return ArrayUtil.EMPTY_STRING_ARRAY;
  }

  @NotNull
  @Override
  public NavigationItem[] getItemsByName(String name, String pattern, Project project, boolean includeNonProjectItems) {
    return NavigationItem.EMPTY_NAVIGATION_ITEM_ARRAY;
  }

  @Override
  public void processNames(@NotNull Processor<String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
    StubIndex.getInstance().processAllKeys(Angular2SourceDirectiveIndex.KEY, processor, scope, filter);
  }

  @Override
  public void processElementsWithName(@NotNull String name,
                                      @NotNull Processor<NavigationItem> processor,
                                      @NotNull FindSymbolParameters parameters) {
    StubIndex.getInstance().processElements(
      Angular2SourceDirectiveIndex.KEY, name, parameters.getProject(), parameters.getSearchScope(),
      parameters.getIdFilter(), JSImplicitElementProvider.class, provider -> {
        final JSElementIndexingData indexingData = provider.getIndexingData();
        if (indexingData != null) {
          final Collection<JSImplicitElement> elements = indexingData.getImplicitElements();
          if (elements != null) {
            for (JSImplicitElement element : elements) {
              if (element.isValid()) {
                Angular2Directive directive = getDirective(element);
                if (directive != null) {
                  if (!processor.process(directive.getTypeScriptClass())
                      || !processSelectors(name, directive.getSelector().getSimpleSelectorsWithPsi(), processor)) {
                    return false;
                  }
                  return true;
                }
              }
            }
          }
        }
        return true;
      });
  }

  private static boolean processSelectors(@NotNull String name,
                                          @NotNull List<Angular2DirectiveSelector.SimpleSelectorWithPsi> selectors,
                                          @NotNull Processor<NavigationItem> processor) {

    for (Angular2DirectiveSelector.SimpleSelectorWithPsi selector : selectors) {
      if (Angular2EntityUtils.isElementDirectiveIndexName(name)
          && !processSelectorElement(Angular2EntityUtils.getElementName(name),
                                     selector.getElement(), processor)) {
        return false;
      }
      if (Angular2EntityUtils.isAttributeDirectiveIndexName(name)) {
        String attrName = Angular2EntityUtils.getAttributeName(name);
        for (Angular2DirectiveSelectorPsiElement attribute : selector.getAttributes()) {
          if (!processSelectorElement(attrName, attribute, processor)) {
            return false;
          }
        }
      }
      if (!processSelectors(name, selector.getNotSelectors(), processor)) {
        return false;
      }
    }
    return true;
  }

  private static boolean processSelectorElement(@NotNull String name,
                                                @Nullable Angular2DirectiveSelectorPsiElement element,
                                                @NotNull Processor<NavigationItem> processor) {
    return element == null || !name.equals(element.getName()) || processor.process(element);
  }
}
