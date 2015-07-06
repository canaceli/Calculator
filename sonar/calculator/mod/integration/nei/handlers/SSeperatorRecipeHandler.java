package sonar.calculator.mod.integration.nei.handlers;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import sonar.calculator.mod.client.gui.machines.GuiDualOutputSmelting;
import sonar.calculator.mod.common.recipes.machines.StoneSeperatorRecipes;
import sonar.core.utils.helpers.FontHelper;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class SSeperatorRecipeHandler extends TemplateRecipeHandler {
	public class SmeltingPair extends TemplateRecipeHandler.CachedRecipe {
		PositionedStack input;
		PositionedStack result;
		PositionedStack result2;

		public SmeltingPair(Object input, Object result, Object result2) {
			super();
			// input.stackSize = 1;
			this.input = new PositionedStack(input, 34, 13);
			this.result = new PositionedStack(result, 88, 13);
			this.result2 = new PositionedStack(result2, 117, 13);
		}

		@Override
		public List<PositionedStack> getIngredients() {
			return getCycledIngredients(SSeperatorRecipeHandler.this.cycleticks / 48, Arrays.asList(new PositionedStack[] { this.input }));
		}

		@Override
		public PositionedStack getResult() {
			return this.result;
		}

		@Override
		public PositionedStack getOtherStack() {
			return this.result2;
		}
	}

	@Override
	public void loadTransferRects() {
		this.transferRects.add(new TemplateRecipeHandler.RecipeTransferRect(new Rectangle(52, 20, 24, 10), "stone", new Object[0]));
		this.transferRects.add(new TemplateRecipeHandler.RecipeTransferRect(new Rectangle(49 - 5, 63 - 11, 78, 10), "calculatordischarge", new Object[0]));

	}

	@Override
	public Class<? extends GuiContainer> getGuiClass() {
		return GuiDualOutputSmelting.StoneSeperator.class;
	}

	@Override
	public String getRecipeName() {
		return FontHelper.translate("tile.StoneSeperatorIdle.name");
	}

	@Override
	public void loadCraftingRecipes(String outputId, Object... results) {
		if ((outputId.equals("stone")) && (getClass() == SSeperatorRecipeHandler.class)) {
			Map<Object[], Object[]> recipes = StoneSeperatorRecipes.instance().getRecipes();
			for (Map.Entry<Object[], Object[]> recipe : recipes.entrySet()) {
				this.arecipes.add(new SmeltingPair(recipe.getKey()[0], recipe.getValue()[0], recipe.getValue()[1]));
			}
		} else {
			super.loadCraftingRecipes(outputId, results);
		}
	}

	@Override
	public void loadCraftingRecipes(ItemStack result) {
		Map<Object[], Object[]> recipes = StoneSeperatorRecipes.instance().getRecipes();
		for (Map.Entry<Object[], Object[]> recipe : recipes.entrySet()) {
			int pos = StoneSeperatorRecipes.instance().containsStack(result, recipe.getValue(), false);
			if (pos != -1) {
				this.arecipes.add(new SmeltingPair(recipe.getKey()[0], recipe.getValue()[0], recipe.getValue()[1]));
			}
		}
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients) {
		if ((inputId.equals("stone")) && (getClass() == SSeperatorRecipeHandler.class)) {
			loadCraftingRecipes("stone", new Object[0]);
		} else {
			super.loadUsageRecipes(inputId, ingredients);
		}
	}

	@Override
	public void loadUsageRecipes(ItemStack ingredient) {
		Map<Object[], Object[]> recipes = StoneSeperatorRecipes.instance().getRecipes();
		for (Map.Entry<Object[], Object[]> recipe : recipes.entrySet()) {
			int pos = StoneSeperatorRecipes.instance().containsStack(ingredient, recipe.getKey(), false);
			if (pos != -1) {
				this.arecipes.add(new SmeltingPair(recipe.getKey()[0], recipe.getValue()[0], recipe.getValue()[1]));
			}
		}
	}

	@Override
	public String getGuiTexture() {
		return "Calculator:textures/gui/nei/seperator.png";
	}

	@Override
	public void drawExtras(int recipe) {
		drawProgressBar(57, 13, 176, 10, 24, 16, 48, 0);
	}

	@Override
	public String getOverlayIdentifier() {
		return "stone";
	}
}