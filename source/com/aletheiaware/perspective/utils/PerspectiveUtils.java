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

package com.aletheiaware.perspective.utils;

import com.aletheiaware.common.utils.CommonUtils;
import com.aletheiaware.joy.JoyProto.Shader;
import com.aletheiaware.joy.scene.Vector;
import com.aletheiaware.perspective.PerspectiveProto.Location;
import com.aletheiaware.perspective.PerspectiveProto.Puzzle;
import com.aletheiaware.perspective.PerspectiveProto.Solution;
import com.aletheiaware.perspective.PerspectiveProto.World;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public final class PerspectiveUtils {

    public static final String TAG = "Perspective";

    public static final int MAX_STARS = 5;
    public static final String HASH_DIGEST = "SHA-512";

    // Colour
    public static final float[] BLACK = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] BLUE = new float[] {0.145f, 0.508f, 0.887f, 1.0f};// 2582E3 - 37,130,227
    public static final float[] DARK_BLUE = new float[] {0.129f, 0.457f, 0.797f, 1.0f};// 2175CC - 33,117,204
    public static final float[] DARK_GREEN = new float[] {0.133f, 0.586f, 0.289f, 1.0f};// 22964A - 34,150,74
    public static final float[] DARK_GREY = new float[] {0.199f, 0.199f, 0.199f, 1.0f};
    public static final float[] DARK_ORANGE = new float[] {0.891f, 0.406f, 0.012f, 1.0f};// E46803 - 228,104,3
    public static final float[] DARK_PURPLE = new float[] {0.58f, 0.337f, 0.898f, 1.0f};// 9456E5 - 148,86,229
    public static final float[] DARK_RED = new float[] {0.813f, 0.176f, 0.176f, 1.0f};// D02D2D - 208,45,45
    public static final float[] DARK_YELLOW = new float[] {0.887f, 0.73f, 0.059f, 1.0f};// E3BB0F - 227,187,15
    public static final float[] GREEN = new float[] {0.148f, 0.652f, 0.324f, 1.0f};// 26A753 - 38,167,83
    public static final float[] GREY = new float[] {0.496f, 0.496f, 0.496f, 1.0f};
    public static final float[] LIGHT_BLUE = new float[] {0.227f, 0.555f, 0.895f, 1.0f};// 3A8EE5 - 58,142,229
    public static final float[] LIGHT_GREEN = new float[] {0.23f, 0.684f, 0.391f, 1.0f};// 3BAF64 - 59,175,100
    public static final float[] LIGHT_GREY = new float[] {0.797f, 0.797f, 0.797f, 1.0f};
    public static final float[] LIGHT_ORANGE = new float[] {0.992f, 0.504f, 0.113f, 1.0f};// FE811D - 254,129,29
    public static final float[] LIGHT_PURPLE = new float[] {0.682f, 0.435f, 1.0f, 1.0f};// AE6FFF - 174,111,255
    public static final float[] LIGHT_RED = new float[] {0.914f, 0.273f, 0.277f, 1.0f};// EA4647 - 234,70,71
    public static final float[] LIGHT_YELLOW = new float[] {0.988f, 0.828f, 0.156f, 1.0f};// FDD428 - 253,212,40
    public static final float[] ORANGE = new float[] {0.992f, 0.453f, 0.016f, 1.0f};// FE7404 - 254,116,4
    public static final float[] PURPLE = new float[] {0.647f, 0.376f, 1.0f, 1.0f};// A560FF - 165,96,255
    public static final float[] RED = new float[] {0.906f, 0.195f, 0.199f, 1.0f};// E83233 - 232,50,51
    public static final float[] WHITE = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] YELLOW = new float[] {0.988f, 0.813f, 0.066f, 1.0f};// FDD011 - 253,208,17
    public static final float[][] COLOURS = {
        BLACK,
        BLUE,
        DARK_BLUE,
        DARK_GREEN,
        DARK_GREY,
        DARK_ORANGE,
        DARK_PURPLE,
        DARK_RED,
        DARK_YELLOW,
        GREEN,
        GREY,
        LIGHT_BLUE,
        LIGHT_GREEN,
        LIGHT_GREY,
        LIGHT_ORANGE,
        LIGHT_PURPLE,
        LIGHT_RED,
        LIGHT_YELLOW,
        ORANGE,
        PURPLE,
        RED,
        WHITE,
        YELLOW,
    };
    public static final String DEFAULT_BG_COLOUR = "black";
    public static final String DEFAULT_FG_COLOUR = "white";
    public static final String[] COLOUR_NAMES = {
        "black",
        "blue",
        "dark-blue",
        "dark-green",
        "dark-grey",
        "dark-orange",
        "dark-purple",
        "dark-red",
        "dark-yellow",
        "green",
        "grey",
        "light-blue",
        "light-green",
        "light-grey",
        "light-orange",
        "light-purple",
        "light-red",
        "light-yellow",
        "orange",
        "purple",
        "red",
        "white",
        "yellow",
    };

    // Material
    public static final float[] MATTE = new float[]{0.25f, 0.25f, 0.25f};
    public static final float[] MEDIUM = new float[]{0.5f, 0.5f, 0.5f};
    public static final float[] GLOSSY = new float[]{1.0f, 1.0f, 1.0f};
    public static final float[] AMBIENT_ONLY = new float[]{1.0f, 0.0f, 0.0f};
    public static final float[] DIFFUSE_ONLY = new float[]{0.0f, 1.0f, 0.0f};
    public static final float[] SPECULAR_ONLY = new float[]{0.0f, 0.0f, 1.0f};
    public static final float[][] MATERIALS = {
        MATTE,
        MEDIUM,
        GLOSSY,
        AMBIENT_ONLY,
        DIFFUSE_ONLY,
        SPECULAR_ONLY,
    };
    public static final String[] MATERIAL_NAMES = {
        "matte",
        "medium",
        "glossy",
        "ambient-only",
        "diffuse-only",
        "specular-only",
    };

    // World
    public static final String WORLD_TUTORIAL = "tutorial";
    public static final String WORLD_ONE = "world1";
    public static final String WORLD_TWO = "world2";
    public static final String WORLD_THREE = "world3";
    public static final String WORLD_FOUR = "world4";
    public static final String WORLD_FIVE = "world5";
    public static final String WORLD_SIX = "world6";
    public static final String WORLD_SEVEN = "world7";
    public static final String WORLD_EIGHT = "world8";
    public static final String WORLD_NINE = "world9";
    public static final String WORLD_TEN = "world10";
    public static final String WORLD_ELEVEN = "world11";
    public static final String WORLD_TWELVE = "world12";
    public static final String WORLD_THIRTEEN = "world13";
    public static final String WORLD_FOURTEEN = "world14";
    public static final String WORLD_FIFTEEN = "world15";
    public static final String[] FREE_WORLDS = {
            WORLD_TUTORIAL,
            WORLD_ONE,
            WORLD_TWO,
            WORLD_THREE,
            WORLD_FOUR,
            WORLD_FIVE,
            WORLD_SIX,
    };
    public static final String[] PAID_WORLDS = {
            WORLD_SEVEN,
            WORLD_EIGHT,
            WORLD_NINE,
            WORLD_TEN,
            WORLD_ELEVEN,
            WORLD_TWELVE,
            WORLD_THIRTEEN,
            WORLD_FOURTEEN,
            WORLD_FIFTEEN,
    };

    private PerspectiveUtils() {}

    public static int scoreToStars(int score, int target) {
        return Math.min(MAX_STARS, Math.max(MAX_STARS - (score - target), 0));
    }

    public static boolean isTutorial(String worldName) {
        return WORLD_TUTORIAL.equals(worldName);
    }

    public static String getHash(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(HASH_DIGEST);
        digest.reset();
        return new String(CommonUtils.encodeBase64URL(digest.digest(data)));
    }

    public static Puzzle getPuzzle(World world, int puzzle) {
        int index = puzzle - 1;
        if (index >= 0 && index < world.getPuzzleCount()) {
            return world.getPuzzle(index);
        }
        return null;
    }

    public static Puzzle readPuzzle(File file) throws IOException {
        FileInputStream in = null;
        Puzzle puzzle = null;
        try {
            System.out.println("Reading: " + file.getName());
            in = new FileInputStream(file);
            puzzle = readPuzzle(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    /* Ignored */
                }
            }
        }
        return puzzle;
    }

    public static Puzzle readPuzzle(InputStream in) throws IOException {
        return Puzzle.parseDelimitedFrom(in);
    }

    public static void writePuzzle(File file, Puzzle puzzle) throws IOException {
        FileOutputStream out = null;
        try {
            System.out.println("Writing: " + file.getName());
            out = new FileOutputStream(file);
            writePuzzle(out, puzzle);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    /* Ignored */
                }
            }
        }
    }

    public static void writePuzzle(OutputStream out, Puzzle puzzle) throws IOException {
        puzzle.writeDelimitedTo(out);
    }

    public static void saveSolution(File root, String world, String puzzle, Solution solution) throws IOException {
        File directory = new File(new File(root, "solutions"), world);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Could not create directory: " + directory.getAbsolutePath());
            }
        }
        File file = new File(directory, puzzle + ".pb");
        if (file.exists()) {
            Solution s = readSolution(file);
            // Only overwrite existing solution if new solution has better (lower) score
            if (s.getScore() < solution.getScore()) {
                return;
            }
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            solution.writeDelimitedTo(out);
        }
    }

    public static Solution loadSolution(File root, String world, String puzzle) throws IOException {
        File directory = new File(new File(root, "solutions"), world);
        if (directory.exists()) {
            File file = new File(directory, puzzle + ".pb");
            if (file.exists()) {
                return readSolution(file);
            }
        }
        return null;
    }

    public static void clearSolutions(File root) throws IOException {
        File directory = new File(root, "solutions");
        if (!CommonUtils.recursiveDelete(directory)) {
            throw new IOException("Could not delete directory: " + directory.getAbsolutePath());
        }
    }

    public static Solution readSolution(File file) throws IOException {
        FileInputStream in = null;
        Solution solution = null;
        try {
            System.out.println("Reading: " + file.getName());
            in = new FileInputStream(file);
            solution = readSolution(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    /* Ignored */
                }
            }
        }
        return solution;
    }

    public static Solution readSolution(InputStream in) throws IOException {
        return Solution.parseDelimitedFrom(in);
    }

    public static void writeSolution(File file, Solution solution) throws IOException {
        FileOutputStream out = null;
        try {
            System.out.println("Writing: " + file.getName());
            out = new FileOutputStream(file);
            writeSolution(out, solution);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    /* Ignored */
                }
            }
        }
    }

    public static void writeSolution(OutputStream out, Solution solution) throws IOException {
        solution.writeDelimitedTo(out);
    }

    public static World readWorld(File file) throws IOException {
        FileInputStream in = null;
        World world = null;
        try {
            System.out.println("Reading: " + file.getName());
            in = new FileInputStream(file);
            world = readWorld(in);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    /* Ignored */
                }
            }
        }
        return world;
    }

    public static World readWorld(InputStream in) throws IOException {
        return World.parseDelimitedFrom(in);
    }

    public static void writeWorld(File file, World world) throws IOException {
        FileOutputStream out = null;
        try {
            System.out.println("Writing: " + file.getName());
            out = new FileOutputStream(file);
            writeWorld(out, world);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    /* Ignored */
                }
            }
        }
    }

    public static void writeWorld(OutputStream out, World world) throws IOException {
        world.writeDelimitedTo(out);
    }

    public static Vector getNextCell(Vector result, Vector position, float x, float y, float z) {
        result.setX(position.getX() + x);
        result.setY(position.getY() + y);
        result.setZ(position.getZ() + z);
        return result;
    }

    public static Vector getNextCell(Vector result, Vector position, Vector axis) {
        return getNextCell(result, position,  axis.getX(), axis.getY(), axis.getZ());
    }

    public static boolean isCellCenter(Vector position) {
        float x = position.getX() % 1.0f;
        float y = position.getY() % 1.0f;
        float z = position.getZ() % 1.0f;
        return x == 0 && y == 0 && z == 0;
    }

    public static boolean isOutOfBounds(Vector vector, float size) {
        return Math.abs(vector.getX()) > size
                || Math.abs(vector.getY()) > size
                || Math.abs(vector.getZ()) > size;
    }

    public static Location vectorToLocation(Vector v) {
        return Location.newBuilder()
            .setX((int) v.getX())
            .setY((int) v.getY())
            .setZ((int) v.getZ())
            .build();
    }

    public static Vector locationToVector(Location l) {
        return new Vector(l.getX(), l.getY(), l.getZ());
    }

}
