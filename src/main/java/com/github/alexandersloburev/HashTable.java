package com.github.alexandersloburev;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class HashTable<K, V> {

  private static final int DEFAULT_CAPACITY = 16;
  private static final int MAXIMUM_CAPACITY = 1 << 30;
  private static final float LOAD_FACTOR = 0.75f;

  private Node<K, V>[] table;
  private int size;
  private int threshold;

  // Counts structural modifications (insertion of a new key, removal, clear).
  // Iterators capture it and verify on every next()/remove() — a mismatch
  // means the map was modified concurrently
  private int modCount;

  // Lazily cached view instances
  private Set<K> keySet;
  private Collection<V> values;
  private Set<Map.Entry<K, V>> entrySet;

  private static class Node<K, V> implements Entry<K, V> {
    final int hash;
    final K key;
    V value;
    Node<K, V> next;

    Node(int hash, K key, V value, Node<K, V> next) {
      this.hash = hash;
      this.key = key;
      this.value = value;
      this.next = next;
    }

    boolean matches(int hash, Object key) {
      return this.hash == hash && Objects.equals(key, this.key);
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V newValue) {
      V oldValue = value;
      value = newValue;
      return oldValue;
    }

    @Override
    public boolean equals(Object other) {
      if (other == this) {
        return true;
      }
      if (!(other instanceof Entry<?, ?> otherEntry)) {
        return false;
      }

      return Objects.equals(key, otherEntry.getKey()) &&
          Objects.equals(value, otherEntry.getValue());
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }
  }

  private Node<K, V> findNode(Object key) {
    int hash = hash(key);
    for (Node<K, V> node = table[indexFor(hash)]; node != null;
         node = node.next) {
      if (node.matches(hash, key)) {
        return node;
      }
    }
    return null;
  }

  private int indexFor(int hash) {
    // Contract: length is always a power of two
    return hash & (table.length - 1);
  }

  private static int thresholdFor(int capacity) {
    return (int)(capacity * LOAD_FACTOR);
  }

  private static int hash(Object key) {
    if (key == null) {
      return 0;
    }
    int hsh = key.hashCode();
    return hsh ^ (hsh >>> 16);
  }

  @SuppressWarnings("unchecked")
  private static <K, V> Node<K, V>[] newTable(int capacity) {
    return (Node<K, V>[]) new Node[capacity];
  }

  private static int roundUpToPowerOfTwo(int value) {
    if (value >= MAXIMUM_CAPACITY) {
      return MAXIMUM_CAPACITY;
    }
    return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
  }

  private void resize() {
    int oldCapacity = table.length;
    if (oldCapacity == MAXIMUM_CAPACITY) {
      // cannot grow: stop calling resize on every put
      threshold = Integer.MAX_VALUE;
      return;
    }

    int newCapacity = oldCapacity << 1; // safe: oldCapacity < 2^30 here

    Node<K, V>[] oldTable = table;
    Node<K, V>[] newTable = newTable(newCapacity);

    for (int i = 0; i < oldCapacity; i++) {
      Node<K, V> head = oldTable[i];

      if (head == null) {
        continue;
      }

      Node<K, V> loHead = null; // stay at index i
      Node<K, V> loTail = null;
      Node<K, V> hiHead = null; // move to i + oldCapacity
      Node<K, V> hiTail = null;

      for (Node<K, V> curr = head; curr != null; curr = curr.next) {
        if ((curr.hash & oldCapacity) == 0) {
          if (loTail == null) {
            loHead = curr;
          } else {
            loTail.next = curr;
          }
          loTail = curr;
        } else {
          if (hiTail == null) {
            hiHead = curr;
          } else {
            hiTail.next = curr;
          }
          hiTail = curr;
        }
      }

      if (loTail != null) {
        loTail.next = null;
        newTable[i] = loHead;
      }
      if (hiTail != null) {
        hiTail.next = null;
        newTable[i + oldCapacity] = hiHead;
      }
    }

    table = newTable;
    threshold = thresholdFor(newCapacity);
  }

  public HashTable() { this(DEFAULT_CAPACITY); }

  public HashTable(int initialCapacity) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("initialCapacity must be positive: " +
                                         initialCapacity);
    }
    int capacity = roundUpToPowerOfTwo(initialCapacity);
    table = newTable(capacity);
    threshold = thresholdFor(capacity);
  }

  public V put(K key, V value) {
    int hash = hash(key);
    int index = indexFor(hash);

    Node<K, V> prev = null;
    for (Node<K, V> node = table[index]; node != null;
         prev = node, node = node.next) {
      if (node.matches(hash, key)) {
        V oldValue = node.value;
        node.value = value;
        return oldValue;
      }
    }

    Node<K, V> newNode = new Node<>(hash, key, value, null);
    if (prev == null) {
      table[index] = newNode; // the bucket was empty
    } else {
      prev.next = newNode; // insertion at the end of the list
    }

    ++size;
    ++modCount;
    if (size > threshold) {
      resize();
    }

    return null;
  }

  public V get(Object key) {
    Node<K, V> node = findNode(key);
    return node == null ? null : node.value;
  }

  public boolean containsKey(Object key) { return findNode(key) != null; }

  private Node<K, V> removeNode(Object key, Object value, boolean matchValue) {
    int hash = hash(key);
    int index = indexFor(hash);
    Node<K, V> prev = null;

    for (Node<K, V> node = table[index]; node != null;
         prev = node, node = node.next) {
      if (!node.matches(hash, key)) {
        continue;
      }
      if (matchValue && !Objects.equals(node.value, value)) {
        return null;
      }
      if (prev == null) {
        table[index] = node.next;
      } else {
        prev.next = node.next;
      }
      --size;
      ++modCount;
      return node;
    }
    return null;
  }

  public V remove(Object key) {
    Node<K, V> node = removeNode(key, null, false);
    return node == null ? null : node.value;
  }

  public int size() { return size; }

  public boolean isEmpty() { return size == 0; }

  public void clear() {
    int capacity = roundUpToPowerOfTwo(DEFAULT_CAPACITY);
    table = newTable(capacity);
    threshold = thresholdFor(capacity);
    size = 0;
    ++modCount;
  }

  // Views
  private class KeySet extends AbstractSet<K> {
    @Override
    public int size() {
      return HashTable.this.size;
    }

    @Override
    public void clear() {
      HashTable.this.clear();
    }

    @Override
    public boolean contains(Object key) {
      return HashTable.this.containsKey(key);
    }

    @Override
    public boolean remove(Object key) {
      return HashTable.this.removeNode(key, null, false) != null;
    }

    @Override
    public Iterator<K> iterator() {
      return new KeyIterator();
    }
  }

  public Set<K> keySet() {
    if (keySet == null) {
      keySet = new KeySet();
    }
    return keySet;
  }

  private class Values extends AbstractCollection<V> {
    @Override
    public int size() {
      return HashTable.this.size;
    }

    @Override
    public void clear() {
      HashTable.this.clear();
    }

    @Override
    public Iterator<V> iterator() {
      return new ValueIterator();
    }
  }

  public Collection<V> values() {
    if (values == null) {
      values = new Values();
    }
    return values;
  }

  private class EntrySet extends AbstractSet<Entry<K, V>> {
    @Override
    public int size() {
      return HashTable.this.size;
    }

    @Override
    public void clear() {
      HashTable.this.clear();
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
      return new EntryIterator();
    }

    @Override
    public boolean contains(Object entry) {
      if (!(entry instanceof Entry<?, ?> e)) {
        return false;
      }
      Node<K, V> node = findNode(e.getKey());

      return node != null && node.equals(e);
    }

    @Override
    public boolean remove(Object entry) {
      if(!(entry instanceof Entry<?,?> e)) {
        return false;
      }
      Node<K, V> node = findNode(e.getKey());
      if (node == null || !node.equals(e)) {
        return false;
      }

      HashTable.this.removeNode(e.getKey(), e.getValue(), true);

      return true;
    }
  }

  public Set<Entry<K, V>> entrySet() {
    if (entrySet == null) {
      entrySet = new EntrySet();
    }
    return entrySet;
  }

  // Iterators
  private abstract class HashIterator {
    Node<K, V> nextNode;    // the node the upcoming next() will return
    Node<K, V> currentNode; // the last returned element
    int expectedModCount; // modCount snapshot for concurrent modification check
    int index; // bucket cursor: the next bucket to scan once the current chain
               // is exhausted

    private void scanForNextNode() {
      nextNode = null;

      // The post-increment fires on the exiting check as well, so after the
      // loop index points PAST the bucket assigned to next (or equals
      // table.length when the table is exhausted).
      while (index < table.length && (nextNode = table[index++]) == null) {
        // skip empty buckets
      }
    }

    Node<K, V> nextNode() {
      if (expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }
      if (nextNode == null) {
        throw new NoSuchElementException();
      }

      currentNode = nextNode;
      nextNode = nextNode.next; // continue along the chain...

      if (nextNode == null) {
        // ...or scan the next non-empty bucket
        scanForNextNode();
      }

      return currentNode;
    }

    HashIterator() {
      expectedModCount = modCount;
      currentNode = nextNode = null;
      index = 0;

      if (size > 0) {
        scanForNextNode();
      }
    }

    public boolean hasNext() { return nextNode != null; }

    public void remove() {
      if (currentNode == null) {
        throw new IllegalStateException();
      }
      if (expectedModCount != modCount) {
        throw new ConcurrentModificationException();
      }

      HashTable.this.remove(currentNode.key);
      expectedModCount = modCount;
      currentNode = null;
    }
  }

  private class KeyIterator extends HashIterator implements Iterator<K> {
    @Override
    public K next() {
      return nextNode().key;
    }
  }

  private class ValueIterator extends HashIterator implements Iterator<V> {
    @Override
    public V next() {
      return nextNode().value;
    }
  }

  private class EntryIterator
      extends HashIterator implements Iterator<Entry<K, V>> {
    @Override
    public Entry<K, V> next() {
      return nextNode();
    }
  }
}