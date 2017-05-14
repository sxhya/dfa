/**
 * Created by sxh on 14.05.17.
 */
class UnionState<X, Y> {
  private X x = null;
  private Y y = null;
  OperationState state = null;
  enum OperationState {BEGIN, WORKING, END}

  UnionState(X x, Y y) {
    this.x = x;
    this.y = y;
    state = OperationState.WORKING;
  }

  UnionState(OperationState state) {
    this.state = state;
  }

  static <X1, Y1> UnionState getX(X1 x) {
    return new UnionState<X1, Y1>(x, null);
  }

  static <X1, Y1> UnionState getY(Y1 y) {
    return new UnionState<X1, Y1>(null, y);
  }

  static <X1, Y1> UnionState begin() {
    return new UnionState(OperationState.BEGIN);
  }

  static <X1, Y1> UnionState end() {
    return new UnionState(OperationState.END);
  }

  @Override
  public int hashCode() {
    int result = state.hashCode() * 31;
    if (x != null) result += x.hashCode();
    result *= 31;
    if (y != null) result += y.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof UnionState) {
      UnionState us = (UnionState) o;
      if (state.equals(us.state)) {
        if (!state.equals(OperationState.WORKING)) return true;
        if (x != null && x.equals(us.x)) {
          assert (y == null && us.y == null);
          return true;
        }
        if (y != null && y.equals(us.y)) {
          assert (x == null && us.x == null);
          return true;
        }
      }
      return false;
    }
    return false;
  }

  @Override
  public String toString() {
    switch (state) {
      case BEGIN:
          return "BEGIN";
      case END:
          return "END";
      default:
    }
    if (x != null) return "[L " + x.toString() + "]";
    if (y != null) return "[R " + y.toString() + "]";
    return "NULL";
  }
}
