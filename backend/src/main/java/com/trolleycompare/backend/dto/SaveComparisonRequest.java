package com.trolleycompare.backend.dto;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;

import java.util.List;

public record SaveComparisonRequest(List<ItemInput> items) {}
