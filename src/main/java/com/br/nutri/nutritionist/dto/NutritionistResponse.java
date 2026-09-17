package com.br.nutri.nutritionist.dto;

import com.br.nutri.nutritionist.Nutritionist;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record NutritionistResponse(Long id, String name, String company, String email) {

    public static NutritionistResponse from(Nutritionist nutritionist) {
        return new NutritionistResponse(
                nutritionist.getId(), nutritionist.getName(), nutritionist.getCompany(), nutritionist.getEmail());
    }
}
