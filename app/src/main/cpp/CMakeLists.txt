cmake_minimum_required(VERSION 3.18.1)
project("nossl")

find_library(log-lib log)

find_package(shadowhook REQUIRED CONFIG)

add_library(nossl SHARED nossl.cpp)

target_link_libraries(nossl shadowhook::shadowhook ${log-lib})
