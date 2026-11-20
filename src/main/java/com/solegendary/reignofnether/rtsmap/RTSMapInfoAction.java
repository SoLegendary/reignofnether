package com.solegendary.reignofnether.rtsmap;

public enum RTSMapInfoAction {
    SET_MODE,
    ADD_MODE,
    SET_MAP_NAME,
    SET_DESCRIPTION,
    ADD_AUTHOR,
    SET_VERSION,
    SET_TEAM_MODE,
    SET_JSON, // the whole map.json, for the Editor's map tab (and back to the server when it saves)
    SET_BORDER, // world border size, from the Editor
    SET_BORDER_CENTER, // world border centre "x z", from the Editor
    SET_LANG, // ron/lang/*.json to the client, for the map's own text (RTSMapInfoClientEvents.mapText)
    SET_FORMAT, // the map's format number to the client (see RTSMapInfoServerEvents.SUPPORTED_FORMAT)
    CONVERT_MAP // from the mapmaker: write this format 1 map as the supported format
}
