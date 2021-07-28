package i2.act.grammargraph.properties.subtree;

import i2.act.packrat.Token;
import i2.act.peg.symbols.LexerSymbol;
import i2.act.peg.symbols.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SubTreeSequence implements Iterable<SubTree> {

  private final List<SubTree> subTrees;

  public SubTreeSequence(final SubTreeSequence firstSequence,
      final SubTreeSequence secondSequence) {
    final List<SubTree> subTrees = new ArrayList<>();

    subTrees.addAll(firstSequence.subTrees);
    subTrees.addAll(secondSequence.subTrees);

    this.subTrees = Collections.unmodifiableList(subTrees);
  }

  public SubTreeSequence(final List<SubTree> subTrees) {
    this.subTrees = Collections.unmodifiableList(subTrees);
  }

  public SubTreeSequence(final SubTree subTree) {
    final List<SubTree> subTrees = new ArrayList<>();
    subTrees.add(subTree);

    this.subTrees = Collections.unmodifiableList(subTrees);
  }

  public SubTreeSequence() {
    this.subTrees = Collections.unmodifiableList(new ArrayList<>());
  }

  public final int numberOfTrees() {
    return this.subTrees.size();
  }

  public final boolean isEmptySequence() {
    return this.subTrees.isEmpty();
  }

  public final int size() {
    int size = 0;

    for (final SubTree subTree : this.subTrees) {
      size += subTree.size();
    }

    return size;
  }

  public final int numberOfTerminals() {
    int numberOfTerminals = 0;

    for (final SubTree subTree : this.subTrees) {
      numberOfTerminals += subTree.numberOfTerminals();
    }

    return numberOfTerminals;
  }

  public final List<SubTree> getSubTrees() {
    return this.subTrees;
  }

  public final List<LexerSymbol> getTerminals() {
    final List<LexerSymbol> terminals = new ArrayList<>();

    for (final SubTree subTree : this.subTrees) {
      terminals.addAll(subTree.getTerminals());
    }

    return terminals;
  }

  public final List<Token> toTokens(final Map<Symbol<?>, List<Token>> replacements) {
    final List<Token> tokens = new ArrayList<>();

    for (final SubTree subTree : this.subTrees) {
      subTree.toTokens(replacements, tokens);
    }

    return tokens;
  }

  @Override
  public final int hashCode() {
    return this.subTrees.hashCode();
  }

  @Override
  public final boolean equals(final Object other) {
    if (!(other instanceof SubTreeSequence)) {
      return false;
    }

    final SubTreeSequence otherSubTreeSequence = (SubTreeSequence) other;
    return this.subTrees.equals(otherSubTreeSequence.subTrees);
  }

  @Override
  public final Iterator<SubTree> iterator() {
    return this.subTrees.iterator();
  }

  @Override
  public final String toString() {
    return this.subTrees.toString();
  }

}
