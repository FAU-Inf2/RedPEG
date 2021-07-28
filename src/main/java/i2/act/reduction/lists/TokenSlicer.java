package i2.act.reduction.lists;

import i2.act.packrat.Token;
import i2.act.packrat.cst.Node;
import i2.act.packrat.cst.TerminalNode;
import i2.act.packrat.cst.visitors.SyntaxTreeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class TokenSlicer implements Slicer<Token> {

  @Override
  public final List<Token> slice(final Node<?> syntaxTree) {
    final List<Token> tokens = new ArrayList<>();

    syntaxTree.accept(new SyntaxTreeVisitor<Void, Void>() {

      @Override
      public final Void visit(final TerminalNode node, final Void parameter) {
        final Token token = node.getToken();
        tokens.add(token);

        return null;
      }

    }, null);

    return tokens;
  }

  @Override
  public final String join(final List<Token> list) {
    return list.stream()
        .map((token) -> {
          final StringBuilder builder = new StringBuilder();

          for (final Token skippedToken : token.getSkippedTokensBefore()) {
            builder.append(skippedToken.getValue());
          }

          builder.append(token.getValue());

          return builder.toString();
        })
        .collect(Collectors.joining(""));
  }

}
