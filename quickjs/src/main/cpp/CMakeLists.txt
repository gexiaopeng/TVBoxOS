cmake_minimum_required(VERSION 3.10.2)
project(quickjs LANGUAGES C)
#将编译的源码设置到quickjs_src中
set(quickjs_src quickjs.c libunicode.c libregexp.c cutils.c quickjs-libc.c)
#将预编译宏设置到quickjs_def中
set(quickjs_def CONFIG_VERSION="${version}" _GNU_SOURCE)
#条件编译值，这个条件编译宏表示是否编译提供大数功能，后面的NO表示不提供if条件为false
option(QUICKJS_BIGNUM "Compile BigNum support" ON)

#设置生成的so动态库最后输出的路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

#上面编译条件，NO，YES
if(QUICKJS_BIGNUM)
    list(APPEND quickjs_src libbf.c)
    list(APPEND quickjs_def CONFIG_BIGNUM)
endif()

#生成libquick.so静态库
add_library(quickjs SHARED ${quickjs_src})
#将上面quickjs_def定义的宏应用到libquickjs.so这个库中
target_compile_definitions(quickjs PRIVATE ${quickjs_def} )
