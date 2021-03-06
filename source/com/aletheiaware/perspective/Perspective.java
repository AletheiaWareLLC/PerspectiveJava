/*
 * Copyright 2019 Aletheia Ware LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aletheiaware.perspective;

import com.aletheiaware.joy.scene.Matrix;
import com.aletheiaware.joy.scene.RotationAnimation;
import com.aletheiaware.joy.scene.ScaleNode;
import com.aletheiaware.joy.scene.Scene;
import com.aletheiaware.joy.scene.SceneGraphNode;
import com.aletheiaware.joy.scene.TranslateNode;
import com.aletheiaware.joy.scene.Vector;
import com.aletheiaware.joy.utils.JoyUtils;
import com.aletheiaware.perspective.PerspectiveProto.Block;
import com.aletheiaware.perspective.PerspectiveProto.Dialog;
import com.aletheiaware.perspective.PerspectiveProto.Location;
import com.aletheiaware.perspective.PerspectiveProto.Goal;
import com.aletheiaware.perspective.PerspectiveProto.Move;
import com.aletheiaware.perspective.PerspectiveProto.Outline;
import com.aletheiaware.perspective.PerspectiveProto.Portal;
import com.aletheiaware.perspective.PerspectiveProto.Puzzle;
import com.aletheiaware.perspective.PerspectiveProto.Scenery;
import com.aletheiaware.perspective.PerspectiveProto.Sky;
import com.aletheiaware.perspective.PerspectiveProto.Sphere;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.scene.DropAnimation;
import com.aletheiaware.perspective.scene.RotateToAxisAnimation;
import com.aletheiaware.perspective.utils.PerspectiveUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Perspective {

    public interface Callback {
        void onDropComplete();
        void onRotateComplete();
        void onTurnComplete();
        void onGameLost();
        void onGameWon();
        void addSceneGraphNode(String shader, String name, String type, String mesh, String colour, String texture, String material);
    }

    public final float[] down = new float[] {0, -1, 0, 1};
    public final float[] up = new float[] {0, 1, 0, 1};
    public final float[] cameraFrustum = new float[2];
    public final float[] light = new float[4];
    public final float[] temp = new float[4];
    public final Matrix model = new Matrix();
    public final Matrix view = new Matrix();
    public final Matrix projection = new Matrix();
    public final Matrix mv = new Matrix();
    public final Matrix mvp = new Matrix();
    public final Matrix mainRotation = new Matrix();
    public final Matrix inverseRotation = new Matrix();
    public final Matrix tempRotation = new Matrix();
    public final Vector cameraEye = new Vector();
    public final Vector cameraLookAt = new Vector();
    public final Vector cameraUp = new Vector();
    public final Vector outlineScale = new Vector();
    public final Vector skyScale = new Vector();
    public final Vector tempVector = new Vector();

    public final Callback callback;
    public final Scene scene;
    public int size;// Outer dimension of puzzle cube

    public Puzzle puzzle;
    public Solution.Builder solution;
    public Map<String, SceneGraphNode> scenegraphs = new HashMap<>();
    public boolean gameOver = false;
    public boolean gameWon = false;
    public boolean outlineEnabled = true;
    public boolean skyEnabled = true;

    public static class Element {
        public SceneGraphNode root;
        public String name;
        public String mesh;
        public String colour;
        public String texture;
        public String material;
        public String shader;
    }
    // Elements of the puzzle addressed type -> element
    public final Map<String, List<Element>> elements = new HashMap<>();
    // Holds portalA -> portalB and portalB -> portalA
    public final Map<Vector, Vector> linkedPortals = new HashMap<>();
    // Dialogs of the puzzle addressed name -> dialog
    public final Map<String, Dialog> dialogs = new HashMap<>();

    public Perspective(Callback callback, Scene scene, int size) {
        this.callback = callback;
        this.scene = scene;
        this.size = size;

        setSize(size);

        // Outline
        scene.putVector("outline-scale", outlineScale);
        // Sky
        scene.putVector("sky-scale", skyScale);
        // Colours
        for (int i = 0; i < PerspectiveUtils.COLOUR_NAMES.length; i++) {
            scene.putFloatArray(PerspectiveUtils.COLOUR_NAMES[i], PerspectiveUtils.COLOURS[i]);
        }
        // Materials
        for (int i = 0; i < PerspectiveUtils.MATERIAL_NAMES.length; i++) {
            scene.putFloatArray(PerspectiveUtils.MATERIAL_NAMES[i], PerspectiveUtils.MATERIALS[i]);
        }
        // Down
        scene.putFloatArray("down", down);
        // Up
        scene.putFloatArray("up", up);
        // Light
        scene.putFloatArray("light", light);
        // MVP
        scene.putMatrix("model", model.makeIdentity());
        scene.putMatrix("view", view.makeIdentity());
        scene.putMatrix("projection", projection.makeIdentity());
        scene.putMatrix("model-view", mv.makeIdentity());
        scene.putMatrix("model-view-projection", mvp.makeIdentity());
        // Rotation
        scene.putMatrix("main-rotation", mainRotation.makeIdentity());
        scene.putMatrix("inverse-rotation", inverseRotation.makeIdentity());
        scene.putMatrix("temp-rotation", tempRotation.makeIdentity());
        // Camera
        scene.putVector("camera-eye", cameraEye);
        scene.putVector("camera-look-at", cameraLookAt);
        scene.putVector("camera-up", cameraUp);
        scene.putFloatArray("camera-frustum", cameraFrustum);
    }

    public Scene getScene() {
        return scene;
    }

    public String getDefaultShader() {
        for (String shader : new String[]{
            "basic",
            "main",
        }) {
            if (scenegraphs.containsKey(shader)) {
                return shader;
            }
        }
        System.err.println("Default Shader not found: " + scenegraphs.keySet());
        return null;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        System.out.println("Size: " + size);

        // Set the outline scale
        outlineScale.set(size, size, size);
        System.out.println("OutlineScale: " + outlineScale);
        float square = size * size;
        System.out.println("Square: " + square);
        // Set the sky scale
        skyScale.set(square, square, square);
        System.out.println("SkyScale: " + skyScale);

        float distance = square / 2f;
        System.out.println("Distance: " + distance);
        // Ensure light is always outside
        light[0] = 0;
        light[1] = 0;
        light[2] = distance;
        light[3] = 1.0f;
        System.out.println("Light: " + Arrays.toString(light));

        // Ensure camera is always outside
        cameraEye.set(0.0f, 0.0f, distance);
        System.out.println("CameraEye: " + cameraEye);
        // Looking at the center
        cameraLookAt.set(0.0f, 0.0f, 0.0f);
        System.out.println("CameraLookAt: " + cameraLookAt);
        // Head pointing up Y axis
        cameraUp.set(0.0f, 1.0f, 0.0f);
        System.out.println("CameraUp: " + cameraUp);
        // Crop the scene proportionally
        cameraFrustum[0] = size / 2f;
        cameraFrustum[1] = square;
        System.out.println("Frustum: " + Arrays.toString(cameraFrustum));
    }

    public Solution getSolution() {
        return solution.build();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean wasGameWon() {
        return gameWon;
    }

    public List<Element> getElements(String type) {
        List<Element> es = elements.get(type);
        if (es == null) {
            es = new ArrayList<>();
            elements.put(type, es);
        }
        return es;
    }

    public void setOutline(String shader, String mesh, String colour, String texture, String material) {
        if (!outlineEnabled) {
            return;
        }
        if (shader == null || shader.isEmpty()) {
            shader = getDefaultShader();
        }
        System.out.println("Outline " + shader + " : " + mesh + " : " + colour + " : " + texture + " : " + material);
        String name = "o0";
        String type = "outline";
        callback.addSceneGraphNode(shader, name, type, mesh, colour, texture, material);

        List<Element> es = getElements(type);
        Element element = new Element();
        element.name = name;
        element.mesh = mesh;
        element.colour = colour;
        element.texture = texture;
        element.material = material;
        element.shader = shader;
        es.add(element);
    }

    public void addSky(String shader, String name, String mesh, String colour, String texture, String material) {
        if (!skyEnabled) {
            return;
        }
        if (shader == null || shader.isEmpty()) {
            shader = getDefaultShader();
        }
        System.out.println("Sky " + shader + " : " + mesh + " : " + colour + " : " + texture + " : " + material);
        String type = "sky";
        callback.addSceneGraphNode(shader, name, type, mesh, colour, texture, material);

        List<Element> es = getElements(type);
        Element element = new Element();
        element.name = name;
        element.mesh = mesh;
        element.colour = colour;
        element.texture = texture;
        element.material = material;
        element.shader = shader;
        es.add(element);
    }

    public void addElement(String shader, String name, String type, String mesh, Vector location, String colour, String texture, String material) {
        if (shader == null || shader.isEmpty()) {
            shader = getDefaultShader();
        }
        System.out.println("Adding " + shader + " : " + type + " : " + name + " : " + mesh + " : " + location + " : " + colour + " : " + texture + " : " + material);
        scene.putVector(name, location);
        callback.addSceneGraphNode(shader, name, type, mesh, colour, texture, material);

        List<Element> es = getElements(type);
        Element element = new Element();
        element.name = name;
        element.mesh = mesh;
        element.colour = colour;
        element.texture = texture;
        element.material = material;
        element.shader = shader;
        es.add(element);
    }

    public void addDialog(Dialog dialog, Vector location) {
        String name = dialog.getName();
        System.out.println("Adding " + name + " : " + dialog + " : " + location);
        scene.putVector(name, location);
        dialogs.put(name, dialog);
    }

    public void clearLocation(Vector location) {
        System.out.println("Clearing " + location);
        Element element = null;
        for (String t : elements.keySet()) {
            List<Element> es = getElements(t);
            for (Element e : es) {
                Vector v = scene.getVector(e.name);
                if (location.equals(v)) {
                    Vector l = linkedPortals.remove(v);
                    if (l != null) {
                        linkedPortals.remove(l);
                    }
                    element = e;
                    break;
                }
            }
            if (element != null) {
                es.remove(element);
                if (!scenegraphs.get(element.shader).removeChild(element.root)) {
                    System.err.println("Could not remove " + element.name);
                }
                return;
            }
        }
        System.err.println("No elements found at " + location);
    }

    public void clearAllLocations() {
        System.out.println("Clearing all locations");
        elements.clear();
        linkedPortals.clear();
        for (SceneGraphNode scene : scenegraphs.values()) {
            scene.clear();
        }
    }

    public void importPuzzle(Puzzle puzzle) {
        System.out.println("Importing: " + puzzle);
        this.gameOver = false;
        this.gameWon = false;
        this.puzzle = puzzle;
        this.solution = Solution.newBuilder();
        model.makeIdentity();
        mainRotation.makeIdentity();
        inverseRotation.makeIdentity();
        int half = size / 2;

        if (puzzle.hasOutline()) {
            Outline o = puzzle.getOutline();
            setOutline(o.getShader(), o.getMesh(), o.getColour(), o.getTexture(), o.getMaterial());
        }

        for (Sky s : puzzle.getSkyList()) {
            addSky(s.getShader(), s.getName(), s.getMesh(), s.getColour(), s.getTexture(), s.getMaterial());
        }

        for (Block b : puzzle.getBlockList()) {
            Vector v = PerspectiveUtils.locationToVector(b.getLocation()).cap(-half, half);
            addElement(b.getShader(), b.getName(), "block", b.getMesh(), v, b.getColour(), b.getTexture(), b.getMaterial());
        }
        for (Goal g : puzzle.getGoalList()) {
            Vector v = PerspectiveUtils.locationToVector(g.getLocation()).cap(-half, half);
            addElement(g.getShader(), g.getName(), "goal", g.getMesh(), v, g.getColour(), g.getTexture(), g.getMaterial());
        }
        for (Portal p : puzzle.getPortalList()) {
            Vector v = PerspectiveUtils.locationToVector(p.getLocation()).cap(-half, half);
            Vector l = PerspectiveUtils.locationToVector(p.getLink()).cap(-half, half);
            addElement(p.getShader(), p.getName(), "portal", p.getMesh(), v, p.getColour(), p.getTexture(), p.getMaterial());
            linkedPortals.put(v, l);
        }
        for (Sphere s : puzzle.getSphereList()) {
            Vector v = PerspectiveUtils.locationToVector(s.getLocation()).cap(1 - size, size - 1);
            addElement(s.getShader(), s.getName(), "sphere", s.getMesh(), v, s.getColour(), s.getTexture(), s.getMaterial());
        }
        for (Dialog d : puzzle.getDialogList()) {
            Vector v = PerspectiveUtils.locationToVector(d.getLocation()).cap(1 - size, size - 1);
            addDialog(d, v);
        }
        for (Scenery s : puzzle.getSceneryList()) {
            Vector v = PerspectiveUtils.locationToVector(s.getLocation());// Don't cap scenery
            addElement(s.getShader(), s.getName(), "scenery", s.getMesh(), v, s.getColour(), s.getTexture(), s.getMaterial());
        }
    }

    public Puzzle exportPuzzle() {
        Puzzle.Builder pb = Puzzle.newBuilder();
        List<Element> outlines = getElements("outline");
        if (outlines != null && !outlines.isEmpty()) {
            for (Element o : outlines) {
                pb.setOutline(Outline.newBuilder()
                    .setMesh(o.mesh)
                    .setColour(o.colour)
                    .setTexture(o.texture)
                    .setMaterial(o.material)
                    .setShader(o.shader));
            }
        }
        List<Element> skys = getElements("sky");
        if (skys != null && !skys.isEmpty()) {
            for (Element s : skys) {
                pb.addSky(Sky.newBuilder()
                    .setName(s.name)
                    .setMesh(s.mesh)
                    .setColour(s.colour)
                    .setTexture(s.texture)
                    .setMaterial(s.material)
                    .setShader(s.shader));
            }
        }
        List<Element> blocks = getElements("block");
        if (blocks != null) {
            for (Element b : blocks) {
                Vector v = scene.getVector(b.name);
                Location loc = PerspectiveUtils.vectorToLocation(v);
                pb.addBlock(Block.newBuilder()
                    .setName(b.name)
                    .setMesh(b.mesh)
                    .setColour(b.colour)
                    .setLocation(loc)
                    .setTexture(b.texture)
                    .setMaterial(b.material)
                    .setShader(b.shader));
            }
        }
        List<Element> goals = getElements("goal");
        if (goals != null) {
            for (Element g : goals) {
                Vector v = scene.getVector(g.name);
                Location loc = PerspectiveUtils.vectorToLocation(v);
                pb.addGoal(Goal.newBuilder()
                    .setName(g.name)
                    .setMesh(g.mesh)
                    .setColour(g.colour)
                    .setLocation(loc)
                    .setTexture(g.texture)
                    .setMaterial(g.material)
                    .setShader(g.shader));
            }
        }
        List<Element> portals = getElements("portal");
        if (portals != null) {
            for (Element p : portals) {
                Vector v = scene.getVector(p.name);
                Location loc = PerspectiveUtils.vectorToLocation(v);
                Location link = PerspectiveUtils.vectorToLocation(linkedPortals.get(v));
                pb.addPortal(Portal.newBuilder()
                    .setName(p.name)
                    .setMesh(p.mesh)
                    .setColour(p.colour)
                    .setLocation(loc)
                    .setLink(link)
                    .setTexture(p.texture)
                    .setMaterial(p.material)
                    .setShader(p.shader));
            }
        }
        List<Element> spheres = getElements("sphere");
        if (spheres != null) {
            for (Element s : spheres) {
                Vector v = scene.getVector(s.name);
                Location loc = PerspectiveUtils.vectorToLocation(v);
                pb.addSphere(Sphere.newBuilder()
                    .setName(s.name)
                    .setMesh(s.mesh)
                    .setColour(s.colour)
                    .setLocation(loc)
                    .setTexture(s.texture)
                    .setMaterial(s.material)
                    .setShader(s.shader));
            }
        }
        if (dialogs != null) {
            for (Entry<String, Dialog> e : dialogs.entrySet()) {
                String name = e.getKey();
                Dialog d = e.getValue();
                Vector v = scene.getVector(name);
                Location loc = PerspectiveUtils.vectorToLocation(v);
                pb.addDialog(Dialog.newBuilder()
                    .setName(name)
                    .setBackgroundColour(d.getBackgroundColour())
                    .setForegroundColour(d.getForegroundColour())
                    .setAuthor(d.getAuthor())
                    .setContent(d.getContent())
                    .setLocation(loc)
                    .addAllElement(d.getElementList()));
            }
        }
        List<Element> scenerys = getElements("scenery");
        if (scenerys != null) {
            for (Element s : scenerys) {
                Vector v = scene.getVector(s.name);
                Location loc = PerspectiveUtils.vectorToLocation(v);
                pb.addScenery(Scenery.newBuilder()
                    .setName(s.name)
                    .setMesh(s.mesh)
                    .setColour(s.colour)
                    .setLocation(loc)
                    .setTexture(s.texture)
                    .setMaterial(s.material)
                    .setShader(s.shader));
            }
        }
        Puzzle p = pb.build();
        System.out.println("Exporting: " + p);
        return p;
    }

    public void drop() {
        synchronized (scene) {
            if (!scene.hasAnimation()) {
                System.out.println("drop");
                if (inverseRotation.makeInverse(mainRotation)) {
                    // TODO improve this - creating new sets and maps each time is expensive
                    Set<Vector> blocks = new HashSet<>();
                    List<Element> bs = getElements("block");
                    if (bs != null) {
                        for (Element b : bs) {
                            blocks.add(scene.getVector(b.name));
                        }
                    }
                    Set<Vector> goals = new HashSet<>();
                    List<Element> gs = getElements("goal");
                    if (gs != null) {
                        for (Element g : gs) {
                            goals.add(scene.getVector(g.name));
                        }
                    }
                    Map<String, Vector> spheres = new HashMap<>();
                    List<Element> ss = getElements("sphere");
                    if (ss != null) {
                        for (Element s : ss) {
                            spheres.put(s.name, scene.getVector(s.name));
                        }
                    }
                    scene.setAnimation(new DropAnimation(size, inverseRotation, down, blocks, goals, linkedPortals, spheres) {
                        @Override
                        public void onComplete() {
                            boolean gameLost = false;
                            boolean gameWon = true;
                            for (Entry<String, Vector> s : spheres.entrySet()) {
                                String k = s.getKey();
                                Vector v = s.getValue();
                                if (PerspectiveUtils.isOutOfBounds(v, size)) {
                                    // if any spheres are out of bounds - game over
                                    gameLost = true;
                                } else if (!goals.contains(v)) {
                                    // if all spheres are in the goals - game won
                                    gameWon = false;
                                }
                                System.out.println("Move: " + k + " " + v);
                                solution.addMove(Move.newBuilder()
                                        .setKey(k)
                                        .setValue(PerspectiveUtils.vectorToLocation(v))
                                        .build());
                            }
                            if (gameLost) {
                                gameOver = true;
                                gameWon = false;
                                callback.onGameLost();
                            } else if (gameWon) {
                                gameOver = true;
                                gameWon = true;
                                callback.onGameWon();
                            } else {
                                callback.onDropComplete();
                            }
                        }
                    });
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    public void rotate(float x, float y) {
        synchronized (scene) {
            if (!scene.hasAnimation()) {
                System.out.println(String.format("rotate %f, %f", x, y));
                if (inverseRotation.makeInverse(mainRotation)) {
                    if (y != 0) {
                        // Y
                        inverseRotation.multiply(JoyUtils.Y, temp);
                        tempVector.set(temp[0], temp[1], temp[2]);
                        tempRotation.makeRotationAxis(y, tempVector);
                        mainRotation.makeMultiplication(mainRotation, tempRotation);
                    }
                    if (x != 0) {
                        // X
                        inverseRotation.multiply(JoyUtils.X, temp);
                        tempVector.set(temp[0], temp[1], temp[2]);
                        tempRotation.makeRotationAxis(x, tempVector);
                        mainRotation.makeMultiplication(mainRotation, tempRotation);
                    }
                    if (!inverseRotation.makeInverse(mainRotation)) {
                        System.err.println("Matrix invert failed");
                    }
                    callback.onRotateComplete();
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    public void rotateToAxis() {
        synchronized (scene) {
            if (!scene.hasAnimation()) {
                System.out.println("rotateToAxis");
                if (inverseRotation.makeInverse(mainRotation)) {
                    scene.setAnimation(new RotateToAxisAnimation(mainRotation, inverseRotation, tempRotation, cameraEye, cameraUp) {
                        @Override
                        public void onComplete() {
                            solution.setScore(solution.getScore() + 1);
                            callback.onTurnComplete();
                        }
                    });
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    public void turn(int x, int y, int z) {
        synchronized (scene) {
            if (!scene.hasAnimation()) {
                System.out.println(String.format("turn %d, %d, %d", x, y, z));
                if (inverseRotation.makeInverse(mainRotation)) {
                    scene.setAnimation(new RotationAnimation(mainRotation, inverseRotation, tempRotation, 250, (float) Math.PI / 2.0f, x, y, z) {
                        @Override
                        public void onComplete() {
                            callback.onTurnComplete();
                        }
                    });
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }
}
