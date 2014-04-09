/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.util.collection;

import java.util.Arrays;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.util.HashFunctions;
import org.neo4j.kernel.impl.util.PrimeFinder;

/**
 * A single-threaded GC free primitive long hashmap. Uses open addressing and has automatic resizing.
 *
 * Based off of CERNs COLT library, see LICENSES.txt for details.
 *
 * @param <V>
 */
public class LongObjectOpenHashMap<V> extends AbstractMap
{
    /**
     * The hash table keys.
     *
     * @serial
     */
    protected long table[];

    /**
     * The hash table values.
     *
     * @serial
     */
    protected V values[];

    /**
     * The state of each hash table entry (FREE, FULL, REMOVED).
     *
     * @serial
     */
    protected byte state[];

    /**
     * The number of table entries in state==FREE.
     *
     * @serial
     */
    protected int freeEntries;

    protected static final byte FREE = 0;

    protected static final byte FULL = 1;

    protected static final byte REMOVED = 2;

    /**
     * Constructs an empty map with default capacity and default load factors.
     */
    public LongObjectOpenHashMap() {
        this( defaultCapacity );
    }

    /**
     * Constructs an empty map with the specified initial capacity and default
     * load factors.
     *
     * @param initialCapacity
     *            the initial capacity of the map.
     * @throws IllegalArgumentException
     *             if the initial capacity is less than zero.
     */
    public LongObjectOpenHashMap(int initialCapacity) {
        this( initialCapacity, defaultMinLoadFactor, defaultMaxLoadFactor );
    }

    /**
     * Constructs an empty map with the specified initial capacity and the
     * specified minimum and maximum load factor.
     *
     * @param initialCapacity
     *            the initial capacity.
     * @param minLoadFactor
     *            the minimum load factor.
     * @param maxLoadFactor
     *            the maximum load factor.
     * @throws IllegalArgumentException
     *             if
     *
     *             <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) || (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >= maxLoadFactor)</tt>
     *             .
     */
    public LongObjectOpenHashMap(int initialCapacity, double minLoadFactor, double maxLoadFactor) {
        setUp( initialCapacity, minLoadFactor, maxLoadFactor );
    }

    /**
     * Removes all (key,value) associations from the receiver. Implicitly calls
     * <tt>trimToSize()</tt>.
     */

    public void clear() {
        Arrays.fill( state, FREE );
        Arrays.fill( values, null );

        this.distinct = 0;
        this.freeEntries = table.length; // delta
        trimToSize();
    }

    /**
     * Returns <tt>true</tt> if the receiver contains the specified key.
     *
     * @return <tt>true</tt> if the receiver contains the specified key.
     */

    public boolean containsKey(long key) {
        return indexOfKey( key ) >= 0;
    }

    /**
     * Returns <tt>true</tt> if the receiver contains the specified value.
     *
     * @return <tt>true</tt> if the receiver contains the specified value.
     */

    public boolean containsValue(V value) {
        return indexOfValue(value) >= 0;
    }

    /**
     * Ensures that the receiver can hold at least the specified number of
     * associations without needing to allocate new internal memory. If
     * necessary, allocates new internal memory and increases the capacity of
     * the receiver.
     * <p>
     * This method never need be called; it is for performance tuning only.
     * Calling this method before <tt>put()</tt>ing a large number of
     * associations boosts performance, because the receiver will grow only once
     * instead of potentially many times and hash collisions get less probable.
     *
     * @param minCapacity
     *            the desired minimum capacity.
     */

    public void ensureCapacity(int minCapacity) {
        if (table.length < minCapacity) {
            int newCapacity = nextPrime(minCapacity);
            rehash(newCapacity);
        }
    }

    /**
     * Returns the value associated with the specified key. It is often a good
     * idea to first check with {@link #containsKey(long)} whether the given key
     * has a value associated or not, i.e. whether there exists an association
     * for the given key or not.
     *
     * @param key
     *            the key to be searched for.
     * @return the value associated with the specified key; <tt>null</tt> if no
     *         such key is present.
     */

    public V get(long key) {
        int i = indexOfKey(key);
        if (i < 0)
            return null; // not contained
        return values[i];
    }

