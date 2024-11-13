package com.ssafy.shieldron.repository;

import com.ssafy.shieldron.domain.Drone;
import com.ssafy.shieldron.domain.Hive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface HiveRepository extends JpaRepository<Hive, Integer> {

    // TODO 인덱스 고려
    @Query(value = """
    SELECT h FROM Hive h 
    WHERE h.hiveLat BETWEEN :minLat AND :maxLat 
    AND h.hiveLng BETWEEN :minLng AND :maxLng
    """)
    List<Hive> findHivesInBoundingBox(@Param("minLat") BigDecimal minLat,
                                      @Param("maxLat") BigDecimal maxLat,
                                      @Param("minLng") BigDecimal minLng,
                                      @Param("maxLng") BigDecimal maxLng);


    // TODO LIKE 연산자는 성능 저하를 유발할 수 있다. Full-Text Search 고려
    @Query("""
    SELECT h FROM Hive h 
    WHERE LOWER(h.hiveName) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY 
        CASE 
            WHEN LOWER(h.hiveName) = LOWER(:keyword) THEN 1
            WHEN LOWER(h.hiveName) LIKE LOWER(CONCAT(:keyword, '%')) THEN 2
            ELSE 3
        END,
        h.hiveName ASC
    """)
    List<Hive> searchByKeyword(@Param("keyword") String keyword);
}
