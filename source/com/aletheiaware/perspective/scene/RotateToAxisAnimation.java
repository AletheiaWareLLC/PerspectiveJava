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

public class RotateToAxisAnimation extends Animation {

    private static final float MAX_ANGLE = (float) Math.PI / 20.0f;
    private static final float[][] AXES = { JoyUtils.X, JoyUtils.Y, JoyUtils.Z };
    private final float[][] axes = new float[3][4];
    private final Vector[] vectors = new Vector[3];
    private final Vector cameraEye;
    private final Vector cameraUp;
    private final Matrix mainRotation;
    private final Matrix inverseRotation;
    private final Matrix tempRotation;
    private final int closestAxisIndexEye;
    private final int closestAxisIndexUp;
    private final float closestAxisSignEye;
    private final float closestAxisSignUp;

    public RotateToAxisAnimation(Matrix mainRotation, Matrix inverseRotation, Matrix tempRotation, Vector cameraEye, Vector cameraUp) {
        super();
        this.mainRotation = mainRotation;
        this.inverseRotation = inverseRotation;
        this.tempRotation = tempRotation;
        this.cameraEye = cameraEye.clone().normalize();
        this.cameraUp = cameraUp.normalize();

        float[] dotEye = new float[3];
        float[] dotUp = new float[3];
        float[] absDotEye = new float[3];
        float[] absDotUp = new float[3];
        // For each axis,
        for (int i = 0; i < 3; i++) {
            // Multiply by the invert rotation matrix,
            inverseRotation.multiply(AXES[i], axes[i]);
            // Make into a normalized vector,
            vectors[i] = new Vector();
            vectors[i].set(axes[i]);
            vectors[i].normalize();
            // And calculate how far it is from the camera vectors
            dotEye[i] = cameraEye.dot(vectors[i]);
            dotUp[i] = cameraUp.dot(vectors[i]);
            absDotEye[i] = Math.abs(dotEye[i]);
            absDotUp[i] = Math.abs(dotUp[i]);
        }

        // System.out.println("Dot Eye " + Arrays.toString(dotEye));
        // System.out.println("Dot Up " + Arrays.toString(dotUp));
        // System.out.println("Abs Dot Eye " + Arrays.toString(absDotEye));
        // System.out.println("Abs Dot Up " + Arrays.toString(absDotUp));

        // Determine which is the closest to camera eye
        if (absDotEye[0] > absDotEye[1] && absDotEye[0] > absDotEye[2]) {
            closestAxisIndexEye = 0;
            absDotUp[0] = 0;// Make sure X cannot win up axis as well
        } else if (absDotEye[1] > absDotEye[2]) {
            closestAxisIndexEye = 1;
            absDotUp[1] = 0;// Make sure Y cannot win up axis as well
        } else {
            closestAxisIndexEye = 2;
            absDotUp[2] = 0;// Make sure Z cannot win up axis as well
        }

        // Determine which is the closest to camera up
        if (absDotUp[0] > absDotUp[1] && absDotUp[0] > absDotUp[2]) {
            closestAxisIndexUp = 0;
        } else if (absDotUp[1] > absDotUp[2]) {
            closestAxisIndexUp = 1;
        } else {
            closestAxisIndexUp = 2;
        }

        // System.out.println("Closest Eye Axis Index: " + closestAxisIndexEye);
        // System.out.println("Closest Up Axis Index: " + closestAxisIndexUp);

        closestAxisSignEye = Math.signum(dotEye[closestAxisIndexEye]);
        closestAxisSignUp = Math.signum(dotUp[closestAxisIndexUp]);

        // System.out.println("Closest Eye Axis Sign: " + closestAxisSignEye);
        // System.out.println("Closest Up Axis Sign: " + closestAxisSignUp);
    }

    @Override
    public boolean tick() {
        // Camera Eye
        if (!inverseRotation.makeInverse(mainRotation)) {
            System.err.println("Matrix invert failed");
        }
        inverseRotation.multiply(AXES[closestAxisIndexEye], axes[closestAxisIndexEye]);
        vectors[closestAxisIndexEye].set(axes[closestAxisIndexEye]);
        vectors[closestAxisIndexEye].scale(closestAxisSignEye);

        float angleEye = vectors[closestAxisIndexEye].angle(cameraEye);
        // System.out.println("Eye Angle: " + angleEye);

        angleEye = Math.min(MAX_ANGLE, angleEye);
        // System.out.println("Capped Eye Angle: " + angleEye);

        if (angleEye != 0) {
            Vector axisEye = cameraEye.cross(vectors[closestAxisIndexEye]);
            // System.out.println("Eye Axis: " + axisEye);

            tempRotation.makeRotationAxis(angleEye, axisEye);
            mainRotation.makeMultiplication(mainRotation, tempRotation);
        }

        // Camera Up
        if (!inverseRotation.makeInverse(mainRotation)) {
            System.err.println("Matrix invert failed");
        }
        inverseRotation.multiply(AXES[closestAxisIndexUp], axes[closestAxisIndexUp]);
        vectors[closestAxisIndexUp].set(axes[closestAxisIndexUp]);
        vectors[closestAxisIndexUp].scale(closestAxisSignUp);

        float angleUp = vectors[closestAxisIndexUp].angle(cameraUp);
        // System.out.println("Up Angle: " + angleUp);

        angleUp = Math.min(MAX_ANGLE, angleUp);
        // System.out.println("Capped Up Angle: " + angleUp);

        if (angleUp != 0) {
            Vector axisUp = cameraUp.cross(vectors[closestAxisIndexUp]);
            // System.out.println("Up Axis: " + axisUp);

            tempRotation.makeRotationAxis(angleUp, axisUp);
            mainRotation.makeMultiplication(mainRotation, tempRotation);
        }

        return angleEye == 0 && angleUp == 0;
    }
}
