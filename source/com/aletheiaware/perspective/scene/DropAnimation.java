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

package com.aletheiaware.perspective.scene;

import com.aletheiaware.joy.scene.Animation;
import com.aletheiaware.joy.scene.Matrix;
import com.aletheiaware.joy.scene.Vector;
import com.aletheiaware.joy.utils.JoyUtils;
import com.aletheiaware.perspective.utils.PerspectiveUtils;

import java.util.Map;
import java.util.Set;

public class DropAnimation extends Animation {

    private static final float ACCELERATION = 9.81f;
    private static final float INCREMENT = 0.1f;

    private final Vector dest = new Vector();
    private final Vector temp = new Vector();
    private final float[] dropAxis = new float[4];
    private final float size;
    private final Set<Vector> blocks;
    private final Set<Vector> goals;
    private final Map<Vector, Vector> portals;
    private final Map<String, Vector> spheres;
    private long start = -1;

    public DropAnimation(float size, Matrix inverseRotation, float[] axis, Set<Vector> blocks, Set<Vector> goals, Map<Vector, Vector> portals, Map<String, Vector> spheres) {
        super();
        this.size = size;
        this.blocks = blocks;
        this.goals = goals;
        this.portals = portals;
        this.spheres = spheres;

        inverseRotation.multiply(axis, dropAxis);
        System.out.println("Drop Axis A: " + java.util.Arrays.toString(dropAxis));
        JoyUtils.round(dropAxis);
        System.out.println("Drop Axis B: " + java.util.Arrays.toString(dropAxis));
    }

    public void setStart(long start) {
        this.start = start;
    }

    @Override
    public boolean tick() {
        if (start < 0) {
            start = System.currentTimeMillis();
        }
        if (spheres.isEmpty()) {
            return true;
        }
        // TODO this works for one position, now need to refactor for multiple
        Vector position = null;
        for (Vector v : spheres.values()) {
            position = v;// Hack - gets the last in the set
        }

        System.out.println("Drop axis: " + java.util.Arrays.toString(dropAxis));
        float progress = (System.currentTimeMillis() - start) / 1000.0f;// Time to seconds
        System.out.println("Time: " + progress);
        // TODO SUVAT
        // S = ?
        // U = 0
        // V = ?
        // A = gravity
        // T = progress
        // Solve for S (distance)
        // S = (U * T) + (0.5 * A * T * T)
        float distance = (0 * progress) + (0.5f * ACCELERATION * progress * progress);
        System.out.println("Distance: " + distance);
        for (float i = 0; i < distance; i += INCREMENT) {
            dest.setX(position.getX() + (INCREMENT * dropAxis[0]));
            dest.setY(position.getY() + (INCREMENT * dropAxis[1]));
            dest.setZ(position.getZ() + (INCREMENT * dropAxis[2]));
            dest.round(1);// Round to 1 decimal place
            if (PerspectiveUtils.isCellCenter(dest)) {
                // Double size so ball is offscreen, well out of bounds
                if (PerspectiveUtils.isOutOfBounds(dest, size * 2)) {
                    System.out.println("Ball out of bounds");
                    position.set(dest);
                    return true;
                }

                if (goals != null && goals.contains(dest)) {
                    System.out.println("Ball in Goal");
                    position.set(dest);
                    return true;
                }

                if (portals != null && portals.containsKey(dest)) {
                    Vector prev = dest.clone();
                    dest.set(portals.get(dest));
                    System.out.println("Ball moved through Portal at " + prev + " to " + dest);
                    // move ball to paired portal
                    position.set(dest);
                }
            }

            if (blocks != null && PerspectiveUtils.isCellCenter(position) && blocks.contains(PerspectiveUtils.getNextCell(temp, position, dropAxis[0], dropAxis[1], dropAxis[2]))) {
                System.out.println("Ball stopped at Block");
                // TODO handle bounce
                return true;
            }

            System.out.println("Ball moved to " + dest);
            position.set(dest);
        }
        return false;
    }
}
