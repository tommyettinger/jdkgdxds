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

import com.github.tommyettinger.ds.HolderSet;
import org.junit.Assert;
import org.junit.Test;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Created by Tommy Ettinger on 10/26/2020.
 */
@SuppressWarnings("SuspiciousMethodCalls")
public class HolderSetTest {
	public static class Person {
		@NonNull private String name;
		private int x, y;

		@NonNull
		public String getName () {
			return name;
		}

		public void setName (@NonNull String name) {
			this.name = name;
		}

		public int getX () {
			return x;
		}

		public void setX (int x) {
			this.x = x;
		}

		public int getY () {
			return y;
		}

		public void setY (int y) {
			this.y = y;
		}

		public Person () {
			this("Nihilus", 0, 0);
		}

		public Person (@NonNull String name) {
			this(name, 0, 0);
		}

		public Person (@NonNull String name, int x, int y) {
			this.name = name;
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString () {
			return name;
		}

		@Override
		public boolean equals (Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			Person person = (Person)o;

			if (x != person.x)
				return false;
			if (y != person.y)
				return false;
			return name.equals(person.name);
		}

		@Override
		public int hashCode () {
			int result = name.hashCode();
			result = 421 * result + x;
			result = 421 * result + y;
			return result;
		}
	}
	@Test
	public void testMultipleOperations() {
		HolderSet<Person, String> people = new HolderSet<>(Person::getName, 8);
		people.add(new Person("Alice"));
		Assert.assertEquals(people.size(), 1);
		Assert.assertTrue(people.contains("Alice"));
		Assert.assertFalse(people.contains("Bob"));
		Assert.assertFalse(people.contains("Carol"));
		Assert.assertEquals(people.get("Alice").x, 0);
		Assert.assertEquals(people.get("Alice").y, 0);
		people.add(new Person("Bob", 1, 0));
		Assert.assertEquals(people.size(), 2);
		Assert.assertTrue(people.contains("Alice"));
		Assert.assertTrue(people.contains("Bob"));
		Assert.assertFalse(people.contains("Carol"));
		Assert.assertEquals(people.get("Bob").x, 1);
		Assert.assertEquals(people.get("Bob").y, 0);
		people.add(new Person("Carol", -1 , -1));
		Assert.assertEquals(people.size(), 3);
		Assert.assertTrue(people.contains("Alice"));
		Assert.assertTrue(people.contains("Bob"));
		Assert.assertTrue(people.contains("Carol"));
		Assert.assertEquals(people.get("Carol").x, -1);
		Assert.assertEquals(people.get("Carol").y, -1);
		HolderSet<Person, String> peopleCopy = new HolderSet<>(people);
		people.remove("Alice");
		Assert.assertFalse(people.contains("Alice"));
		Assert.assertEquals(people.size(), 2);
		peopleCopy.remove("Alice");
		Assert.assertEquals(people, peopleCopy);

	}
}
