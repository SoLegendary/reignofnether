package com.solegendary.reignofnether.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class RTSPlayerScores {
    public LinkedHashMap<RTSPlayerScoresEnum, Integer> scoreList = new LinkedHashMap<>();

    public RTSPlayerScores() {
        scoreList.put(RTSPlayerScoresEnum.TOTAL_BUILDINGS_CONSTRUCTED, 0);
        scoreList.put(RTSPlayerScoresEnum.TOTAL_UNITS_PRODUCED, 0);
        scoreList.put(RTSPlayerScoresEnum.WORKER_UNITS_PRODUCED, 0);
        scoreList.put(RTSPlayerScoresEnum.MILITARY_UNITS_PRODUCED, 0);
        scoreList.put(RTSPlayerScoresEnum.TOTAL_RESOURCES_HARVESTED, 0);
        scoreList.put(RTSPlayerScoresEnum.FOOD_HARVESTED, 0);
        scoreList.put(RTSPlayerScoresEnum.WOOD_HARVESTED, 0);
        scoreList.put(RTSPlayerScoresEnum.ORES_HARVESTED, 0);
    }

    public LinkedHashMap<RTSPlayerScoresEnum, Integer> getScoreList() {
        return scoreList;
    }

    public void addToScore(RTSPlayerScoresEnum scoresEnum) {
        int score = scoreList.get(scoresEnum) + 1;
        scoreList.put(scoresEnum, score);
    }

    public void addToScore(RTSPlayerScoresEnum scoresEnum, int amount) {
        int score = scoreList.get(scoresEnum) + amount;
        scoreList.put(scoresEnum, score);
    }

    public static String getScoreInitials(RTSPlayerScoresEnum scoresEnum) {
        String[] words = scoresEnum.toString().split("_");
        StringBuilder initials = new StringBuilder();

        for (String word : words) {
            initials.append(word.charAt(0));
        }
        return initials.toString();
    }

    public int[] getScoreListAsArray() {
        RTSPlayerScoresEnum[] values = RTSPlayerScoresEnum.values();
        int[] result = new int[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = scoreList.get(values[i]);
        }
        return result;
    }

    public void setScoreListFromArray(int[] arrayList) {
        int i = 0;
        for (RTSPlayerScoresEnum scoresEnum : RTSPlayerScoresEnum.values()) {
            scoreList.put(scoresEnum, arrayList[i]);
            i++;
        }
    }

    // May be redundant
    /* public int getScore(RTSPlayerScoresEnum scoreEnum) {
        return scoreList.get(scoreEnum);
    } */
}
