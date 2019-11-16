package com.example.integrationdsl;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the gender of the person
 *
 * @author Amol Nayak
 *
 */
public enum Gender {

    MALE("M"), FEMALE("F");

    private String identifier;

    private static Map<String, Gender> identifierMap;

    Gender(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    static {
        EnumSet<Gender> all = EnumSet.allOf(Gender.class);
        identifierMap = new HashMap<String, Gender>();
        for (Gender gender : all) {
            identifierMap.put(gender.getIdentifier(), gender);
        }
    }

    public static Gender getGenderFromIdentifier(String identifier) {
        return identifierMap.get(identifier);

    }
}
