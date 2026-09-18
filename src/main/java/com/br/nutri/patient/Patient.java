package com.br.nutri.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sex sex;

    private String email;

    private String phone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "nutritionist_id", nullable = false)
    private Long nutritionistId;

    protected Patient() {
    }

    public Patient(String fullName, LocalDate birthDate, Sex sex, String email, String phone, Long nutritionistId) {
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.sex = sex;
        this.email = email;
        this.phone = phone;
        this.nutritionistId = nutritionistId;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Sex getSex() {
        return sex;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getNutritionistId() {
        return nutritionistId;
    }
}