    /**
     * @param key
     *            the key to be added to the receiver.
     * @return the index where the key would need to be inserted, if it is not
     *         already contained. Returns -index-1 if the key is already
     *         contained at slot index. Therefore, if the returned index < 0,
     *         then it is already contained at slot -index-1. If the returned
     *         index >= 0, then it is NOT already contained and should be
     *         inserted at slot index.
     */
    protected int indexOfInsertion(long key) {
        final long tab[] = table;
        final byte stat[] = state;
        final int length = tab.length;

        final int hash = HashFunctions.hash( key ) & 0x7FFFFFFF;
        int i = hash % length;
        int decrement = hash % (length - 2); // double hashing, see
        // http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        // int decrement = (hash / length) % length;
        if (decrement == 0)
            decrement = 1;

        // stop if we find a removed or free slot, or if we find the key itself
        // do NOT skip over removed slots (yes, open addressing is like that...)
        while (stat[i] == FULL && tab[i] != key) {
            i -= decrement;
            // hashCollisions++;
            if (i < 0)
                i += length;
        }

        if (stat[i] == REMOVED) {
            // stop if we find a free slot, or if we find the key itself.
            // do skip over removed slots (yes, open addressing is like that...)
            // assertion: there is at least one FREE slot.
            int j = i;
            while (stat[i] != FREE && (stat[i] == REMOVED || tab[i] != key)) {
                i -= decrement;
                // hashCollisions++;
                if (i < 0)
                    i += length;
            }
            if (stat[i] == FREE)
                i = j;
        }

        if (stat[i] == FULL) {
            // key already contained at slot i.
            // return a negative number identifying the slot.
            return -i - 1;
        }
        // not already contained, should be inserted at slot i.
        // return a number >= 0 identifying the slot.
        return i;
    }

    /**
     * @param key
     *            the key to be searched in the receiver.
     * @return the index where the key is contained in the receiver, returns -1
     *         if the key was not found.
     */
    protected int indexOfKey(long key) {
        final long tab[] = table;
        final byte stat[] = state;
        final int length = tab.length;

        final int hash = HashFunctions.hash(key) & 0x7FFFFFFF;
        int i = hash % length;
        int decrement = hash % (length - 2); // double hashing, see
        // http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        // int decrement = (hash / length) % length;
        if (decrement == 0)
            decrement = 1;

        // stop if we find a free slot, or if we find the key itself.
        // do skip over removed slots (yes, open addressing is like that...)
        while (stat[i] != FREE && (stat[i] == REMOVED || tab[i] != key)) {
            i -= decrement;
            // hashCollisions++;
            if (i < 0)
                i += length;
        }

        if (stat[i] == FREE)
            return -1; // not found
        return i; // found, return index where key is contained
    }

    /**
     * @param value
     *            the value to be searched in the receiver.
     * @return the index where the value is contained in the receiver, returns
     *         -1 if the value was not found.
     */
    protected int indexOfValue(V value) {
        final Object val[] = values;
        final byte stat[] = state;

        for (int i = stat.length; --i >= 0;) {
            if (stat[i] == FULL && val[i] == value)
                return i;
        }

        return -1; // not found
    }

    /**
     * Returns the first key the given value is associated with. It is often a
     * good idea to first check with {@link #containsValue(Object)} whether
     * there exists an association from a key to this value.
     *
     * @param value
     *            the value to search for.
     * @return the first key for which holds <tt>get(key) == value</tt>; returns
     *         <tt>Long.MIN_VALUE</tt> if no such key exists.
     */

    public long keyOf(V value) {
        int i = indexOfValue( value );
        if (i < 0)
            return Long.MIN_VALUE;
        return table[i];
    }

    /**
     * Associates the given key with the given value. Replaces any old
     * <tt>(key,someOtherValue)</tt> association, if existing.
     *
     * @param key
     *            the key the value shall be associated with.
     * @param value
     *            the value to be associated.
     * @return <tt>true</tt> if the receiver did not already contain such a key;
     *         <tt>false</tt> if the receiver did already contain such a key -
     *         the new value has now replaced the formerly associated value.
     */

