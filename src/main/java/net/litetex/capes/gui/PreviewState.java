package net.litetex.capes.gui;

public final class PreviewState {
    private PreviewState() {}

    public static volatile boolean hideBodyForPreview = false;

    public static volatile boolean forceOwnCapeRender = false;
}