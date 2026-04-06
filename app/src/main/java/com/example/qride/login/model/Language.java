package com.example.qride.login.model;

public class Language {
    private String name;
    private int flagRes;

    public Language(String name, int flagRes) {
        this.name = name;
        this.flagRes = flagRes;
    }

    public String getName() {
        return name;
    }

    public int getFlagRes() {
        return flagRes;
    }
}
