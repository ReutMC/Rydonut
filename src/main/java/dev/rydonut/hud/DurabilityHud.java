package dev.rydonut.hud;

import dev.rydonut.render.Colors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Armor & held item durability HUD.
 */
public class DurabilityHud extends HudModule {
	public DurabilityHud() {
		super("Durability", "Armor and held item durability.");
		setPosition(6, 112);
	}

	@Override
	public void onHudRender(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.client.DeltaTracker tickCounter) {
		if (mc.player == null) {
			return;
		}
		var font = mc.font;
		int y = getY();

		graphics.fill(getX(), y, getX() + 130, y + (font.lineHeight + 1) * 5 + 6, Colors.PANEL);
		y += 3;

		ItemStack[] slots = new ItemStack[]{
				mc.player.getItemBySlot(EquipmentSlot.HEAD),
				mc.player.getItemBySlot(EquipmentSlot.CHEST),
				mc.player.getItemBySlot(EquipmentSlot.LEGS),
				mc.player.getItemBySlot(EquipmentSlot.FEET)
		};
		String[] labels = {"Head", "Chest", "Legs", "Feet"};
		for (int i = 0; i < slots.length; i++) {
			String text = labels[i] + ": " + describe(slots[i]);
			int color = slots[i].isDamageableItem() ? durabilityColor(slots[i]) : Colors.TEXT_DIM;
			graphics.drawString(font, Component.literal(text), getX() + 4, y, color);
			y += font.lineHeight + 1;
		}
		ItemStack held = mc.player.getMainHandItem();
		String heldText = "Held: " + describe(held);
		graphics.drawString(font, Component.literal(heldText), getX() + 4, y,
				held.isDamageableItem() ? durabilityColor(held) : Colors.TEXT_DIM);
		setSize(130, (font.lineHeight + 1) * 5 + 6);
	}

	private String describe(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return "-";
		}
		String name = stack.getHoverName().getString();
		if (name.length() > 12) {
			name = name.substring(0, 12);
		}
		if (stack.isDamageableItem()) {
			return name + " (" + (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() + ")";
		}
		return name;
	}

	private int durabilityColor(ItemStack stack) {
		float fraction = 1.0f - stack.getDamageValue() / (float) stack.getMaxDamage();
		return Colors.healthColor(fraction);
	}
}
