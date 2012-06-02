/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package org.languagetool.dev.index;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.LuceneTestCase;
import org.languagetool.Language;
import org.languagetool.rules.Rule;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.patterns.Element;
import org.languagetool.rules.patterns.PatternRule;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class IndexerSearcherTest extends LuceneTestCase {

  private final File ruleFile = new File("src/rules/en/grammar.xml");
  private final Searcher errorSearcher = new Searcher();

  private IndexSearcher searcher;
  private Directory directory;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    directory = newDirectory();
    // Note that the second sentence ends with "lid" instead of "lids" (the inflated one)
    final String content = "How to move back and fourth from linux to xmb? Calcium deposits on eye lid.";
    Indexer.run(content, directory, Language.ENGLISH, false);
    searcher = new IndexSearcher(directory);
  }

  @Override
  public void tearDown() throws Exception {
    if (searcher != null) {
      searcher.close();
    }
    directory.close();
    super.tearDown();
  }

  public void testIndexerSearcher() throws Exception {
    List<MatchingSentence> matchingSentences =
            errorSearcher.findRuleMatchesOnIndex(getRule("BACK_AND_FOURTH"), Language.ENGLISH, searcher);
    assertEquals(1, matchingSentences.size());

    matchingSentences = errorSearcher.findRuleMatchesOnIndex(getRule("EYE_BROW"), Language.ENGLISH, searcher);
    assertEquals(1, matchingSentences.size());

    matchingSentences = errorSearcher.findRuleMatchesOnIndex(getRule("ALL_OVER_THE_WORD"), Language.ENGLISH, searcher);
    assertEquals(0, matchingSentences.size());

    try {
      errorSearcher.findRuleMatchesOnIndex(getRule("Invalid Rule Id"), Language.ENGLISH, searcher);
      fail("Exception should be thrown for invalid rule id.");
    } catch (PatternRuleNotFoundException expected) {}
  }

  private PatternRule getRule(String ruleId) throws IOException {
    return errorSearcher.getRuleById(ruleId, ruleFile);
  }

  public void testIndexerSearcherWithNewRule() throws Exception {
    final Searcher errorSearcher = new Searcher();
    final List<Element> elements = Arrays.asList(
            new Element("move", false, false, false),
            new Element("back", false, false, false)
    );
    final PatternRule rule1 = new PatternRule("RULE1", Language.ENGLISH, elements, "desc", "msg", "shortMsg");
    final IndexSearcher indexSearcher = new IndexSearcher(directory);
    try {
      final List<MatchingSentence> matchingSentences = errorSearcher.findRuleMatchesOnIndex(rule1, Language.ENGLISH, indexSearcher);
      assertEquals(1, matchingSentences.size());
      final List<RuleMatch> ruleMatches = matchingSentences.get(0).getRuleMatches();
      assertEquals(1, ruleMatches.size());
      final Rule rule = ruleMatches.get(0).getRule();
      assertEquals("RULE1", rule.getId());
    } finally {
      indexSearcher.close();
    }
  }

}
