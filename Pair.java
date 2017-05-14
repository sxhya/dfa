/**
 * Created by user on 5/11/17.
 */
public class Pair<A, B> {
  public A a;
  public B b;

  public Pair(A a, B b) {
    this.a = a;
    this.b = b;
  }

  @Override
  public int hashCode() {
    int result = (a == null) ? 0 : a.hashCode();
    result *= 31;
    result += (b == null) ? 0 : b.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Pair) {
      Pair p = (Pair) obj;
      return !(p.a != null && a == null || a != null && p.a == null) &&
              !(a != null && !p.a.equals(a)) &&
              !(p.b != null && b == null || b != null && p.b == null) &&
              !(b != null && !p.b.equals(b));
    }
    return false;
  }

  @Override
  public String toString() {
    return "(" + (a == null ? "null" : a.toString()) + ", " + (b == null ? "null" : b.toString()) + ")";
  }
}
