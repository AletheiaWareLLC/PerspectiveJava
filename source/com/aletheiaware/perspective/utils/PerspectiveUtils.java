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

    // Colour
    public static final float[] BLACK = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
    public static final float[] BLUE = new float[] {0.168f, 0.332f, 0.613f, 1.0f};// 2B559D - 43,85,157
    public static final float[] BROWN = new float[] {0.5f, 0.26f, 0.16f, 1.0f};// TODO
    public static final float[] DARK_BLUE = new float[] {0.055f, 0.211f, 0.484f, 1.0f};// 0E367C - 14,54,124
    public static final float[] DARK_GREY = new float[] {0.199f, 0.199f, 0.199f, 1.0f};// 333333 - 51,51,51
    public static final float[] GREEN = new float[] {0.062f, 0.484f, 0.281f, 1.0f};// 107C48 - 16,124,72
    public static final float[] GREY = new float[] {0.496f, 0.496f, 0.496f, 1.0f};// 7F7F7F - 127,127,127
    public static final float[] LIGHT_BLUE = new float[] {0.281f, 0.453f, 0.746f, 1.0f};//  4874BF - 72,116,191
    public static final float[] LIGHT_GREY = new float[] {0.797f, 0.797f, 0.797f, 1.0f};// CCCCCC - 204,204,204
    public static final float[] ORANGE = new float[] {0.967f, 0.371f, 0.109f, 1.0f};// F75F1C - 247,95,28
    public static final float[] PURPLE = new float[] {0.531f, 0.117f, 0.891f, 1.0f};// 881EE4 - 136,30,228
    public static final float[] RED = new float[] {0.8f, 0.2f, 0.2f, 1.0f};// TODO
    public static final float[] WHITE = new float[] {1.0f, 1.0f, 1.0f, 1.0f};
    public static final float[] YELLOW = new float[] {0.9f, 0.9f, 0.0f, 1.0f};// TODO
    public static final float[][] COLOURS = {
        BLACK,
        BLUE,
        BROWN,
        DARK_BLUE,
        DARK_GREY,
        GREEN,
        GREY,
        LIGHT_BLUE,
        LIGHT_GREY,
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
        "brown",
        "dark-blue",
        "dark-grey",
        "green",
        "grey",
        "light-blue",
        "light-grey",
        "orange",
        "purple",
        "red",
        "white",
        "yellow",
    };

    private PerspectiveUtils() {}

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
