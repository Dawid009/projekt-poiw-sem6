package com.polsl.poiw.engine.net.replication;

import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;
import com.polsl.poiw.engine.net.ReplicationCondition;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;


// tacks dirty state of one replicated field

public class ReplicatedProperty {
    private static final Object UNSET = new Object();

    private final String fieldName;
    private final Field field;
    private final ReplicationCondition condition;
    private final String repNotifyCallback; // nullable
    private final Map<Object, PropertyState> instanceStates = new WeakHashMap<>();

    public ReplicatedProperty(Field field) {
        this.field = field;
        this.field.setAccessible(true);
        this.fieldName = field.getName();

        Replicated rep = field.getAnnotation(Replicated.class);
        this.condition = rep != null ? rep.condition() : ReplicationCondition.ALWAYS;

        RepNotify notify = field.getAnnotation(RepNotify.class);
        this.repNotifyCallback = notify != null ? notify.value() : null;
    }

    // checks if the field value has changed since the last send.
    public boolean checkDirty(Object instance) {
        try {
            PropertyState state = getState(instance);
            Object currentValue = field.get(instance);
            if (!Objects.equals(currentValue, state.lastSentValue)) {
                state.dirty = true;
                return true;
            }
            return state.dirty;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    // reads the current value of the field from the instance
    public Object getValue(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    // sets the field value on the instance (client-side replication)
    public void setValue(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    // Marks the field as sent (clean)
    public void markClean(Object instance) {
        getState(instance).dirty = false;
    }

    // updates lastSentValue after sending
    public void updateLastSent(Object instance) {
        try {
            PropertyState state = getState(instance);
            state.lastSentValue = field.get(instance);
            state.dirty = false;
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    // forces the dirty flag (e.g. after markDirty(propertyName))
    public void forceDirty(Object instance) {
        getState(instance).dirty = true;
    }

    public String getFieldName() { return fieldName; }
    public ReplicationCondition getCondition() { return condition; }
    public String getRepNotifyCallback() { return repNotifyCallback; }

    private PropertyState getState(Object instance) {
        return instanceStates.computeIfAbsent(instance, ignored -> new PropertyState());
    }

    private static final class PropertyState {
        private Object lastSentValue = UNSET;
        private boolean dirty = true;
    }
}
