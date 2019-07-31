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

import com.aletheiaware.joy.scene.AttributeNode;
import com.aletheiaware.joy.scene.Matrix;
import com.aletheiaware.joy.scene.RotationAnimation;
import com.aletheiaware.joy.scene.ScaleNode;
import com.aletheiaware.joy.scene.Scene;
import com.aletheiaware.joy.scene.SceneGraphNode;
import com.aletheiaware.joy.scene.TranslateNode;
import com.aletheiaware.joy.scene.Vector;
import com.aletheiaware.joy.utils.JoyUtils;
import com.aletheiaware.perspective.PerspectiveProto.Block;
import com.aletheiaware.perspective.PerspectiveProto.Location;
import com.aletheiaware.perspective.PerspectiveProto.Goal;
import com.aletheiaware.perspective.PerspectiveProto.Move;
import com.aletheiaware.perspective.PerspectiveProto.Outline;
import com.aletheiaware.perspective.PerspectiveProto.Portal;
import com.aletheiaware.perspective.PerspectiveProto.Puzzle;
import com.aletheiaware.perspective.PerspectiveProto.Sphere;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.scene.DropAnimation;
import com.aletheiaware.perspective.scene.RotateToAxisAnimation;
import com.aletheiaware.perspective.utils.PerspectiveUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class Perspective {

    public final float[] down = new float[] {0, -1, 0, 1};
    public final float[] guide = new float[] {0, 0, 0, 1};
    public final float[] frustum = new float[2];
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
    public final Vector guidePosition = new Vector();
    public final Vector tempVector = new Vector();

    public final Scene scene;
    public int size;// Outer dimension of puzzle cube
    public boolean guideShown = false;

    public Puzzle puzzle;
    public Solution.Builder solution;
    public SceneGraphNode basicRotation;
    public SceneGraphNode lineRotation;

    public static class Element {
        public SceneGraphNode root;
        public String name;
        public String mesh;
        public String colour;
    }
    // Elements of the puzzle addressed type -> element
    public final Map<String, List<Element>> elements = new HashMap<>();
    // Holds portalA -> portalB and portalB -> portalA
    public final Map<Vector, Vector> linkedPortals = new HashMap<>();

    public Perspective(Scene scene, int size) {
        this.scene = scene;
        this.size = size;

        setSize(size);

        // Outline
        scene.putVector("outline-scale", outlineScale);
        scene.putFloatArray("outline-colour", PerspectiveUtils.WHITE);
        // Colours
        for (int i = 0; i < PerspectiveUtils.COLOUR_NAMES.length; i++) {
            scene.putFloatArray(PerspectiveUtils.COLOUR_NAMES[i], PerspectiveUtils.COLOURS[i]);
        }
        // Down
        scene.putFloatArray("down", down);
        // Frustum
        scene.putFloatArray("frustum", frustum);
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
        // Guide
        scene.putFloatArray("guide", guide);
        scene.putFloatArray("guide-colour", PerspectiveUtils.BLUE);
    }

    public Scene getScene() {
        return scene;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
        // Set the outline big enough
        outlineScale.set(size, size, size);
        float distance = (size * size) / 2f;
        System.out.println("Distance: " + distance);
        // Crop the scene proportionally
        frustum[0] = size * 0.5f;
        frustum[1] = distance + size;
        // Ensure light is always outside
        light[0] = 0;
        light[1] = 0;
        light[2] = size / 2f;
        light[3] = 1.0f;
        // Ensure camera is always outside
        cameraEye.set(0.0f, 0.0f, distance);
        // Looking at the center
        cameraLookAt.set(0.0f, 0.0f, 0.0f);
        // Head pointing up Y axis
        cameraUp.set(0.0f, 1.0f, 0.0f);
    }

    public Solution getSolution() {
        return solution.build();
    }

    public abstract void onDropComplete();

    public abstract void onTurnComplete();

    public abstract void onGameLost();

    public abstract void onGameWon();

    public abstract SceneGraphNode getSceneGraphNode(String program, String name, String type, String mesh);

    public abstract AttributeNode getAttributeNode(String program, String name, String type, String colour);

    public List<Element> getElements(String type) {
        List<Element> es = elements.get(type);
        if (es == null) {
            es = new ArrayList<>();
            elements.put(type, es);
        }
        return es;
    }

    public void setOutline(String program, String mesh, String colour) {
        if (lineRotation == null) {
            return;
        }
        System.out.println("Outline " + program + " : " + mesh + " : " + colour);
        String name = "outline0";
        String type = "outline";
        ScaleNode outlineScale = new ScaleNode("outline-scale");
        lineRotation.addChild(outlineScale);
        AttributeNode attributeNode = getAttributeNode(program, name, type, colour);
        System.out.println(attributeNode);
        System.out.println(java.util.Arrays.toString(attributeNode.getAttributes()));
        outlineScale.addChild(attributeNode);
        attributeNode.addChild(getSceneGraphNode(program, name, type, mesh));

        List<Element> es = getElements(type);
        Element element = new Element();
        element.name = name;
        element.colour = colour;
        element.mesh = mesh;
        es.add(element);
    }

    public void setGuide(String program, String mesh, String colour) {
        if (lineRotation == null) {
            return;
        }
        System.out.println("Guide " + program + " : " + mesh + " : " + colour);
        String name = "guide0";
        String type = "guide";
        TranslateNode guideTranslate = new TranslateNode("guide");
        lineRotation.addChild(guideTranslate);
        AttributeNode attributeNode = getAttributeNode(program, name, type, colour);
        System.out.println(attributeNode);
        System.out.println(java.util.Arrays.toString(attributeNode.getAttributes()));
        guideTranslate.addChild(attributeNode);
        attributeNode.addChild(getSceneGraphNode(program, name, type, mesh));

        List<Element> es = getElements(type);
        Element element = new Element();
        element.name = name;
        element.colour = colour;
        element.mesh = mesh;
        es.add(element);
    }

    public void addElement(String program, String name, String type, String mesh, Vector location, String colour) {
        if (basicRotation == null) {
            return;
        }
        System.out.println("Adding " + program + " : " + type + " : " + name + " : " + mesh + " : " + location + " : " + colour);
        TranslateNode translateNode = new TranslateNode(name);
        basicRotation.addChild(translateNode);
        AttributeNode attributeNode = getAttributeNode(program, name, type, colour);
        translateNode.addChild(attributeNode);
        attributeNode.addChild(getSceneGraphNode(program, name, type, mesh));

        List<Element> es = getElements(type);
        Element element = new Element();
        element.name = name;
        element.colour = colour;
        element.mesh = mesh;
        es.add(element);
        scene.putVector(name, location);
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
                if (!basicRotation.removeChild(element.root)) {
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
        basicRotation.clear();
    }

    public void reset() {
        clearAllLocations();
        if (lineRotation != null) {
            lineRotation.clear();
        }
        importPuzzle(puzzle);
    }

    public void importPuzzle(Puzzle puzzle) {
        System.out.println("Importing: " + puzzle);
        this.puzzle = puzzle;
        this.solution = Solution.newBuilder();
        int half = size / 2;

        if (puzzle.hasOutline()) {
            Outline o = puzzle.getOutline();
            setOutline("line", o.getMesh(), o.getColour());
        } else {
            setOutline("line", "box", "white");
        }

        for (Block b : puzzle.getBlockList()) {
            Vector v = PerspectiveUtils.locationToVector(b.getLocation()).cap(-half, half);
            // "block", "light-grey"
            addElement("basic", b.getName(), "block", b.getMesh(), v, b.getColour());
        }
        for (Goal g : puzzle.getGoalList()) {
            Vector v = PerspectiveUtils.locationToVector(g.getLocation()).cap(-half, half);
            // "goal", "green"// TODO make goal yellow gold
            addElement("basic", g.getName(), "goal", g.getMesh(), v, g.getColour());
        }
        for (Portal p : puzzle.getPortalList()) {
            Vector v = PerspectiveUtils.locationToVector(p.getLocation()).cap(-half, half);
            Vector l = PerspectiveUtils.locationToVector(p.getLink()).cap(-half, half);
            // "portal", "blue"
            addElement("basic", p.getName(), "portal", p.getMesh(), v, p.getColour());
            linkedPortals.put(v, l);
        }
        for (Sphere s : puzzle.getSphereList()) {
            Vector v = PerspectiveUtils.locationToVector(s.getLocation()).cap(1 - size, size - 1);
            // "sphere", "green"
            addElement("basic", s.getName(), "sphere", s.getMesh(), v, s.getColour());
        }
    }

    public Puzzle exportPuzzle() {
        Puzzle.Builder pb = Puzzle.newBuilder();
        List<Element> outlines = getElements("outline");
        if (outlines != null) {
            for (Element o : outlines) {
                pb.setOutline(Outline.newBuilder()
                    .setMesh(o.mesh)
                    .setColour(o.colour));
            }
        } else {
            pb.setOutline(Outline.newBuilder()
                .setMesh("box")
                .setColour("white"));
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
                    .setLocation(loc));
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
                    .setLocation(loc));
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
                    .setLink(link));
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
                    .setLocation(loc));
            }
        }
        Puzzle p = pb.build();
        System.out.println("Exporting: " + p);
        return p;
    }

    public void drop() {
        synchronized (basicRotation) {
            if (!basicRotation.hasAnimation()) {
                System.out.println("drop");
                if (inverseRotation.makeInverse(mainRotation)) {
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
                    basicRotation.setAnimation(new DropAnimation(size, inverseRotation, down, blocks, goals, linkedPortals, spheres) {
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
                                onGameLost();
                            } else if (gameWon) {
                                onGameWon();
                            } else {
                                onDropComplete();
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
        synchronized (basicRotation) {
            if (!basicRotation.hasAnimation()) {
                System.out.println(String.format("rotate %f, %f", x, y));
                if (inverseRotation.makeInverse(mainRotation)) {
                    // Y
                    inverseRotation.multiply(JoyUtils.Y, temp);
                    tempVector.set(temp[0], temp[1], temp[2]);
                    tempRotation.makeRotationAxis(y, tempVector);
                    mainRotation.makeMultiplication(mainRotation, tempRotation);
                    // X
                    inverseRotation.multiply(JoyUtils.X, temp);
                    tempVector.set(temp[0], temp[1], temp[2]);
                    tempRotation.makeRotationAxis(x, tempVector);
                    mainRotation.makeMultiplication(mainRotation, tempRotation);
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    public void rotateToAxis() {
        synchronized (basicRotation) {
            if (!basicRotation.hasAnimation()) {
                System.out.println("rotateToAxis");
                if (inverseRotation.makeInverse(mainRotation)) {
                    basicRotation.setAnimation(new RotateToAxisAnimation(mainRotation, inverseRotation, tempRotation, cameraEye, cameraUp) {
                        @Override
                        public void onComplete() {
                            onTurnComplete();
                        }
                    });
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    public void turn(int x, int y, int z) {
        synchronized (basicRotation) {
            if (!basicRotation.hasAnimation()) {
                System.out.println(String.format("turn %d, %d, %d", x, y, z));
                if (inverseRotation.makeInverse(mainRotation)) {
                    basicRotation.setAnimation(new RotationAnimation(mainRotation, inverseRotation, tempRotation, 250, (float) Math.PI / 2.0f, x, y, z) {
                        @Override
                        public void onComplete() {
                            onTurnComplete();
                        }
                    });
                } else {
                    System.err.println("Matrix invert failed");
                }
            }
        }
    }

    public void move(float[] m) {
        System.out.println("Move: " + java.util.Arrays.toString(m));
        if (inverseRotation.makeInverse(mainRotation)) {
            float[] move = new float[4];
            inverseRotation.multiply(m, move);
            JoyUtils.round(move);
            System.out.println("Move Axis: " + java.util.Arrays.toString(move));
            guide[0] += move[0];
            guide[1] += move[1];
            guide[2] += move[2];
            System.out.println("Guide: " + java.util.Arrays.toString(guide));
        } else {
            System.err.println("Matrix invert failed");
        }
    }

}
