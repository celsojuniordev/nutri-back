package com.br.nutri.nutritionist;

import com.br.nutri.nutritionist.dto.NutritionistResponse;
import com.br.nutri.nutritionist.dto.RegisterRequest;
import com.br.nutri.security.CurrentNutritionist;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nutricionistas")
public class NutritionistController {

    private final NutritionistService nutritionistService;

    public NutritionistController(NutritionistService nutritionistService) {
        this.nutritionistService = nutritionistService;
    }

    @PostMapping
    public ResponseEntity<NutritionistResponse> register(@Valid @RequestBody RegisterRequest request) {
        Nutritionist nutritionist = nutritionistService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(NutritionistResponse.from(nutritionist));
    }

    @GetMapping("/me")
    public ResponseEntity<NutritionistResponse> me() {
        Nutritionist nutritionist = nutritionistService.getById(CurrentNutritionist.id());
        return ResponseEntity.ok(NutritionistResponse.from(nutritionist));
    }
}
