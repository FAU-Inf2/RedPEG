package i2.act.reduction.lists;

import i2.act.packrat.cst.Node;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class LineSlicer implements Slicer<String> {

  @Override
  public final List<String> slice(final Node<?> syntaxTree) {
    final String serialized = syntaxTree.print();
    return Arrays.asList(serialized.split("\n"));
  }

  @Override
  public final String join(final List<String> list) {
    return list.stream()
        .collect(Collectors.joining("\n"));
  }

}
