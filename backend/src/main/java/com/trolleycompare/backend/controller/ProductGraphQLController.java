package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.BasketComparisonResponse.*;
import com.trolleycompare.backend.dto.ProductComparisonResponse;
import com.trolleycompare.backend.service.ProductComparisonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ProductGraphQLController {

    @Autowired
    private ProductComparisonService productComparisonService;

    @QueryMapping
    public List<ProductComparisonResponse> compareProduct(
            @Argument String name, 
            @Argument double quantity, 
            @Argument String unit) {
        return productComparisonService.findAndScaleProducts(name, quantity, unit);
    }

    @QueryMapping
    public List<StoreBasketComparison> compareBasket(@Argument List<ItemInput> items) {
        return productComparisonService.compareBasket(items);
    }
}