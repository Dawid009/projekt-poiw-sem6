package com.polsl.poiw.engine.net.replication;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.engine.net.Replicated;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * metadata cache for replication per component class
 * scans @Replicated fields and caches results
 */
public class ReplicationInfo {

    private static final String TAG = "ReplicationInfo";
    private static final Map<Class<?>, ReplicationInfo> cache = new HashMap<>();

    private final List<ReplicatedProperty> properties;

    private ReplicationInfo(List<ReplicatedProperty> properties) {
        this.properties = properties;
    }

    // scans class for @Replicated fields (result cached)
    public static ReplicationInfo scan(Class<?> clazz) {
        return cache.computeIfAbsent(clazz, c -> {
            List<ReplicatedProperty> props = new ArrayList<>();
            // scan fields in class hierarchy
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Replicated.class)) {
                        props.add(new ReplicatedProperty(field));
                    }
                }
                current = current.getSuperclass();
            }
            return new ReplicationInfo(props);
        });
    }

    /**
     * collects dirty properties from the component instance
     *
     * @return map of fieldName → value (only changed fields), or empty map if no change
     */
    public Map<String, Object> collectDirty(Object instance) {
        Map<String, Object> dirty = new HashMap<>();
        for (ReplicatedProperty prop : properties) {
            if (prop.checkDirty(instance)) {
                dirty.put(prop.getFieldName(), prop.getValue(instance));
            }
        }
        return dirty;
    }

    // marks all properties as clean after sending
    public void markAllClean(Object instance) {
        for (ReplicatedProperty prop : properties) {
            prop.updateLastSent(instance);
        }
    }

    // applies server properties to the instance (client-side)
    // firing RepNotify callbacks if defined
    public void apply(Object instance, Map<String, Object> props) {
        for (ReplicatedProperty prop : properties) {
            Object newValue = props.get(prop.getFieldName());
            if (newValue != null || props.containsKey(prop.getFieldName())) {
                prop.setValue(instance, newValue);

                // fire RepNotify callback
                String callback = prop.getRepNotifyCallback();
                if (callback != null) {
                    try {
                        Method method = instance.getClass().getDeclaredMethod(callback);
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (Exception e) {
                        Gdx.app.error(TAG, "RepNotify callback failed: " + callback, e);
                    }
                }
            }
        }
    }

    // forces dirty on a specific property (e.g. after markDirty("fieldName"))
    public void forceDirty(String propertyName) {
        for (ReplicatedProperty prop : properties) {
            if (prop.getFieldName().equals(propertyName)) {
                prop.forceDirty();
                return;
            }
        }
    }

    public List<ReplicatedProperty> getProperties() { return properties; }
    public boolean hasReplicatedProperties() { return !properties.isEmpty(); }

    // clears cache (for testing or hot-reload)
    public static void clearCache() { cache.clear(); }
}
