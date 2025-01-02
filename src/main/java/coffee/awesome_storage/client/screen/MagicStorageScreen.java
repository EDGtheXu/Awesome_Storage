package coffee.awesome_storage.client.screen;

import coffee.awesome_storage.client.screen.widget.MagicCraftWidget;
import coffee.awesome_storage.client.screen.widget.MagicStorageWidget;
import coffee.awesome_storage.menu.MagicStorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import static coffee.awesome_storage.Awesome_storage.space;


public class MagicStorageScreen extends AbstractContainerScreen<MagicStorageMenu> {
    private static final ResourceLocation BACKGROUND = space("textures/gui/magic_storage_menu.png");

    private Button switchButton;
    int state;
    private MagicCraftWidget craftWidget;
    private AbstractContainerWidget creatorWidget;

    public MagicStorageScreen(MagicStorageMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);

    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = imageWidth - font.width(title) - 8;
        this.inventoryLabelX = imageWidth - font.width(playerInventoryTitle) - 8;
        this.creatorWidget = new MagicStorageWidget(this, 0, 20, leftPos, minecraft.getWindow().getHeight() / 2 - 20, Component.literal("creator"));
        this.craftWidget = new MagicCraftWidget(this, 0, 20, leftPos, minecraft.getWindow().getHeight() / 2 - 20, Component.literal("craft"));

        switchButton = Button.builder(Component.translatable("magic_storage_screen.storage"), (press) -> {
            if (state == 1) {
                state = 2;
                this.removeWidget(craftWidget);
                this.addRenderableWidget(creatorWidget);
                switchButton.setMessage(Component.translatable("magic_storage_screen.storage"));

            } else {
                state = 1;
                this.removeWidget(creatorWidget);
                this.addRenderableWidget(craftWidget);
                switchButton.setMessage(Component.translatable("magic_storage_screen.craft"));
            }
        }).pos(leftPos+5, topPos+ 10).size(25, 16).build();

//        this.addRenderableWidget(craftWidget);
        this.addRenderableWidget(creatorWidget);
        this.addRenderableWidget(switchButton);
        this.addRenderableWidget(craftWidget.displayWidget);
    }

    @Override
    public void render(@NotNull GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);


    }

    @Override
    protected void renderBg(@NotNull GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        pGuiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);

    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {

        for(var widget : this.children()){
            if(widget instanceof AbstractContainerWidget widget1){
                if(widget1.isHovered() && widget1.mouseClicked(pMouseX, pMouseY, pButton))
                    return true;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

}
