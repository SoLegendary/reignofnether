package com.solegendary.reignofnether.player;

import java.util.LinkedHashMap;

public class RTSPlayerScores {
    public LinkedHashMap<String, Integer> scoreList = new LinkedHashMap<>();

    public RTSPlayerScores() {
        scoreList.put("Total buildings constructed", 0);
        scoreList.put("Total units produced", 0);
    }

    public String displayScores(String playerName) {
        String text = playerName + "\n\n";

        for (int i : scoreList.values()) {
            text += Integer.toString(i) + "\n";
        }

        return text;
    }
}
