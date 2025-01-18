/*
 * Copyright (c) 2025 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.ds.test;

import com.github.tommyettinger.ds.EnumLongMap;
import com.github.tommyettinger.ds.EnumLongOrderedMap;
import com.github.tommyettinger.ds.LongList;
import com.github.tommyettinger.ds.support.sort.LongComparator;
import com.github.tommyettinger.ds.support.sort.LongComparators;
import com.github.tommyettinger.ds.support.util.LongIterator;
import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class EnumLongMapOpTest {
    enum Chem {
        HYDROGEN, HELIUM, LITHIUM, BERYLLIUM, BORON, CARBON, NITROGEN, OXYGEN, FLUORINE, NEON,
        SODIUM, MAGNESIUM, ALUMINIUM, SILICON, PHOSPHORUS, SULFUR, CHLORINE, ARGON, POTASSIUM,
        CALCIUM, SCANDIUM, TITANIUM, VANADIUM, CHROMIUM, MANGANESE, IRON, COBALT, NICKEL,
        COPPER, ZINC, GALLIUM, GERMANIUM, ARSENIC, SELENIUM, BROMINE, KRYPTON, RUBIDIUM,
        STRONTIUM, YTTRIUM, ZIRCONIUM, NIOBIUM, MOLYBDENUM, TECHNETIUM, RUTHENIUM, RHODIUM,
        PALLADIUM, SILVER, CADMIUM, INDIUM, TIN, ANTIMONY, TELLURIUM, IODINE, XENON, CAESIUM,
        BARIUM, LANTHANUM, CERIUM, PRASEODYMIUM, NEODYMIUM, PROMETHIUM, SAMARIUM, EUROPIUM,
        GADOLINIUM, TERBIUM, DYSPROSIUM, HOLMIUM, ERBIUM, THULIUM, YTTERBIUM, LUTETIUM, HAFNIUM,
        TANTALUM, TUNGSTEN, RHENIUM, OSMIUM, IRIDIUM, PLATINUM, GOLD, MERCURY, THALLIUM, LEAD,
        BISMUTH, POLONIUM, ASTATINE, RADON, FRANCIUM, RADIUM, ACTINIUM, THORIUM, PROTACTINIUM,
        URANIUM, NEPTUNIUM, PLUTONIUM, AMERICIUM, CURIUM, BERKELIUM, CALIFORNIUM, EINSTEINIUM,
        FERMIUM, MENDELEVIUM, NOBELIUM, LAWRENCIUM, RUTHERFORDIUM, DUBNIUM, SEABORGIUM, BOHRIUM,
        HASSIUM, MEITNERIUM, DARMSTADTIUM, ROENTGENIUM, COPERNICIUM, NIHONIUM, FLEROVIUM, MOSCOVIUM,
        LIVERMORIUM, TENNESSINE, OGANESSON;

        public static final Chem[] ALL = values();
    }

    private static final int LIMIT = Chem.ALL.length;

    @Test
    public void addThenRemoveTest() {
        AceRandom random = new AceRandom(123);
        EnumLongMap map = new EnumLongMap(Chem.ALL);
        LongList neo = new LongList(LIMIT);

        for (int i = 0; i < LIMIT; i++) {
            Chem randomKey = random.randomElement(Chem.ALL);
            long randomValue = random.nextLong();
            map.put(randomKey, randomValue);
            neo.clear(); neo.addAll(map.values()); neo.sort();
            Assert.assertEquals(neo.size(), map.size());

        }
        System.out.printf("After adding, the map has %d items\n", map.size());
        for (int i = 0; i < LIMIT; i++) {
            Chem randomKey = random.randomElement(Chem.ALL);
            map.remove(randomKey);
            neo.clear(); neo.addAll(map.values()); neo.sort();
            Assert.assertEquals(neo.size(), map.size());
        }
        System.out.printf("After removing, the map has %d items\n", map.size());
    }

    @Test
    public void mixedAddRemoveTest() {
        AceRandom random = new AceRandom(123);
        EnumLongMap map = new EnumLongMap(Chem.ALL);
        LongList neo = new LongList(LIMIT);

        for (int i = 0; i < LIMIT; i++) {
            Chem randomKey = random.randomElement(Chem.ALL);
            long randomValue = random.nextLong();
            if (random.nextBoolean(0.7f)) {
                map.put(randomKey, randomValue);
            } else {
                map.remove(randomKey);
            }
            neo.clear(); neo.addAll(map.values()); neo.sort();
            Assert.assertEquals(neo.size(), map.size());
        }
        System.out.printf("The map has %d items\n", map.size());
    }

    @Test
    public void orderingTest() {
        AceRandom random = new AceRandom(123);
        EnumLongOrderedMap map = new EnumLongOrderedMap(Chem.ALL);
        for (int i = 0; i < 100; i++) {
            map.put(Chem.ALL[i], i);
        }
        int startLength = map.size();
        map.shuffle(random);
        Assert.assertEquals(startLength, map.size());
        Assert.assertEquals(startLength, map.order().size());
        Assert.assertEquals(startLength, map.keySet().size());

        EnumLongOrderedMap byKey = new EnumLongOrderedMap(map), byValue = new EnumLongOrderedMap(map);
        byKey.sort();
        byValue.sortByValue(LongComparators.NATURAL_COMPARATOR);
        for (int i = 0; i < startLength; i++) {
            Assert.assertEquals(byKey.keyAt(i), byValue.keyAt(i));
        }
        Assert.assertEquals(byKey, byValue);
        Assert.assertEquals(byKey.entrySet(), byValue.entrySet());
        Assert.assertEquals(byKey.keySet(), byValue.keySet());
        Assert.assertEquals(byKey.values(), byValue.values());
        Iterator<Enum<?>> byKeyIt = byKey.keySet().iterator();
        LongIterator byValIt = byValue.values().iterator();
        int count = 0;
        while (byKeyIt.hasNext() && byValIt.hasNext()){
            Assert.assertEquals(map.get(byKeyIt.next()), byValIt.nextLong());
            count++;
        }
        Assert.assertEquals(byKeyIt.hasNext(), byValIt.hasNext());
        Assert.assertEquals(startLength, count);

        byKey.reverse();
        byValue.sortByValue(LongComparators.OPPOSITE_COMPARATOR);
        for (int i = 0; i < startLength; i++) {
            Assert.assertEquals(byKey.keyAt(i), byValue.keyAt(i));
        }
        Assert.assertEquals(byKey, byValue);
        Assert.assertEquals(byKey.entrySet(), byValue.entrySet());
        Assert.assertEquals(byKey.keySet(), byValue.keySet());
        Assert.assertEquals(byKey.values(), byValue.values());
    }
}
