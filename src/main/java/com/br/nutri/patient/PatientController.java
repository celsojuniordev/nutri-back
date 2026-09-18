package com.br.nutri.patient;

import com.br.nutri.common.PageResponse;
import com.br.nutri.patient.dto.PatientResponse;
import com.br.nutri.patient.dto.PatientSearchCriteria;
import com.br.nutri.security.CurrentNutritionist;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pacientes")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<PatientResponse>> list(@PageableDefault(size = 25) Pageable pageable) {
        Page<Patient> result = patientService.listActive(CurrentNutritionist.id(), pageable);
        return ResponseEntity.ok(PageResponse.from(result, PatientResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> get(@PathVariable Long id) {
        Patient patient = patientService.getOwnedById(CurrentNutritionist.id(), id);
        return ResponseEntity.ok(PatientResponse.from(patient));
    }

    @GetMapping("/busca")
    public ResponseEntity<PageResponse<PatientResponse>> search(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Sex sexo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimentoInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataNascimentoFim,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String telefone,
            @PageableDefault(size = 25) Pageable pageable) {
        PatientSearchCriteria criteria =
                new PatientSearchCriteria(nome, sexo, dataNascimentoInicio, dataNascimentoFim, email, telefone);
        Page<Patient> result = patientService.search(CurrentNutritionist.id(), criteria, pageable);
        return ResponseEntity.ok(PageResponse.from(result, PatientResponse::from));
    }
}
