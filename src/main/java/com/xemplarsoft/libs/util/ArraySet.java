package com.xemplarsoft.libs.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class ArraySet<T> implements Set<T> {
    protected T[] items = (T[])new Object[0];

    public boolean contains(Object o) {
        for(T t : items){
            if(o.equals(t)) return true;
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        boolean ret = true;
        for(Object o : c){
            ret &= contains(o);
        }
        return ret;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean add(T t) {
        if(this.contains(t)) return false;

        Object[] items = new Object[this.items.length + 1];
        System.arraycopy(this.items, 0, items, 0, this.items.length);
        items[this.items.length] = t;
        return true;
    }

    public boolean addAll(Collection<? extends T> c) {
        boolean ret = false;
        for(T o : c){
            ret |= add(o);
        }
        return ret;
    }

    public boolean remove(Object o) {
        if(contains(o)){
            Object[] items = new Object[this.items.length - 1];
            int index = 0;
            for(Object dat : this.items){
                if(!dat.equals(o)){
                    items[index] = dat;
                    index++;
                }
            }

            return true;
        }
        return false;
    }

    public boolean removeAll(Collection<?> c) {
        int count = 0;
        for(Object dat : c){
            if(!contains(dat)) count++;
        }
        Object[] items = new Object[count];
        int index = 0;
        for(Object dat : c){
            if(!contains(dat)){
                items[index] = dat;
                index++;
            }
        }

        if(count == c.size()){
            this.items = (T[])items;
        }

        return count == c.size();
    }

    public boolean equals(Object o) {
        if(o instanceof ArraySet){
            return o.hashCode() == this.hashCode();
        }
        return false;
    }

    public int hashCode() {
        byte[] msg = new byte[items.length * 4];
        for(int i = 0; i < items.length; i++){ 
            int dat = items[i].hashCode();
            msg[i] = (byte)((dat >> 24) & 0xFF);
            msg[i + 1] = (byte)((dat >> 16) & 0xFF);
            msg[i + 2] = (byte)((dat >> 8) & 0xFF);
            msg[i + 3] = (byte)((dat) & 0xFF);
        }
        
        byte[] hash = Sha256.getHash(msg);
        int ret = hash[0] + (hash[1] << 8) + (hash[2] << 16) + (hash[3] << 24);
        return ret;
    }

    public void clear() {
        this.items = (T[])new Object[0];
    }

    public boolean retainAll(Collection<?> c) {
        int count = 0;
        for(Object o : c){
            if(this.contains(o)) count++;
        }
        T[] items = (T[]) new Object[count];
        int index = 0;
        for(Object o : c){
            if(this.contains(o)){
                items[index] = (T) o;
                index++;
            }
        }

        return count > 0;
    }

    public <T1> T1[] toArray(T1[] a) {
        System.arraycopy(items, 0, a, 0, a.length);
        return a;
    }

    public Object[] toArray() {
        return items;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int position;
            
            public boolean hasNext() {
                return position < size();
            }

            public T next() {
                T item = items[position];
                position++;
                return item;
            }
        };
    }

    public int size() {
        return items.length;
    }
}
