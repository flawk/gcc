/* HashMap.java -- a class providing a basic hashtable data structure,
   mapping Object --> Object
   Copyright (C) 1998, 1999, 2000, 2001 Free Software Foundation, Inc.

This file is part of GNU Classpath.

GNU Classpath is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2, or (at your option)
any later version.

GNU Classpath is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU Classpath; see the file COPYING.  If not, write to the
Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
02111-1307 USA.

As a special exception, if you link this library with other files to
produce an executable, this library does not by itself cause the
resulting executable to be covered by the GNU General Public License.
This exception does not however invalidate any other reasons why the
executable file might be covered by the GNU General Public License. */


package java.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// NOTE: This implementation is very similar to that of Hashtable. If you fix
// a bug in here, chances are you should make a similar change to the Hashtable
// code.

// NOTE: This implementation has some nasty coding style in order to
// support LinkedHashMap, which extends this.

/**
 * This class provides a hashtable-backed implementation of the
 * Map interface.
 * <p>
 *
 * It uses a hash-bucket approach; that is, hash collisions are handled
 * by linking the new node off of the pre-existing node (or list of
 * nodes).  In this manner, techniques such as linear probing (which
 * can cause primary clustering) and rehashing (which does not fit very
 * well with Java's method of precomputing hash codes) are avoided.
 * <p>
 *
 * Under ideal circumstances (no collisions), HashMap offers O(1)
 * performance on most operations (<pre>containsValue()</pre> is,
 * of course, O(n)).  In the worst case (all keys map to the same
 * hash code -- very unlikely), most operations are O(n).
 * <p>
 *
 * HashMap is part of the JDK1.2 Collections API.  It differs from
 * Hashtable in that it accepts the null key and null values, and it
 * does not support "Enumeration views."
 * <p>
 *
 * The iterators are <i>fail-fast</i>, meaning that any structural
 * modification, except for <code>remove()</code> called on the iterator
 * itself, cause the iterator to throw a
 * <code>ConcurrentModificationException</code> rather than exhibit
 * non-deterministic behavior.
 *
 * @author Jon Zeppieri
 * @author Jochen Hoenicke
 * @author Bryce McKinlay
 * @author Eric Blake <ebb9@email.byu.edu>
 * @see Object#hashCode()
 * @see Collection
 * @see Map
 * @see TreeMap
 * @see LinkedHashMap
 * @see IdentityHashMap
 * @see Hashtable
 * @since 1.2
 */
