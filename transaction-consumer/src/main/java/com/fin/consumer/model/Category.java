package com.fin.consumer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String icon;
    
    @Column(nullable = false)
    private String color;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transaction.TransactionType type;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}








