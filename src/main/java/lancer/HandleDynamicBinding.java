package lancer;

// связывание
public @interface HandleDynamicBinding {

    enum Type {
        FIELD,
        METHOD
    }

    Type linkType();
}