public class HashMap extends AbstractMap
  implements Map, Cloneable, Serializable
{
  /**
   * Default number of buckets. This is the value the JDK 1.3 uses. Some
   * early documentation specified this value as 101. That is incorrect.
   */
  static final int DEFAULT_CAPACITY = 11;

  /**
   * The default load factor; this is explicitly specified by the spec.
   */
  static final float DEFAULT_LOAD_FACTOR = 0.75f;

  /** "enum" of iterator types. */
  static final int KEYS = 0,
                   VALUES = 1,
                   ENTRIES = 2;

  /**
   * Compatible with JDK 1.2.
   */
  private static final long serialVersionUID = 362498820763181265L;

  /**
   * The rounded product of the capacity and the load factor; when the number
   * of elements exceeds the threshold, the HashMap calls <pre>rehash()</pre>.
   * @serial
   */
  int threshold;

  /**
   * Load factor of this HashMap:  used in computing the threshold.
   * @serial
   */
  final float loadFactor;

  /**
   * Array containing the actual key-value mappings.
   */
  transient HashEntry[] buckets;

  /**
   * Counts the number of modifications this HashMap has undergone, used
   * by Iterators to know when to throw ConcurrentModificationExceptions.
   */
  transient int modCount;

  /**
   * The size of this HashMap:  denotes the number of key-value pairs.
   */
  transient int size;

  /**
   * Class to represent an entry in the hash table. Holds a single key-value
   * pair.  This is extended again in LinkedHashMap.  See {@link clone()}
   * for why this must be Cloneable.
   */
  static class HashEntry extends BasicMapEntry implements Cloneable
  {
    /** The next entry in the linked list. */
    HashEntry next;

    /**
     * Simple constructor.
     * @param key the key
     * @param value the value
     */
    HashEntry(Object key, Object value)
    {
      super(key, value);
    }

    /**
     * Called when this entry is removed from the map. This version simply
     * returns the value, but in LinkedHashMap, it must also do bookkeeping.
     * @return the value of this key as it is removed.
     */
    Object cleanup()
    {
      return value;
    }
  }

  /**
   * Construct a new HashMap with the default capacity (11) and the default
   * load factor (0.75).
   */
  public HashMap()
  {
    this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Construct a new HashMap from the given Map, with initial capacity
   * the greater of the size of <code>m</code> or the default of 11.
   * <p>
   *
   * Every element in Map m will be put into this new HashMap.
   *
   * @param m a Map whose key / value pairs will be put into
   *          the new HashMap.  <b>NOTE: key / value pairs
   *          are not cloned in this constructor.</b>
   * @throws NullPointerException if m is null
   */
  public HashMap(Map m)
  {
    this(Math.max(m.size() * 2, DEFAULT_CAPACITY), DEFAULT_LOAD_FACTOR);
    putAllInternal(m);
  }

  /**
   * Construct a new HashMap with a specific inital capacity and
   * default load factor of 0.75.
   *
   * @param initialCapacity the initial capacity of this HashMap (>=0)
   * @throws IllegalArgumentException if (initialCapacity < 0)
   */
  public HashMap(int initialCapacity)
  {
    this(initialCapacity, DEFAULT_LOAD_FACTOR);
  }

  /**
   * Construct a new HashMap with a specific inital capacity and load factor.
   *
   * @param initialCapacity the initial capacity (>=0)
   * @param loadFactor the load factor (>0, not NaN)
   * @throws IllegalArgumentException if (initialCapacity < 0) ||
   *                                     ! (loadFactor > 0.0)
   */
  public HashMap(int initialCapacity, float loadFactor)
  {
    if (initialCapacity < 0)
      throw new IllegalArgumentException("Illegal Capacity: "
                                         + initialCapacity);
    if (! (loadFactor > 0)) // check for NaN too
      throw new IllegalArgumentException("Illegal Load: " + loadFactor);

    if (initialCapacity == 0)
      initialCapacity = 1;
    buckets = new HashEntry[initialCapacity];
    this.loadFactor = loadFactor;
    threshold = (int) (initialCapacity * loadFactor);
  }

  /**
   * Returns the number of kay-value mappings currently in this Map
   * @return the size
   */
  public int size()
  {
    return size;
  }

  /**
   * Returns true if there are no key-value mappings currently in this Map
   * @return <code>size() == 0</code>
   */
  public boolean isEmpty()
  {
    return size == 0;
  }

  /**
   * Returns true if this HashMap contains a value <pre>o</pre>, such that
   * <pre>o.equals(value)</pre>.
   *
   * @param value the value to search for in this HashMap
   * @return true if at least one key maps to the value
   */
  public boolean containsValue(Object value)
  {
    for (int i = buckets.length - 1; i >= 0; i--)
      {
        HashEntry e = buckets[i];
        while (e != null)
          {
            if (value == null ? e.value == null : value.equals(e.value))
              return true;
            e = e.next;
          }
      }
    return false;
  }

  /**
   * Returns true if the supplied object <pre>equals()</pre> a key
   * in this HashMap.
   *
   * @param key the key to search for in this HashMap
   * @return true if the key is in the table
   * @see #containsValue(Object)
   */
  public boolean containsKey(Object key)
  {
    int idx = hash(key);
    HashEntry e = buckets[idx];
    while (e != null)
      {
        if (key == null ? e.key == null : key.equals(e.key))
          return true;
        e = e.next;
      }
    return false;
  }

  /**
   * Return the value in this HashMap associated with the supplied key,
   * or <pre>null</pre> if the key maps to nothing.  NOTE: Since the value
   * could also be null, you must use containsKey to see if this key
   * actually maps to something.
   *
   * @param key the key for which to fetch an associated value
   * @return what the key maps to, if present
   * @see #put(Object, Object)
   * @see #containsKey(Object)
   */
  public Object get(Object key)
  {
    int idx = hash(key);
    HashEntry e = buckets[idx];
    while (e != null)
      {
        if (key == null ? e.key == null : key.equals(e.key))
          return e.value;
        e = e.next;
      }
    return null;
  }

  /**
   * Puts the supplied value into the Map, mapped by the supplied key.
   * The value may be retrieved by any object which <code>equals()</code>
   * this key. NOTE: Since the prior value could also be null, you must
   * first use containsKey if you want to see if you are replacing the
   * key's mapping.
   *
   * @param key the key used to locate the value
   * @param value the value to be stored in the HashMap
   * @return the prior mapping of the key, or null if there was none
   * @see #get(Object)
   * @see Object#equals(Object)
   */
  public Object put(Object key, Object value)
  {
    modCount++;
    int idx = hash(key);
    HashEntry e = buckets[idx];

    while (e != null)
      {
        if (key == null ? e.key == null : key.equals(e.key))
          // Must use this method for necessary bookkeeping in LinkedHashMap.
          return e.setValue(value);
        else
          e = e.next;
      }

    // At this point, we know we need to add a new entry.
    if (++size > threshold)
      {
        rehash();
        // Need a new hash value to suit the bigger table.
        idx = hash(key);
      }

    // LinkedHashMap cannot override put(), hence this call.
    addEntry(key, value, idx, true);
    return null;
  }

  /**
   * Helper method for put, that creates and adds a new Entry.  This is
   * overridden in LinkedHashMap for bookkeeping purposes.
   *
   * @param key the key of the new Entry
   * @param value the value
   * @param idx the index in buckets where the new Entry belongs
   * @param callRemove Whether to call the removeEldestEntry method.
   * @see #put(Object, Object)
   */
  void addEntry(Object key, Object value, int idx, boolean callRemove)
  {
    HashEntry e = new HashEntry(key, value);

    e.next = buckets[idx];
    buckets[idx] = e;
  }

  /**
   * Removes from the HashMap and returns the value which is mapped by the
   * supplied key. If the key maps to nothing, then the HashMap remains
   * unchanged, and <pre>null</pre> is returned. NOTE: Since the value
   * could also be null, you must use containsKey to see if you are
   * actually removing a mapping.
   *
   * @param key the key used to locate the value to remove
   * @return whatever the key mapped to, if present
   */
  public Object remove(Object key)
  {
    modCount++;
    int idx = hash(key);
    HashEntry e = buckets[idx];
    HashEntry last = null;

    while (e != null)
      {
        if (key == null ? e.key == null : key.equals(e.key))
          {
            if (last == null)
              buckets[idx] = e.next;
            else
              last.next = e.next;
            size--;
            // Method call necessary for LinkedHashMap to work correctly.
            return e.cleanup();
          }
        last = e;
        e = e.next;
      }
    return null;
  }

  /**
   * Copies all elements of the given map into this hashtable.  If this table
   * already has a mapping for a key, the new mapping replaces the current
   * one.
   *
   * @param m the map to be hashed into this
   */
  public void putAll(Map m)
  {
    Iterator itr = m.entrySet().iterator();

    for (int msize = m.size(); msize > 0; msize--)
      {
        Map.Entry e = (Map.Entry) itr.next();
        // Optimize in case the Entry is one of our own.
        if (e instanceof BasicMapEntry)
          {
            BasicMapEntry entry = (BasicMapEntry) e;
            put(entry.key, entry.value);
          }
        else
          {
            put(e.getKey(), e.getValue());
          }
      }
  }
  
  /**
   * Clears the Map so it has no keys. This is O(1).
   */
  public void clear()
  {
    modCount++;
    Arrays.fill(buckets, null);
    size = 0;
  }

  /**
   * Returns a shallow clone of this HashMap. The Map itself is cloned,
   * but its contents are not.  This is O(n).
   *
   * @return the clone
   */
  public Object clone()
  {
    HashMap copy = null;
    try
      {
        copy = (HashMap) super.clone();
      }
    catch (CloneNotSupportedException x)
      {
        // This is impossible.
      }
    copy.buckets = new HashEntry[buckets.length];
    copy.putAllInternal(this);
    return copy;
  }

  /**
   * Returns a "set view" of this HashMap's keys. The set is backed by the
   * HashMap, so changes in one show up in the other.  The set supports
   * element removal, but not element addition.
   *
   * @return a set view of the keys
   * @see #values()
   * @see #entrySet()
   */
  public Set keySet()
  {
    // Create an AbstractSet with custom implementations of those methods that
    // can be overridden easily and efficiently.
    return new AbstractSet()
    {
      public int size()
      {
        return size;
      }

      public Iterator iterator()
      {
        // Cannot create the iterator directly, because of LinkedHashMap.
        return HashMap.this.iterator(KEYS);
      }

      public void clear()
      {
        HashMap.this.clear();
      }

      public boolean contains(Object o)
      {
        return HashMap.this.containsKey(o);
      }

      public boolean remove(Object o)
      {
        // Test against the size of the HashMap to determine if anything
        // really got removed. This is neccessary because the return value of
        // HashMap.remove() is ambiguous in the null case.
        int oldsize = size;
        HashMap.this.remove(o);
        return (oldsize != size);
      }
    };
  }

  /**
   * Returns a "collection view" (or "bag view") of this HashMap's values.
   * The collection is backed by the HashMap, so changes in one show up
   * in the other.  The collection supports element removal, but not element
   * addition.
   *
   * @return a bag view of the values
   * @see #keySet()
   * @see #entrySet()
   */
  public Collection values()
  {
    // We don't bother overriding many of the optional methods, as doing so
    // wouldn't provide any significant performance advantage.
    return new AbstractCollection()
    {
      public int size()
      {
        return size;
      }

      public Iterator iterator()
      {
        // Cannot create the iterator directly, because of LinkedHashMap.
        return HashMap.this.iterator(VALUES);
      }

      public void clear()
      {
        HashMap.this.clear();
      }
    };
  }

  /**
   * Returns a "set view" of this HashMap's entries. The set is backed by
   * the HashMap, so changes in one show up in the other.  The set supports
   * element removal, but not element addition.
   * <p>
   *
   * Note that the iterators for all three views, from keySet(), entrySet(),
   * and values(), traverse the HashMap in the same sequence.
   *
   * @return a set view of the entries
   * @see #keySet()
   * @see #values()
   * @see Map.Entry
   */
  public Set entrySet()
  {
    // Create an AbstractSet with custom implementations of those methods that
    // can be overridden easily and efficiently.
    return new AbstractSet()
    {
      public int size()
      {
        return size;
      }

      public Iterator iterator()
      {
        // Cannot create the iterator directly, because of LinkedHashMap.
        return HashMap.this.iterator(ENTRIES);
      }

      public void clear()
      {
        HashMap.this.clear();
      }

      public boolean contains(Object o)
      {
        return getEntry(o) != null;
      }

      public boolean remove(Object o)
      {
        HashEntry e = getEntry(o);
        if (e != null)
          {
            HashMap.this.remove(e.key);
            return true;
          }
        return false;
      }
    };
  }

  /** Helper method that returns an index in the buckets array for `key;
   * based on its hashCode().
   *
   * @param key the key
   * @return the bucket number
   */
  int hash(Object key)
  {
    return (key == null) ? 0 : Math.abs(key.hashCode() % buckets.length);
  }

  /**
   * Helper method for entrySet(), which matches both key and value
   * simultaneously.
   *
   * @param o the entry to match
   * @return the matching entry, if found, or null
   * @see #entrySet()
   */
  private HashEntry getEntry(Object o)
  {
    if (!(o instanceof Map.Entry))
      return null;
    Map.Entry me = (Map.Entry) o;
    int idx = hash(me.getKey());
    HashEntry e = buckets[idx];
    while (e != null)
      {
        if (e.equals(me))
          return e;
        e = e.next;
      }
    return null;
  }

  /**
   * Increases the size of the HashMap and rehashes all keys to new array
   * indices; this is called when the addition of a new value would cause
   * size() > threshold. Note that the existing Entry objects are reused in
   * the new hash table.
   * <p>
   *
   * This is not specified, but the new size is twice the current size plus
   * one; this number is not always prime, unfortunately.
   */
  private void rehash()
  {
    HashEntry[] oldBuckets = buckets;

    int newcapacity = (buckets.length * 2) + 1;
    threshold = (int) (newcapacity * loadFactor);
    buckets = new HashEntry[newcapacity];

    for (int i = oldBuckets.length - 1; i >= 0; i--)
      {
        HashEntry e = oldBuckets[i];
        while (e != null)
          {
            int idx = hash(e.key);
            HashEntry dest = buckets[idx];

            if (dest != null)
              {
                while (dest.next != null)
                  dest = dest.next;
                dest.next = e;
              }
            else
              {
                buckets[idx] = e;
              }

            HashEntry next = e.next;
            e.next = null;
            e = next;
          }
      }
  }

  /**
   * Generates a parameterized iterator.  Must be overrideable, since
   * LinkedHashMap iterates in a different order.
   * @param type {@link #KEYS}, {@link #VALUES}, or {@link #ENTRIES}
   * @return the appropriate iterator
   */
  Iterator iterator(int type)
  {
    return new HashIterator(type);
  }

  /**
   * A simplified, more efficient internal implementation of putAll(). The 
   * Map constructor and clone() should not call putAll or put, in order to 
   * be compatible with the JDK implementation with respect to subclasses.
   */
  void putAllInternal(Map m)
  {
    Iterator itr = m.entrySet().iterator();

    for (int msize = m.size(); msize > 0; msize--)
      {
	Map.Entry e = (Map.Entry) itr.next();
	Object key = e.getKey();
	int idx = hash(key);
	addEntry(key, e.getValue(), idx, false);
      }
  }

  /**
   * Serializes this object to the given stream.
   *
   * @param s the stream to write to
   * @throws IOException if the underlying stream fails
   * @serialData the <i>capacity</i>(int) that is the length of the
   *             bucket array, the <i>size</i>(int) of the hash map
   *             are emitted first.  They are followed by size entries,
   *             each consisting of a key (Object) and a value (Object).
   */
  private void writeObject(ObjectOutputStream s) throws IOException
  {
    // Write the threshold and loadFactor fields.
    s.defaultWriteObject();

    s.writeInt(buckets.length);
    s.writeInt(size);
    // Avoid creating a wasted Set by creating the iterator directly.
    Iterator it = iterator(ENTRIES);
    while (it.hasNext())
      {
        HashEntry entry = (HashEntry) it.next();
        s.writeObject(entry.key);
        s.writeObject(entry.value);
      }
  }

  /**
   * Deserializes this object from the given stream.
   *
   * @param s the stream to read from
   * @throws ClassNotFoundException if the underlying stream fails
   * @throws IOException if the underlying stream fails
   * @serialData the <i>capacity</i>(int) that is the length of the
   *             bucket array, the <i>size</i>(int) of the hash map
   *             are emitted first.  They are followed by size entries,
   *             each consisting of a key (Object) and a value (Object).
   */
  private void readObject(ObjectInputStream s)
    throws IOException, ClassNotFoundException
  {
    // Read the threshold and loadFactor fields.
    s.defaultReadObject();

    // Read and use capacity.
    buckets = new HashEntry[s.readInt()];
    int len = s.readInt();
    // Already happens automatically.
    // size = 0;
    // modCount = 0;

    // Read and use key/value pairs.
    for ( ; len > 0; len--)
      put(s.readObject(), s.readObject());
  }

  /**
   * Iterate over HashMap's entries.
   * This implementation is parameterized to give a sequential view of
   * keys, values, or entries.
   *
   * @author Jon Zeppieri
   */
  class HashIterator implements Iterator
  {
    /**
     * The type of this Iterator: {@link #KEYS}, {@link #VALUES},
     * or {@link #ENTRIES}.
     */
    final int type;
    /**
     * The number of modifications to the backing HashMap that we know about.
     */
    int knownMod = modCount;
    /** The number of elements remaining to be returned by next(). */
    int count = size;
    /** Current index in the physical hash table. */
    int idx = buckets.length;
    /** The last Entry returned by a next() call. */
    HashEntry last;
    /**
     * The next entry that should be returned by next(). It is set to something
     * if we're iterating through a bucket that contains multiple linked
     * entries. It is null if next() needs to find a new bucket.
     */
    HashEntry next;

    /**
     * Construct a new HashIterator with the supplied type.
     * @param type {@link #KEYS}, {@link #VALUES}, or {@link #ENTRIES}
     */
    HashIterator(int type)
    {
      this.type = type;
    }

    /**
     * Returns true if the Iterator has more elements.
     * @return true if there are more elements
     * @throws ConcurrentModificationException if the HashMap was modified
     */
    public boolean hasNext()
    {
      if (knownMod != modCount)
        throw new ConcurrentModificationException();
      return count > 0;
    }

    /**
     * Returns the next element in the Iterator's sequential view.
     * @return the next element
     * @throws ConcurrentModificationException if the HashMap was modified
     * @throws NoSuchElementException if there is none
     */
    public Object next()
    {
      if (knownMod != modCount)
        throw new ConcurrentModificationException();
      if (count == 0)
        throw new NoSuchElementException();
      count--;
      HashEntry e = next;

      while (e == null)
        e = buckets[--idx];

      next = e.next;
      last = e;
      if (type == VALUES)
        return e.value;
      else if (type == KEYS)
        return e.key;
      return e;
    }

    /**
     * Removes from the backing HashMap the last element which was fetched
     * with the <pre>next()</pre> method.
     * @throws ConcurrentModificationException if the HashMap was modified
     * @throws IllegalStateException if called when there is no last element
     */
    public void remove()
    {
      if (knownMod != modCount)
        throw new ConcurrentModificationException();
      if (last == null)
        throw new IllegalStateException();

      HashMap.this.remove(last.key);
      knownMod++;
      last = null;
    }
  }
}
