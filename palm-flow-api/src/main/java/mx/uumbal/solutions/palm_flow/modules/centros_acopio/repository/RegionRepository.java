package mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository;

import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    Optional<Region> findByNombreIgnoreCase(String nombre);
}
