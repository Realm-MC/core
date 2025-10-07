package br.com.realmmc.core.banner.action;

public enum ActionType {
    COMMAND,
    MESSAGE,
    OPEN_MENU;

    public static ActionType fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}