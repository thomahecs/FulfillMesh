package com.fulfillmesh.fulfillment.config;

import com.fulfillmesh.fulfillment.routing.AStarRouter;
import com.fulfillmesh.fulfillment.routing.PickingRoutePlanner;
import com.fulfillmesh.fulfillment.routing.WarehouseGrid;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoutingConfig {

    @Bean
    PickingRoutePlanner pickingRoutePlanner() {
        WarehouseGrid grid = WarehouseGrid.defaultLayout();
        return new PickingRoutePlanner(new AStarRouter(grid));
    }
}
