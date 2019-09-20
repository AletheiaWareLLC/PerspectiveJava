PerspectiveJava
===============

This is a Java implementation of Perspective based on Joy.

Setup
=====
Libraries

    mkdir libs
    ln -s <joyjavalib> libs/JoyJava.jar
    ln -s <protolib> libs/protobuf-lite-3.0.1.jar

Protocol Buffers

    cd <path/to/Perspective>
    ./build.sh --java_out=lite:<path/to/PerspectiveJava>/source/

Build
=====

    ./build.sh
