package dev.rydonut.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.IntConsumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;

import net.minecraft.client.Minecraft;

/**
 * GLFW-level keyboard hook used for module keybinds and GUI key capture.
 *
 * <p>Client-side only: it installs a chained GLFWKeyCallback on the game
 * window, records "just pressed" keys and forwards every event to the
 * vanilla callback so input handling stays intact. It does not send or
 * modify any packets.
 */
public final class KeyInput {
	private static final Set<Integer> justPressed = new HashSet<>();
	private static final Set<IntConsumer> listeners = new CopyOnWriteArraySet<>();
	private static GLFWKeyCallback previousCallback;

	private KeyInput() {
	}

	public static void init() {
		Minecraft mc = Minecraft.getInstance();
		long window = mc.getWindow().handle();
		previousCallback = GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
			if (key >= 0) {
				if (action == GLFW.GLFW_PRESS) {
					justPressed.add(key);
					for (IntConsumer listener : listeners) {
						listener.accept(key);
					}
				}
			}
			if (previousCallback != null) {
				previousCallback.invoke(win, key, scancode, action, mods);
			}
		});
	}

	/** Returns true once per physical press of the given GLFW key code. */
	public static boolean consumePress(int glfwKey) {
		return justPressed.remove(glfwKey);
	}

	/** Registers a global key press listener (used by keybind capture in the GUI). */
	public static void addListener(IntConsumer listener) {
		listeners.add(listener);
	}

	public static void removeListener(IntConsumer listener) {
		listeners.remove(listener);
	}

	/** Called at the end of every client tick to drop unclaimed presses. */
	public static void endTick() {
		justPressed.clear();
	}
}
