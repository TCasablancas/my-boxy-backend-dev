package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "main_image", nullable = false)
    private String mainImage;

    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite;

    // Produto customizável (personalizado sob encomenda).
    @Column(name = "is_custom", nullable = false)
    private Boolean isCustom = false;

    @Column(name = "in_stock", nullable = false)
    private Boolean inStock;

    @Column(name = "num_sells", nullable = false)
    private Integer numSells;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false)
    private Integer reviews;

    private Integer weight;
    private String color;
    private String format;
    private String duration;
    private String dimensions;
    private String composition;
    private String material;
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreModel store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private SubcategoryModel subcategory;

    // Construtores, Getters e Setters
    public ProductModel() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean favorite) { isFavorite = favorite; }
    public Boolean getIsCustom() { return isCustom; }
    public void setIsCustom(Boolean isCustom) { this.isCustom = isCustom; }
    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
    public Integer getNumSells() { return numSells; }
    public void setNumSells(Integer numSells) { this.numSells = numSells; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public Integer getReviews() { return reviews; }
    public void setReviews(Integer reviews) { this.reviews = reviews; }
    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getDimensions() { return dimensions; }
    public void setDimensions(String dimensions) { this.dimensions = dimensions; }
    public String getComposition() { return composition; }
    public void setComposition(String composition) { this.composition = composition; }
    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
    public StoreModel getStore() { return store; }
    public void setStore(StoreModel store) { this.store = store; }
    public SubcategoryModel getSubcategory() { return subcategory; }
    public void setSubcategory(SubcategoryModel subcategory) { this.subcategory = subcategory; }
}
