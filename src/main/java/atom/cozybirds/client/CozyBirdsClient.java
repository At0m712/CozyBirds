package atom.cozybirds.client;

import atom.cozybirds.client.model.*;
import atom.cozybirds.client.renderer.*;
import atom.cozybirds.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;

public class CozyBirdsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register Model Layers (V1: Pigeon & Magpie)
        ModelLayerRegistry.registerModelLayer(PigeonModel.LAYER_LOCATION, PigeonModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(MagpieModel.LAYER_LOCATION, MagpieModel::createBodyLayer);
        /* V2 birds: commented for V1
        ModelLayerRegistry.registerModelLayer(FalconModel.LAYER_LOCATION, FalconModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(FlamingoModel.LAYER_LOCATION, FlamingoModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(GoldenEagleModel.LAYER_LOCATION, GoldenEagleModel::createBodyLayer);
        */

        // Register Entity Renderers (V1: Pigeon & Magpie)
        EntityRendererRegistry.register(ModEntities.PIGEON, PigeonRenderer::new);
        EntityRendererRegistry.register(ModEntities.MAGPIE, MagpieRenderer::new);
        /* V2 birds: commented for V1
        EntityRendererRegistry.register(ModEntities.FALCON, FalconRenderer::new);
        EntityRendererRegistry.register(ModEntities.FLAMINGO, FlamingoRenderer::new);
        EntityRendererRegistry.register(ModEntities.GOLDEN_EAGLE, GoldenEagleRenderer::new);
        */
    }
}
