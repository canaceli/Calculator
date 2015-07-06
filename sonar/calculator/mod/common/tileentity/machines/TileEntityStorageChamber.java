package sonar.calculator.mod.common.tileentity.machines;

import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.util.StatCollector;
import cofh.api.energy.EnergyStorage;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import sonar.calculator.mod.Calculator;
import sonar.calculator.mod.CalculatorConfig;
import sonar.calculator.mod.api.IStability;
import sonar.calculator.mod.api.ISyncTile;
import sonar.calculator.mod.api.SyncData;
import sonar.calculator.mod.network.packets.PacketSonarSides;
import sonar.core.common.tileentity.TileEntityInventory;
import sonar.core.common.tileentity.TileEntitySidedInventory;
import sonar.core.utils.IDropTile;

/** needs clean up */
public class TileEntityStorageChamber extends TileEntitySidedInventory implements IDropTile, ISidedInventory {

	public static int maxSize = 1000;
	public int[] stored;
	public ItemStack savedStack;

	public TileEntityStorageChamber() {

		this.stored = new int[14];
		super.slots = new ItemStack[14];
		super.input = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
		super.output = new int[] { 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27 };
	}

	@Override
	public int getSizeInventory() {
		return stored.length * 2;
	}

	@Override
	public int getInventoryStackLimit() {
		return 1;
	}

	public boolean isInputSlot(int slot) {
		return slot < stored.length;
	}

	public boolean isOutputSlot(int slot) {
		return slot >= stored.length;
	}

	/**
	 * has the functionality for any stack size;
	 * 
	 * @param slot
	 * @return
	 */
	public ItemStack getInputStack(int slot) {
		if (this.getSavedStack() == null || this.stored[slot] == 0) {
			return null;
		}
		int stackSize = 0;
		int current = (int) Math.floor((this.stored[slot] / savedStack.getMaxStackSize()));
		if (current == this.maxSize) {
			stackSize = savedStack.getMaxStackSize();
		} else {
			stackSize = this.stored[slot] - (current * savedStack.getMaxStackSize());
		}
		if (stackSize == 0) {
			return null;
		}
		ItemStack outputStack = savedStack.copy();
		outputStack.setItemDamage(slot);
		outputStack.stackSize = stackSize;
		if (stackSize == savedStack.getMaxStackSize()) {
			return null;
		}
		return outputStack;

	}

	@Override
	public ItemStack getStackInSlot(int var1) {
		int slot = var1 - stored.length;
		if (isInputSlot(var1)) {
			return this.getInputStack(var1);
		} else if (this.isOutputSlot(var1)) {
			return this.getFullStack(slot);
		}
		return null;
	}

	@Override
	public ItemStack decrStackSize(int slot, int var2) {
		if (this.isOutputSlot(slot)) {
			ItemStack output = this.getSlotStack(slot - stored.length, var2);
			this.stored[slot - stored.length] -= var2;
			if (this.stored[slot - stored.length] == 0) {
				this.slots[slot - stored.length] = null;
				this.resetSavedStack(slot - stored.length);
			}
			return output;
		} else if (this.slots[slot] != null) {

			if (this.slots[slot].stackSize <= var2) {
				ItemStack itemstack = this.slots[slot];
				this.slots[slot] = null;
				return itemstack;
			}
			ItemStack itemstack = this.slots[slot].splitStack(var2);

			if (this.slots[slot].stackSize == 0) {
				this.slots[slot] = null;
			}

			return itemstack;
		}

		return null;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		if (itemstack == null) {
			return;
		}
		if (this.isInputSlot(i)) {
			this.stored[itemstack.getItemDamage()] += 1;
			if (getSavedStack() == null) {
				setSavedStack(itemstack);
			}
		} else if (this.isOutputSlot(i)) {
			if (stored[i - stored.length] == 1) {
				resetSavedStack(i - stored.length);
			}
			this.stored[i - stored.length] -= 1;
		}

	}

