package com.deploysync.view.support;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public final class WindowChrome {
    private static final double RESIZE_MARGIN = 6;
    private static final double MIN_WIDTH = 480;
    private static final double MIN_HEIGHT = 320;

    private enum Edge { N, S, E, W, NE, NW, SE, SW }

    private final Stage stage;
    private final BooleanProperty maximized = new SimpleBooleanProperty(false);
    private double[] restoreBounds;

    private Edge activeResize;
    private double pressScreenX, pressScreenY, pressStageX, pressStageY, pressWidth, pressHeight;
    private double dragOffsetX, dragOffsetY;

    private WindowChrome(Stage stage) {
        this.stage = stage;
    }

    public static WindowChrome install(Stage stage, Scene scene, Node dragHandle) {
        WindowChrome chrome = new WindowChrome(stage);
        chrome.installResize(scene);
        chrome.installDrag(dragHandle);
        return chrome;
    }

    public BooleanProperty maximizedProperty() {
        return maximized;
    }

    public void toggleMaximize() {
        if (maximized.get()) {
            restore();
        } else {
            maximize();
        }
    }

    private void maximize() {
        restoreBounds = new double[]{stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()};
        Rectangle2D bounds = currentScreenVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        maximized.set(true);
    }

    private void restore() {
        if (restoreBounds != null) {
            stage.setX(restoreBounds[0]);
            stage.setY(restoreBounds[1]);
            stage.setWidth(restoreBounds[2]);
            stage.setHeight(restoreBounds[3]);
            restoreBounds = null;
        }
        maximized.set(false);
    }

    private Rectangle2D currentScreenVisualBounds() {
        List<Screen> screens = Screen.getScreensForRectangle(
                stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return screen.getVisualBounds();
    }

    private void installDrag(Node dragHandle) {
        dragHandle.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (activeResize != null || e.getTarget() instanceof ButtonBase) {
                return;
            }
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });
        dragHandle.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (activeResize != null || maximized.get() || e.getTarget() instanceof ButtonBase) {
                return;
            }
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        dragHandle.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !(e.getTarget() instanceof ButtonBase)) {
                toggleMaximize();
            }
        });
    }

    private void installResize(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> scene.setCursor(
                maximized.get() ? Cursor.DEFAULT : cursorFor(edgeAt(scene, e.getSceneX(), e.getSceneY()))));

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (maximized.get()) {
                activeResize = null;
                return;
            }
            activeResize = edgeAt(scene, e.getSceneX(), e.getSceneY());
            if (activeResize != null) {
                pressScreenX = e.getScreenX();
                pressScreenY = e.getScreenY();
                pressStageX = stage.getX();
                pressStageY = stage.getY();
                pressWidth = stage.getWidth();
                pressHeight = stage.getHeight();
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (activeResize != null) {
                resize(activeResize, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> activeResize = null);
    }

    private Edge edgeAt(Scene scene, double x, double y) {
        boolean left = x <= RESIZE_MARGIN;
        boolean right = x >= scene.getWidth() - RESIZE_MARGIN;
        boolean top = y <= RESIZE_MARGIN;
        boolean bottom = y >= scene.getHeight() - RESIZE_MARGIN;
        if (top && left) return Edge.NW;
        if (top && right) return Edge.NE;
        if (bottom && left) return Edge.SW;
        if (bottom && right) return Edge.SE;
        if (top) return Edge.N;
        if (bottom) return Edge.S;
        if (left) return Edge.W;
        if (right) return Edge.E;
        return null;
    }

    private Cursor cursorFor(Edge edge) {
        if (edge == null) {
            return Cursor.DEFAULT;
        }
        return switch (edge) {
            case N -> Cursor.N_RESIZE;
            case S -> Cursor.S_RESIZE;
            case E -> Cursor.E_RESIZE;
            case W -> Cursor.W_RESIZE;
            case NE -> Cursor.NE_RESIZE;
            case NW -> Cursor.NW_RESIZE;
            case SE -> Cursor.SE_RESIZE;
            case SW -> Cursor.SW_RESIZE;
        };
    }

    private void resize(Edge edge, double screenX, double screenY) {
        double dx = screenX - pressScreenX;
        double dy = screenY - pressScreenY;

        if (edge == Edge.E || edge == Edge.NE || edge == Edge.SE) {
            stage.setWidth(Math.max(MIN_WIDTH, pressWidth + dx));
        }
        if (edge == Edge.S || edge == Edge.SE || edge == Edge.SW) {
            stage.setHeight(Math.max(MIN_HEIGHT, pressHeight + dy));
        }
        if (edge == Edge.W || edge == Edge.NW || edge == Edge.SW) {
            double newWidth = Math.max(MIN_WIDTH, pressWidth - dx);
            stage.setX(pressStageX + (pressWidth - newWidth));
            stage.setWidth(newWidth);
        }
        if (edge == Edge.N || edge == Edge.NE || edge == Edge.NW) {
            double newHeight = Math.max(MIN_HEIGHT, pressHeight - dy);
            stage.setY(pressStageY + (pressHeight - newHeight));
            stage.setHeight(newHeight);
        }
    }
}
