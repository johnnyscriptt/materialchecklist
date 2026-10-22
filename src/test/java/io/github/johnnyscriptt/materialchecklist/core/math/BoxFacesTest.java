package io.github.johnnyscriptt.materialchecklist.core.math;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoxFacesTest {

    @Test
    void boxStraightAheadAlongPositiveXShowsOnlyItsNearSide() {
        // Camera at the origin, box two blocks down +X: only the -X face points back at the eye.
        assertEquals(BoxFaces.MIN_X, BoxFaces.facingCamera(2, -0.5, -0.5, 3, 0.5, 0.5));
    }

    @Test
    void boxStraightAheadAlongNegativeZShowsOnlyItsNearSide() {
        assertEquals(BoxFaces.MAX_Z, BoxFaces.facingCamera(-0.5, -0.5, -4, 0.5, 0.5, -3));
    }

    @Test
    void boxBelowAndOffToTheSideShowsTopAndOneSide() {
        // Looking down at a chest one block below and ahead: its lid and its near side.
        int faces = BoxFaces.facingCamera(1, -2, -0.5, 2, -1, 0.5);
        assertEquals(BoxFaces.MIN_X | BoxFaces.MAX_Y, faces);
    }

    @Test
    void boxOffOnADiagonalShowsThreeFaces() {
        int faces = BoxFaces.facingCamera(1, 1, 1, 2, 2, 2);
        assertEquals(BoxFaces.MIN_X | BoxFaces.MIN_Y | BoxFaces.MIN_Z, faces);
    }

    @Test
    void cameraInsideTheBoxSeesEveryFace() {
        assertEquals(BoxFaces.ALL, BoxFaces.facingCamera(-1, -1, -1, 1, 1, 1));
    }

    @Test
    void cameraExactlyInAFacePlaneDoesNotDrawThatEdgeOnFace() {
        // Level with the lid: the top face is seen edge-on and contributes nothing, so it is
        // left out rather than drawn as a zero-width sliver.
        assertEquals(BoxFaces.MIN_X, BoxFaces.facingCamera(1, -1, -0.5, 2, 0, 0.5));
    }
}
