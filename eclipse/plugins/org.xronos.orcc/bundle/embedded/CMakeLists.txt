if(NO_DISPLAY)
add_definitions(-DNO_DISPLAY)
else()
find_package(SDL)
endif()

include_directories(
${EMBEDDEDCPP_INCLUDE_DIR}
if(NOT NO_DISPLAY)
${SDL_INCLUDE_DIR}
endif()
)

add_library(EmbeddedCPP STATIC
#include files
include/actor.h
include/condition.h
include/config_parser.h
include/ethernet.h
include/fifo.h
include/get_opt.h
include/interface.h
include/mutex.h
include/receiver.h
include/scheduler.h
include/sender.h
include/thread.h
include/thread_pool.h
include/timer.h
#source files
src/condition.cpp
src/display.cpp
src/ethernet.cpp
src/get_opt.cpp
src/mutex.cpp
src/source.cpp
src/timer.cpp
src/thread.cpp
src/writer.cpp
src/ddr.cpp
)

