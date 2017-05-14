import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by sxh on 12.05.17.
 */
public class Tests {
    @Test
    public void test1() {
        NFA<String, Integer> nfa1 = new NFA<>();
        NFA<String, Integer> nfa2 = new NFA<>();
        NFA<Pair<String, String>, Integer> nfap;

        nfa1.setInitialState("s1");
        nfa1.addAcceptState("t1");

        nfa2.setInitialState("s2");
        nfa2.addAcceptState("t21");
        nfa2.addAcceptState("t22");

        nfa1.addTransition("s1",0,"s1");
        nfa1.addTransition("s1",1,"t1");
        nfa1.addTransition("t1",0,"s1");
        nfa1.addTransition("t1",1,"t1");

        nfa2.addTransition("s2",1,"s2");
        nfa2.addTransition("s2",0,"q2");
        nfa2.addTransition("q2",0,"q2");
        nfa2.addTransition("q2",1,"t21");
        nfa2.addTransition("q2",1,"t22");
        nfa2.addDefaultTransition("t21", "t21");
        nfa2.addDefaultTransition("t22", "t22");

        List<Integer> state = new ArrayList<Integer>();
        state.add(0);

        nfap = NFA.directProduct(nfa1, nfa2, NFA.ProductAnnotation.AND);

        assert (!nfa1.runNFA(state));

        state.clear(); state.add(1);

        assert (nfa1.runNFA(state));
        assert (!nfap.runNFA(state));

        state.clear(); state.add(0); state.add(1);

        assert (nfap.runNFA(state));
    }

    @Test
    public void test2() {
        NFA<Integer, Character> a = NFA.singleSymbolNFA('a');
        NFA<Integer, Character> b = NFA.singleSymbolNFA('b');
        NFA<Pair<Integer, Integer>, Character> ak = NFA.kleeneClosure(a);
        NFA<UnionState<Pair<Integer, Integer>,Integer>, Character> ak_b = NFA.concatNFA(ak, b);
        assert (a.runNFA_('a'));
        assert (!a.runNFA_('b'));
        assert (!a.runNFA_('a', 'a'));
        assert (!a.runNFA(new ArrayList<>()));
        assert (ak.runNFA(new ArrayList<>()));
        assert (ak.runNFA_('a'));
        assert (ak.runNFA_('a', 'a'));
        assert (ak_b.runNFA_('b'));
        assert (ak_b.runNFA_('a', 'b'));
        assert (ak_b.runNFA_('a', 'a', 'b'));
        assert (!ak_b.runNFA_('a', 'b', 'b'));

        System.out.println(ak_b);
    }
}
