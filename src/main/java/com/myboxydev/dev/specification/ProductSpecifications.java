package com.myboxydev.dev.specification;

import com.myboxydev.dev.model.ProductModel;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Monta um Specification<ProductModel> combinando apenas os filtros que vierem preenchidos.
 * Usado pelo GET /api/products, que serve tanto pra "home" (sem filtro nenhum) quanto
 * pra busca/navegação por categoria/subcategoria/preço.
 */
public class ProductSpecifications {

    public static Specification<ProductModel> build(
        String name,
        UUID storeId,
        UUID categoryId,
        UUID subcategoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (name != null && !name.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (storeId != null) {
                predicates.add(cb.equal(root.get("store").get("id"), storeId));
            }
            if (subcategoryId != null) {
                predicates.add(cb.equal(root.get("subcategory").get("id"), subcategoryId));
            }
            if (categoryId != null) {
                var subcategoryJoin = root.join("subcategory", JoinType.INNER);
                predicates.add(cb.equal(subcategoryJoin.get("category").get("id"), categoryId));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
