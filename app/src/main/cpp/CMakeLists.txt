cmake_minimum_required(VERSION 3.22.1)

project("ngrokforwarder")

add_library(
    ngrokforwarder
    SHARED
    native-lib.cpp
    tunnel_forwarder.cpp
    socket_pool.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    ngrokforwarder
    ${log-lib}
)
