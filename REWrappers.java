import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by user on 5/17/17.
 */
public class REWrappers {
  // ID_FRAGMENT : [a-zA-Z_] [a-zA-Z0-9_\']* | BIN_OP_CHAR+;
  public static final AbstractSRE BINOPCHAR = new SimpleClassSRE('~','!','@','#','$','%','^','&','*','\\','-','+','=','<','>','?','/','|','.',':');
  public static final SimpleSRE SYMBOL = new SimpleClassSRE(new RangeSRE('a', 'z'), new RangeSRE('A', 'Z'), new CharSRE('_'));
  public static final SimpleSRE DIGIT = new RangeSRE('0','9');
  public static final AbstractSRE NEXT_SYMBOL = new SimpleClassSRE(SYMBOL, DIGIT, new CharSRE('\\'), new CharSRE('\''));

  public static final AbstractSRE VCLANG_INFIX = new PlusSRE(BINOPCHAR);
  public static final AbstractSRE VCLANG_ID = new FollowSRE(SYMBOL, new StarSRE(NEXT_SYMBOL));

  abstract static class AbstractSRE {
    abstract String toJavaRegexp();
    abstract NFA<?, Character> toAutomaton();

    @Override
    public String toString() {
      return toJavaRegexp() + "\n" + toAutomaton().toString();
    }
  }

  abstract static class SimpleSRE extends AbstractSRE {}

  static class RangeSRE extends SimpleSRE {
    private char a;
    private char b;

    RangeSRE(char a, char b) {
      this.a = a;
      this.b = b;
    }

    @Override
    String toJavaRegexp() {
      return String.valueOf(a) + "-" + String.valueOf(b);
    }

    @Override
    NFA<?, Character> toAutomaton() {
      assert (a <= b);
      NFA<?, Character> result = null;
      for (char x=a; x<=b; x++)
        if (result == null) result = NFA.singleSymbolNFA(x); else
          result = NFA.uniteNFA(result, NFA.singleSymbolNFA(x));
      return result;
    }
  }

  static class CharSRE extends SimpleSRE {
    private char c;

    CharSRE(char c) {
      this.c = c;
    }

    @Override
    String toJavaRegexp() {
      return String.valueOf(c);
    }

    @Override
    NFA<?, Character> toAutomaton() {
      return NFA.singleSymbolNFA(c);
    }
  }

  static class UnionSRE extends AbstractSRE {
    private List<AbstractSRE> sres = new ArrayList<>();

    UnionSRE(AbstractSRE... sres) {
      Collections.addAll(this.sres, sres);
    }

    @Override
    String toJavaRegexp() {
      String result = "";
      for (int k=0; k<sres.size(); k++) {
        result += sres.get(k).toJavaRegexp();
        if (k < sres.size()-1) result += "|";
      }
      return result + "";
    }

    @Override
    NFA<?, Character> toAutomaton() {
      NFA<?, Character> result = new NFA<>();
      for (AbstractSRE sre : sres)
        if (result == null) result = sre.toAutomaton(); else
          result = NFA.uniteNFA(result, sre.toAutomaton());
      return result;
    }
  }

  //behavior is the same as in UnionSRE -- the only difference is in toJavaRegexp() method
  static class SimpleClassSRE extends SimpleSRE {
    private List<SimpleSRE> sres = new ArrayList<>();

    SimpleClassSRE(SimpleSRE... sres) {
      Collections.addAll(this.sres, sres);
    }

    SimpleClassSRE(Character... chars) {
      for (Character c : chars)
        sres.add(new CharSRE(c));
    }

    @Override
    String toJavaRegexp() {
      String result = "[";
      for (AbstractSRE sre : sres) result += sre.toJavaRegexp();
      return result + "]";
    }

    @Override
    NFA<?, Character> toAutomaton() {
      NFA<?, Character> result = new NFA<>();
      for (AbstractSRE sre : sres)
        if (result == null) result = sre.toAutomaton(); else
          result = NFA.uniteNFA(result, sre.toAutomaton());
      return result;
    }
  }

  static class StarSRE extends AbstractSRE {
    private AbstractSRE sre;

    StarSRE(AbstractSRE sre) {
      this.sre = sre;
    }

    @Override
    String toJavaRegexp() {
      return sre.toJavaRegexp()+"*";
    }

    @Override
    NFA<?, Character> toAutomaton() {
      return NFA.kleeneClosure(sre.toAutomaton());
    }
  }

  static class PlusSRE extends AbstractSRE {
    private AbstractSRE sre;

    PlusSRE(AbstractSRE sre) {
      this.sre = sre;
    }

    @Override
    String toJavaRegexp() {
      return sre.toJavaRegexp()+"+";
    }

    @Override
    NFA<?, Character> toAutomaton() {
      NFA<?, Character> x = sre.toAutomaton();
      return NFA.concatNFA(x, NFA.kleeneClosure(x));
    }
  }

  static class FollowSRE extends AbstractSRE {
    private List<AbstractSRE> sres = new ArrayList<>();

    FollowSRE(AbstractSRE... sres) {
      Collections.addAll(this.sres, sres);
    }

    @Override
    String toJavaRegexp() {
      String result = "";
      for (AbstractSRE sre : sres) result += sre.toJavaRegexp();
      return result;
    }

    @Override
    NFA<?, Character> toAutomaton() {
      NFA<?, Character> result = null;
      for (AbstractSRE sre : sres)
        if (result == null) result = sre.toAutomaton(); else
          result = NFA.concatNFA(result, sre.toAutomaton());
      return result;
    }
  }
}
