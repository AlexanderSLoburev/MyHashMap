package com.github.alexandersloburev;

import java.util.Objects;

public class MyHashMap<K, V> {

  private static final int DEFAULT_CAPACITY = 16;
  private static final int MAXIMUM_CAPACITY = 1 << 30;

  private Node<K, V>[] table;
  private int size;

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

  public MyHashMap() { this(DEFAULT_CAPACITY); }

  public MyHashMap(int initialCapacity) {
    if (initialCapacity <= 0) {
      throw new IllegalArgumentException("initialCapacity must be positive: " +
                                         initialCapacity);
    }
    this.table = newTable(roundUpToPowerOfTwo(initialCapacity));
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
}