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
        NFA<Integer, Character> c = NFA.singleSymbolNFA('c');
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

        NFA ab = NFA.concatNFA(a, b);
        NFA abk = NFA.kleeneClosure(ab);
        NFA b_or_c = NFA.uniteNFA(b, c);
        NFA aborc = NFA.concatNFA(a, b_or_c);
        NFA abck = NFA.kleeneClosure(aborc);

        assert (abk.runNFA(new ArrayList<>()));
        assert (!abk.runNFA_('a'));
        assert (abk.runNFA_('a', 'b'));
        assert (!abk.runNFA_('a', 'b', 'a'));
        assert (abk.runNFA_('a', 'b', 'a', 'b'));

        assert (abck.runNFA(new ArrayList<>()));
        assert (!abck.runNFA_('a'));
        assert (abck.runNFA_('a', 'c'));
        assert (!abck.runNFA_('a', 'b', 'a'));
        assert (abck.runNFA_('a', 'b', 'a', 'c'));
        assert (abck.runNFA_('a', 'b', 'a', 'c', 'a', 'b'));
        assert (!abck.runNFA_('a', 'b', 'c'));

        NFA abck2 = NFA.kleeneClosure(NFA.uniteNFA(a, NFA.concatNFA(a, NFA.uniteNFA(b, c))));

        assert (abck2.runNFA(new ArrayList<>()));
        assert (abck2.runNFA_('a'));
        assert (abck2.runNFA_('a', 'a'));
        assert (abck2.runNFA_('a', 'c'));
        assert (abck2.runNFA_('a', 'a', 'c'));
        assert (abck2.runNFA_('a', 'a', 'c', 'a'));
        assert (abck2.runNFA_('a', 'b', 'a'));
        assert (abck2.runNFA_('a', 'b', 'a', 'c'));
        assert (abck2.runNFA_('a', 'b', 'a', 'c', 'a', 'b'));
        assert (!abck2.runNFA_('a', 'b', 'c'));
        assert (!abck2.runNFA_('a', 'b', 'a', 'c', 'c'));


        NFA xa = NFA.concatNFA(NFA.anyWordNFA(), a);

        NFA axa = NFA.concatNFA(a, NFA.concatNFA(NFA.anyWordNFA(), a));

        assert (!axa.runNFA(new ArrayList()));
        assert (!axa.runNFA_('a'));
        assert (!axa.runNFA_('a', 'b'));
        assert (!axa.runNFA_('a', 'b', 'c'));
        assert (axa.runNFA_('a', 'a'));
        assert (axa.runNFA_('a', 'b', 'a'));
        assert (axa.runNFA_('a', 'b', 'c', 'a'));
        assert (axa.runNFA_('a', 'b', 'a', 'c', 'a'));

        NFA x1 = NFA.directProduct(axa, abck2, FA.ProductAnnotation.AND);
        x1.purgeUnattainableStates();

        assert (!x1.runNFA_('a'));
        assert (!x1.runNFA_('a', 'b'));
        assert (!x1.runNFA_('a', 'b', 'c'));
        assert (x1.runNFA_('a', 'a'));
        assert (x1.runNFA_('a', 'b', 'a'));
        assert (!x1.runNFA_('a', 'b', 'c', 'a'));
        assert (!x1.runNFA_('a', 'c'));
        assert (x1.runNFA_('a', 'b', 'a'));
        assert (!x1.runNFA_('a', 'b', 'a', 'c'));
        assert (!x1.runNFA_('a', 'b', 'a', 'c', 'a', 'b'));
        assert (!x1.runNFA_('a', 'b', 'c'));
        assert (x1.runNFA_('a', 'b', 'a', 'c', 'a'));
        assert (x1.runNFA_('a', 'b', 'a', 'a', 'c', 'a'));

        System.out.println(x1);
    }
}
