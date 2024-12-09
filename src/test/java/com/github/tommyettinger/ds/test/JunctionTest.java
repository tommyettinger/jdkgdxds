package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.Junction;
import com.github.tommyettinger.ds.Junction.*;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectObjectMap;
import com.github.tommyettinger.ds.ObjectObjectOrderedMap;
import org.junit.Test;

import java.util.Map;

public class JunctionTest {
    @Test
    public void testBasics() {
        Junction<String> j = new Junction<>(Any.of(All.of(Leaf.of("miss"), Leaf.of("block")), Leaf.of("fumble")));
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
}
