package com.solegendary.reignofnether.player;

import java.util.LinkedHashMap;

public class RTSPlayerScores {
    public LinkedHashMap<RTSPlayerScoresEnum, Integer> scoreList = new LinkedHashMap<>();

    public RTSPlayerScores() {
        scoreList.put(RTSPlayerScoresEnum.TOTAL_BUILDINGS_CONSTRUCTED, 0);
        scoreList.put(RTSPlayerScoresEnum.TOTAL_UNITS_PRODUCED, 0);
    }

    public LinkedHashMap<RTSPlayerScoresEnum, Integer> getScoreList() {
        return scoreList;
    }

    public void addToScore(RTSPlayerScoresEnum scoresEnum) {
        // On built, add 1 to score
        int score = scoreList.get(scoresEnum) + 1;
        scoreList.put(scoresEnum, score);
    }

    // May be redundant
    /* public int getScore(RTSPlayerScoresEnum scoreEnum) {
        return scoreList.get(scoreEnum);
    } */
}
