package net.mokatech.testable.sample.generics;

import net.mokatech.testable.api.Testable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Testable
public class Box<T extends Comparable<T>, U extends Number & Serializable, V> {

    private T value;
    private List<? extends U> numbers;
    private Map<String, List<V>> map;
    private V[] array;
    private int[][] grid;

    public Box(T value, List<? extends U> numbers, Map<String, List<V>> map, V[] array, int[][] grid) {
        this.value = value;
        this.numbers = numbers;
        this.map = map;
        this.array = array;
        this.grid = grid;
    }

    private boolean isGreaterThan(T other) {
        return value.compareTo(other) > 0;
    }

    /** Inner class using its enclosing class' type variable: the field is exposed with its erased type. */
    @Testable
    public class Inner {

        private T outerValue;
        private String name;

        public Inner(T outerValue, String name) {
            this.outerValue = outerValue;
            this.name = name;
        }

        /** Uses the enclosing class' type variable: exposed with its erased types. */
        private T orDefault(T other) {
            return outerValue != null ? outerValue : other;
        }
    }
}