	public void setDisplayContents(int i, ItemStack itemstack) {
		this.slots[i] = itemstack;
		if ((itemstack != null) && (itemstack.stackSize > getInventoryStackLimit())) {
			itemstack.stackSize = getInventoryStackLimit();
		}
	}

	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (stack != null) {
			if (stored[stack.getItemDamage()] == this.maxSize) {
				return false;
			}
			if (getSavedStack() != null) {
				if (this.getCircuitType(this.getSavedStack()) == this.getCircuitType(stack)) {
					return true;
				}
			} else {
				if (this.getCircuitType(stack) != null) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, int side) {

		return this.isInputSlot(slot) && isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, int side) {
		return this.isOutputSlot(slot) && !this.isInputSlot(slot);
	}

	public boolean isItemValid(int slot, ItemStack stack) {
		if (stack != null && this.isInputSlot(slot)) {
			if (stack.getItemDamage() != slot) {
				return false;
			}
			if (getSavedStack() != null) {
				if (this.getCircuitType(this.getSavedStack()) == this.getCircuitType(stack)) {
					return true;
				}
			} else {
				if (this.getCircuitType(stack) != null) {
					return true;
				}
			}
		}
		return false;
	}

	public ItemStack getFullStack(int slot) {
		if (stored[slot] != 0 && getSavedStack() != null) {
			ItemStack fullStack = new ItemStack(getSavedStack().getItem(), stored[slot], slot);
			fullStack.setTagCompound(getSavedStack().getTagCompound());
			return fullStack;
		}
		return null;
	}

	public ItemStack getSlotStack(int slot, int size) {
		if (stored[slot] != 0 && getSavedStack() != null) {
			ItemStack slotStack = new ItemStack(getSavedStack().getItem(), size, slot);
			slotStack.setTagCompound(getSavedStack().getTagCompound());
			return slotStack;
		}
		return null;
	}

	public void resetSavedStack(int removed) {
		boolean found = false;
		for (int i = 0; i < slots.length - 1; i++) {
			if (i != removed) {
				if (stored[i] != 0) {
					found = true;
					return;
				}
			}
		}
		if (!found) {
			this.setSavedStack(null);
		}
	}

	public ItemStack getSavedStack() {
		return savedStack;
	}

	public void setSavedStack(ItemStack stack) {
		savedStack = stack;
		this.worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

	public static CircuitType getCircuitType(ItemStack stack) {
		if (stack != null) {
			if (stack.getItem() == Calculator.circuitBoard) {
				if (stack.getItem() instanceof IStability) {
					IStability stability = (IStability) stack.getItem();
					if (stability.getStability(stack)) {
						NBTTagCompound tag = new NBTTagCompound();
						tag.setInteger("Stable", 1);
						tag.setInteger("Item1", 0);
						tag.setInteger("Item2", 0);
						tag.setInteger("Item3", 0);
						tag.setInteger("Item4", 0);
						tag.setInteger("Item5", 0);
						tag.setInteger("Item6", 0);
						tag.setInteger("Energy", 0);
						ItemStack stable = new ItemStack(stack.getItem(), 1, stack.getItemDamage());
						stable.setTagCompound(tag);
						if (ItemStack.areItemStackTagsEqual(stable, stack)) {
							return CircuitType.Stable;
						}
					} else if (!stack.hasTagCompound()) {
						return CircuitType.Analysed;
					} else if (stack.hasTagCompound()) {
						NBTTagCompound tag = new NBTTagCompound();
						tag.setInteger("Stable", 0);
						tag.setInteger("Item1", 0);
						tag.setInteger("Item2", 0);
						tag.setInteger("Item3", 0);
						tag.setInteger("Item4", 0);
						tag.setInteger("Item5", 0);
						tag.setInteger("Item6", 0);
						tag.setInteger("Energy", 0);
						ItemStack analysed = new ItemStack(stack.getItem(), 1, stack.getItemDamage());
						analysed.setTagCompound(tag);
						if (ItemStack.areItemStackTagsEqual(analysed, stack)) {
							return CircuitType.Analysed;
						}
					}
				}
			} else if (stack.getItem() == Calculator.circuitDamaged) {
				return CircuitType.Damaged;
			} else if (stack.getItem() == Calculator.circuitDirty) {
				return CircuitType.Dirty;
			}
		}

		return null;
	}

	public static CircuitType getCircuitType(int type) {
		if (type == 2) {
			return CircuitType.Stable;
		}
		if (type == 3) {
			return CircuitType.Damaged;
		}
		if (type == 5) {
			return CircuitType.Dirty;
		}
		return CircuitType.Analysed;
	}

	public static int getCircuitValue(CircuitType type) {
		if (type == CircuitType.Stable) {
			return 2;
		}
		if (type == CircuitType.Damaged) {
			return 3;
		}
		if (type == CircuitType.Dirty) {
			return 4;
		}
		return 1;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		stored = nbt.getIntArray("Stored");
		this.savedStack = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("saved"));
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setIntArray("Stored", stored);
		NBTTagCompound stack = new NBTTagCompound();
		if (savedStack != null) {
			savedStack.writeToNBT(stack);
		}
		nbt.setTag("saved", stack);

	}

	@Override
	public void readInfo(NBTTagCompound tag) {
		this.stored = tag.getIntArray("stored");
		this.savedStack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("saved"));
	}

	@Override
	public void writeInfo(NBTTagCompound tag) {
		tag.setIntArray("stored", stored);
		if (getSavedStack() != null) {
			NBTTagCompound stack = new NBTTagCompound();
			this.getSavedStack().writeToNBT(stack);
			tag.setTag("saved", stack);
			tag.setInteger("type", TileEntityStorageChamber.getCircuitValue(TileEntityStorageChamber.getCircuitType(this.getSavedStack())));
		}

	}

	private enum CircuitType {
		Analysed, Stable, Damaged, Dirty;
	}

	@Override
	public void sendPacket(int dimension, int side, int value) {
		Calculator.network.sendToAllAround(new PacketSonarSides(xCoord, yCoord, zCoord, side, value), new TargetPoint(dimension, xCoord, yCoord, zCoord, 32));

	}
}