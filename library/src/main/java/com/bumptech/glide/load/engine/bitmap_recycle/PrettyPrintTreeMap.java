package com.bumptech.glide.load.engine.bitmap_recycle;

import java.util.TreeMap;

class PrettyPrintTreeMap<K, V> extends TreeMap<K, V> {
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("( ");
        for (Entry<K, V> entry : entrySet()) {
            sb.append('{').append(entry.getKey()).append(':').append(entry.getValue()).append("}, ");
        }
        final String result;
        if (!isEmpty()) {
            result = sb.substring(0, sb.length() - 2);
        } else {
            result = sb.toString();
        }
        return result + " )";
    }
}
