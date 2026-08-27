package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "store")
public class StoreModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private String image;

    @Column(nullable = false)
    private BigDecimal rating;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "store_categories",
            joinColumns = @JoinColumn(name = "store_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<CategoryModel> categories = new HashSet<>();

    public StoreModel() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public Set<CategoryModel> getCategories() { return categories; }
    public void setCategories(Set<CategoryModel> categories) { this.categories = categories; }
}