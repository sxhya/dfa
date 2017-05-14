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
        NFA nfap;

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

        System.out.println(nfa1);
        System.out.println(nfa2);
        System.out.println(nfap);

        //assert (!DFA.runDFA(nfa1, state));

        state.clear(); state.add(1);

        //assert (DFA.runDFA(nfa1, state));
        //assert (!DFA.runDFA(dfap, state));

        state.clear(); state.add(0); state.add(1);

        //assert (DFA.runDFA(dfap, state));
    }

    @Test
    public void test2() {
        NFA<Boolean, Character> a = NFA.simpleNFA('a');
        System.out.println(a);
        NFA<Boolean, Character> b = NFA.simpleNFA('b');
        System.out.println(b);
        System.out.println(NFA.concatNFA(a, b));
        //System.out.println(NFA.concatNFA(NFA.concatNFA(nfa_a,nfa_b),nfa_c));
    }
}
