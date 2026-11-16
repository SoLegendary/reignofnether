package com.solegendary.reignofnether.guide;

public enum GuideAction {
    SAY,
    CLEAR_DIALOGUE,
    HIGHLIGHT,
    CLEAR_HIGHLIGHT,
    NEXT, // finishes the current line, even one that waits (say ... wait)
    LISTEN, // the input events the map has files for (RonEvents.INPUT)
    CONTROL, // the player may select and order, and sees the HUD (CinematicClient)
    FOLLOW, // the entity to keep the camera on, for ticks
    ROTATE, // turns the camera by degrees over ticks
    SHAKE // shakes the view, fading over ticks
}
