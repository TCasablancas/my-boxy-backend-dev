package com.myboxydev.dev.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "lojas")
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

    public StoreModel() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}
