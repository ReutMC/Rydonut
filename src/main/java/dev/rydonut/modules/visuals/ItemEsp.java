package dev.rydonut.modules.visuals;

import dev.rydonut.core.Category;
import dev.rydonut.core.Module;
import dev.rydonut.render.Colors;
import dev.rydonut.render.WorldShapes;
import dev.rydonut.settings.BoolSetting;
import dev.rydonut.settings.ColorSetting;
import dev.rydonut.settings.DoubleSetting;
import dev.rydonut.settings.IntSetting;
import dev.rydonut.settings.StringListSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Item ESP: dropped items with name + count labels, minimum stack filter and
 * a "valuable items only" name filter list.
 */
public class ItemEsp extends Module {
	private final DoubleSetting range = new DoubleSetting("Range", 48, 8, 128, 8);
	private final IntSetting minCount = new IntSetting("Min stack size", 1, 1, 64, 1);
	private final BoolSetting valuableOnly = new BoolSetting("Valuable only", false);
	private final StringListSetting valuableList = new StringListSetting("Valuable items",
			java.util.List.of("minecraft:diamond", "minecraft:emerald", "minecraft:netherite_ingot",
					"minecraft:netherite_scrap", "minecraft:ancient_debris", "minecraft:elytra",
					"minecraft:totem_of_undying", "minecraft:enchanted_golden_apple", "minecraft:nether_star"));
	private final BoolSetting showLabel = new BoolSetting("Labels", true);
	private final BoolSetting showBox = new BoolSetting("Box", true);
	private final BoolSetting throughWalls = new BoolSetting("Through walls", true);
	private final DoubleSetting fillOpacity = new DoubleSetting("Fill opacity", 0.2, 0.05, 1.0, 0.05);
	private final ColorSetting color = new ColorSetting("Color", 0xFF7BE38B);

	public ItemEsp() {
		super("Item ESP", Category.VISUALS, "Highlights dropped items.");
		addSettings(range, minCount, valuableOnly, valuableList, showLabel, showBox, throughWalls, fillOpacity, color);
		valuableList.onChange(v -> { /* kept in config; filter reads list live */ });
	}

	private boolean isValuable(ItemStack stack) {
		String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		for (String entry : valuableList.get()) {
			if (id.equals(entry) || id.endsWith(":" + entry)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onWorldRender(WorldRenderContext context) {
		if (mc.player == null || mc.level == null) {
			return;
		}
		WorldShapes.begin(context);
		try {
			double rangeSq = range.get() * range.get();
			for (Entity entity : mc.level.entitiesForRendering()) {
				if (!(entity instanceof ItemEntity item)) {
					continue;
				}
				if (entity.distanceToSqr(mc.player) > rangeSq) {
					continue;
				}
				ItemStack stack = item.getItem();
				if (stack.getCount() < minCount.get()) {
					continue;
				}
				if (valuableOnly.isOn() && !isValuable(stack)) {
					continue;
				}
				AABB box = entity.getBoundingBox();
				int c = color.get();
				if (showBox.isOn()) {
					WorldShapes.filledBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
							Colors.withAlpha(c, fillOpacity.get().floatValue()), throughWalls.isOn());
					WorldShapes.outlinedBox(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, c, throughWalls.isOn());
				}
				if (showLabel.isOn()) {
					String label = stack.getCount() > 1
							? stack.getHoverName().getString() + " x" + stack.getCount()
							: stack.getHoverName().getString();
					WorldShapes.text(label, entity.getX(), box.maxY + 0.25, entity.getZ(), 0.8f, Colors.WHITE, 0x90000000, throughWalls.isOn());
				}
			}
		} finally {
			WorldShapes.end();
		}
	}
}
