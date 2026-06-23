cmake_minimum_required(VERSION 3.22.1)
project("shield")
add_library(shield SHARED src/main/cpp/shield.c)
target_link_libraries(shield log)
