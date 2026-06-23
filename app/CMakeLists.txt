cmake_minimum_required(VERSION 3.22.1)
project("pixelcore")
add_library(pixelcore SHARED src/main/cpp/native_bridge.c)
target_link_libraries(pixelcore log)
