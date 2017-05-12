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
    return a.hashCode()*31 + b.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Pair) {
      Pair p = (Pair) obj;
      return (p.a.equals(a) && p.b.equals(b));
    }
    return false;
  }

  @Override
  public String toString() {
    return "(" + a.toString() + ", " + b.toString() + ")";
  }
}
