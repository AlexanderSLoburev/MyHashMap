package com.github.alexandersloburev;

import java.util.Objects;

public class MyHashMap<K, V> {

  private static final int DEFAULT_CAPACITY = 16;
  private static final int MAXIMUM_CAPACITY = 1 << 30;
  private static final float LOAD_FACTOR = 0.75f;

  private Node<K, V>[] table;
  private int size;
  private int threshold;

  private static class Node<K, V> {
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

    boolean matches(int hash, K key) {
      return this.hash == hash && Objects.equals(key, this.key);
    }
  }

  private Node<K, V> findNode(K key) {
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
    return hash & (table.length - 1); // length is always a power of two
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
    int result = 1;
    while (result < value) {
      result <<= 1;
    }
    return result;
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

  public MyHashMap() { this(DEFAULT_CAPACITY); }

  public MyHashMap(int initialCapacity) {
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
    if (size > threshold) {
      resize();
    }

    return null;
  }

  public V get(K key) {
    Node<K, V> node = findNode(key);
    return node == null ? null : node.value;
  }

  public boolean containsKey(K key) { return findNode(key) != null; }

  public V remove(K key) {
    int hash = hash(key);
    int index = indexFor(hash);

    Node<K, V> prev = null;
    for (Node<K, V> node = table[index]; node != null;
         prev = node, node = node.next) {
      if (node.matches(hash, key)) {
        if (prev == null) {
          table[index] = node.next; // remove the head of the list
        } else {
          prev.next = node.next;
        }
        --size;
        return node.value;
      }
    }
    return null;
  }

  public int size() { return size; }

  public boolean isEmpty() { return size == 0; }

  public void clear() {
    int capacity = roundUpToPowerOfTwo(DEFAULT_CAPACITY);
    table = newTable(capacity);
    threshold = thresholdFor(capacity);
    size = 0;
  }
}