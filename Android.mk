LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PACKAGE_NAME := VOXXControl
LOCAL_CERTIFICATE := platform
LOCAL_PROGUARD_ENABLED := disabled
include $(BUILD_PACKAGE)
