package com.github.tommyettinger.ds;

import com.github.tommyettinger.random.AceRandom;
import org.junit.Assert;
import org.junit.Test;

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
}
