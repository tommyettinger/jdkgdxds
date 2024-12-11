package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.Junction.*;
import org.junit.Test;

import java.util.Map;

public class JunctionTest {
    @Test
    public void testBasics() {
        Junction<String> j = new Junction<>(Any.of(All.of(Leaf.of("miss"), Leaf.of("block")), All.of(Leaf.of("fumble"), Not.of(Leaf.of("cardiac-arrest")))));
        ObjectObjectOrderedMap<String, ObjectList<String>> possible =
                ObjectObjectOrderedMap.with("miss", ObjectList.with("miss"),
                        "blocking miss", ObjectList.with("miss", "block"),
                        "blocking miss and counter", ObjectList.with("miss", "block", "counter"),
                        "fumble", ObjectList.with("fumble"),
                        "fumble with miss", ObjectList.with("miss", "fumble"));
        System.out.println("Checking Junction: " + j);
        for(Map.Entry<String, ObjectList<String>> e : possible.entrySet()){
            System.out.println(e.getKey() + " matches " + e.getValue().toString(", ", true) + " ? " + j.match(e.getValue()));
        }
    }

    @Test
    public void testLexer() {
        String text = "((~cardiac-arrest&fumble)|(block&miss))";
        ObjectDeque<String> deque = Junction.lex(text, 0, text.length());
        System.out.println(deque.toString("] [", true));
    }

    @Test
    public void testOperators() {
        String text = "((~cardiac-arrest&fumble)|(block&miss))";
        System.out.println(text);
        ObjectDeque<String> deque = Junction.lex(text, 0, text.length());
        System.out.println(deque.toString("] [", true));
        deque = Junction.shuntingYard(deque);
        System.out.println(deque.toString("] [", true));
    }

    @Test
    public void testParse() {
        String text = "((~cardiac-arrest&fumble)|(block&miss))";
        System.out.println("Ideal:  " + text);
        Junction<String> manual = new Junction<>(Any.of(All.of(Leaf.of("miss"), Leaf.of("block")), All.of(Leaf.of("fumble"), Not.of(Leaf.of("cardiac-arrest")))));
        System.out.println("Manual: " + manual);
        Junction<String> alternate = new Junction<>(Any.of(All.of(Leaf.of("fumble"), Not.of(Leaf.of("cardiac-arrest"))), All.of(Leaf.of("miss"), Leaf.of("block"))));
        System.out.println("Alt:    " + alternate);
        Junction<String> junction = Junction.parse(text);
        System.out.println("Parsed: " + junction);
    }
}
