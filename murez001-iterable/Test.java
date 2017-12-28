package com.murez.test;

import java.util.Iterator;

public class Test {
    public static void main(String[] args) {
        for(int number : new Trove())
            System.out.print(number + " ");
    }

    public static class Trove implements Iterable<Integer> {
        @Override
        public Iterator<Integer> iterator() {
            return new Treasure(-2, 4);
        }

        public static class Treasure implements Iterator<Integer> {
            private int start, limit, current;

            private Treasure(int start, int limit) {
                if(start > limit)
                    throw new UnsupportedOperationException();
                this.start = start;
                this.limit = limit;
                this.current = start;
            }

            @Override
            public boolean hasNext() { return limit >= current; }

            @Override
            public Integer next() { return current++; }
        }
    }
}