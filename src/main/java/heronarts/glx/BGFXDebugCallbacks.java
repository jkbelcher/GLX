package heronarts.glx;

import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.bgfx.*;
import org.lwjgl.system.MemoryStack;

public class BGFXDebugCallbacks {
  private BGFXCallbackInterface callbackInterface;
  private BGFXCallbackVtbl vtbl;

  // Individual callback implementations
  private BGFXFatalCallback fatalCallback;
  private BGFXTraceVarArgsCallback traceCallback;

  private BGFXProfilerBegin profilerBegin;
  private BGFXProfilerEnd profilerEnd;

  private BGFXCacheReadSizeCallback cacheReadSizeCallback;
  private BGFXCacheReadCallback cacheReadCallback;
  private BGFXCacheWriteCallback cacheWriteCallback;
  private BGFXScreenShotCallback screenShotCallback;
  private BGFXCaptureBeginCallback captureBeginCallback;
  private BGFXCaptureEndCallback captureEndCallback;
  private BGFXCaptureFrameCallback captureFrameCallback;

  public BGFXCallbackInterface create() {
    try (MemoryStack stack = stackPush()) {
      // Create the callback interface and vtbl
      callbackInterface = BGFXCallbackInterface.malloc();
      vtbl = BGFXCallbackVtbl.malloc();

      // Create individual callbacks
      fatalCallback =
          BGFXFatalCallback.create(
              (thisPtr, filePath, line, code, str) -> {
                String file = memUTF8(filePath);
                String message = memUTF8(str);
                System.err.printf(
                    "BGFX Fatal Error [%d] at %s:%d - %s%n", code, file, line, message);
                throw new RuntimeException("BGFX Fatal Error: " + message);
              });

      traceCallback =
          BGFXTraceVarArgsCallback.create(
              (thisPtr, filePath, line, format, args) -> {
                System.out.println("TRACE CALLBACK INVOKED!");
                String file = memUTF8(filePath);
                String formatStr = memUTF8(format);
                // Extract just the filename from path
                String fileName =
                    file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1);
                System.out.printf("[BGFX] %s:%d - %s%n", fileName, line, formatStr);
              });

      profilerBegin =
          BGFXProfilerBegin.create(
              (thisPtr, name, abgr, filePath, line) -> {
                String nameStr = memUTF8(name);
                System.out.println("Profiler begin: " + nameStr);
              });

      profilerEnd =
          BGFXProfilerEnd.create(
              (thisPtr) -> {
                System.out.println("Profiler end");
              });

      /*
           BGFXFatalCallbackI fatal,
           BGFXTraceVarArgsCallbackI trace_vargs,
           BGFXProfilerBeginI profiler_begin,
           BGFXProfilerBeginLiteralI profiler_begin_literal,
           BGFXProfilerEndI profiler_end,
           BGFXCacheReadSizeCallbackI cache_read_size,
           BGFXCacheReadCallbackI cache_read,
           BGFXCacheWriteCallbackI cache_write,
           BGFXScreenShotCallbackI screen_shot,
           BGFXCaptureBeginCallbackI capture_begin,
           BGFXCaptureEndCallbackI capture_end,
           BGFXCaptureFrameCallbackI capture_frame
      */

      // Create no-op callbacks for unused functionality
      cacheReadSizeCallback = BGFXCacheReadSizeCallback.create((thisPtr, id) -> 0);
      cacheReadCallback = BGFXCacheReadCallback.create((thisPtr, id, data, size) -> false);
      cacheWriteCallback = BGFXCacheWriteCallback.create((thisPtr, id, data, size) -> {});
      screenShotCallback =
          BGFXScreenShotCallback.create(
              (thisPtr, filePath, width, height, pitch, data, size, yflip) -> {});
      captureBeginCallback =
          BGFXCaptureBeginCallback.create((thisPtr, width, height, pitch, format, yflip) -> {});
      captureEndCallback = BGFXCaptureEndCallback.create((thisPtr) -> {});
      captureFrameCallback = BGFXCaptureFrameCallback.create((thisPtr, data, size) -> {});

      // Set up the virtual table
      vtbl.set(
          fatalCallback, // fatal
          traceCallback, // trace_vargs
          profilerBegin, // profiler_begin
          null, // profiler_begin_literal (can be null)
          profilerEnd, // profiler_end
          cacheReadSizeCallback, // cache_read_size (can be null)
          cacheReadCallback, // cache_read (can be null)
          cacheWriteCallback, // cache_write (can be null)
          screenShotCallback, // screen_shot (can be null)
          captureBeginCallback, // capture_begin (can be null)
          captureEndCallback, // capture_end (can be null)
          captureFrameCallback // capture_frame (can be null)
          );

      // Set the vtbl in the callback interface
      callbackInterface.vtbl(vtbl);

      return callbackInterface;
    }
  }

  public void free() {
    if (fatalCallback != null) fatalCallback.free();
    if (traceCallback != null) traceCallback.free();
    if (profilerBegin != null) profilerBegin.free();
    if (profilerEnd != null) profilerEnd.free();
    if (cacheReadSizeCallback != null) cacheReadSizeCallback.free();
    if (cacheReadCallback != null) cacheReadCallback.free();
    if (cacheWriteCallback != null) cacheWriteCallback.free();
    if (screenShotCallback != null) screenShotCallback.free();
    if (captureBeginCallback != null) captureBeginCallback.free();
    if (captureEndCallback != null) captureEndCallback.free();
    if (captureFrameCallback != null) captureFrameCallback.free();
    if (vtbl != null) vtbl.free();
    if (callbackInterface != null) callbackInterface.free();
  }
}
