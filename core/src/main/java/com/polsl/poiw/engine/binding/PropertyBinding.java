package com.polsl.poiw.engine.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Obserwowalna właściwość — przechowuje wartość i powiadamia listenerów o zmianach.
 * Umożliwia jednokierunkowy binding: zmiana wartości w modelu (np. health w PlayerCharacter)
 * automatycznie aktualizuje UI (np. TextBlock z HP).
 */
public class PropertyBinding<T> {

    private T value;
    private final List<BindingEntry<T>> listeners = new ArrayList<>();

    public PropertyBinding(T initialValue) {
        this.value = initialValue;
    }

    /** Zwraca aktualną wartość */
    public T get() {
        return value;
    }

    /**
     * Ustawia nową wartość i powiadamia listenerów jeśli wartość się zmieniła.
     */
    public void set(T newValue) {
        if (!Objects.equals(this.value, newValue)) {
            T oldValue = this.value;
            this.value = newValue;
            notifyListeners(oldValue, newValue);
        }
    }

    /**
     * Wymusza powiadomienie listenerów nawet jeśli wartość się nie zmieniła.
     * Przydatne do inicjalizacji UI.
     */
    public void forceBroadcast() {
        notifyListeners(value, value);
    }

    /**
     * Binduje listener do tej właściwości.
     * Listener jest wywoływany natychmiast z aktualną wartością, a potem przy każdej zmianie.
     *
     * @param listener callback wywoływany z nową wartością
     * @return BindingHandle do odłączenia listenera
     */
    public BindingHandle bind(Consumer<T> listener) {
        BindingEntry<T> entry = new BindingEntry<>(listener);
        listeners.add(entry);
        // Natychmiastowe wywołanie z aktualną wartością
        listener.accept(value);
        return entry;
    }

    /**
     * Binduje listener bez natychmiastowego wywołania.
     *
     * @param listener callback wywoływany z nową wartością
     * @return BindingHandle do odłączenia listenera
     */
    public BindingHandle bindWithoutInitial(Consumer<T> listener) {
        BindingEntry<T> entry = new BindingEntry<>(listener);
        listeners.add(entry);
        return entry;
    }

    /** Usuwa konkretny binding */
    public void unbind(BindingHandle handle) {
        listeners.remove(handle);
    }

    /** Usuwa wszystkie bindingi */
    public void unbindAll() {
        listeners.clear();
    }

    /** Liczba aktywnych bindingów */
    public int getBindingCount() {
        return listeners.size();
    }

    private void notifyListeners(T oldValue, T newValue) {
        for (int i = listeners.size() - 1; i >= 0; i--) {
            BindingEntry<T> entry = listeners.get(i);
            if (entry.isBound()) {
                entry.getListener().accept(newValue);
            } else {
                listeners.remove(i);
            }
        }
    }

    // ===== Wewnętrzna implementacja BindingHandle =====

    private static class BindingEntry<T> implements BindingHandle {
        private final Consumer<T> listener;
        private boolean bound = true;

        BindingEntry(Consumer<T> listener) {
            this.listener = listener;
        }

        @Override
        public void unbind() {
            this.bound = false;
        }

        @Override
        public boolean isBound() {
            return bound;
        }

        Consumer<T> getListener() {
            return listener;
        }
    }
}
