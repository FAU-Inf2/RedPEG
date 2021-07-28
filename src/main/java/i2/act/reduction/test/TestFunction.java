package i2.act.reduction.test;

public interface TestFunction {

  public boolean test(final String program);

  default void cleanup() {
    // intentionally left blank
  }

}