    public boolean put(long key, V value) {
        int i = indexOfInsertion(key);
        if (i < 0) { // already contained
            i = -i - 1;
            this.values[i] = value;
            return false;
        }

        if (this.distinct > this.highWaterMark) {
            int newCapacity = chooseGrowCapacity(this.distinct + 1, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
            return put(key, value);
        }

        this.table[i] = key;
        this.values[i] = value;
        if (this.state[i] == FREE)
            this.freeEntries--;
        this.state[i] = FULL;
        this.distinct++;

        if (this.freeEntries < 1) { // delta
            int newCapacity = chooseGrowCapacity(this.distinct + 1, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return true;
    }

    /**
     * Rehashes the contents of the receiver into a new table with a smaller or
     * larger capacity. This method is called automatically when the number of
     * keys in the receiver exceeds the high water mark or falls below the low
     * water mark.
     */
    protected void rehash(int newCapacity) {
        int oldCapacity = table.length;
        // if (oldCapacity == newCapacity) return;

        long oldTable[] = table;
        V oldValues[] = values;
        byte oldState[] = state;

        long newTable[] = new long[newCapacity];
        V newValues[] = (V[]) new Object[newCapacity];
        byte newState[] = new byte[newCapacity];

        this.lowWaterMark = chooseLowWaterMark(newCapacity, this.minLoadFactor);
        this.highWaterMark = chooseHighWaterMark(newCapacity, this.maxLoadFactor);

        this.table = newTable;
        this.values = newValues;
        this.state = newState;
        this.freeEntries = newCapacity - this.distinct; // delta

        for (int i = oldCapacity; i-- > 0;) {
            if (oldState[i] == FULL) {
                long element = oldTable[i];
                int index = indexOfInsertion(element);
                newTable[index] = element;
                newValues[index] = oldValues[i];
                newState[index] = FULL;
            }
        }
    }

    /**
     * Removes the given key with its associated element from the receiver, if
     * present.
     *
     * @param key
     *            the key to be removed from the receiver.
     * @return <tt>true</tt> if the receiver contained the specified key,
     *         <tt>false</tt> otherwise.
     */

    public boolean removeKey(long key) {
        int i = indexOfKey(key);
        if (i < 0)
            return false; // key not contained

        this.state[i] = REMOVED;
        this.values[i] = null; // delta
        this.distinct--;

        if (this.distinct < this.lowWaterMark) {
            int newCapacity = chooseShrinkCapacity(this.distinct, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return true;
    }

    public V getAndRemove( long key )
    {
        int i = indexOfKey(key);
        if (i < 0)
            return null;

        V old = values[i];
        this.state[i] = REMOVED;
        this.values[i] = null; // delta
        this.distinct--;

        if (this.distinct < this.lowWaterMark) {
            int newCapacity = chooseShrinkCapacity(this.distinct, this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return old;
    }

    /**
     * Initializes the receiver.
     *
     * @param initialCapacity
     *            the initial capacity of the receiver.
     * @param minLoadFactor
     *            the minLoadFactor of the receiver.
     * @param maxLoadFactor
     *            the maxLoadFactor of the receiver.
     * @throws IllegalArgumentException
     *             if
     *
     *             <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) || (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >= maxLoadFactor)</tt>
     *             .
     */

    protected void setUp(int initialCapacity, double minLoadFactor, double maxLoadFactor) {
        int capacity = initialCapacity;
        super.setUp(capacity, minLoadFactor, maxLoadFactor);
        capacity = nextPrime(capacity);
        if (capacity == 0)
            capacity = 1; // open addressing needs at least one FREE slot at
        // any time.

        this.table = new long[capacity];
        this.values = (V[]) new Object[capacity];
        this.state = new byte[capacity];

        // memory will be exhausted long before this pathological case happens,
        // anyway.
        this.minLoadFactor = minLoadFactor;
        if (capacity == PrimeFinder.largestPrime)
            this.maxLoadFactor = 1.0;
        else
            this.maxLoadFactor = maxLoadFactor;

        this.distinct = 0;
        this.freeEntries = capacity; // delta

        // lowWaterMark will be established upon first expansion.
        // establishing it now (upon instance construction) would immediately
        // make the table shrink upon first put(...).
        // After all the idea of an "initialCapacity" implies violating
        // lowWaterMarks when an object is young.
        // See ensureCapacity(...)
        this.lowWaterMark = 0;
        this.highWaterMark = chooseHighWaterMark(capacity, this.maxLoadFactor);
    }

    /**
     * Trims the capacity of the receiver to be the receiver's current size.
     * Releases any superfluous internal memory. An application can use this
     * operation to minimize the storage of the receiver.
     */
    public void trimToSize() {
        // * 1.2 because open addressing's performance exponentially degrades
        // beyond that point
        // so that even rehashing the table can take very long
        int newCapacity = nextPrime((int) (1 + 1.2 * size()));
        if (table.length > newCapacity) {
            rehash(newCapacity);
        }
    }

    public <FAILURE extends Exception> void visitValues( Visitor<V, FAILURE> visitor ) throws FAILURE
    {
        for ( int i = 0; i < table.length; i++ )
        {
            if( state[i] == FULL )
            {
                if(visitor.visit( (V) values[i] ))
                {
                    return;
                }
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder( getClass().getSimpleName() + "[" + super.hashCode() + "]{ " );
        for ( int i = 0; i < table.length; i++ )
        {
            if( state[i] == FULL )
            {
                sb.append( table[i] + " -> " + values[i] + ", " );
            }
        }
        return sb.append( "}" ).toString();
    }
}
